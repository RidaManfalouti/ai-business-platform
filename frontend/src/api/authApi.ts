import { apiClient } from './client';
import {
  AuthResponse,
  CurrentUser,
  LoginRequest,
  RegisterRequest,
  UpdateUserRoleRequest,
  UserSummary,
} from '@/types/auth';

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/auth/login', request);
    return response.data;
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/auth/register', request);
    return response.data;
  },

  getMe: async (): Promise<CurrentUser> => {
    const response = await apiClient.get<CurrentUser>('/api/users/me');
    return response.data;
  },

  getAllUsers: async (): Promise<UserSummary[]> => {
    const response = await apiClient.get<UserSummary[]>('/api/users');
    return response.data;
  },

  updateUserRole: async (id: number, request: UpdateUserRoleRequest): Promise<UserSummary> => {
    const response = await apiClient.patch<UserSummary>(`/api/users/${id}/role`, request);
    return response.data;
  },
};
