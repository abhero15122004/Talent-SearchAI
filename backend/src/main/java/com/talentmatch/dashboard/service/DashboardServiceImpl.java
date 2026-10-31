package com.talentmatch.dashboard.service;

import com.talentmatch.audit.entity.AuditLog;
import com.talentmatch.audit.repository.AuditLogRepository;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.dashboard.dto.DashboardRecentActivityDto;
import com.talentmatch.dashboard.dto.DashboardRecentJobDto;
import com.talentmatch.dashboard.dto.DashboardStatsDto;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobStatus;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.matching.repository.JobCandidateMatchRepository;
import com.talentmatch.matching.repository.JobMatchRunRepository;
import com.talentmatch.resume.entity.ResumeFileStatus;
import com.talentmatch.resume.repository.ResumeFileRepository;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final ResumeFileRepository resumeFileRepository;
    private final JobMatchRunRepository jobMatchRunRepository;
    private final JobCandidateMatchRepository jobCandidateMatchRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardServiceImpl(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ResumeFileRepository resumeFileRepository,
            JobMatchRunRepository jobMatchRunRepository,
            JobCandidateMatchRepository jobCandidateMatchRepository,
            JobApplicationRepository jobApplicationRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.resumeFileRepository = resumeFileRepository;
        this.jobMatchRunRepository = jobMatchRunRepository;
        this.jobCandidateMatchRepository = jobCandidateMatchRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public DashboardStatsDto getDashboardStats(Long organizationId) {
        long activeJobsCount = jobRepository.countByOrganizationIdAndStatus(organizationId, JobStatus.OPEN);
        long totalJobsCount = jobRepository.countByOrganizationId(organizationId);
        long candidateProfilesCount = candidateRepository.countByOrganizationId(organizationId);
        long resumesProcessedCount = resumeFileRepository.countByOrganizationIdAndStatus(organizationId, ResumeFileStatus.SUCCEEDED);
        long resumesFailedCount = resumeFileRepository.countByOrganizationIdAndStatus(organizationId, ResumeFileStatus.FAILED);
        long aiMatchesEvaluatedCount = jobCandidateMatchRepository.countByOrganizationId(organizationId);
        long shortlistedCandidatesCount = jobApplicationRepository.countByOrganizationIdAndShortlisted(organizationId, true);
        long totalApplicationsCount = jobApplicationRepository.countByOrganizationId(organizationId);

        // Stage breakdown
        Map<String, Long> stageBreakdown = new LinkedHashMap<>();
        for (ApplicationStage stage : ApplicationStage.values()) {
            long count = jobApplicationRepository.countByOrganizationIdAndStage(organizationId, stage);
            stageBreakdown.put(stage.name(), count);
        }

        // Recent 5 jobs
        Page<Job> recentJobsPage = jobRepository.findByOrganizationId(
                organizationId,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<DashboardRecentJobDto> recentJobs = recentJobsPage.getContent().stream()
                .map(j -> new DashboardRecentJobDto(
                        j.getId(),
                        j.getTitle(),
                        j.getDepartment(),
                        j.getLocation(),
                        j.getStatus().name(),
                        j.getMinYearsExperience(),
                        jobApplicationRepository.countByJobIdAndOrganizationId(j.getId(), organizationId),
                        jobApplicationRepository.countByJobIdAndOrganizationIdAndShortlisted(j.getId(), organizationId, true),
                        j.getCreatedAt() != null ? j.getCreatedAt().toString() : ""
                ))
                .toList();

        // Recent 8 audit activities
        Page<AuditLog> recentLogsPage = auditLogRepository.findByOrganizationId(
                organizationId,
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        List<DashboardRecentActivityDto> recentActivities = recentLogsPage.getContent().stream()
                .map(l -> new DashboardRecentActivityDto(
                        l.getId(),
                        l.getAction(),
                        l.getEntityType(),
                        l.getDetails(),
                        l.getUserEmail() != null ? l.getUserEmail() : "System",
                        l.getTimestamp() != null ? l.getTimestamp().toString() : ""
                ))
                .toList();

        return new DashboardStatsDto(
                activeJobsCount,
                totalJobsCount,
                candidateProfilesCount,
                resumesProcessedCount,
                resumesFailedCount,
                aiMatchesEvaluatedCount,
                shortlistedCandidatesCount,
                totalApplicationsCount,
                stageBreakdown,
                recentJobs,
                recentActivities
        );
    }
}
