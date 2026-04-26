import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import { apiClient } from './client';

export interface SystemSettings {
  expiryWarningDays: number;
  invoiceSellerCompanyName: string;
  invoiceSellerGstNumber: string;
  invoiceSellerAddress: string;
  invoiceSellerPhone: string;
  invoiceSellerEmail: string;
  invoicePaymentInstructions: string;
}

export interface UpdateSystemSettingsRequest {
  expiryWarningDays: number;
  invoiceSellerCompanyName: string;
  invoiceSellerGstNumber: string;
  invoiceSellerAddress: string;
  invoiceSellerPhone: string;
  invoiceSellerEmail: string;
  invoicePaymentInstructions: string;
}

async function fetchSettings(): Promise<SystemSettings> {
  const response = await apiClient.get<Result<SystemSettings>>('/settings');
  return response.data.data;
}

async function updateSettings(request: UpdateSystemSettingsRequest): Promise<SystemSettings> {
  const response = await apiClient.put<Result<SystemSettings>>('/settings', request);
  return response.data.data;
}

export function useSettings() {
  return useQuery({
    queryKey: ['settings'],
    queryFn: fetchSettings,
  });
}

export function useUpdateSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
  });
}
