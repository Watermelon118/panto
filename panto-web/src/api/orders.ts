import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateOrderRequest,
  OrderDetail,
  OrderPage,
  OrderStatus,
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

export function useOrders(params: ListOrdersParams) {
  return useQuery({
    queryKey: ['orders', params],
    queryFn: () => fetchOrders(params),
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
