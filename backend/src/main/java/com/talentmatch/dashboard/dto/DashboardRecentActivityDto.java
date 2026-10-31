package com.talentmatch.dashboard.dto;

public record DashboardRecentActivityDto(
        Long id,
        String action,
        String entityType,
        String details,
        String performedBy,
        String timestamp
) {}
