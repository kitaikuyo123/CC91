import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getAnnouncements, adminCreateAnnouncement, adminUpdateAnnouncement, adminDeleteAnnouncement,
  type Announcement, type CreateAnnouncementRequest, type UpdateAnnouncementRequest
} from '../../api/announcement';
import ErrorMessage from '../../components/ErrorMessage';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 公告管理页面
 */
export default function AnnouncementManage() {
  const queryClient = useQueryClient();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState<CreateAnnouncementRequest>({
    title: '',
    content: '',
    isPinned: false
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 使用 React Query 获取公告列表
  const { data: announcements = [], isLoading } = useQuery({
    queryKey: queryKeys.announcements.list(),
    queryFn: getAnnouncements,
  });

  const handleCreate = () => {
    setEditingId(null);
    setFormData({ title: '', content: '', isPinned: false });
    setShowForm(true);
    setError('');
    setSuccess('');
  };

  const handleEdit = (announcement: Announcement) => {
    setEditingId(announcement.id);
    setFormData({
      title: announcement.title,
      content: announcement.content,
      isPinned: announcement.isPinned
    });
    setShowForm(true);
    setError('');
    setSuccess('');
  };

  // 创建/更新公告的 mutation
  const saveMutation = useMutation({
    mutationFn: ({ id, data }: { id?: number; data: CreateAnnouncementRequest | UpdateAnnouncementRequest }) => {
      if (id) {
        return adminUpdateAnnouncement(id, data as UpdateAnnouncementRequest);
      } else {
        return adminCreateAnnouncement(data as CreateAnnouncementRequest);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.announcements.all });
      setShowForm(false);
      setSuccess(editingId ? '公告更新成功' : '公告创建成功');
      setEditingId(null);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '操作失败');
    },
  });

  // 删除公告的 mutation
  const deleteMutation = useMutation({
    mutationFn: adminDeleteAnnouncement,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.announcements.list() });
      setSuccess('公告删除成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除失败');
    },
  });

  const handleDelete = async (id: number, title: string) => {
    if (!confirm(`确定要删除公告「${title}」吗？`)) return;
    deleteMutation.mutate(id);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.title.trim()) {
      setError('公告标题不能为空');
      return;
    }

    if (!formData.content.trim()) {
      setError('公告内容不能为空');
      return;
    }

    saveMutation.mutate({ id: editingId || undefined, data: formData });
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>公告管理</h1>
        <button className="btn btn-primary" onClick={handleCreate}>
          + 新建公告
        </button>
      </div>

      {error && <ErrorMessage message={error} onDismiss={() => setError('')} />}
      {success && <ErrorMessage type="success" message={success} onDismiss={() => setSuccess('')} />}

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
          <h2 style={{ marginBottom: '1rem' }}>{editingId ? '编辑公告' : '新建公告'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="ann-title">公告标题 <span aria-hidden="true">*</span></label>
              <input
                id="ann-title"
                type="text"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="请输入公告标题"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="ann-content">公告内容 <span aria-hidden="true">*</span></label>
              <textarea
                id="ann-content"
                value={formData.content}
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                placeholder="请输入公告内容"
                rows={6}
                required
              />
            </div>

            <div className="form-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={formData.isPinned || false}
                  onChange={(e) => setFormData({ ...formData, isPinned: e.target.checked })}
                />
                置顶公告
              </label>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn btn-primary" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? '提交中...' : (editingId ? '保存' : '创建')}
              </button>
              <button type="button" className="btn" onClick={() => setShowForm(false)}>
                取消
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>置顶</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {announcements.length === 0 ? (
                <tr>
                  <td colSpan={4} className="empty-state">
                    暂无公告
                  </td>
                </tr>
              ) : (
                announcements.map((announcement) => (
                  <tr key={announcement.id}>
                    <td style={{ fontWeight: '500' }}>{announcement.title}</td>
                    <td>
                      <span style={{
                        display: 'inline-block',
                        padding: '0.15rem 0.5rem',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        backgroundColor: announcement.isPinned ? '#fef3c7' : '#f3f4f6',
                        color: announcement.isPinned ? '#92400e' : '#6b7280',
                      }}>
                        {announcement.isPinned ? '置顶' : '普通'}
                      </span>
                    </td>
                    <td className="hide-mobile">
                      {new Date(announcement.createdAt).toLocaleDateString('zh-CN')}
                    </td>
                    <td>
                      <button
                        className="btn btn-sm"
                        onClick={() => handleEdit(announcement)}
                        style={{ marginRight: '0.5rem' }}
                      >
                        编辑
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(announcement.id, announcement.title)}
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
