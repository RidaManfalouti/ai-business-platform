export type UserRole = 'USER' | 'ADMIN';
export type TenantRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'SELLER' | 'VIEWER';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED';
}

export interface TenantMembership {
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  role: TenantRole;
  status: 'ACTIVE' | 'INVITED' | 'SUSPENDED';
}

export interface TenantMember {
  id: string;
  userId: number;
  email: string;
  username: string;
  role: TenantRole;
  status: string;
  createdAt: string;
}

export interface CreateTenantRequest {
  name: string;
  slug?: string;
}

export interface SwitchTenantRequest {
  tenantId: string;
}

export interface SwitchTenantResponse {
  token: string;
  tokenType: string;
  activeTenantId: string;
  activeTenantSlug: string;
  activeTenantRole: TenantRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  role: UserRole;
  activeTenantId?: string;
  activeTenantSlug?: string;
  activeTenantRole?: TenantRole;
}

export interface CurrentUser {
  email: string;
  username: string;
  role: UserRole;
  userId: string;
  activeTenantId?: string;
  activeTenantSlug?: string;
  activeTenantRole?: TenantRole;
}

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export interface UpdateUserRoleRequest {
  role: UserRole;
}

export interface DecodedJwt {
  sub?: string;
  roles?: string[];
  tenant_id?: string;
  tenant_slug?: string;
  tenant_role?: TenantRole;
  iat?: number;
  exp?: number;
  [key: string]: unknown;
}
