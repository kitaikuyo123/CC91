import client from './client';

/**
 * 登录响应类型
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

/**
 * 注册响应类型
 */
export interface RegisterResponse {
  username: string;
  email: string;
}

/**
 * API 响应包装类型
 */
export interface ApiResponse<T> {
  message: string;
  data?: T;
}

/**
 * 忘记密码请求类型
 */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * 重置密码请求类型
 */
export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

/**
 * 登录
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
 * 注册
 * POST /api/auth/register
 */
export async function register(username: string, email: string, password: string): Promise<RegisterResponse> {
  const response = await client.post<ApiResponse<RegisterResponse>>('/auth/register', {
    username,
    email,
    password,
  });
  return response.data.data || response.data as any;
}

/**
 * 发送密码重置验证码
 * POST /api/auth/forgot-password
 */
export async function forgotPassword(email: string): Promise<ApiResponse<void>> {
  const response = await client.post<ApiResponse<void>>('/auth/forgot-password', {
    email,
  });
  return response.data;
}

/**
 * 重置密码
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
 * 刷新令牌
 * POST /api/auth/refresh
 */
export async function refreshToken(refreshToken: string): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/auth/refresh', {
    refreshToken,
  });
  return response.data;
}

/**
 * 登出
 * POST /api/auth/logout
 */
export async function logout(): Promise<void> {
  await client.post('/auth/logout');
}
