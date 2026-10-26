export type UserRole = 'USER' | 'ADMIN';

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
}

export interface CurrentUser {
  email: string;
  username: string;
  role: UserRole;
  userId: string;
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
  iat?: number;
  exp?: number;
  [key: string]: unknown;
}
