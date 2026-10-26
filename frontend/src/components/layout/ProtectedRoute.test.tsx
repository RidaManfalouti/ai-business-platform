import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import * as AuthContextModule from '@/context/AuthContext';

vi.mock('@/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('ProtectedRoute', () => {
  it('redirects to /login when user is unauthenticated', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: false,
      isAdmin: false,
      isLoading: false,
      user: null,
      token: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshUser: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route
            path="/protected"
            element={
              <ProtectedRoute>
                <div>Contenu Secret</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Écran de connexion</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Écran de connexion')).toBeInTheDocument();
    expect(screen.queryByText('Contenu Secret')).not.toBeInTheDocument();
  });

  it('renders child component when user is authenticated', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isAdmin: false,
      isLoading: false,
      user: {
        userId: '1',
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
      },
      token: 'valid.token',
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshUser: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route
            path="/protected"
            element={
              <ProtectedRoute>
                <div>Contenu Autorisé</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Contenu Autorisé')).toBeInTheDocument();
  });

  it('blocks non-admin users on adminOnly routes', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isAdmin: false,
      isLoading: false,
      user: {
        userId: '1',
        username: 'regular_user',
        email: 'user@example.com',
        role: 'USER',
      },
      token: 'valid.token',
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshUser: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/admin-route']}>
        <Routes>
          <Route
            path="/admin-route"
            element={
              <ProtectedRoute adminOnly>
                <div>Zone Administrateur</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(
      screen.getByText(/Accès restreint aux administrateurs/i)
    ).toBeInTheDocument();
    expect(screen.queryByText('Zone Administrateur')).not.toBeInTheDocument();
  });

  it('allows admin users on adminOnly routes', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isAdmin: true,
      isLoading: false,
      user: {
        userId: '2',
        username: 'admin_user',
        email: 'admin@example.com',
        role: 'ADMIN',
      },
      token: 'admin.token',
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshUser: vi.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/admin-route']}>
        <Routes>
          <Route
            path="/admin-route"
            element={
              <ProtectedRoute adminOnly>
                <div>Zone Administrateur Accessible</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Zone Administrateur Accessible')).toBeInTheDocument();
  });
});
