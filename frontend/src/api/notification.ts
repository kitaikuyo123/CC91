import client from './client';
import type { ApiResponse } from './types';

/**
 * Notification type
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
 * Get notification list
 * GET /api/notifications?page=0&size=20 - backend returns List directly
 */
export async function getNotifications(page: number = 0, size: number = 20): Promise<Notification[]> {
  const response = await client.get<Notification[]>('/notifications', {
    params: { page, size }
  });
  return response.data;
}

/**
 * Get unread notification count
 * GET /api/notifications/unread-count - backend returns number directly
 */
export async function getUnreadCount(): Promise<number> {
  const response = await client.get<number>('/notifications/unread-count');
  return response.data;
}

/**
 * Mark notification as read
 * PUT /api/notifications/{id}/read - backend wraps in ApiResponse
 */
export async function markAsRead(id: number): Promise<void> {
  await client.put(`/notifications/${id}/read`);
}

/**
 * Mark all notifications as read
 * PUT /api/notifications/read-all - backend wraps in ApiResponse
 */
export async function markAllAsRead(): Promise<void> {
  await client.put('/notifications/read-all');
}
