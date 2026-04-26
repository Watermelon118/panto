import { apiClient } from './client';
import type { LoginResponse, Result } from '../types/auth';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<Result<LoginResponse>>('/auth/login', request);
  return response.data.data;
}

export async function refresh(): Promise<LoginResponse> {
  const response = await apiClient.post<Result<LoginResponse>>('/auth/refresh');
  return response.data.data;
}

export async function changePassword(request: ChangePasswordRequest): Promise<LoginResponse> {
  const response = await apiClient.post<Result<LoginResponse>>('/auth/change-password', request);
  return response.data.data;
}

export async function logout(): Promise<void> {
  await apiClient.post<Result<void>>('/auth/logout');
}
