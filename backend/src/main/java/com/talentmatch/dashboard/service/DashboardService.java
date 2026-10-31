package com.talentmatch.dashboard.service;

import com.talentmatch.dashboard.dto.DashboardStatsDto;

public interface DashboardService {
    DashboardStatsDto getDashboardStats(Long organizationId);
}
