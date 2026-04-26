import { useMutation } from '@tanstack/react-query';
import { downloadBlobResponse } from '../lib/download';
import { apiClient } from './client';

export type ReportFormat = 'xlsx' | 'csv';

export interface ExportReportRequest {
  from: string;
  to: string;
  format: ReportFormat;
}

async function exportSalesReport(request: ExportReportRequest) {
  const response = await apiClient.get<Blob>('/reports/sales/export', {
    params: request,
    responseType: 'blob',
  });

  return downloadBlobResponse(
    response,
    `sales-report-${request.from}-to-${request.to}.${request.format}`,
  );
}

async function exportLossReport(request: ExportReportRequest) {
  const response = await apiClient.get<Blob>('/reports/losses/export', {
    params: request,
    responseType: 'blob',
  });

  return downloadBlobResponse(
    response,
    `loss-report-${request.from}-to-${request.to}.${request.format}`,
  );
}

export function useExportSalesReport() {
  return useMutation({
    mutationFn: exportSalesReport,
  });
}

export function useExportLossReport() {
  return useMutation({
    mutationFn: exportLossReport,
  });
}
