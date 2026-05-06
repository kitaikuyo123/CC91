import client from './client';
import type { ApiResponse } from './types';

/**
 * Login response type
 * Backend returns this directly (NOT wrapped in ApiResponse)
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/**
 * Register response type
 */
export interface RegisterResponse {
  username: string;
  email: string;
}

/**
 * Forgot password request type
 */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * Reset password request type
 */
export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

/**
 * Login
 * POST /api/auth/login
 */
export async function login(username: string, password: string): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/auth/login', {
    username,
    password,
  });
  return response.data;
}

/**
 * Register
 * POST /api/auth/register
 */
export async function register(username: string, email: string, password: string): Promise<RegisterResponse> {
  const response = await client.post<RegisterResponse>('/auth/register', {
    username,
    email,
    password,
  });
  return response.data;
}

/**
 * Send password reset verification code
 * POST /api/auth/forgot-password
 */
export async function forgotPassword(email: string): Promise<ApiResponse<void>> {
  const response = await client.post<ApiResponse<void>>('/auth/forgot-password', {
    email,
  });
  return response.data;
}

/**
 * Reset password
 * POST /api/auth/reset-password
 */
export async function resetPassword(email: string, code: string, newPassword: string): Promise<ApiResponse<void>> {
  const response = await client.post<ApiResponse<void>>('/auth/reset-password', {
    email,
    code,
    newPassword,
  });
  return response.data;
}

/**
 * Refresh token
 * POST /api/auth/refresh
 */
export async function refreshToken(refreshTokenValue: string): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/auth/refresh', {
    refreshToken: refreshTokenValue,
  });
  return response.data;
}

/**
 * Logout (invalidate refresh token on server)
 * POST /api/auth/logout
 */
export async function logout(): Promise<void> {
  const storedRefreshToken = localStorage.getItem('refresh_token');
  if (storedRefreshToken) {
    await client.post('/auth/logout', {
      refreshToken: storedRefreshToken,
    });
  }
}
