import axios from 'axios';
import { apiClient } from './client';
import { useAuthStore } from '../store/auth-store';

interface RetryableRequestConfig {
  _retry?: boolean;
}

export function setupAuthInterceptors() {
  apiClient.interceptors.request.use((config) => {
    const accessToken = useAuthStore.getState().accessToken;

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
  });

  apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
      if (!axios.isAxiosError(error) || !error.config) {
        return Promise.reject(error);
      }

      const requestConfig = error.config as typeof error.config & RetryableRequestConfig;
      const status = error.response?.status;

      if (status !== 401 || requestConfig._retry) {
        return Promise.reject(error);
      }

      requestConfig._retry = true;

      try {
        await useAuthStore.getState().refresh();

        const newAccessToken = useAuthStore.getState().accessToken;
        if (newAccessToken) {
          requestConfig.headers.Authorization = `Bearer ${newAccessToken}`;
        }

        return apiClient(requestConfig);
      } catch (refreshError) {
        await useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    },
  );
}
