package com.talentmatch.dashboard.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.dashboard.dto.DashboardStatsDto;
import com.talentmatch.dashboard.service.DashboardService;
import com.talentmatch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/dashboard", "/api/v1/dashboard"})
@Tag(name = "Dashboard", description = "Real-time dashboard metrics, pipeline stats, and recent activity")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard metrics and pipeline stats", description = "Returns active jobs, candidate counts, processed resumes, match stats, and recent activities scoped to current organization")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats(
            @RequestParam(required = false) Long organizationId) {

        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new ApiException("Authentication required to access dashboard", HttpStatus.UNAUTHORIZED));

        Long effectiveOrgId = organizationId != null ? organizationId : principal.getOrganizationId();
        if (effectiveOrgId == null) {
            throw new ApiException("No organization context found", HttpStatus.BAD_REQUEST);
        }

        SecurityUtils.validateOrganizationAccess(effectiveOrgId);

        DashboardStatsDto stats = dashboardService.getDashboardStats(effectiveOrgId);
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard statistics retrieved successfully"));
    }
}
