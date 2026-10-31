import { apiClient } from './client';
import { ApiResponse } from '../types';

export interface DashboardRecentJob {
  id: number;
  title: string;
  department: string;
  location: string;
  status: string;
  minYearsExperience: number;
  candidateCount: number;
  shortlistedCount: number;
  createdAt: string;
}

export interface DashboardRecentActivity {
  id: number;
  action: string;
  entityType: string;
  details: string;
  performedBy: string;
  timestamp: string;
}

export interface DashboardStats {
  activeJobsCount: number;
  totalJobsCount: number;
  candidateProfilesCount: number;
  resumesProcessedCount: number;
  resumesFailedCount: number;
  aiMatchesEvaluatedCount: number;
  shortlistedCandidatesCount: number;
  totalApplicationsCount: number;
  stageBreakdown: Record<string, number>;
  recentJobs: DashboardRecentJob[];
  recentActivities: DashboardRecentActivity[];
}

export const dashboardApi = {
  getStats: async (organizationId?: number): Promise<ApiResponse<DashboardStats>> => {
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats', {
      params: organizationId ? { organizationId } : undefined,
    });
    return response.data;
  },
};
