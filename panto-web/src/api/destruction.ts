import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateDestructionRequest,
  DestructionDetail,
  DestructionPage,
} from '../types/destruction';
import { apiClient } from './client';

interface ListDestructionsParams {
  productId?: number;
  dateFrom?: string;
  dateTo?: string;
  page: number;
  size: number;
}

async function fetchDestructions(params: ListDestructionsParams): Promise<DestructionPage> {
  const response = await apiClient.get<Result<DestructionPage>>('/destructions', { params });
  return response.data.data;
}

async function fetchDestruction(id: number): Promise<DestructionDetail> {
  const response = await apiClient.get<Result<DestructionDetail>>(`/destructions/${id}`);
  return response.data.data;
}

async function createDestruction(request: CreateDestructionRequest): Promise<DestructionDetail> {
  const response = await apiClient.post<Result<DestructionDetail>>('/destructions', request);
  return response.data.data;
}

export function useDestructions(params: ListDestructionsParams) {
  return useQuery({
    queryKey: ['destructions', params],
    queryFn: () => fetchDestructions(params),
  });
}

export function useDestruction(id: number) {
  return useQuery({
    queryKey: ['destructions', id],
    queryFn: () => fetchDestruction(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useCreateDestruction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createDestruction,
    onSuccess: (destruction) => {
      queryClient.invalidateQueries({ queryKey: ['destructions'] });
      queryClient.invalidateQueries({ queryKey: ['destructions', destruction.id] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}
