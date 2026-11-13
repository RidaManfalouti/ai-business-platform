import { apiClient } from './client';
import {
  AuthResponse,
  CreateTenantRequest,
  CurrentUser,
  LoginRequest,
  RegisterRequest,
  SwitchTenantRequest,
  SwitchTenantResponse,
  Tenant,
  TenantMember,
  TenantMembership,
  TenantRole,
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

  // Multi-tenant endpoints
  createTenant: async (request: CreateTenantRequest): Promise<Tenant> => {
    const response = await apiClient.post<Tenant>('/api/auth/tenants', request);
    return response.data;
  },

  getMyTenants: async (): Promise<TenantMembership[]> => {
    const response = await apiClient.get<TenantMembership[]>('/api/auth/tenants/me');
    return response.data;
  },

  switchTenant: async (request: SwitchTenantRequest): Promise<SwitchTenantResponse> => {
    const response = await apiClient.post<SwitchTenantResponse>('/api/auth/switch-tenant', request);
    return response.data;
  },

  getTenantMembers: async (tenantId: string): Promise<TenantMember[]> => {
    const response = await apiClient.get<TenantMember[]>(`/api/auth/tenants/${tenantId}/members`);
    return response.data;
  },

  addTenantMember: async (
    tenantId: string,
    member: { email: string; role: TenantRole }
  ): Promise<TenantMember> => {
    const response = await apiClient.post<TenantMember>(`/api/auth/tenants/${tenantId}/members`, member);
    return response.data;
  },

  updateTenantMemberRole: async (
    tenantId: string,
    userId: number,
    role: TenantRole
  ): Promise<TenantMember> => {
    const response = await apiClient.put<TenantMember>(
      `/api/auth/tenants/${tenantId}/members/${userId}/role`,
      { role }
    );
    return response.data;
  },

  removeTenantMember: async (tenantId: string, userId: number): Promise<void> => {
    await apiClient.delete(`/api/auth/tenants/${tenantId}/members/${userId}`);
  },
};
