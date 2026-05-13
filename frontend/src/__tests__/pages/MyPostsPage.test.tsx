import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import MyPostsPage from '../../pages/MyPostsPage';
import * as userApi from '../../api/user';

vi.mock('../../api/user');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('MyPostsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));
  });

  const createWrapper = (initialEntries: string[] = ['/dashboard/posts']) => {
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

  it('应该渲染我的帖子列表', async () => {
    vi.mocked(userApi.getMyPosts).mockResolvedValue([
      {
        id: 1,
        title: 'My Post 1',
        content: 'content',
        authorId: 1,
        authorUsername: 'testuser',
        createdAt: '2024-01-02T10:00:00',
        updatedAt: '2024-01-02T10:00:00',
        viewCount: 0,
        commentCount: 2,
        status: 'PUBLISHED',
      },
    ]);

    render(<MyPostsPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('My Post 1')).toBeInTheDocument();
    });
  });

  it('点击帖子应跳转到帖子详情页', async () => {
    const user = userEvent.setup();

    vi.mocked(userApi.getMyPosts).mockResolvedValue([
      {
        id: 123,
        title: 'Target Post',
        content: 'content',
        authorId: 1,
        authorUsername: 'testuser',
        createdAt: '2024-01-02T10:00:00',
        updatedAt: '2024-01-02T10:00:00',
        viewCount: 0,
        commentCount: 0,
        status: 'PUBLISHED',
      },
    ]);

    render(<MyPostsPage />, { wrapper: createWrapper() });

    const row = await screen.findByText('Target Post');
    await user.click(row);

    expect(mockNavigate).toHaveBeenCalledWith('/posts/123');
  });

  it('点击返回 Dashboard 应导航到 /dashboard', async () => {
    const user = userEvent.setup();
    vi.mocked(userApi.getMyPosts).mockResolvedValue([]);

    render(<MyPostsPage />, { wrapper: createWrapper() });

    const btn = await screen.findByRole('button', { name: /返回 Dashboard/ });
    await user.click(btn);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });
});
