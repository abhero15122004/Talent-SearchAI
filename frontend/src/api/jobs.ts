import { apiClient } from './client';
import { ApiResponse, PagedResponse, Job, JobSummary, CreateJobPayload, JobFilterParams, JobStatus } from '../types';

export const jobsApi = {
  getJobs: async (params?: JobFilterParams): Promise<PagedResponse<JobSummary>> => {
    const queryParams: Record<string, string | number> = {};
    if (params?.page !== undefined) queryParams.page = params.page;
    if (params?.size !== undefined) queryParams.size = params.size;
    if (params?.status) queryParams.status = params.status;
    if (params?.search && params.search.trim()) queryParams.search = params.search.trim();

    const response = await apiClient.get<ApiResponse<PagedResponse<JobSummary>>>('/jobs', {
      params: queryParams,
    });
    return response.data.data;
  },

  getJobById: async (id: number): Promise<Job> => {
    const response = await apiClient.get<ApiResponse<Job>>(`/jobs/${id}`);
    return response.data.data;
  },

  createJob: async (payload: CreateJobPayload): Promise<Job> => {
    const response = await apiClient.post<ApiResponse<Job>>('/jobs', payload);
    return response.data.data;
  },

  updateJob: async (id: number, payload: CreateJobPayload): Promise<Job> => {
    const response = await apiClient.put<ApiResponse<Job>>(`/jobs/${id}`, payload);
    return response.data.data;
  },

  updateJobStatus: async (id: number, status: JobStatus): Promise<Job> => {
    const response = await apiClient.patch<ApiResponse<Job>>(`/jobs/${id}/status`, { status });
    return response.data.data;
  },

  archiveJob: async (id: number): Promise<void> => {
    await apiClient.delete<ApiResponse<void>>(`/jobs/${id}`);
  },

  matchCandidates: async (jobId: number) => {
    const response = await apiClient.post<ApiResponse<any>>(`/jobs/${jobId}/match`);
    return response.data.data;
  },
};
