import { apiClient } from './client';
import {
  ApiResponse,
  PagedResponse,
  ApplicationStage,
  JobApplication,
  JobApplicationNote,
  JobMatchRun,
  CandidateComparison,
} from '../types';

export const workflowApi = {
  getApplications: async (
    jobId: number,
    params?: { stage?: ApplicationStage; search?: string; page?: number; size?: number }
  ): Promise<PagedResponse<JobApplication>> => {
    const queryParams: Record<string, string | number> = {};
    if (params?.page !== undefined) queryParams.page = params.page;
    if (params?.size !== undefined) queryParams.size = params.size;
    if (params?.stage) queryParams.stage = params.stage;
    if (params?.search && params.search.trim()) queryParams.search = params.search.trim();

    const response = await apiClient.get<ApiResponse<PagedResponse<JobApplication>>>(
      `/jobs/${jobId}/applications`,
      { params: queryParams }
    );
    return response.data.data;
  },

  updateStage: async (
    jobId: number,
    applicationId: number,
    payload: { stage: ApplicationStage; decisionReason?: string; note?: string }
  ): Promise<JobApplication> => {
    const response = await apiClient.patch<ApiResponse<JobApplication>>(
      `/jobs/${jobId}/applications/${applicationId}/stage`,
      payload
    );
    return response.data.data;
  },

  toggleShortlist: async (jobId: number, applicationId: number): Promise<JobApplication> => {
    const response = await apiClient.patch<ApiResponse<JobApplication>>(
      `/jobs/${jobId}/applications/${applicationId}/shortlist`
    );
    return response.data.data;
  },

  addNote: async (
    jobId: number,
    applicationId: number,
    payload: { noteText: string; decisionReason?: string }
  ): Promise<JobApplicationNote> => {
    const response = await apiClient.post<ApiResponse<JobApplicationNote>>(
      `/jobs/${jobId}/applications/${applicationId}/notes`,
      payload
    );
    return response.data.data;
  },

  getNotes: async (jobId: number, applicationId: number): Promise<JobApplicationNote[]> => {
    const response = await apiClient.get<ApiResponse<JobApplicationNote[]>>(
      `/jobs/${jobId}/applications/${applicationId}/notes`
    );
    return response.data.data;
  },

  compareCandidates: async (jobId: number, candidateIds: number[]): Promise<CandidateComparison> => {
    const response = await apiClient.post<ApiResponse<CandidateComparison>>(`/jobs/${jobId}/compare`, {
      candidateIds,
    });
    return response.data.data;
  },

  runMatching: async (jobId: number): Promise<JobMatchRun> => {
    const response = await apiClient.post<ApiResponse<JobMatchRun>>(`/jobs/${jobId}/matches/run`);
    return response.data.data;
  },

  getLatestMatchRun: async (jobId: number): Promise<JobMatchRun | null> => {
    const response = await apiClient.get<ApiResponse<JobMatchRun>>(`/jobs/${jobId}/matches/latest`);
    return response.data.data;
  },

  getMatchRunHistory: async (
    jobId: number,
    params?: { page?: number; size?: number }
  ): Promise<PagedResponse<JobMatchRun>> => {
    const queryParams: Record<string, number> = {};
    if (params?.page !== undefined) queryParams.page = params.page;
    if (params?.size !== undefined) queryParams.size = params.size;

    const response = await apiClient.get<ApiResponse<PagedResponse<JobMatchRun>>>(
      `/jobs/${jobId}/matches/runs`,
      { params: queryParams }
    );
    return response.data.data;
  },

  getMatchRunById: async (jobId: number, runId: number): Promise<JobMatchRun> => {
    const response = await apiClient.get<ApiResponse<JobMatchRun>>(
      `/jobs/${jobId}/matches/runs/${runId}`
    );
    return response.data.data;
  },
};
