import { type ReactElement } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  isAuthenticated?: boolean;
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', isAuthenticated = false }: CustomRenderOptions = {}
) {
  const queryClient = createTestQueryClient();

  // 预设 localStorage 以模拟认证状态
  if (isAuthenticated) {
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', role: 'USER' }));
  }

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <MemoryRouter initialEntries={[route]}>
            {children}
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    );
  }

  return render(ui, { wrapper: Wrapper });
}

/**
 * 创建带 QueryClient + AuthProvider + MemoryRouter 的 wrapper
 * 用于 render(<Component />, { wrapper: createWrapper() }) 模式
 */
export function createWrapper(options: { route?: string } = {}) {
  const queryClient = createTestQueryClient();
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <MemoryRouter initialEntries={[options.route || '/']}>
            {children}
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    );
  };
}

/**
 * 简单 wrapper：只有 QueryClient + AuthProvider（无 Router）
 * 用于不需要路由的组件测试
 */
export function createSimpleWrapper() {
  const queryClient = createTestQueryClient();
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          {children}
        </AuthProvider>
      </QueryClientProvider>
    );
  };
}
