import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import LoginPage from '../pages/LoginPage';
import client from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter>
      <AuthProvider>{children}</AuthProvider>
    </MemoryRouter>
  );

  it('should render login form', () => {
    render(<LoginPage />, { wrapper });

    expect(screen.getByText('用户登录')).toBeInTheDocument();
    expect(screen.getByLabelText('用户名')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
  });

  it('should show validation errors for empty fields', async () => {
    const user = userEvent.setup();
    render(<LoginPage />, { wrapper });

    const submitButton = screen.getByRole('button', { name: '登录' });
    await user.click(submitButton);

    // HTML5 required validation should prevent submission
    const usernameInput = screen.getByLabelText('用户名');
    const passwordInput = screen.getByLabelText('密码');

    expect(usernameInput).toBeInvalid();
    expect(passwordInput).toBeInvalid();
  });

  it('should call login API on form submit', async () => {
    const user = userEvent.setup();
    vi.mocked(client.post).mockResolvedValue({
      data: { accessToken: 'test-token' },
    } as any);

    render(<LoginPage />, { wrapper });

    await user.type(screen.getByLabelText('用户名'), 'testuser');
    await user.type(screen.getByLabelText('密码'), 'password123');
    await user.click(screen.getByRole('button', { name: '登录' }));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/auth/login', {
        username: 'testuser',
        password: 'password123',
      });
    });
  });

  it('should display error message on login failure', async () => {
    const user = userEvent.setup();
    vi.mocked(client.post).mockRejectedValue({
      response: { data: { message: '用户名或密码错误' } },
    } as any);

    render(<LoginPage />, { wrapper });

    await user.type(screen.getByLabelText('用户名'), 'testuser');
    await user.type(screen.getByLabelText('密码'), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: '登录' }));

    await waitFor(() => {
      expect(screen.getByText('用户名或密码错误')).toBeInTheDocument();
    });
  });

  it('should disable submit button while loading', async () => {
    const user = userEvent.setup();
    vi.mocked(client.post).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(<LoginPage />, { wrapper });

    await user.type(screen.getByLabelText('用户名'), 'testuser');
    await user.type(screen.getByLabelText('密码'), 'password123');

    const submitButton = screen.getByRole('button', { name: '登录' });
    await user.click(submitButton);

    await waitFor(() => {
      expect(submitButton).toBeDisabled();
    });
  });
});
