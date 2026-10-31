package com.talentmatch.candidate.controller;

import com.talentmatch.candidate.dto.CandidateDetailResponse;
import com.talentmatch.candidate.dto.CandidateSummaryResponse;
import com.talentmatch.candidate.service.CandidateService;
import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/candidates", "/api/v1/candidates"})
@Tag(name = "Candidates", description = "Endpoints for viewing and searching candidate talent profiles extracted from resumes")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    @Operation(summary = "List candidates with search and pagination", description = "Retrieves candidates for the current organization with keyword search")
    public ResponseEntity<ApiResponse<PagedResponse<CandidateSummaryResponse>>> listCandidates(
            @Parameter(description = "Optional search query matching name, email, or skills")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long organizationId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        Page<CandidateSummaryResponse> page = candidateService.listCandidates(effectiveOrgId, search, pageable);
        PagedResponse<CandidateSummaryResponse> pagedResponse = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get candidate profile details", description = "Retrieves full candidate profile including raw text, parsed skills, education, and experience")
    public ResponseEntity<ApiResponse<CandidateDetailResponse>> getCandidate(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        CandidateDetailResponse response = candidateService.getCandidateById(id, effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required to access candidate resources", HttpStatus.UNAUTHORIZED));
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
