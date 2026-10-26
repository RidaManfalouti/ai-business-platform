import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { CurrentUser, LoginRequest, RegisterRequest, AuthResponse, UserRole } from '@/types/auth';
import { authApi } from '@/api/authApi';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/api/client';
import { extractRoleFromToken, isTokenExpired } from '@/utils/jwt';

interface AuthContextType {
  user: CurrentUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  logout: () => void;
  refreshUser: () => Promise<void>;
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
  }, []);

  const refreshUser = useCallback(async () => {
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const me = await authApi.getMe();
      // Ensure role matches verified backend role
      const normalizedRole: UserRole = me.role === 'ADMIN' ? 'ADMIN' : 'USER';
      const currentUser: CurrentUser = {
        ...me,
        role: normalizedRole,
      };
      setUser(currentUser);
      try {
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
      } catch {
        // ignore
      }
    } catch {
      // If getMe fails (e.g. 401), discard session
      logout();
    } finally {
      setIsLoading(false);
    }
  }, [token, logout]);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
    const authData = await authApi.login(credentials);
    const roleFromToken = extractRoleFromToken(authData.token);
    const role: UserRole = authData.role === 'ADMIN' || roleFromToken === 'ADMIN' ? 'ADMIN' : 'USER';

    const currentUser: CurrentUser = {
      userId: String(authData.userId),
      username: authData.username,
      email: authData.email,
      role,
    };

    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, authData.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    } catch {
      // ignore
    }

    setToken(authData.token);
    setUser(currentUser);
    return authData;
  };

  const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    const authData = await authApi.register(data);
    const roleFromToken = extractRoleFromToken(authData.token);
    const role: UserRole = authData.role === 'ADMIN' || roleFromToken === 'ADMIN' ? 'ADMIN' : 'USER';

    const currentUser: CurrentUser = {
      userId: String(authData.userId),
      username: authData.username,
      email: authData.email,
      role,
    };

    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, authData.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    } catch {
      // ignore
    }

    setToken(authData.token);
    setUser(currentUser);
    return authData;
  };

  const isAuthenticated = !!token && !!user && !isTokenExpired(token);
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
        isLoading,
        login,
        register,
        logout,
        refreshUser,
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
