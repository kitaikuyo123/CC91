import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ForgotPasswordPage from '../../pages/ForgotPasswordPage';
import * as authApi from '../../api/auth';

// Mock API
vi.mock('../../api/auth');

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('ForgotPasswordPage', () => {
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
    it('should render the form', () => {
      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      expect(screen.getByText('忘记密码')).toBeInTheDocument();
      expect(screen.getByLabelText('邮箱')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '发送验证码' })).toBeInTheDocument();
      expect(screen.getByText('返回登录')).toBeInTheDocument();
    });
  });

  describe('Form submission - success', () => {
    it('should show success message after submission', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.forgotPassword).mockResolvedValue({} as any);

      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      await user.type(emailInput, 'test@example.com');
      await user.click(screen.getByRole('button', { name: '发送验证码' }));

      await waitFor(() => {
        expect(screen.getByText('验证码已发送')).toBeInTheDocument();
        expect(screen.getByText(/我们已向 test@example.com 发送了验证码/)).toBeInTheDocument();
      });
    });

    it('should navigate to reset password page when clicking button', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.forgotPassword).mockResolvedValue({} as any);

      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      await user.type(emailInput, 'test@example.com');
      await user.click(screen.getByRole('button', { name: '发送验证码' }));

      await waitFor(() => {
        expect(screen.getByText('前往重置密码')).toBeInTheDocument();
      });

      await user.click(screen.getByText('前往重置密码'));

      expect(mockNavigate).toHaveBeenCalledWith('/reset-password?email=test%40example.com');
    });
  });

  describe('Form submission - error', () => {
    it('should show error message on failure', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.forgotPassword).mockRejectedValue({
        response: { data: { message: 'Network error' } },
      });

      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      await user.type(emailInput, 'test@example.com');
      await user.click(screen.getByRole('button', { name: '发送验证码' }));

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('should show default error message', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.forgotPassword).mockRejectedValue({});

      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      await user.type(emailInput, 'test@example.com');
      await user.click(screen.getByRole('button', { name: '发送验证码' }));

      await waitFor(() => {
        expect(screen.getByText('发送验证码失败，请重试')).toBeInTheDocument();
      });
    });
  });

  describe('Loading state', () => {
    it('should disable button and show spinner during submission', async () => {
      const user = userEvent.setup();
      vi.mocked(authApi.forgotPassword).mockImplementation(
        () => new Promise(() => {})
      );

      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      await user.type(emailInput, 'test@example.com');
      const submitButton = screen.getByRole('button', { name: '发送验证码' });
      await user.click(submitButton);

      await waitFor(() => {
        // 等待按钮进入 loading 状态 - 按钮应该被禁用
        const buttons = screen.getAllByRole('button');
        const button = buttons.find(b => b.getAttribute('type') === 'submit');
        expect(button).toBeDisabled();
      });
    });
  });

  describe('Validation', () => {
    it('should require email input', async () => {
      const user = userEvent.setup();
      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      expect(emailInput).toBeRequired();
    });

    it('should require valid email format', async () => {
      const user = userEvent.setup();
      render(<ForgotPasswordPage />, { wrapper: createWrapper() });

      const emailInput = screen.getByLabelText('邮箱');
      expect(emailInput).toHaveAttribute('type', 'email');
    });
  });
});
