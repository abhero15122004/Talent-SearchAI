package com.talentmatch.dashboard.dto;

public record DashboardRecentJobDto(
        Long id,
        String title,
        String department,
        String location,
        String status,
        int minYearsExperience,
        int candidateCount,
        int shortlistedCount,
        String createdAt
) {}
