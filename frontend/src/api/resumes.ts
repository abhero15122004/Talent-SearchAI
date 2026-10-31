import { apiClient } from './client';
import { ApiResponse, PagedResponse, BatchSummary, BatchDetail, BatchUploadResponse, ResumeFile } from '../types';

export const resumesApi = {
  uploadZipArchive: async (file: File): Promise<ApiResponse<BatchUploadResponse>> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<ApiResponse<BatchUploadResponse>>('/resumes/upload/zip', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  uploadFiles: async (files: File[], batchName?: string): Promise<ApiResponse<BatchUploadResponse>> => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });
    if (batchName) {
      formData.append('batchName', batchName);
    }

    const response = await apiClient.post<ApiResponse<BatchUploadResponse>>('/resumes/upload/files', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getBatches: async (page = 0, size = 10): Promise<ApiResponse<PagedResponse<BatchSummary>>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<BatchSummary>>>('/resumes/batches', {
      params: { page, size },
    });
    return response.data;
  },

  getBatchById: async (id: number): Promise<ApiResponse<BatchDetail>> => {
    const response = await apiClient.get<ApiResponse<BatchDetail>>(`/resumes/batches/${id}`);
    return response.data;
  },

  retryResumeFile: async (fileId: number): Promise<ApiResponse<ResumeFile>> => {
    const response = await apiClient.post<ApiResponse<ResumeFile>>(`/resumes/files/${fileId}/retry`);
    return response.data;
  },
};
