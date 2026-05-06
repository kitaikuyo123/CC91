import client from './client';

/**
 * 版块分类类型
 */
export interface Category {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
  createdAt: string;
}

/**
 * 创建版块请求类型
 */
export interface CreateCategoryRequest {
  name: string;
  description?: string;
  sortOrder?: number;
}

/**
 * 更新版块请求类型
 */
export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  sortOrder?: number;
}

/**
 * API 响应包装类型
 */
export interface ApiResponse<T> {
  message: string;
  data: T;
}

/**
 * 获取所有版块
 * GET /api/categories
 */
export async function getCategories(): Promise<Category[]> {
  const response = await client.get<Category[]>('/categories');
  return response.data;
}

/**
 * 根据ID获取版块详情
 * GET /api/categories/{id}
 */
export async function getCategoryById(id: number): Promise<Category> {
  const response = await client.get<Category>(`/categories/${id}`);
  return response.data;
}

/**
 * 创建版块（管理员）
 * POST /api/categories
 */
export async function createCategory(data: CreateCategoryRequest): Promise<Category> {
  const response = await client.post<ApiResponse<Category>>('/categories', data);
  return response.data.data;
}

/**
 * 更新版块（管理员）
 * PUT /api/categories/{id}
 */
export async function updateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  const response = await client.put<ApiResponse<Category>>(`/categories/${id}`, data);
  return response.data.data;
}

/**
 * 删除版块（管理员）
 * DELETE /api/categories/{id}
 */
export async function deleteCategory(id: number): Promise<void> {
  await client.delete(`/categories/${id}`);
}
