import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import DashboardPage from '../../pages/DashboardPage';
import * as notificationApi from '../../api/notification';
import * as userApi from '../../api/user';

vi.mock('../../api/notification');
vi.mock('../../api/user');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));
  });

  const createWrapper = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return function Wrapper({ children }: { children: React.ReactNode }) {
      return (
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <AuthProvider>{children}</AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      );
    };
  };

  it('应该渲染我的帖子与我的评论列表', async () => {
    vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);

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

    render(<DashboardPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('My Post 1')).toBeInTheDocument();
      expect(screen.getByText('My Comment 1')).toBeInTheDocument();
      expect(screen.getByText('来自：My Post 1')).toBeInTheDocument();
    });
  });

  it('点击评论应跳转到对应帖子详情页', async () => {
    const user = userEvent.setup();

    vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);
    vi.mocked(userApi.getMyPosts).mockResolvedValue([]);
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

    render(<DashboardPage />, { wrapper: createWrapper() });

    const commentRow = await screen.findByText('Jump Comment');
    await user.click(commentRow);

    expect(mockNavigate).toHaveBeenCalledWith('/posts/123');
  });

  it('点击查看全部帖子应跳转到我的帖子页面', async () => {
    const user = userEvent.setup();

    vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);
    vi.mocked(userApi.getMyPosts).mockResolvedValue([]);
    vi.mocked(userApi.getMyComments).mockResolvedValue([]);

    render(<DashboardPage />, { wrapper: createWrapper() });

    const btn = await screen.findByRole('button', { name: '查看全部帖子' });
    await user.click(btn);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/posts');
  });

  it('点击查看所有评论应跳转到我的评论页面', async () => {
    const user = userEvent.setup();

    vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);
    vi.mocked(userApi.getMyPosts).mockResolvedValue([]);
    vi.mocked(userApi.getMyComments).mockResolvedValue([]);

    render(<DashboardPage />, { wrapper: createWrapper() });

    const btn = await screen.findByRole('button', { name: '查看所有评论' });
    await user.click(btn);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/comments');
  });
});
