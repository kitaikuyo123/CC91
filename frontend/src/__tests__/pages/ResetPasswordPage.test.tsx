import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ResetPasswordPage from '../../pages/ResetPasswordPage';
import * as authApi from '../../api/auth';

// Mock API
vi.mock('../../api/auth');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams('email=test%40example.com'), vi.fn()],
}));

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };

  describe('Form rendering', () => {
    it('should render the form with all fields', () => {
      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      expect(screen.getByRole('heading', { name: '重置密码' })).toBeInTheDocument();
      expect(screen.getByLabelText('邮箱')).toBeInTheDocument();
      expect(screen.getByLabelText('验证码')).toBeInTheDocument();
      expect(screen.getByLabelText('新密码')).toBeInTheDocument();
      expect(screen.getByLabelText('确认密码')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: '重置密码' })).toBeInTheDocument();
    });

    it('should pre-fill email from URL params', () => {
      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
    });
  });

  describe('Form validation', () => {
    it('should show error when passwords do not match', async () => {
      const user = userEvent.setup();
      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      // Fill required fields first
      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), 'password123');
      await user.type(screen.getByLabelText('确认密码'), 'password456');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      expect(screen.getByText('两次输入的密码不一致')).toBeInTheDocument();
    });

    it('should show error when password is too short', async () => {
      const user = userEvent.setup();
      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      // Fill required fields first
      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), '12345');
      await user.type(screen.getByLabelText('确认密码'), '12345');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      expect(screen.getByText('密码长度至少为6位')).toBeInTheDocument();
    });
  });

  describe('Form submission - success', () => {
    it('should show success message after reset', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.resetPassword).mockResolvedValue({} as any);

      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), 'newpassword123');
      await user.type(screen.getByLabelText('确认密码'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      await waitFor(() => {
        expect(screen.getByText('密码重置成功')).toBeInTheDocument();
      });
    });

    it('should navigate to login page when clicking button', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.resetPassword).mockResolvedValue({} as any);

      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), 'newpassword123');
      await user.type(screen.getByLabelText('确认密码'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      await waitFor(() => {
        expect(screen.getByText('前往登录')).toBeInTheDocument();
      });

      await user.click(screen.getByText('前往登录'));

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  describe('Form submission - error', () => {
    it('should show error message on failure', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.resetPassword).mockRejectedValue({
        response: { data: { message: 'Invalid code' } },
      });

      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      await user.type(screen.getByLabelText('验证码'), 'wrongcode');
      await user.type(screen.getByLabelText('新密码'), 'newpassword123');
      await user.type(screen.getByLabelText('确认密码'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      await waitFor(() => {
        expect(screen.getByText('Invalid code')).toBeInTheDocument();
      });
    });

    it('should show default error message', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.resetPassword).mockRejectedValue({});

      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), 'newpassword123');
      await user.type(screen.getByLabelText('确认密码'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      await waitFor(() => {
        expect(screen.getByText('重置密码失败，请重试')).toBeInTheDocument();
      });
    });
  });

  describe('Loading state', () => {
    it('should disable inputs during submission', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.resetPassword).mockImplementation(
        () => new Promise(() => {})
      );

      render(<ResetPasswordPage />, { wrapper: createWrapper() });

      await user.type(screen.getByLabelText('验证码'), '123456');
      await user.type(screen.getByLabelText('新密码'), 'newpassword123');
      await user.type(screen.getByLabelText('确认密码'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '重置密码' }));

      await waitFor(() => {
        const button = screen.getByRole('button');
        expect(button).toBeDisabled();
      });
    });
  });
});
