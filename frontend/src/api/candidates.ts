import { apiClient } from './client';
import { ApiResponse, PagedResponse, CandidateSummary, CandidateDetail } from '../types';

export interface GetCandidatesParams {
  search?: string;
  page?: number;
  size?: number;
}

export const candidatesApi = {
  getCandidates: async (params?: GetCandidatesParams): Promise<ApiResponse<PagedResponse<CandidateSummary>>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<CandidateSummary>>>('/candidates', {
      params,
    });
    return response.data;
  },

  getCandidateById: async (id: number): Promise<ApiResponse<CandidateDetail>> => {
    const response = await apiClient.get<ApiResponse<CandidateDetail>>(`/candidates/${id}`);
    return response.data;
  },
};
