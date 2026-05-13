import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import MyCommentsPage from '../../pages/MyCommentsPage';
import * as userApi from '../../api/user';

vi.mock('../../api/user');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('MyCommentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));
  });

  const createWrapper = (initialEntries: string[] = ['/dashboard/comments']) => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return (
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={initialEntries}>
            <AuthProvider>{children}</AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
    };
  };

  it('应该渲染我的评论列表', async () => {
    vi.mocked(userApi.getMyComments).mockResolvedValue([
      {
        id: 11,
        postId: 1,
        postTitle: 'My Post 1',
        content: 'My Comment 1',
        parentId: null,
        createdAt: '2024-01-03T10:00:00',
        status: 'PUBLISHED',
      },
    ]);

    render(<MyCommentsPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('My Comment 1')).toBeInTheDocument();
      expect(screen.getByText('来自：My Post 1')).toBeInTheDocument();
    });
  });

  it('点击评论应跳转到对应帖子详情页', async () => {
    const user = userEvent.setup();

    vi.mocked(userApi.getMyComments).mockResolvedValue([
      {
        id: 99,
        postId: 123,
        postTitle: 'Target Post',
        content: 'Jump Comment',
        parentId: null,
        createdAt: '2024-01-03T10:00:00',
        status: 'PUBLISHED',
      },
    ]);

    render(<MyCommentsPage />, { wrapper: createWrapper() });

    const row = await screen.findByText('Jump Comment');
    await user.click(row);

    expect(mockNavigate).toHaveBeenCalledWith('/posts/123');
  });

  it('点击返回 Dashboard 应导航到 /dashboard', async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.getMyComments).mockResolvedValue([]);

    render(<MyCommentsPage />, { wrapper: createWrapper() });

    const btn = await screen.findByRole('button', { name: /返回 Dashboard/ });
    await user.click(btn);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });
});
