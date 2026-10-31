package com.talentmatch.workflow.service;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.user.entity.User;
import com.talentmatch.workflow.dto.AddNoteRequest;
import com.talentmatch.workflow.dto.JobApplicationDto;
import com.talentmatch.workflow.dto.JobApplicationNoteDto;
import com.talentmatch.workflow.dto.UpdateStageRequest;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import com.talentmatch.workflow.entity.JobApplicationNote;
import com.talentmatch.workflow.repository.JobApplicationNoteRepository;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RecruitmentWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentWorkflowService.class);

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobApplicationNoteRepository noteRepository;
    private final AuditLogService auditLogService;

    public RecruitmentWorkflowService(JobRepository jobRepository,
                                      JobApplicationRepository applicationRepository,
                                      JobApplicationNoteRepository noteRepository,
                                      AuditLogService auditLogService) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.noteRepository = noteRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobApplicationDto> getJobApplications(Long jobId,
                                                               ApplicationStage stage,
                                                               String search,
                                                               Pageable pageable,
                                                               User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<JobApplication> page = applicationRepository.searchApplications(
                job.getId(),
                currentUser.getOrganization().getId(),
                stage,
                cleanSearch,
                pageable
        );

        List<JobApplicationDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return new PagedResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional
    public JobApplicationDto updateApplicationStage(Long jobId,
                                                    Long applicationId,
                                                    UpdateStageRequest request,
                                                    User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        JobApplication app = applicationRepository.findByIdAndOrganizationId(applicationId, currentUser.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job application " + applicationId + " not found"));

        if (!app.getJob().getId().equals(job.getId())) {
            throw new BadRequestException("Application " + applicationId + " does not belong to job " + jobId);
        }

        ApplicationStage currentStage = app.getStage();
        ApplicationStage targetStage = request.stage();

        // Enforce state transition rules
        if (!currentStage.canTransitionTo(targetStage)) {
            throw new BadRequestException(String.format(
                    "Invalid workflow state transition: cannot move candidate from %s to %s",
                    currentStage, targetStage
            ));
        }

        app.setStage(targetStage);
        if (request.decisionReason() != null && !request.decisionReason().isBlank()) {
            app.setDecisionReason(request.decisionReason().trim());
        }

        // Automatically manage shortlisted flag if stage changes
        if (targetStage == ApplicationStage.SHORTLISTED) {
            app.setShortlisted(true);
        } else if (targetStage == ApplicationStage.REJECTED) {
            app.setShortlisted(false);
        }

        // If optional note provided, record it
        if (request.note() != null && !request.note().isBlank()) {
            JobApplicationNote note = new JobApplicationNote(
                    app,
                    currentUser.getOrganization(),
                    currentUser,
                    request.note().trim(),
                    targetStage.name(),
                    request.decisionReason()
            );
            app.addNote(note);
        }

        JobApplication saved = applicationRepository.save(app);

        // Audit state transition
        auditLogService.recordEvent(
                currentUser.getOrganization().getId(),
                currentUser.getId(),
                currentUser.getEmail(),
                "APPLICATION_STAGE_CHANGED",
                "JobApplication",
                String.valueOf(saved.getId()),
                String.format("Candidate %s (ID %d) transitioned from %s to %s for job '%s'",
                        saved.getCandidate().getFullName(), saved.getCandidate().getId(),
                        currentStage, targetStage, job.getTitle()),
                "127.0.0.1"
        );

        return mapToDto(saved);
    }

    @Transactional
    public JobApplicationDto toggleShortlist(Long jobId, Long applicationId, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        JobApplication app = applicationRepository.findByIdAndOrganizationId(applicationId, currentUser.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job application " + applicationId + " not found"));

        if (!app.getJob().getId().equals(job.getId())) {
            throw new BadRequestException("Application " + applicationId + " does not belong to job " + jobId);
        }

        boolean newShortlisted = !app.isShortlisted();
        app.setShortlisted(newShortlisted);

        if (newShortlisted && app.getStage() == ApplicationStage.NEW) {
            app.setStage(ApplicationStage.SHORTLISTED);
        } else if (!newShortlisted && app.getStage() == ApplicationStage.SHORTLISTED) {
            app.setStage(ApplicationStage.REVIEWING);
        }

        JobApplication saved = applicationRepository.save(app);

        auditLogService.recordEvent(
                currentUser.getOrganization().getId(),
                currentUser.getId(),
                currentUser.getEmail(),
                newShortlisted ? "CANDIDATE_SHORTLISTED" : "CANDIDATE_REMOVED_FROM_SHORTLIST",
                "JobApplication",
                String.valueOf(saved.getId()),
                String.format("Candidate %s %s for job '%s'",
                        saved.getCandidate().getFullName(),
                        newShortlisted ? "added to shortlist" : "removed from shortlist",
                        job.getTitle()),
                "127.0.0.1"
        );

        return mapToDto(saved);
    }

    @Transactional
    public JobApplicationNoteDto addApplicationNote(Long jobId,
                                                    Long applicationId,
                                                    AddNoteRequest request,
                                                    User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        JobApplication app = applicationRepository.findByIdAndOrganizationId(applicationId, currentUser.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job application " + applicationId + " not found"));

        if (!app.getJob().getId().equals(job.getId())) {
            throw new BadRequestException("Application " + applicationId + " does not belong to job " + jobId);
        }

        JobApplicationNote note = new JobApplicationNote(
                app,
                currentUser.getOrganization(),
                currentUser,
                request.noteText().trim(),
                app.getStage().name(),
                request.decisionReason() != null ? request.decisionReason().trim() : null
        );

        JobApplicationNote saved = noteRepository.save(note);

        auditLogService.recordEvent(
                currentUser.getOrganization().getId(),
                currentUser.getId(),
                currentUser.getEmail(),
                "APPLICATION_NOTE_ADDED",
                "JobApplicationNote",
                String.valueOf(saved.getId()),
                String.format("Note added to candidate %s application for job '%s'",
                        app.getCandidate().getFullName(), job.getTitle()),
                "127.0.0.1"
        );

        return mapNoteToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationNoteDto> getApplicationNotes(Long jobId, Long applicationId, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        JobApplication app = applicationRepository.findByIdAndOrganizationId(applicationId, currentUser.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job application " + applicationId + " not found"));

        if (!app.getJob().getId().equals(job.getId())) {
            throw new BadRequestException("Application " + applicationId + " does not belong to job " + jobId);
        }

        return noteRepository.findByApplicationIdAndOrganizationIdOrderByCreatedAtDesc(app.getId(), currentUser.getOrganization().getId())
                .stream()
                .map(this::mapNoteToDto)
                .toList();
    }

    private Job getJobAndValidateAccess(Long jobId, User currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition " + jobId + " not found"));

        if (!job.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            log.warn("Unauthorized access by user {} to job {} in different organization",
                    currentUser.getEmail(), jobId);
            throw new AccessDeniedException("Access denied: You do not have permission to view or modify this job requisition");
        }
        return job;
    }

    private JobApplicationDto mapToDto(JobApplication app) {
        return new JobApplicationDto(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getCandidate().getId(),
                app.getCandidate().getFullName(),
                app.getCandidate().getEmail(),
                app.getCandidate().getPhone(),
                app.getCandidate().getPrimarySkills(),
                app.getStage(),
                app.isShortlisted(),
                app.getDecisionReason(),
                app.getAssignedToUser() != null ? app.getAssignedToUser().getFullName() : null,
                app.getLatestMatch() != null ? app.getLatestMatch().getFinalScore() : null,
                app.getLatestMatch() != null ? app.getLatestMatch().getRankPosition() : null,
                app.getLatestMatch() != null ? app.getLatestMatch().isEligible() : null,
                app.getStageUpdatedAt(),
                app.getCreatedAt()
        );
    }

    private JobApplicationNoteDto mapNoteToDto(JobApplicationNote note) {
        return new JobApplicationNoteDto(
                note.getId(),
                note.getApplication().getId(),
                note.getAuthorUser().getFullName(),
                note.getAuthorUser().getEmail(),
                note.getNoteText(),
                note.getStageAtCreation(),
                note.getDecisionReason(),
                note.getCreatedAt()
        );
    }
}
