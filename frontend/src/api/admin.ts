import client from './client';
import type { ApiResponse } from './types';
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from './category';
import type { Post } from './post';

/**
 * Admin user info type
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
 * Update post status request type
 */
export interface UpdatePostStatusRequest {
  status: string;
}

// ============ Category Management ============

/**
 * Create category (admin)
 * POST /api/admin/categories - backend wraps in ApiResponse
 */
export async function adminCreateCategory(data: CreateCategoryRequest): Promise<Category> {
  const response = await client.post<ApiResponse<Category>>('/admin/categories', data);
  return response.data.data!;
}

/**
 * Update category (admin)
 * PUT /api/admin/categories/{id} - backend wraps in ApiResponse
 */
export async function adminUpdateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  const response = await client.put<ApiResponse<Category>>(`/admin/categories/${id}`, data);
  return response.data.data!;
}

/**
 * Delete category (admin)
 * DELETE /api/admin/categories/{id}
 */
export async function adminDeleteCategory(id: number): Promise<void> {
  await client.delete(`/admin/categories/${id}`);
}

// ============ Content Moderation ============

/**
 * Get all posts (with optional status filter)
 * GET /api/admin/posts?status= - backend returns List directly
 */
export async function adminGetPosts(status?: string): Promise<Post[]> {
  const params = status ? { status } : {};
  const response = await client.get<Post[]>('/admin/posts', { params });
  return response.data;
}

/**
 * Update post status
 * PUT /api/admin/posts/{id}/status - backend wraps in ApiResponse
 */
export async function adminUpdatePostStatus(id: number, status: string): Promise<void> {
  await client.put(`/admin/posts/${id}/status`, { status });
}

/**
 * Force delete post
 * DELETE /api/admin/posts/{id}
 */
export async function adminDeletePost(id: number): Promise<void> {
  await client.delete(`/admin/posts/${id}`);
}

/**
 * Force delete comment
 * DELETE /api/admin/comments/{id}
 */
export async function adminDeleteComment(id: number): Promise<void> {
  await client.delete(`/admin/comments/${id}`);
}

// ============ User Management ============

/**
 * Get all users
 * GET /api/admin/users - backend returns List directly
 */
export async function adminGetUsers(): Promise<AdminUser[]> {
  const response = await client.get<AdminUser[]>('/admin/users');
  return response.data;
}

/**
 * Ban user
 * PUT /api/admin/users/{id}/ban - backend wraps in ApiResponse
 */
export async function adminBanUser(id: number): Promise<void> {
  await client.put(`/admin/users/${id}/ban`);
}

/**
 * Unban user
 * PUT /api/admin/users/{id}/unban - backend wraps in ApiResponse
 */
export async function adminUnbanUser(id: number): Promise<void> {
  await client.put(`/admin/users/${id}/unban`);
}
