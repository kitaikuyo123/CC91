import client from './client';

/**
 * 评论响应类型
 */
export interface Comment {
  id: number;
  postId: number;
  authorId: number;
  authorUsername: string;
  content: string;
  parentId: number | null;
  createdAt: string;
  status: string;
  replies: Comment[];
}

/**
 * 创建评论请求类型
 */
export interface CreateCommentRequest {
  content: string;
}

/**
 * API 响应包装类型
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/**
 * 创建评论
 * POST /api/posts/{postId}/comments
 */
export async function createComment(postId: number, data: CreateCommentRequest): Promise<Comment> {
  const response = await client.post<ApiResponse<Comment>>(`/posts/${postId}/comments`, data);
  return response.data.data;
}

/**
 * 回复评论
 * POST /api/comments/{id}/reply
 */
export async function replyToComment(commentId: number, data: CreateCommentRequest): Promise<Comment> {
  const response = await client.post<ApiResponse<Comment>>(`/comments/${commentId}/reply`, data);
  return response.data.data;
}

/**
 * 删除评论
 * DELETE /api/comments/{id}
 */
export async function deleteComment(commentId: number): Promise<void> {
  await client.delete(`/comments/${commentId}`);
}

/**
 * 获取帖子的所有评论（树形结构）
 * GET /api/posts/{postId}/comments
 */
export async function getCommentsByPostId(postId: number): Promise<Comment[]> {
  const response = await client.get<Comment[]>(`/posts/${postId}/comments`);
  return response.data;
}
