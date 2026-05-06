import client from './client';

/**
 * 通知类型
 */
export interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  relatedId?: number;
  isRead: boolean;
  createdAt: string;
}

/**
 * API 响应包装类型
 */
export interface ApiResponse<T> {
  message: string;
  data?: T;
}

/**
 * 获取通知列表
 * GET /api/notifications?page=0&size=20
 */
export async function getNotifications(page: number = 0, size: number = 20): Promise<Notification[]> {
  const response = await client.get<Notification[]>('/notifications', {
    params: { page, size }
  });
  return response.data;
}

/**
 * 获取未读通知数量
 * GET /api/notifications/unread-count
 */
export async function getUnreadCount(): Promise<number> {
  const response = await client.get<number>('/notifications/unread-count');
  return response.data;
}

/**
 * 标记通知为已读
 * PUT /api/notifications/{id}/read
 */
export async function markAsRead(id: number): Promise<ApiResponse<void>> {
  const response = await client.put<ApiResponse<void>>(`/notifications/${id}/read`);
  return response.data;
}

/**
 * 标记所有通知为已读
 * PUT /api/notifications/read-all
 */
export async function markAllAsRead(): Promise<ApiResponse<void>> {
  const response = await client.put<ApiResponse<void>>('/notifications/read-all');
  return response.data;
}
