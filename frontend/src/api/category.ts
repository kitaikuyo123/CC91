import client from './client';
import type { ApiResponse } from './types';

/**
 * Category type
 */
export interface Category {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
  createdAt: string;
}

/**
 * Create category request type
 */
export interface CreateCategoryRequest {
  name: string;
  description?: string;
  sortOrder?: number;
}

/**
 * Update category request type
 */
export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  sortOrder?: number;
}

/**
 * Get all categories
 * GET /api/categories - backend returns List directly
 */
export async function getCategories(): Promise<Category[]> {
  const response = await client.get<Category[]>('/categories');
  return response.data;
}

/**
 * Get category by ID
 * GET /api/categories/{id} - backend returns CategoryDTO directly
 */
export async function getCategoryById(id: number): Promise<Category> {
  const response = await client.get<Category>(`/categories/${id}`);
  return response.data;
}

/**
 * Create category (admin)
 * POST /api/categories - backend wraps in ApiResponse
 */
export async function createCategory(data: CreateCategoryRequest): Promise<Category> {
  const response = await client.post<ApiResponse<Category>>('/categories', data);
  return response.data.data!;
}

/**
 * Update category (admin)
 * PUT /api/categories/{id} - backend wraps in ApiResponse
 */
export async function updateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  const response = await client.put<ApiResponse<Category>>(`/categories/${id}`, data);
  return response.data.data!;
}

/**
 * Delete category (admin)
 * DELETE /api/categories/{id}
 */
export async function deleteCategory(id: number): Promise<void> {
  await client.delete(`/categories/${id}`);
}
