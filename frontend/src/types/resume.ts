export type BatchStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'PARTIAL_SUCCESS' | 'FAILED';

export type ResumeFileStatus = 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED_DUPLICATE';

export interface ResumeFile {
  id: number;
  batchId: number;
  candidateId: number | null;
  originalFilename: string;
  fileSizeBytes: number;
  contentType: string;
  status: ResumeFileStatus;
  errorMessage?: string | null;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface BatchSummary {
  id: number;
  organizationId: number;
  createdByUserName?: string;
  name: string;
  status: BatchStatus;
  totalFiles: number;
  processedFiles: number;
  successfulFiles: number;
  failedFiles: number;
  duplicateFiles: number;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BatchDetail extends BatchSummary {
  files: ResumeFile[];
}

export interface BatchUploadResponse {
  batchId: number;
  name: string;
  status: BatchStatus;
  totalFiles: number;
  message: string;
}
