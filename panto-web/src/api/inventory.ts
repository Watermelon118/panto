import { useQuery } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  BatchItem,
  BatchPage,
  ExpiryStatus,
  StockPage,
  StockSummary,
  TransactionPage,
  TransactionType,
} from '../types/inventory';

interface StockParams {
  keyword?: string;
  category?: string;
  page: number;
  size: number;
}

interface BatchParams {
  productId?: number;
  expiryStatus?: ExpiryStatus;
  page: number;
  size: number;
}

interface TransactionParams {
  productId?: number;
  transactionType?: TransactionType;
  page: number;
  size: number;
}

export function useStockSummary(params: StockParams) {
  return useQuery({
    queryKey: ['inventory', 'stock', params],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: StockPage }>('/api/v1/inventory', { params });
      return data.data;
    },
  });
}

export function useBatches(params: BatchParams) {
  return useQuery({
    queryKey: ['inventory', 'batches', params],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: BatchPage }>('/api/v1/inventory/batches', {
        params,
      });
      return data.data;
    },
  });
}

export function useTransactions(params: TransactionParams) {
  return useQuery({
    queryKey: ['inventory', 'transactions', params],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: TransactionPage }>(
        '/api/v1/inventory/transactions',
        { params },
      );
      return data.data;
    },
  });
}

export function useLowStockProducts() {
  return useQuery({
    queryKey: ['inventory', 'low-stock'],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: StockSummary[] }>('/api/v1/inventory/low-stock');
      return data.data;
    },
  });
}

export function useExpiringBatches(withinDays = 30) {
  return useQuery({
    queryKey: ['inventory', 'expiring', withinDays],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: BatchItem[] }>('/api/v1/inventory/expiring', {
        params: { withinDays },
      });
      return data.data;
    },
  });
}
