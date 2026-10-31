package com.talentmatch.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
        long activeJobsCount,
        long totalJobsCount,
        long candidateProfilesCount,
        long resumesProcessedCount,
        long resumesFailedCount,
        long aiMatchesEvaluatedCount,
        long shortlistedCandidatesCount,
        long totalApplicationsCount,
        Map<String, Long> stageBreakdown,
        List<DashboardRecentJobDto> recentJobs,
        List<DashboardRecentActivityDto> recentActivities
) {}
