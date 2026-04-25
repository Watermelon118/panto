import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateInboundRequest,
  InboundDetail,
  InboundPage,
  UpdateInboundRequest,
} from '../types/inbound';
import { apiClient } from './client';

interface ListInboundsParams {
  dateFrom?: string;
  dateTo?: string;
  productId?: number;
  page: number;
  size: number;
}

async function fetchInbounds(params: ListInboundsParams): Promise<InboundPage> {
  const response = await apiClient.get<Result<InboundPage>>('/inbound', { params });
  return response.data.data;
}

async function fetchInbound(id: number): Promise<InboundDetail> {
  const response = await apiClient.get<Result<InboundDetail>>(`/inbound/${id}`);
  return response.data.data;
}

async function createInbound(request: CreateInboundRequest): Promise<InboundDetail> {
  const response = await apiClient.post<Result<InboundDetail>>('/inbound', request);
  return response.data.data;
}

async function updateInbound(id: number, request: UpdateInboundRequest): Promise<InboundDetail> {
  const response = await apiClient.put<Result<InboundDetail>>(`/inbound/${id}`, request);
  return response.data.data;
}

export function useInbounds(params: ListInboundsParams) {
  return useQuery({
    queryKey: ['inbounds', params],
    queryFn: () => fetchInbounds(params),
  });
}

export function useInbound(id: number) {
  return useQuery({
    queryKey: ['inbounds', id],
    queryFn: () => fetchInbound(id),
  });
}

export function useCreateInbound() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createInbound,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['inbounds'] }),
  });
}

export function useUpdateInbound() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateInboundRequest }) =>
      updateInbound(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['inbounds'] }),
  });
}
