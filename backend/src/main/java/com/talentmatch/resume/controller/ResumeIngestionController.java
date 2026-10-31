package com.talentmatch.resume.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.resume.dto.BatchResponse;
import com.talentmatch.resume.dto.BatchSummaryResponse;
import com.talentmatch.resume.dto.BatchUploadResponse;
import com.talentmatch.resume.dto.ResumeFileResponse;
import com.talentmatch.resume.service.ResumeIngestionService;
import com.talentmatch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/resumes", "/api/v1/resumes"})
@Tag(name = "Resume Ingestion", description = "Endpoints for uploading ZIP archives or folder files, tracking ingestion batches, and retrying failed files")
public class ResumeIngestionController {

    private final ResumeIngestionService resumeIngestionService;

    public ResumeIngestionController(ResumeIngestionService resumeIngestionService) {
        this.resumeIngestionService = resumeIngestionService;
    }

    @PostMapping(value = "/upload/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ZIP archive of resumes", description = "Uploads a ZIP file containing PDF/DOCX resumes for asynchronous processing")
    public ResponseEntity<ApiResponse<BatchUploadResponse>> uploadZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        BatchUploadResponse response = resumeIngestionService.uploadZipArchive(file, effectiveOrgId, principal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "ZIP archive uploaded and scheduled for extraction"));
    }

    @PostMapping(value = "/upload/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload individual or folder resumes", description = "Uploads multiple PDF/DOCX files selected from folder or multi-file picker")
    public ResponseEntity<ApiResponse<BatchUploadResponse>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "batchName", required = false) String batchName,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        BatchUploadResponse response = resumeIngestionService.uploadFiles(files, batchName, effectiveOrgId, principal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Files uploaded and scheduled for extraction"));
    }

    @GetMapping("/batches")
    @Operation(summary = "List resume ingestion batches", description = "Retrieves paginated resume batches for the current organization")
    public ResponseEntity<ApiResponse<PagedResponse<BatchSummaryResponse>>> listBatches(
            @RequestParam(required = false) Long organizationId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        Page<BatchSummaryResponse> page = resumeIngestionService.listBatches(effectiveOrgId, pageable);
        PagedResponse<BatchSummaryResponse> pagedResponse = new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/batches/{id}")
    @Operation(summary = "Get batch progress and file statuses", description = "Polls real-time progress, file counts, and per-file extraction statuses")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatch(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        BatchResponse response = resumeIngestionService.getBatchById(id, effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/files/{id}/retry")
    @Operation(summary = "Retry a failed resume file", description = "Re-queues a failed resume file for processing without re-uploading")
    public ResponseEntity<ApiResponse<ResumeFileResponse>> retryFile(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = getAuthenticatedPrincipal();
        Long effectiveOrgId = resolveEffectiveOrgId(principal, organizationId);

        ResumeFileResponse response = resumeIngestionService.retryFile(id, effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success(response, "File queued for retry"));
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        return SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required to access resume resources", HttpStatus.UNAUTHORIZED));
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
