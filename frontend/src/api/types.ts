/**
 * Unified API response type matching backend ApiResponse.java
 *
 * Backend structure: { message: string, data?: T }
 * - Login/Refresh endpoints return response directly (NOT wrapped in ApiResponse)
 * - GET list endpoints return PageResponse<T> or T[] directly (NOT wrapped)
 * - POST/PUT/DELETE mutations return ApiResponse<T>
 */
export interface ApiResponse<T> {
  message: string;
  data?: T;
}
