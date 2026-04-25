import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateOrderRequest,
  InvoiceDetail,
  OrderDetail,
  OrderPage,
  OrderStatus,
  RollbackOrderRequest,
} from '../types/order';
import { apiClient } from './client';

interface ListOrdersParams {
  customerId?: number;
  dateFrom?: string;
  dateTo?: string;
  status?: OrderStatus;
  page: number;
  size: number;
}

async function fetchOrders(params: ListOrdersParams): Promise<OrderPage> {
  const response = await apiClient.get<Result<OrderPage>>('/orders', { params });
  return response.data.data;
}

async function createOrder(request: CreateOrderRequest): Promise<OrderDetail> {
  const response = await apiClient.post<Result<OrderDetail>>('/orders', request);
  return response.data.data;
}

async function fetchOrder(id: number): Promise<OrderDetail> {
  const response = await apiClient.get<Result<OrderDetail>>(`/orders/${id}`);
  return response.data.data;
}

async function fetchInvoice(id: number): Promise<InvoiceDetail> {
  const response = await apiClient.get<Result<InvoiceDetail>>(`/orders/${id}/invoice`);
  return response.data.data;
}

async function rollbackOrder({
  id,
  request,
}: {
  id: number;
  request: RollbackOrderRequest;
}): Promise<OrderDetail> {
  const response = await apiClient.post<Result<OrderDetail>>(`/orders/${id}/rollback`, request);
  return response.data.data;
}

export function useOrders(params: ListOrdersParams) {
  return useQuery({
    queryKey: ['orders', params],
    queryFn: () => fetchOrders(params),
  });
}

export function useOrder(id: number) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => fetchOrder(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useInvoice(id: number) {
  return useQuery({
    queryKey: ['orders', id, 'invoice'],
    queryFn: () => fetchInvoice(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useCreateOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createOrder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
  });
}

export function useRollbackOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: rollbackOrder,
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      queryClient.invalidateQueries({ queryKey: ['orders', order.id] });
      queryClient.invalidateQueries({ queryKey: ['orders', order.id, 'invoice'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });
}
