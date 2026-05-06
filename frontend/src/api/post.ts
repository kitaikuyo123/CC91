import client from './client';

/**
 * 帖子响应类型
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
}

/**
 * 创建帖子请求类型
 */
export interface CreatePostRequest {
  title: string;
  content: string;
  categoryId?: number;
  status?: string;
}

/**
 * 更新帖子请求类型
 */
export interface UpdatePostRequest {
  title?: string;
  content?: string;
  categoryId?: number;
}

/**
 * 分页响应类型
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
 * API 响应包装类型（用于创建/更新/删除操作）
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/**
 * 创建帖子
 * POST /api/posts
 */
export async function createPost(data: CreatePostRequest): Promise<Post> {
  const response = await client.post<ApiResponse<Post>>('/posts', data);
  return response.data.data;
}

/**
 * 获取帖子详情
 * GET /api/posts/{id}
 */
export async function getPostById(id: number): Promise<Post> {
  const response = await client.get<Post>(`/posts/${id}`);
  return response.data;
}

/**
 * 更新帖子
 * PUT /api/posts/{id}
 */
export async function updatePost(id: number, data: UpdatePostRequest): Promise<Post> {
  const response = await client.put<ApiResponse<Post>>(`/posts/${id}`, data);
  return response.data.data;
}

/**
 * 删除帖子
 * DELETE /api/posts/{id}
 */
export async function deletePost(id: number): Promise<void> {
  await client.delete(`/posts/${id}`);
}

/**
 * 分页获取帖子列表
 * GET /api/posts?page=0&size=10
 */
export async function getPostList(page: number = 0, size: number = 10, status?: string): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>('/posts', {
    params: { page, size, ...(status ? { status } : {}) }
  });
  return response.data;
}

/**
 * 根据版块ID分页获取帖子
 * GET /api/posts/by-category/{categoryId}?page=0&size=10
 */
export async function getPostsByCategory(categoryId: number, page: number = 0, size: number = 10): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>(`/posts/by-category/${categoryId}`, {
    params: { page, size }
  });
  return response.data;
}

/**
 * 搜索帖子
 * GET /api/posts/search?keyword=xxx&page=0&size=10
 */
export async function searchPosts(keyword: string, page: number = 0, size: number = 10): Promise<PageResponse<Post>> {
  const response = await client.get<PageResponse<Post>>('/posts/search', {
    params: { keyword, page, size }
  });
  return response.data;
}
