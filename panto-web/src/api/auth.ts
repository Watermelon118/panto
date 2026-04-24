import { apiClient } from './client';
import type { LoginResponse, Result } from '../types/auth';

export interface LoginRequest {
  username: string;
  password: string;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<Result<LoginResponse>>('/auth/login', request);
  return response.data.data;
}

export async function refresh(): Promise<LoginResponse> {
  const response = await apiClient.post<Result<LoginResponse>>('/auth/refresh');
  return response.data.data;
}
