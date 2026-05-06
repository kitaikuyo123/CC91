import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetPosts, adminUpdatePostStatus, adminDeletePost, adminDeleteComment, type Post } from '../../api/admin';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 内容审核页面
 */
export default function ContentModeration() {
  const queryClient = useQueryClient();

  const [filterStatus, setFilterStatus] = useState<string>('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 使用 React Query 获取帖子列表
  const { data: posts = [], isLoading } = useQuery({
    queryKey: queryKeys.admin.posts(filterStatus || undefined),
    queryFn: () => adminGetPosts(filterStatus || undefined),
  });

  // 更新帖子状态的 mutation
  const statusMutation = useMutation({
    mutationFn: ({ postId, status }: { postId: number; status: string }) =>
      adminUpdatePostStatus(postId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.posts() });
      setSuccess('状态更新成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '更新失败');
    },
  });

  // 删除帖子的 mutation
  const deleteMutation = useMutation({
    mutationFn: adminDeletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.posts() });
      setSuccess('帖子删除成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除失败');
    },
  });

  const handleStatusChange = (postId: number, newStatus: string) => {
    statusMutation.mutate({ postId, status: newStatus });
  };

  const handleDeletePost = (postId: number, title: string) => {
    if (!confirm(`确定要删除帖子「${title}」吗？`)) return;
    deleteMutation.mutate(postId);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PUBLISHED': return '#2ecc71';
      case 'DRAFT': return '#f39c12';
      case 'DELETED': return '#e74c3c';
      default: return '#95a5a6';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'PUBLISHED': return '已发布';
      case 'DRAFT': return '草稿';
      case 'DELETED': return '已删除';
      default: return status;
    }
  };

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>内容审核</h1>

      {error && (
        <div className="error-message" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {success && (
        <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#d4edda', color: '#155724', borderRadius: '4px' }}>
          {success}
        </div>
      )}

      {/* 筛选器 */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <span>状态筛选：</span>
          <button
            className="btn"
            onClick={() => setFilterStatus('')}
            style={{ background: filterStatus === '' ? '#3498db' : '#95a5a6', color: '#fff' }}
          >
            全部
          </button>
          <button
            className="btn"
            onClick={() => setFilterStatus('PUBLISHED')}
            style={{ background: filterStatus === 'PUBLISHED' ? '#3498db' : '#95a5a6', color: '#fff' }}
          >
            已发布
          </button>
          <button
            className="btn"
            onClick={() => setFilterStatus('DRAFT')}
            style={{ background: filterStatus === 'DRAFT' ? '#3498db' : '#95a5a6', color: '#fff' }}
          >
            草稿
          </button>
          <button
            className="btn"
            onClick={() => setFilterStatus('DELETED')}
            style={{ background: filterStatus === 'DELETED' ? '#3498db' : '#95a5a6', color: '#fff' }}
          >
            已删除
          </button>
        </div>
      </div>

      {loading ? (
        <div>加载中...</div>
      ) : (
        <div className="card">
          <p style={{ color: '#666', marginBottom: '1rem' }}>共 {posts.length} 条帖子</p>

          {posts.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
              暂无帖子
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #eee' }}>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>标题</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>作者</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>版块</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>状态</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>浏览</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {posts.map((post) => (
                  <tr key={post.id} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '0.75rem', maxWidth: '300px' }}>
                      <div style={{ fontWeight: '500', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {post.title}
                      </div>
                      <div style={{ fontSize: '0.85rem', color: '#999' }}>
                        {new Date(post.createdAt).toLocaleDateString()}
                      </div>
                    </td>
                    <td style={{ padding: '0.75rem' }}>{post.authorUsername}</td>
                    <td style={{ padding: '0.75rem' }}>{post.categoryName || '-'}</td>
                    <td style={{ padding: '0.75rem' }}>
                      <select
                        value={post.status || 'PUBLISHED'}
                        onChange={(e) => handleStatusChange(post.id, e.target.value)}
                        disabled={statusMutation.isPending}
                        style={{
                          padding: '0.25rem 0.5rem',
                          border: '1px solid #ddd',
                          borderRadius: '4px',
                          background: getStatusColor(post.status || 'PUBLISHED'),
                          color: '#fff',
                          cursor: 'pointer'
                        }}
                      >
                        <option value="PUBLISHED">已发布</option>
                        <option value="DRAFT">草稿</option>
                        <option value="DELETED">已删除</option>
                      </select>
                    </td>
                    <td style={{ padding: '0.75rem' }}>{post.viewCount}</td>
                    <td style={{ padding: '0.75rem' }}>
                      <a
                        href={`/posts/${post.id}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ marginRight: '0.5rem', color: '#3498db', textDecoration: 'none' }}
                      >
                        查看
                      </a>
                      <button
                        className="btn"
                        onClick={() => handleDeletePost(post.id, post.title)}
                        style={{ background: '#e74c3c', color: '#fff', fontSize: '0.85rem' }}
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
