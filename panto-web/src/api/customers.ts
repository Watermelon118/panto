import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateCustomerRequest,
  Customer,
  CustomerPage,
  UpdateCustomerRequest,
} from '../types/customer';
import { apiClient } from './client';

interface ListCustomersParams {
  keyword?: string;
  active?: boolean;
  page: number;
  size: number;
}

async function fetchCustomers(params: ListCustomersParams): Promise<CustomerPage> {
  const response = await apiClient.get<Result<CustomerPage>>('/customers', { params });
  return response.data.data;
}

async function fetchCustomer(id: number): Promise<Customer> {
  const response = await apiClient.get<Result<Customer>>(`/customers/${id}`);
  return response.data.data;
}

async function createCustomer(request: CreateCustomerRequest): Promise<Customer> {
  const response = await apiClient.post<Result<Customer>>('/customers', request);
  return response.data.data;
}

async function updateCustomer(id: number, request: UpdateCustomerRequest): Promise<Customer> {
  const response = await apiClient.put<Result<Customer>>(`/customers/${id}`, request);
  return response.data.data;
}

async function updateCustomerStatus(id: number, active: boolean): Promise<Customer> {
  const response = await apiClient.patch<Result<Customer>>(`/customers/${id}/status`, { active });
  return response.data.data;
}

export function useCustomers(params: ListCustomersParams) {
  return useQuery({
    queryKey: ['customers', params],
    queryFn: () => fetchCustomers(params),
  });
}

export function useCustomer(id: number) {
  return useQuery({
    queryKey: ['customers', id],
    queryFn: () => fetchCustomer(id),
  });
}

export function useCreateCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createCustomer,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['customers'] }),
  });
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateCustomerRequest }) =>
      updateCustomer(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['customers'] }),
  });
}

export function useUpdateCustomerStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      updateCustomerStatus(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['customers'] }),
  });
}
