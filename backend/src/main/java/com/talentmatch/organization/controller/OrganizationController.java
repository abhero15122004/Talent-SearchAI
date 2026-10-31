package com.talentmatch.organization.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.organization.dto.CreateOrganizationRequest;
import com.talentmatch.organization.dto.OrganizationDto;
import com.talentmatch.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Tenant organization management endpoints")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all organizations (Admin only)")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> listOrganizations() {
        List<OrganizationDto> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID (Scoped to organization)")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(@PathVariable Long id) {
        OrganizationDto org = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(ApiResponse.success(org));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tenant organization (Admin only)")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDto created = organizationService.createOrganization(request);
        return new ResponseEntity<>(ApiResponse.success(created, "Organization created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization details")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDto updated = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Organization updated successfully"));
    }
}
