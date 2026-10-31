package com.talentmatch.ai.controller;

import com.talentmatch.ai.service.MatchingService;
import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.matching.dto.CandidateComparisonDto;
import com.talentmatch.matching.dto.CompareCandidatesRequest;
import com.talentmatch.matching.dto.JobMatchRunDto;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
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

@RestController
@RequestMapping({"/api/jobs", "/api/v1/jobs"})
@Tag(name = "Talent Matching & Explainability", description = "Endpoints for AI-driven semantic candidate retrieval, explainability evidence, and match runs")
public class MatchingController {

    private final MatchingService matchingService;
    private final UserRepository userRepository;

    public MatchingController(MatchingService matchingService, UserRepository userRepository) {
        this.matchingService = matchingService;
        this.userRepository = userRepository;
    }

    @PostMapping({"/{jobId}/match", "/{jobId}/matches/run"})
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Execute candidate match run", description = "Executes multi-factor semantic retrieval, mandatory eligibility filtering, deterministic ranking, and persists match run history")
    public ResponseEntity<ApiResponse<JobMatchRunDto>> runMatching(@PathVariable Long jobId) {
        User currentUser = getAuthenticatedUser();
        JobMatchRunDto response = matchingService.executeMatchRun(jobId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate match run completed successfully"));
    }

    @GetMapping("/{jobId}/matches/latest")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get latest match run", description = "Retrieves the most recent candidate match run for a job with full score breakdowns and explainability evidence")
    public ResponseEntity<ApiResponse<JobMatchRunDto>> getLatestMatchRun(@PathVariable Long jobId) {
        User currentUser = getAuthenticatedUser();
        JobMatchRunDto response = matchingService.getLatestMatchRun(jobId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, response != null ? "Latest match run retrieved" : "No match runs found"));
    }

    @GetMapping("/{jobId}/matches/runs")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "List match run history", description = "Lists historical match run snapshots for a job requisition")
    public ResponseEntity<ApiResponse<PagedResponse<JobMatchRunDto>>> getMatchRunHistory(
            @PathVariable Long jobId,
            @PageableDefault(size = 10, sort = "runNumber", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        PagedResponse<JobMatchRunDto> response = matchingService.getMatchRunHistory(jobId, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Match run history retrieved"));
    }

    @GetMapping("/{jobId}/matches/runs/{runId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get specific match run", description = "Retrieves a specific historical candidate match run by ID")
    public ResponseEntity<ApiResponse<JobMatchRunDto>> getMatchRunById(@PathVariable Long jobId, @PathVariable Long runId) {
        User currentUser = getAuthenticatedUser();
        JobMatchRunDto response = matchingService.getMatchRunById(jobId, runId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Match run details retrieved"));
    }

    @PostMapping("/{jobId}/compare")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Compare candidates side-by-side", description = "Generates a side-by-side comparison matrix of 2 to 5 candidates for a specific job")
    public ResponseEntity<ApiResponse<CandidateComparisonDto>> compareCandidates(
            @PathVariable Long jobId,
            @Valid @RequestBody CompareCandidatesRequest request) {
        User currentUser = getAuthenticatedUser();
        CandidateComparisonDto response = matchingService.compareCandidates(jobId, request.candidateIds(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate comparison matrix generated"));
    }

    private User getAuthenticatedUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required", HttpStatus.UNAUTHORIZED));

        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException("Authenticated user record not found", HttpStatus.UNAUTHORIZED));
    }
}
