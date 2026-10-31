export type Role = 'ADMIN' | 'RECRUITER';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  organizationId: number | null;
  organizationName: string | null;
}

export interface Organization {
  id: number;
  name: string;
  slug: string;
  domain?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export * from './job';
export * from './resume';
export * from './candidate';
export * from './matching';
export * from './workflow';


