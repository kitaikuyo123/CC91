import client from './client';
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from './category';
import type { Post } from './post';

/**
 * 管理员用户信息类型
 */
export interface AdminUser {
  id: number;
  username: string;
  email: string;
  role: string;
  isLocked: boolean;
  createdAt: string;
  lockUntil?: string;
}

/**
 * 更新帖子状态请求类型
 */
export interface UpdatePostStatusRequest {
  status: string;
}

/**
 * API 响应包装类型
 */
export interface ApiResponse<T> {
  message: string;
  data?: T;
}

// ============ 版块管理 ============

/**
 * 创建版块（管理员）
 * POST /api/admin/categories
 */
export async function adminCreateCategory(data: CreateCategoryRequest): Promise<Category> {
  const response = await client.post<ApiResponse<Category>>('/admin/categories', data);
  return response.data.data || response.data as any;
}

/**
 * 更新版块（管理员）
 * PUT /api/admin/categories/{id}
 */
export async function adminUpdateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  const response = await client.put<ApiResponse<Category>>(`/admin/categories/${id}`, data);
  return response.data.data || response.data as any;
}

/**
 * 删除版块（管理员）
 * DELETE /api/admin/categories/{id}
 */
export async function adminDeleteCategory(id: number): Promise<void> {
  await client.delete(`/admin/categories/${id}`);
}

// ============ 内容审核 ============

/**
 * 获取所有帖子（可按状态筛选）
 * GET /api/admin/posts?status=
 */
export async function adminGetPosts(status?: string): Promise<Post[]> {
  const params = status ? { status } : {};
  const response = await client.get<Post[]>('/admin/posts', { params });
  return response.data;
}

/**
 * 修改帖子状态
 * PUT /api/admin/posts/{id}/status
 */
export async function adminUpdatePostStatus(id: number, status: string): Promise<ApiResponse<void>> {
  const response = await client.put<ApiResponse<void>>(`/admin/posts/${id}/status`, { status });
  return response.data;
}

/**
 * 强制删除帖子
 * DELETE /api/admin/posts/{id}
 */
export async function adminDeletePost(id: number): Promise<ApiResponse<void>> {
  const response = await client.delete<ApiResponse<void>>(`/admin/posts/${id}`);
  return response.data;
}

/**
 * 强制删除评论
 * DELETE /api/admin/comments/{id}
 */
export async function adminDeleteComment(id: number): Promise<ApiResponse<void>> {
  const response = await client.delete<ApiResponse<void>>(`/admin/comments/${id}`);
  return response.data;
}

// ============ 用户管理 ============

/**
 * 获取所有用户
 * GET /api/admin/users
 */
export async function adminGetUsers(): Promise<AdminUser[]> {
  const response = await client.get<AdminUser[]>('/admin/users');
  return response.data;
}

/**
 * 封禁用户
 * PUT /api/admin/users/{id}/ban
 */
export async function adminBanUser(id: number): Promise<ApiResponse<void>> {
  const response = await client.put<ApiResponse<void>>(`/admin/users/${id}/ban`);
  return response.data;
}

/**
 * 解封用户
 * PUT /api/admin/users/{id}/unban
 */
export async function adminUnbanUser(id: number): Promise<ApiResponse<void>> {
  const response = await client.put<ApiResponse<void>>(`/admin/users/${id}/unban`);
  return response.data;
}
