import client from './client';
import type { ApiResponse } from './types';

/**
 * Post response type
 */
export interface Post {
  id: number;
  title: string;
  content: string;
  authorId: number;
  authorUsername: string;
  categoryId?: number;
  categoryName?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;
  viewCount: number;
  commentCount: number;
}

/**
 * Create post request type
 */
export interface CreatePostRequest {
  title: string;
  content: string;
  categoryId?: number;
  status?: string;
}

/**
 * Update post request type
 */
export interface UpdatePostRequest {
  title?: string;
  content?: string;
  categoryId?: number;
  status?: string;
}

/**
 * Paginated response type (Spring Boot Page)
 */
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

/**
 * Create post
 * POST /api/posts - backend wraps in ApiResponse
 */
export async function createPost(data: CreatePostRequest): Promise<Post> {
  const response = await client.post<ApiResponse<Post>>('/posts', data);
  return response.data.data!;
}

/**
 * Get post detail
 * GET /api/posts/{id} - backend returns PostResponse directly
 */
export async function getPostById(id: number): Promise<Post> {
  const response = await client.get<Post>(`/posts/${id}`);
  return response.data;
}

/**
 * Update post
 * PUT /api/posts/{id} - backend wraps in ApiResponse
 */
export async function updatePost(id: number, data: UpdatePostRequest): Promise<Post> {
  const response = await client.put<ApiResponse<Post>>(`/posts/${id}`, data);
  return response.data.data!;
}

/**
 * Delete post
 * DELETE /api/posts/{id}
 */
export async function deletePost(id: number): Promise<void> {
  await client.delete(`/posts/${id}`);
}

/**
 * Get paginated post list
 * GET /api/posts?page=0&size=10 - backend returns Page directly
 */
export async function getPostList(page: number = 0, size: number = 10, status?: string): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>('/posts', {
    params: { page, size, ...(status ? { status } : {}) }
  });
  return response.data;
}

/**
 * Get posts by category
 * GET /api/posts/by-category/{categoryId}?page=0&size=10 - backend returns Page directly
 */
export async function getPostsByCategory(categoryId: number, page: number = 0, size: number = 10): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>(`/posts/by-category/${categoryId}`, {
    params: { page, size }
  });
  return response.data;
}

/**
 * Search posts
 * GET /api/posts/search?keyword=xxx&page=0&size=10 - backend returns Page directly
 */
export async function searchPosts(keyword: string, page: number = 0, size: number = 10): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>('/posts/search', {
    params: { keyword, page, size }
  });
  return response.data;
}
