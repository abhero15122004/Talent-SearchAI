package com.talentmatch.job.service;

import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.job.dto.CreateJobRequest;
import com.talentmatch.job.dto.JobResponse;
import com.talentmatch.job.dto.JobStatusUpdateRequest;
import com.talentmatch.job.dto.JobSummaryResponse;
import com.talentmatch.job.dto.UpdateJobRequest;
import com.talentmatch.job.entity.JobStatus;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobResponse createJob(CreateJobRequest request, Long organizationId, Long userId);

    JobResponse getJobById(Long id, Long organizationId);

    PagedResponse<JobSummaryResponse> getJobs(Long organizationId, JobStatus status, String search, Pageable pageable);

    JobResponse updateJob(Long id, UpdateJobRequest request, Long organizationId, Long userId);

    JobResponse updateJobStatus(Long id, JobStatusUpdateRequest request, Long organizationId, Long userId);

    void archiveJob(Long id, Long organizationId, Long userId);
}
