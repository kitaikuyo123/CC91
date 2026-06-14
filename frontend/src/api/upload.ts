import client from './client';

/**
 * Upload an image file for posts
 * POST /api/upload
 */
export async function uploadImage(file: File): Promise<{ url: string }> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post<{ success: boolean; message: string; data: { url: string } }>(
    '/upload/images',
    formData
  );
  return response.data.data;
}
