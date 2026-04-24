export type UserRole = 'ADMIN' | 'WAREHOUSE' | 'MARKETING' | 'ACCOUNTANT';

export interface AuthUser {
  userId: number;
  username: string;
  role: UserRole;
  mustChangePassword: boolean;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  role: UserRole;
  mustChangePassword: boolean;
}

export interface Result<T> {
  code: string;
  message: string;
  data: T;
}
