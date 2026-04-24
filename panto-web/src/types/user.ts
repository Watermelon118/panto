import type { UserRole } from './auth';

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
}

export interface User extends UserSummary {
  lockedUntil: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number;
  updatedBy: number;
}

export interface UserPage {
  items: UserSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  role: UserRole;
}

export interface UpdateUserRequest {
  fullName: string;
  email?: string;
  role: UserRole;
}

export interface ResetPasswordRequest {
  newPassword: string;
}
