import { useQuery } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type { AuditAction, AuditLogPage } from '../types/audit';
import { apiClient } from './client';

export interface ListAuditLogsParams {
  operatorId?: number;
  entityType?: string;
  action?: AuditAction;
  dateFrom?: string;
  dateTo?: string;
  page: number;
  size: number;
}

async function fetchAuditLogs(params: ListAuditLogsParams): Promise<AuditLogPage> {
  const response = await apiClient.get<Result<AuditLogPage>>('/audit-logs', { params });
  return response.data.data;
}

export function useAuditLogs(params: ListAuditLogsParams) {
  return useQuery({
    queryKey: ['audit-logs', params],
    queryFn: () => fetchAuditLogs(params),
  });
}
