import client from './client';
import type { Post } from './post';

/**
 * 用户资料接口类型
 */
export interface UserProfile {
  username: string;
  email: string;
  avatarUrl: string | null;
  bio: string | null;
  location: string | null;
  website: string | null;
  createdAt: string;
}

/**
 * 更新用户资料请求类型
 */
export interface UpdateUserProfileRequest {
  avatarUrl?: string;
  bio?: string;
  location?: string;
  website?: string;
}

/**
 * 当前用户评论（用于 Dashboard "我的评论"）
 */
export interface MyComment {
  id: number;
  postId: number;
  postTitle: string;
  content: string;
  parentId: number | null;
  createdAt: string;
  status: string;
}

/**
 * 获取当前用户资料
 */
export async function getMyProfile(): Promise<UserProfile> {
  const response = await client.get('/users/me');
  return response.data;
}

/**
 * 获取指定用户资料
 */
export async function getUserProfile(username: string): Promise<UserProfile> {
  const response = await client.get(`/users/${username}`);
  return response.data;
}

/**
 * 更新当前用户资料
 */
export async function updateProfile(data: UpdateUserProfileRequest): Promise<UserProfile> {
  const response = await client.put('/users/me/profile', data);
  return response.data;
}

/**
 * 获取当前用户帖子列表（仅已发布）
 */
export async function getMyPosts(): Promise<Post[]> {
  const response = await client.get<Post[]>('/users/me/posts');
  return response.data;
}

/**
 * 获取当前用户草稿列表
 */
export async function getMyDrafts(): Promise<Post[]> {
  const response = await client.get<Post[]>('/users/me/drafts');
  return response.data;
}

/**
 * 获取当前用户评论列表
 */
export async function getMyComments(): Promise<MyComment[]> {
  const response = await client.get<MyComment[]>('/users/me/comments');
  return response.data;
}
