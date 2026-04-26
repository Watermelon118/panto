import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateProductRequest,
  Product,
  ProductPage,
  UpdateProductRequest,
} from '../types/product';
import { apiClient } from './client';

interface ListProductsParams {
  keyword?: string;
  category?: string;
  active?: boolean;
  page: number;
  size: number;
}

async function fetchProducts(params: ListProductsParams): Promise<ProductPage> {
  const response = await apiClient.get<Result<ProductPage>>('/products', { params });
  return response.data.data;
}

async function fetchProduct(id: number): Promise<Product> {
  const response = await apiClient.get<Result<Product>>(`/products/${id}`);
  return response.data.data;
}

async function createProduct(request: CreateProductRequest): Promise<Product> {
  const response = await apiClient.post<Result<Product>>('/products', request);
  return response.data.data;
}

async function updateProduct(id: number, request: UpdateProductRequest): Promise<Product> {
  const response = await apiClient.put<Result<Product>>(`/products/${id}`, request);
  return response.data.data;
}

async function updateProductStatus(id: number, active: boolean): Promise<Product> {
  const response = await apiClient.patch<Result<Product>>(`/products/${id}/status`, { active });
  return response.data.data;
}

async function fetchCategories(): Promise<string[]> {
  const response = await apiClient.get<Result<string[]>>('/products/categories');
  return response.data.data;
}

export function useProducts(params: ListProductsParams) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => fetchProducts(params),
  });
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: ['products', id],
    queryFn: () => fetchProduct(id),
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ['product-categories'],
    queryFn: fetchCategories,
    staleTime: 5 * 60_000,
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
  });
}

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateProductRequest }) =>
      updateProduct(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
  });
}

export function useUpdateProductStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      updateProductStatus(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
  });
}
