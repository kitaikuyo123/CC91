import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../../context/AuthContext';
import ProfileEditPage from '../../pages/ProfileEditPage';
import * as userApi from '../../api/user';

// Mock the user API
vi.mock('../../api/user');

describe('ProfileEditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    // 设置当前登录用户
    localStorage.setItem('access_token', 'test-token');
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', email: 'test@example.com' }));
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
        <MemoryRouter initialEntries={['/profile/edit']}>
          <AuthProvider>
            <Routes>
              <Route path="/profile/edit" element={children} />
              <Route path="/profile/:username" element={<div>Profile Page</div>} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  const mockProfile = {
    username: 'testuser',
    email: 'test@example.com',
    avatarUrl: 'https://example.com/avatar.jpg',
    bio: '这是我的个人签名',
    location: '北京',
    website: 'https://example.com',
    createdAt: '2024-01-01T00:00:00',
  };

  describe('表单渲染和初始值加载', () => {
    it('should load and display current profile data in form', async () => {
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toHaveValue('https://example.com/avatar.jpg');
        expect(screen.getByLabelText('个人签名')).toHaveValue('这是我的个人签名');
        expect(screen.getByLabelText('所在地')).toHaveValue('北京');
        expect(screen.getByLabelText('个人网站')).toHaveValue('https://example.com');
      });
    });

    it('should show loading state initially', () => {
      vi.mocked(userApi.getMyProfile).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      expect(screen.getByText('加载中...')).toBeInTheDocument();
    });
  });

  describe('编辑并提交表单', () => {
    it('should submit updated profile data', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);
      vi.mocked(userApi.updateProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      // 等待表单加载
      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toHaveValue('https://example.com/avatar.jpg');
      });

      // 修改表单值
      const bioInput = screen.getByLabelText('个人签名');
      await user.clear(bioInput);
      await user.type(bioInput, '更新后的个人签名');

      const locationInput = screen.getByLabelText('所在地');
      await user.clear(locationInput);
      await user.type(locationInput, '上海');

      // 提交表单
      const submitButton = screen.getByRole('button', { name: '保存' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(userApi.updateProfile).toHaveBeenCalled();
        const callArgs = vi.mocked(userApi.updateProfile).mock.calls[0][0];
        expect(callArgs).toEqual({
          avatarUrl: 'https://example.com/avatar.jpg',
          bio: '更新后的个人签名',
          location: '上海',
          website: 'https://example.com',
        });
      });
    });

    it('should trim whitespace and convert empty strings to undefined', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);
      vi.mocked(userApi.updateProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toBeInTheDocument();
      });

      // 设置为只有空格的值
      const bioInput = screen.getByLabelText('个人签名');
      await user.clear(bioInput);
      await user.type(bioInput, '   ');

      const locationInput = screen.getByLabelText('所在地');
      await user.clear(locationInput);
      // 不输入任何内容，保持为空

      await user.click(screen.getByRole('button', { name: '保存' }));

      await waitFor(() => {
        const call = vi.mocked(userApi.updateProfile).mock.calls[0][0];
        expect(call.bio).toBeUndefined();
        expect(call.location).toBeUndefined();
      });
    });
  });

  describe('提交后跳转', () => {
    it('should navigate to profile page after successful update', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);
      vi.mocked(userApi.updateProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: '保存' }));

      await waitFor(() => {
        expect(screen.getByText('Profile Page')).toBeInTheDocument();
      });
    });
  });

  describe('空字段处理', () => {
    it('should handle empty profile fields', async () => {
      const emptyProfile = {
        username: 'testuser',
        email: 'test@example.com',
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: '2024-01-01T00:00:00',
      };
      vi.mocked(userApi.getMyProfile).mockResolvedValue(emptyProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toHaveValue('');
        expect(screen.getByLabelText('个人签名')).toHaveValue('');
        expect(screen.getByLabelText('所在地')).toHaveValue('');
        expect(screen.getByLabelText('个人网站')).toHaveValue('');
      });
    });

    it('should update profile from empty to filled', async () => {
      const user = userEvent.setup();
      const emptyProfile = {
        username: 'testuser',
        email: 'test@example.com',
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: '2024-01-01T00:00:00',
      };
      vi.mocked(userApi.getMyProfile).mockResolvedValue(emptyProfile);
      vi.mocked(userApi.updateProfile).mockResolvedValue(emptyProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toHaveValue('');
      });

      // 填充表单
      await user.type(screen.getByLabelText('头像 URL'), 'https://example.com/avatar.jpg');
      await user.type(screen.getByLabelText('个人签名'), '我的签名');

      await user.click(screen.getByRole('button', { name: '保存' }));

      await waitFor(() => {
        expect(userApi.updateProfile).toHaveBeenCalled();
      });
    });
  });

  describe('错误处理', () => {
    it('should display error message on update failure', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);
      vi.mocked(userApi.updateProfile).mockRejectedValue({
        response: { data: { message: '更新失败，网络错误' } },
      });

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: '保存' }));

      await waitFor(() => {
        expect(screen.getByText('更新失败，网络错误')).toBeInTheDocument();
      });
    });

    it('should disable submit button while submitting', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);
      vi.mocked(userApi.updateProfile).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: '保存' });
      await user.click(submitButton);

      await waitFor(() => {
        expect(submitButton).toBeDisabled();
      });
    });
  });

  describe('取消按钮', () => {
    it('should navigate to profile page when cancel is clicked', async () => {
      const user = userEvent.setup();
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByLabelText('头像 URL')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: '取消' }));

      await waitFor(() => {
        expect(screen.getByText('Profile Page')).toBeInTheDocument();
      });
    });
  });

  describe('字符计数', () => {
    it('should display character count for bio field', async () => {
      vi.mocked(userApi.getMyProfile).mockResolvedValue(mockProfile);

      render(<ProfileEditPage />, { wrapper: createWrapper() });

      await waitFor(() => {
        expect(screen.getByText('8/500')).toBeInTheDocument(); // '这是我的个人签名' is 8 chars
      });
    });
  });
});
