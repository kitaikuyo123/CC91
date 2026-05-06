import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import NotificationsPage from '../../pages/NotificationsPage';
import * as notificationApi from '../../api/notification';
import { useAuth } from '../../context/AuthContext';

// Mock API
vi.mock('../../api/notification');

// Mock AuthContext
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(() => ({ isAuthenticated: true })),
}));

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as any);
  });

  const createWrapper = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };

  const mockNotifications = [
    {
      id: 1,
      userId: 1,
      type: 'COMMENT',
      title: 'New comment',
      content: 'Someone commented on your post',
      relatedId: 1,
      isRead: false,
      createdAt: '2024-01-01T10:00:00',
    },
    {
      id: 2,
      userId: 1,
      type: 'LIKE',
      title: 'New like',
      content: 'Someone liked your post',
      relatedId: 2,
      isRead: true,
      createdAt: '2024-01-02T10:00:00',
    },
  ];

  describe('Unauthenticated state', () => {
    it('should show login prompt when not authenticated', () => {
      vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false } as any);

      render(<NotificationsPage />, { wrapper: createWrapper() });

      expect(screen.getByText('请先登录查看通知')).toBeInTheDocument();
      expect(screen.getByText('前往登录')).toBeInTheDocument();
    });

    it('should navigate to login when clicking login button', async () => {
      const user = userEvent.setup();
      vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false } as any);

      render(<NotificationsPage />, { wrapper: createWrapper() });

      await user.click(screen.getByText('前往登录'));

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  describe('Authenticated state', () => {
    describe('Loading state', () => {
      it('should show loading state', () => {
        vi.mocked(notificationApi.getNotifications).mockImplementation(
          () => new Promise(() => {})
        );
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        expect(screen.getByText('加载中...')).toBeInTheDocument();
      });
    });

    describe('Notifications list', () => {
      it('should display notifications', async () => {
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('通知')).toBeInTheDocument();
          expect(screen.getByText('New comment')).toBeInTheDocument();
          expect(screen.getByText('New like')).toBeInTheDocument();
        });
      });

      it('should show empty state when no notifications', async () => {
        vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('暂无通知')).toBeInTheDocument();
        });
      });

      it('should show unread count and mark all button', async () => {
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('全部标记为已读')).toBeInTheDocument();
        });
      });

      it('should not show mark all button when no unread', async () => {
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.queryByText('全部标记为已读')).not.toBeInTheDocument();
        });
      });

      it('should mark notification as read when clicking', async () => {
        const user = userEvent.setup();
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1);
        vi.mocked(notificationApi.markAsRead).mockResolvedValue({} as any);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('New comment')).toBeInTheDocument();
        });

        const notifItem = screen.getByText('New comment').closest('li');
        await user.click(notifItem!);

        expect(notificationApi.markAsRead).toHaveBeenCalled();
      });

      it('should navigate to related post when clicking notification', async () => {
        const user = userEvent.setup();
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1);
        vi.mocked(notificationApi.markAsRead).mockResolvedValue({} as any);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('New comment')).toBeInTheDocument();
        });

        const notifItem = screen.getByText('New comment').closest('li');
        await user.click(notifItem!);

        expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
      });
    });

    describe('Mark all as read', () => {
      it('should mark all notifications as read', async () => {
        const user = userEvent.setup();
        vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
        vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(1);
        vi.mocked(notificationApi.markAllAsRead).mockResolvedValue({} as any);

        render(<NotificationsPage />, { wrapper: createWrapper() });

        await waitFor(() => {
          expect(screen.getByText('全部标记为已读')).toBeInTheDocument();
        });

        await user.click(screen.getByText('全部标记为已读'));

        expect(notificationApi.markAllAsRead).toHaveBeenCalled();
      });
    });
  });
});
