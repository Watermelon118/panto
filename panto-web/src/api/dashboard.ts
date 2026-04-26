import { useQuery } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type { DashboardSummary } from '../types/dashboard';
import { apiClient } from './client';

async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const response = await apiClient.get<Result<DashboardSummary>>('/dashboard/summary');
  return response.data.data;
}

export function useDashboardSummary() {
  return useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: fetchDashboardSummary,
    staleTime: 60_000,
  });
}
