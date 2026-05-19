import client from './client';
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
 * Admin comment type
 */
export interface AdminComment {
  id: number;
  postId: number;
  postTitle: string;
  authorId: number;
  authorUsername: string;
  content: string;
  parentId: number | null;
  status: string;
  createdAt: string;
}

/**
 * Get all comments (admin)
 * GET /api/admin/comments
 */
export async function adminGetComments(): Promise<AdminComment[]> {
  const response = await client.get<AdminComment[]>('/admin/comments');
  return response.data;
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
 * Update user role
 * PUT /api/admin/users/{id}/role - backend wraps in ApiResponse
 */
export async function adminUpdateUserRole(id: number, role: string): Promise<void> {
  await client.put(`/admin/users/${id}/role`, { role });
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
