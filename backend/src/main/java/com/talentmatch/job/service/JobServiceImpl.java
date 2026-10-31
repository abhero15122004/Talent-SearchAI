package com.talentmatch.job.service;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.dto.*;
import com.talentmatch.job.entity.*;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public JobServiceImpl(
            JobRepository jobRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.jobRepository = jobRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public JobResponse createJob(CreateJobRequest request, Long organizationId, Long userId) {
        log.info("Creating new job requisition '{}' for organization {}", request.getTitle(), organizationId);

        validateWeights(request.getWeightSkillCoverage(), request.getWeightExperienceRelevance(),
                request.getWeightProjectRelevance(), request.getWeightSemanticSimilarity());
        validateExperience(request.getMinYearsExperience(), request.getMaxYearsExperience());

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Job job = new Job();
        job.setOrganization(organization);
        job.setCreatedByUser(user);
        mapRequestToJob(request, job);

        // Map and attach skills
        if (request.getSkills() != null) {
            for (JobSkillDto s : request.getSkills()) {
                JobSkill skill = new JobSkill(s.getSkillName().trim(), s.isMandatory(), s.getMinYearsRequired());
                job.addSkill(skill);
            }
        }

        // Map and attach qualifications
        if (request.getQualifications() != null) {
            for (JobQualificationDto q : request.getQualifications()) {
                JobQualification qual = new JobQualification(
                        q.getQualificationType(),
                        q.getRequirementText().trim(),
                        q.isMandatory()
                );
                job.addQualification(qual);
            }
        }

        Job savedJob = jobRepository.save(job);

        auditLogService.recordEvent(
                organizationId,
                userId,
                user.getEmail(),
                "JOB_CREATED",
                "Job",
                String.valueOf(savedJob.getId()),
                "Created job requisition: " + savedJob.getTitle(),
                null
        );

        return mapToJobResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id, Long organizationId) {
        Job job = jobRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
        return mapToJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> getJobs(Long organizationId, JobStatus status, String search, Pageable pageable) {
        Page<Job> jobPage = jobRepository.searchJobs(organizationId, status, search, pageable);
        List<JobSummaryResponse> content = jobPage.getContent().stream()
                .map(this::mapToJobSummaryResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );
    }

    @Override
    public JobResponse updateJob(Long id, UpdateJobRequest request, Long organizationId, Long userId) {
        log.info("Updating job requisition {} for organization {}", id, organizationId);

        validateWeights(request.getWeightSkillCoverage(), request.getWeightExperienceRelevance(),
                request.getWeightProjectRelevance(), request.getWeightSemanticSimilarity());
        validateExperience(request.getMinYearsExperience(), request.getMaxYearsExperience());

        Job job = jobRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        mapRequestToJob(request, job);

        // Re-synchronize child skills
        job.clearSkills();
        if (request.getSkills() != null) {
            for (JobSkillDto s : request.getSkills()) {
                JobSkill skill = new JobSkill(s.getSkillName().trim(), s.isMandatory(), s.getMinYearsRequired());
                job.addSkill(skill);
            }
        }

        // Re-synchronize child qualifications
        job.clearQualifications();
        if (request.getQualifications() != null) {
            for (JobQualificationDto q : request.getQualifications()) {
                JobQualification qual = new JobQualification(
                        q.getQualificationType(),
                        q.getRequirementText().trim(),
                        q.isMandatory()
                );
                job.addQualification(qual);
            }
        }

        Job updatedJob = jobRepository.save(job);

        auditLogService.recordEvent(
                organizationId,
                userId,
                user.getEmail(),
                "JOB_UPDATED",
                "Job",
                String.valueOf(updatedJob.getId()),
                "Updated job requisition: " + updatedJob.getTitle(),
                null
        );

        return mapToJobResponse(updatedJob);
    }

    @Override
    public JobResponse updateJobStatus(Long id, JobStatusUpdateRequest request, Long organizationId, Long userId) {
        Job job = jobRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        JobStatus previousStatus = job.getStatus();
        job.setStatus(request.getStatus());
        Job saved = jobRepository.save(job);

        auditLogService.recordEvent(
                organizationId,
                userId,
                user.getEmail(),
                "JOB_STATUS_CHANGED",
                "Job",
                String.valueOf(saved.getId()),
                String.format("Status changed from %s to %s", previousStatus, request.getStatus()),
                null
        );

        return mapToJobResponse(saved);
    }

    @Override
    public void archiveJob(Long id, Long organizationId, Long userId) {
        Job job = jobRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        job.setStatus(JobStatus.ARCHIVED);
        jobRepository.save(job);

        auditLogService.recordEvent(
                organizationId,
                userId,
                user.getEmail(),
                "JOB_ARCHIVED",
                "Job",
                String.valueOf(job.getId()),
                "Archived job requisition: " + job.getTitle(),
                null
        );
    }

    private void validateWeights(BigDecimal skill, BigDecimal exp, BigDecimal proj, BigDecimal sem) {
        if (skill == null || exp == null || proj == null || sem == null) {
            throw new ApiException("All four matching weights are required", HttpStatus.BAD_REQUEST);
        }
        BigDecimal sum = skill.add(exp).add(proj).add(sem);
        if (sum.compareTo(new BigDecimal("100.00")) != 0 && sum.compareTo(new BigDecimal("100")) != 0) {
            throw new ApiException(
                    String.format("Matching weights must sum exactly to 100.00%% (current sum: %s%%)", sum.toPlainString()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateExperience(int min, Integer max) {
        if (min < 0) {
            throw new ApiException("Minimum experience years cannot be negative", HttpStatus.BAD_REQUEST);
        }
        if (max != null && max < min) {
            throw new ApiException("Maximum experience years cannot be less than minimum experience", HttpStatus.BAD_REQUEST);
        }
    }

    private void mapRequestToJob(CreateJobRequest request, Job job) {
        job.setTitle(request.getTitle().trim());
        job.setDepartment(request.getDepartment().trim());
        job.setLocation(request.getLocation().trim());
        job.setEmploymentType(request.getEmploymentType());
        job.setDescription(request.getDescription().trim());
        job.setMinYearsExperience(request.getMinYearsExperience());
        job.setMaxYearsExperience(request.getMaxYearsExperience());
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }
        job.setShortlistCount(request.getShortlistCount());
        job.setMinScoreThreshold(request.getMinScoreThreshold());
        job.setWeightSkillCoverage(request.getWeightSkillCoverage());
        job.setWeightExperienceRelevance(request.getWeightExperienceRelevance());
        job.setWeightProjectRelevance(request.getWeightProjectRelevance());
        job.setWeightSemanticSimilarity(request.getWeightSemanticSimilarity());
    }

    private JobSummaryResponse mapToJobSummaryResponse(Job job) {
        JobSummaryResponse dto = new JobSummaryResponse();
        dto.setId(job.getId());
        dto.setOrganizationId(job.getOrganization().getId());
        dto.setTitle(job.getTitle());
        dto.setDepartment(job.getDepartment());
        dto.setLocation(job.getLocation());
        dto.setEmploymentType(job.getEmploymentType());
        dto.setStatus(job.getStatus());
        dto.setMinYearsExperience(job.getMinYearsExperience());
        dto.setMaxYearsExperience(job.getMaxYearsExperience());
        dto.setShortlistCount(job.getShortlistCount());
        dto.setMinScoreThreshold(job.getMinScoreThreshold());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());

        int mandatory = 0;
        int preferred = 0;
        if (job.getSkills() != null) {
            for (JobSkill s : job.getSkills()) {
                if (s.isMandatory()) {
                    mandatory++;
                } else {
                    preferred++;
                }
            }
        }
        dto.setMandatorySkillsCount(mandatory);
        dto.setPreferredSkillsCount(preferred);
        dto.setQualificationsCount(job.getQualifications() != null ? job.getQualifications().size() : 0);

        return dto;
    }

    private JobResponse mapToJobResponse(Job job) {
        JobResponse dto = new JobResponse();
        dto.setId(job.getId());
        dto.setOrganizationId(job.getOrganization().getId());
        dto.setOrganizationName(job.getOrganization().getName());
        dto.setCreatedByUserId(job.getCreatedByUser().getId());
        dto.setCreatedByUserName(job.getCreatedByUser().getFullName());
        dto.setTitle(job.getTitle());
        dto.setDepartment(job.getDepartment());
        dto.setLocation(job.getLocation());
        dto.setEmploymentType(job.getEmploymentType());
        dto.setDescription(job.getDescription());
        dto.setMinYearsExperience(job.getMinYearsExperience());
        dto.setMaxYearsExperience(job.getMaxYearsExperience());
        dto.setStatus(job.getStatus());
        dto.setShortlistCount(job.getShortlistCount());
        dto.setMinScoreThreshold(job.getMinScoreThreshold());
        dto.setWeightSkillCoverage(job.getWeightSkillCoverage());
        dto.setWeightExperienceRelevance(job.getWeightExperienceRelevance());
        dto.setWeightProjectRelevance(job.getWeightProjectRelevance());
        dto.setWeightSemanticSimilarity(job.getWeightSemanticSimilarity());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());

        if (job.getSkills() != null) {
            dto.setSkills(job.getSkills().stream()
                    .map(s -> new JobSkillDto(s.getId(), s.getSkillName(), s.isMandatory(), s.getMinYearsRequired()))
                    .collect(Collectors.toList()));
        }

        if (job.getQualifications() != null) {
            dto.setQualifications(job.getQualifications().stream()
                    .map(q -> new JobQualificationDto(q.getId(), q.getQualificationType(), q.getRequirementText(), q.isMandatory()))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
