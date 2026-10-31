package com.talentmatch.workflow.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import com.talentmatch.workflow.dto.AddNoteRequest;
import com.talentmatch.workflow.dto.JobApplicationDto;
import com.talentmatch.workflow.dto.JobApplicationNoteDto;
import com.talentmatch.workflow.dto.UpdateStageRequest;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.service.RecruitmentWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/jobs", "/api/v1/jobs"})
@Tag(name = "Recruitment Workflow", description = "Endpoints for candidate pipeline stages, shortlisting, recruiter notes, and hiring decisions")
public class RecruitmentWorkflowController {

    private final RecruitmentWorkflowService workflowService;
    private final UserRepository userRepository;

    public RecruitmentWorkflowController(RecruitmentWorkflowService workflowService, UserRepository userRepository) {
        this.workflowService = workflowService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{jobId}/applications")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "List candidate applications for job", description = "Lists candidate applications with stage filtering, keyword search, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<JobApplicationDto>>> getApplications(
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStage stage,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "stageUpdatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        PagedResponse<JobApplicationDto> response = workflowService.getJobApplications(jobId, stage, search, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Applications retrieved successfully"));
    }

    @PatchMapping("/{jobId}/applications/{applicationId}/stage")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Update candidate workflow stage", description = "Transitions candidate to a new workflow stage (e.g. REVIEWING, SHORTLISTED, INTERVIEW, REJECTED, HIRED) enforcing state policy")
    public ResponseEntity<ApiResponse<JobApplicationDto>> updateStage(
            @PathVariable Long jobId,
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateStageRequest request) {
        User currentUser = getAuthenticatedUser();
        JobApplicationDto response = workflowService.updateApplicationStage(jobId, applicationId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Application stage updated successfully"));
    }

    @PatchMapping("/{jobId}/applications/{applicationId}/shortlist")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Toggle candidate shortlist status", description = "Explicit recruiter toggle to add or remove candidate from shortlist")
    public ResponseEntity<ApiResponse<JobApplicationDto>> toggleShortlist(
            @PathVariable Long jobId,
            @PathVariable Long applicationId) {
        User currentUser = getAuthenticatedUser();
        JobApplicationDto response = workflowService.toggleShortlist(jobId, applicationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Shortlist status updated successfully"));
    }

    @PostMapping("/{jobId}/applications/{applicationId}/notes")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Add recruiter note to application", description = "Adds a recruiter evaluation note and optional decision reason to the candidate application")
    public ResponseEntity<ApiResponse<JobApplicationNoteDto>> addNote(
            @PathVariable Long jobId,
            @PathVariable Long applicationId,
            @Valid @RequestBody AddNoteRequest request) {
        User currentUser = getAuthenticatedUser();
        JobApplicationNoteDto response = workflowService.addApplicationNote(jobId, applicationId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Note recorded successfully"));
    }

    @GetMapping("/{jobId}/applications/{applicationId}/notes")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get recruiter notes for application", description = "Retrieves all recruiter notes, timeline, and decision reasons for an application")
    public ResponseEntity<ApiResponse<List<JobApplicationNoteDto>>> getNotes(
            @PathVariable Long jobId,
            @PathVariable Long applicationId) {
        User currentUser = getAuthenticatedUser();
        List<JobApplicationNoteDto> response = workflowService.getApplicationNotes(jobId, applicationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Notes retrieved successfully"));
    }

    private User getAuthenticatedUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required", HttpStatus.UNAUTHORIZED));

        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException("Authenticated user record not found", HttpStatus.UNAUTHORIZED));
    }
}
