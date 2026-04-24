import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Result } from '../types/auth';
import type {
  CreateUserRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  User,
  UserPage,
} from '../types/user';
import { apiClient } from './client';

interface ListUsersParams {
  page: number;
  size: number;
}

async function fetchUsers(params: ListUsersParams): Promise<UserPage> {
  const response = await apiClient.get<Result<UserPage>>('/users', { params });
  return response.data.data;
}

async function fetchUser(id: number): Promise<User> {
  const response = await apiClient.get<Result<User>>(`/users/${id}`);
  return response.data.data;
}

async function createUser(request: CreateUserRequest): Promise<User> {
  const response = await apiClient.post<Result<User>>('/users', request);
  return response.data.data;
}

async function updateUser(id: number, request: UpdateUserRequest): Promise<User> {
  const response = await apiClient.put<Result<User>>(`/users/${id}`, request);
  return response.data.data;
}

async function updateUserStatus(id: number, active: boolean): Promise<User> {
  const response = await apiClient.patch<Result<User>>(`/users/${id}/status`, { active });
  return response.data.data;
}

async function resetUserPassword(id: number, request: ResetPasswordRequest): Promise<User> {
  const response = await apiClient.post<Result<User>>(`/users/${id}/reset-password`, request);
  return response.data.data;
}

export function useUsers(params: ListUsersParams) {
  return useQuery({
    queryKey: ['users', params],
    queryFn: () => fetchUsers(params),
  });
}

export function useUser(id: number) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => fetchUser(id),
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateUserRequest }) =>
      updateUser(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      updateUserStatus(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}

export function useResetUserPassword() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: ResetPasswordRequest }) =>
      resetUserPassword(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
}
