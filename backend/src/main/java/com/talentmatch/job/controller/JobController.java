package com.talentmatch.job.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.job.dto.CreateJobRequest;
import com.talentmatch.job.dto.JobResponse;
import com.talentmatch.job.dto.JobStatusUpdateRequest;
import com.talentmatch.job.dto.JobSummaryResponse;
import com.talentmatch.job.dto.UpdateJobRequest;
import com.talentmatch.job.entity.JobStatus;
import com.talentmatch.job.service.JobService;
import com.talentmatch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/jobs", "/api/v1/jobs"})
@Tag(name = "Job Requisitions", description = "Endpoints for creating, updating, searching, and managing job requisitions")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @Operation(summary = "Create a new job requisition", description = "Creates a new job with mandatory/preferred skills, qualifications, and AI matching weights")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        JobResponse response = jobService.createJob(request, effectiveOrgId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Job requisition created successfully"));
    }

    @GetMapping
    @Operation(summary = "List job requisitions with filtering & pagination", description = "Lists jobs scoped to the current recruiter organization with status filter and keyword search")
    public ResponseEntity<ApiResponse<PagedResponse<JobSummaryResponse>>> listJobs(
            @Parameter(description = "Optional status filter (e.g., OPEN, DRAFT, PAUSED, CLOSED, ARCHIVED)")
            @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Optional search query across title, department, or location")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long organizationId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        PagedResponse<JobSummaryResponse> response = jobService.getJobs(effectiveOrgId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Job requisitions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job requisition details", description = "Retrieves full job requisition data including normalized skills and qualifications")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        JobResponse response = jobService.getJobById(id, effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success(response, "Job requisition details retrieved"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing job requisition", description = "Updates job requisition fields, skills, qualifications, and matching weights")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        JobResponse response = jobService.updateJob(id, request, effectiveOrgId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Job requisition updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update job requisition status", description = "Updates the status of a job (DRAFT, OPEN, PAUSED, CLOSED, ARCHIVED)")
    public ResponseEntity<ApiResponse<JobResponse>> updateJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobStatusUpdateRequest request,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        JobResponse response = jobService.updateJobStatus(id, request, effectiveOrgId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Job status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive a job requisition", description = "Safely sets job status to ARCHIVED while preserving audit logs and requirements")
    public ResponseEntity<ApiResponse<Void>> archiveJob(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        jobService.archiveJob(id, effectiveOrgId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Job requisition archived successfully"));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required to access job resources", HttpStatus.UNAUTHORIZED));
    }

    private Long resolveEffectiveOrgId(UserPrincipal principal, Long requestedOrgId) {
        if (requestedOrgId != null) {
            SecurityUtils.validateOrganizationAccess(requestedOrgId);
            return requestedOrgId;
        }
        if (principal.getOrganizationId() != null) {
            return principal.getOrganizationId();
        }
        throw new ApiException("No organization context found for recruiter account", HttpStatus.BAD_REQUEST);
    }
}
