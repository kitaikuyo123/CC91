import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import NotificationBell from '../../components/NotificationBell';
import * as notificationApi from '../../api/notification';

// Mock API
vi.mock('../../api/notification');

// Mock AuthContext
vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: true }),
}));

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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

  describe('Rendering', () => {
    it('should render bell icon', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });
    });

    it('should display unread count badge', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(5);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('5')).toBeInTheDocument();
      });
    });

    it('should show 99+ for large unread counts', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(150);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('99+')).toBeInTheDocument();
      });
    });

    it('should not show badge when no unread notifications', async () => {
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.queryByText('0')).not.toBeInTheDocument();
      });
    });
  });

  describe('Dropdown', () => {
    it('should open dropdown when clicking bell', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('通知')).toBeInTheDocument();
        expect(screen.getByText('查看全部')).toBeInTheDocument();
      });
    });

    it('should display recent notifications in dropdown', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('New comment')).toBeInTheDocument();
        expect(screen.getByText('New like')).toBeInTheDocument();
      });
    });

    it('should show empty state when no notifications', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(0);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue([]);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('暂无通知')).toBeInTheDocument();
      });
    });

    it('should close dropdown when clicking overlay', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('查看全部')).toBeInTheDocument();
      });

      // Click overlay to close
      const bellContainer = screen.getByLabelText('通知').parentElement;
      const overlay = bellContainer!.querySelector('[style*="position: fixed"]');
      if (overlay) await user.click(overlay as HTMLElement);

      await waitFor(() => {
        expect(screen.queryByText('查看全部')).not.toBeInTheDocument();
      });
    });
  });

  describe('Navigation', () => {
    it('should navigate to notifications page when clicking view all', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('查看全部')).toBeInTheDocument();
      });

      await user.click(screen.getByText('查看全部'));

      expect(mockNavigate).toHaveBeenCalledWith('/notifications');
    });

    it('should navigate to post when clicking notification', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
      vi.mocked(notificationApi.markAsRead).mockResolvedValue({} as any);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('New comment')).toBeInTheDocument();
      });

      await user.click(screen.getByText('New comment'));

      expect(mockNavigate).toHaveBeenCalledWith('/posts/1');
    });
  });

  describe('Mark as read', () => {
    it('should mark notification as read when clicking unread notification', async () => {
      const user = userEvent.setup();
      vi.mocked(notificationApi.getUnreadCount).mockResolvedValue(2);
      vi.mocked(notificationApi.getNotifications).mockResolvedValue(mockNotifications);
      vi.mocked(notificationApi.markAsRead).mockResolvedValue({} as any);

      render(<NotificationBell />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
      });

      const bell = screen.getByLabelText('通知');
      await user.click(bell);

      await waitFor(() => {
        expect(screen.getByText('New comment')).toBeInTheDocument();
      });

      await user.click(screen.getByText('New comment'));

      expect(notificationApi.markAsRead).toHaveBeenCalled();
    });
  });
});
