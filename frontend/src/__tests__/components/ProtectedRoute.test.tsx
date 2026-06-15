import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import ProtectedRoute from '../../components/ProtectedRoute';

describe('ProtectedRoute', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/protected']}>
        <AuthProvider>
          <Routes>
            <Route path="/protected" element={<ProtectedRoute>{children}</ProtectedRoute>} />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );

  it('should redirect to login when not authenticated', () => {
    render(<div>Protected Content</div>, { wrapper });

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('should render children when authenticated', () => {
    sessionStorage.setItem('access_token', 'test-token');
    sessionStorage.setItem('user', JSON.stringify({ username: 'testuser', email: '' }));

    render(<div>Protected Content</div>, { wrapper });

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });
});
