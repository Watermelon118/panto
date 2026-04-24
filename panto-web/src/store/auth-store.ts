import { create } from 'zustand';
import { login as loginRequest, refresh as refreshRequest } from '../api/auth';
import type { AuthUser, LoginResponse } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  hydrateAuth: (response: LoginResponse) => void;
  login: (username: string, password: string) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => void;
}

function toAuthUser(response: LoginResponse): AuthUser {
  return {
    userId: response.userId,
    username: response.username,
    role: response.role,
    mustChangePassword: response.mustChangePassword,
  };
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,

  hydrateAuth: (response) => {
    set({
      accessToken: response.accessToken,
      user: toAuthUser(response),
      isAuthenticated: true,
    });
  },

  login: async (username, password) => {
    const response = await loginRequest({ username, password });

    set({
      accessToken: response.accessToken,
      user: toAuthUser(response),
      isAuthenticated: true,
    });
  },

  refresh: async () => {
    const response = await refreshRequest();

    set({
      accessToken: response.accessToken,
      user: toAuthUser(response),
      isAuthenticated: true,
    });
  },

  logout: () => {
    set({
      accessToken: null,
      user: null,
      isAuthenticated: false,
    });
  },
}));
