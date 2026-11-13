import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import {
  CurrentUser,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserRole,
  TenantMembership,
  TenantRole,
  Tenant,
} from '@/types/auth';
import { authApi } from '@/api/authApi';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/api/client';
import { extractRoleFromToken, extractTenantFromToken, isTokenExpired } from '@/utils/jwt';
import { queryClient } from '@/queryClient';

interface AuthContextType {
  user: CurrentUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  activeTenantId: string | null;
  activeTenantSlug: string | null;
  activeTenantRole: TenantRole | null;
  availableTenants: TenantMembership[];
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  switchTenant: (tenantId: string) => Promise<void>;
  createTenant: (name: string, slug?: string) => Promise<Tenant>;
  loadTenants: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => {
    try {
      const stored = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (stored && !isTokenExpired(stored)) {
        return stored;
      }
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      return null;
    } catch {
      return null;
    }
  });

  const [user, setUser] = useState<CurrentUser | null>(() => {
    try {
      const stored = localStorage.getItem(USER_STORAGE_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [availableTenants, setAvailableTenants] = useState<TenantMembership[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(() => {
    try {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
    } catch {
      // ignore
    }
    setToken(null);
    setUser(null);
    setAvailableTenants([]);
    queryClient.clear();
  }, []);

  const loadTenants = useCallback(async () => {
    try {
      const list = await authApi.getMyTenants();
      setAvailableTenants(list);
    } catch {
      setAvailableTenants([]);
    }
  }, []);

  const refreshUser = useCallback(async () => {
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const me = await authApi.getMe();
      const tenantInfo = extractTenantFromToken(token);
      const normalizedRole: UserRole = me.role === 'ADMIN' ? 'ADMIN' : 'USER';
      const currentUser: CurrentUser = {
        ...me,
        role: normalizedRole,
        activeTenantId: tenantInfo.tenantId,
        activeTenantSlug: tenantInfo.tenantSlug,
        activeTenantRole: tenantInfo.tenantRole,
      };
      setUser(currentUser);
      try {
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
      } catch {
        // ignore
      }
      await loadTenants();
    } catch {
      // If getMe fails (e.g. 401), discard session
      logout();
    } finally {
      setIsLoading(false);
    }
  }, [token, logout, loadTenants]);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
    const authData = await authApi.login(credentials);
    const roleFromToken = extractRoleFromToken(authData.token);
    const tenantInfo = extractTenantFromToken(authData.token);
    const role: UserRole = authData.role === 'ADMIN' || roleFromToken === 'ADMIN' ? 'ADMIN' : 'USER';

    const currentUser: CurrentUser = {
      userId: String(authData.userId),
      username: authData.username,
      email: authData.email,
      role,
      activeTenantId: tenantInfo.tenantId,
      activeTenantSlug: tenantInfo.tenantSlug,
      activeTenantRole: tenantInfo.tenantRole,
    };

    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, authData.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    } catch {
      // ignore
    }

    queryClient.clear();
    setToken(authData.token);
    setUser(currentUser);
    await loadTenants();
    return authData;
  };

  const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    const authData = await authApi.register(data);
    const roleFromToken = extractRoleFromToken(authData.token);
    const tenantInfo = extractTenantFromToken(authData.token);
    const role: UserRole = authData.role === 'ADMIN' || roleFromToken === 'ADMIN' ? 'ADMIN' : 'USER';

    const currentUser: CurrentUser = {
      userId: String(authData.userId),
      username: authData.username,
      email: authData.email,
      role,
      activeTenantId: tenantInfo.tenantId,
      activeTenantSlug: tenantInfo.tenantSlug,
      activeTenantRole: tenantInfo.tenantRole,
    };

    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, authData.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    } catch {
      // ignore
    }

    queryClient.clear();
    setToken(authData.token);
    setUser(currentUser);
    await loadTenants();
    return authData;
  };

  const switchTenant = async (tenantId: string): Promise<void> => {
    const res = await authApi.switchTenant({ tenantId });
    const newToken = res.token;
    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
    } catch {
      // ignore
    }
    setToken(newToken);

    // Evacuate all cached queries across the entire app so no cross-tenant data lingers in memory
    queryClient.clear();

    const tenantInfo = extractTenantFromToken(newToken);
    setUser((prev) => {
      if (!prev) return null;
      const updated: CurrentUser = {
        ...prev,
        activeTenantId: tenantInfo.tenantId,
        activeTenantSlug: tenantInfo.tenantSlug,
        activeTenantRole: tenantInfo.tenantRole,
      };
      try {
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updated));
      } catch {
        // ignore
      }
      return updated;
    });

    await loadTenants();
  };

  const createTenant = async (name: string, slug?: string): Promise<Tenant> => {
    const newTenant = await authApi.createTenant({ name, slug });
    await loadTenants();
    await switchTenant(newTenant.id);
    return newTenant;
  };

  const isAuthenticated = !!token && !!user && !isTokenExpired(token);
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  const activeTenantId = user?.activeTenantId ?? null;
  const activeTenantSlug = user?.activeTenantSlug ?? null;
  const activeTenantRole = user?.activeTenantRole ?? null;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
        isLoading,
        activeTenantId,
        activeTenantSlug,
        activeTenantRole,
        availableTenants,
        login,
        register,
        logout,
        refreshUser,
        switchTenant,
        createTenant,
        loadTenants,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
