import client from './client';

export interface Report {
  id: number;
  reporterId: number;
  targetType: 'POST' | 'COMMENT';
  targetId: number;
  reason: string;
  status: 'PENDING' | 'REVIEWED' | 'RESOLVED' | 'DISMISSED';
  createdAt: string;
}

export interface SubmitReportRequest {
  contentType: 'POST' | 'COMMENT';
  contentId: number;
  reason: string;
  description?: string;
}

/**
 * Submit a report
 * POST /api/reports
 */
export async function submitReport(data: SubmitReportRequest): Promise<Report> {
  const response = await client.post<{ success: boolean; message: string; data: Report }>('/reports', data);
  return response.data.data;
}

/**
 * Get all reports (admin)
 * GET /api/admin/reports
 */
export async function adminGetReports(): Promise<Report[]> {
  const response = await client.get<Report[]>('/admin/reports');
  return response.data;
}

/**
 * Handle a report status
 * PUT /api/admin/reports/{id}
 */
export async function adminHandleReport(id: number, status: 'RESOLVED' | 'DISMISSED'): Promise<void> {
  await client.put(`/admin/reports/${id}`, { status });
}
