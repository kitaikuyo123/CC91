import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import ProfilePage from '../../pages/ProfilePage';
import * as userApi from '../../api/user';

// Mock the user API
vi.mock('../../api/user');

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  // 每次调用创建新的 wrapper 和 queryClient，避免缓存污染
  const createWrapper = () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/profile/testuser']}>
          <AuthProvider>
            <Routes>
              <Route path="/profile/:username" element={children} />
              <Route path="/profile/edit" element={<div>Edit Page</div>} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  const mockProfile = {
    username: 'testuser',
    email: 'test@example.com',
    avatarUrl: null,
    bio: '这是我的个人签名',
    location: '北京',
    website: 'https://example.com',
    createdAt: '2024-01-01T00:00:00',
  };

  describe('正常渲染用户资料', () => {
    it('should display user profile data', async () => {
      vi.mocked(userApi.getUserProfile).mockResolvedValue(mockProfile);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('testuser')).toBeInTheDocument();
        expect(screen.getByText('这是我的个人签名')).toBeInTheDocument();
        expect(screen.getByText('北京')).toBeInTheDocument();
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
      });

      const websiteLink = screen.getByText('https://example.com');
      expect(websiteLink).toBeInTheDocument();
      expect(websiteLink).toHaveAttribute('href', 'https://example.com');
      expect(websiteLink).toHaveAttribute('target', '_blank');
    });
  });

  describe('显示头像', () => {
    it('should display avatar image when avatarUrl is provided', async () => {
      const profileWithAvatar = { ...mockProfile, avatarUrl: 'https://example.com/avatar.jpg' };
      vi.mocked(userApi.getUserProfile).mockResolvedValue(profileWithAvatar);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        const avatar = screen.getByAltText('testuser 的头像');
        expect(avatar).toBeInTheDocument();
        expect(avatar).toHaveAttribute('src', 'https://example.com/avatar.jpg');
      });
    });

    it('should display username initial when no avatarUrl', async () => {
      vi.mocked(userApi.getUserProfile).mockResolvedValue(mockProfile);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('T')).toBeInTheDocument(); // First letter of 'testuser' uppercase
      });
    });
  });

  describe('自己的资料显示编辑按钮', () => {
    it('should show edit button when viewing own profile', async () => {
      // 设置当前登录用户为 testuser
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));

      vi.mocked(userApi.getUserProfile).mockResolvedValue(mockProfile);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('编辑资料')).toBeInTheDocument();
      });
    });

    it('should not show edit button when viewing others profile', async () => {
      // 设置当前登录用户为 otheruser，查看 testuser 的资料
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('user', JSON.stringify({ username: 'otheruser', email: 'other@example.com' }));

      vi.mocked(userApi.getUserProfile).mockResolvedValue(mockProfile);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.queryByText('编辑资料')).not.toBeInTheDocument();
      });
    });
  });

  describe('加载中状态', () => {
    it('should show loading state initially', () => {
      vi.mocked(userApi.getUserProfile).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      render(<ProfilePage />, { wrapper: createWrapper() });

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('用户不存在时的错误提示', () => {
    it('should show error message when API call fails', async () => {
      vi.mocked(userApi.getUserProfile).mockRejectedValue({
        response: { data: { message: '用户不存在' } },
      });

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('用户不存在')).toBeInTheDocument();
      });
    });

    it('should show generic error message when no error message in response', async () => {
      vi.mocked(userApi.getUserProfile).mockRejectedValue({});

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        // 组件在没有详细错误消息时显示"用户不存在"
        expect(screen.getByText('用户不存在')).toBeInTheDocument();
      });
    });
  });

  describe('无详细信息时的占位', () => {
    it('should show placeholder when no detailed info', async () => {
      const emptyProfile = {
        username: 'testuser',
        email: 'test@example.com',
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: '2024-01-01T00:00:00',
      };
      vi.mocked(userApi.getUserProfile).mockResolvedValue(emptyProfile);

      render(<ProfilePage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('暂无详细信息')).toBeInTheDocument();
      });
    });
  });
});
