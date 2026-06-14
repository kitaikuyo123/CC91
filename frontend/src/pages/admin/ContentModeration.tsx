import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  adminGetPosts, adminUpdatePostStatus, adminDeletePost,
  adminGetComments, adminDeleteComment
} from '../../api/admin';
import { adminGetReports, adminHandleReport } from '../../api/report';
import ErrorMessage from '../../components/ErrorMessage';
import { queryKeys } from '../../lib/queryKeys';

type TabType = 'posts' | 'comments' | 'reports';

/**
 * 内容审核页面 - 帖子与评论审核
 */
export default function ContentModeration() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>('posts');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 帖子列表
  const { data: posts = [], isLoading: postsLoading } = useQuery({
    queryKey: queryKeys.admin.posts(filterStatus || undefined),
    queryFn: () => adminGetPosts(filterStatus || undefined),
    enabled: activeTab === 'posts',
  });

  // 评论列表
  const { data: comments = [], isLoading: commentsLoading } = useQuery({
    queryKey: ['admin', 'comments'],
    queryFn: adminGetComments,
    enabled: activeTab === 'comments',
  });

  // 举报列表
  const { data: reports = [], isLoading: reportsLoading } = useQuery({
    queryKey: ['admin', 'reports'],
    queryFn: adminGetReports,
    enabled: activeTab === 'reports',
  });

  // 举报页加载评论列表，用于建立 commentId -> postId 映射
  const { data: reportComments = [] } = useQuery({
    queryKey: ['admin', 'comments'],
    queryFn: adminGetComments,
    enabled: activeTab === 'reports',
  });

  const commentPostMap = useMemo(() => {
    const map = new Map<number, number>();
    reportComments.forEach(c => map.set(c.id, c.postId));
    return map;
  }, [reportComments]);

  // 更新帖子状态
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

  // 删除帖子
  const deletePostMutation = useMutation({
    mutationFn: adminDeletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.posts() });
      setSuccess('帖子删除成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除失败');
    },
  });

  // 删除评论
  const deleteCommentMutation = useMutation({
    mutationFn: adminDeleteComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'comments'] });
      setSuccess('评论删除成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除失败');
    },
  });

  // 处理举报
  const handleReportMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'RESOLVED' | 'DISMISSED' }) =>
      adminHandleReport(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'reports'] });
      setSuccess('举报处理成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '处理失败');
    },
  });

  const handleResolveReport = async (report: any) => {
    if (!confirm(`确定要处理此举报并删除该内容吗？此操作不可恢复。`)) return;

    try {
      if (report.targetType === 'POST') {
        await deletePostMutation.mutateAsync(report.targetId);
      } else {
        await deleteCommentMutation.mutateAsync(report.targetId);
      }
      await handleReportMutation.mutateAsync({ id: report.id, status: 'RESOLVED' });
    } catch (err: any) {
      setError(err.response?.data?.message || '操作失败');
    }
  };

  const handleDismissReport = (reportId: number) => {
    if (!confirm('确定要忽略该举报吗？')) return;
    handleReportMutation.mutate({ id: reportId, status: 'DISMISSED' });
  };

  const handleStatusChange = (postId: number, newStatus: string) => {
    statusMutation.mutate({ postId, status: newStatus });
  };

  const handleDeletePost = (postId: number, title: string) => {
    if (!confirm(`确定要删除帖子「${title}」吗？此操作不可恢复。`)) return;
    deletePostMutation.mutate(postId);
  };

  const handleDeleteComment = (commentId: number, content: string) => {
    const preview = content.length > 30 ? content.substring(0, 30) + '...' : content;
    if (!confirm(`确定要删除评论「${preview}」吗？`)) return;
    deleteCommentMutation.mutate(commentId);
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


  const clearMessages = () => {
    setError('');
    setSuccess('');
  };

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>内容审核</h1>

      {error && <ErrorMessage message={error} onDismiss={() => setError('')} />}
      {success && <ErrorMessage type="success" message={success} onDismiss={() => setSuccess('')} />}

      {/* 标签页切换 */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '0.5rem 1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className={`btn ${activeTab === 'posts' ? 'btn-primary' : ''}`}
            onClick={() => { setActiveTab('posts'); clearMessages(); }}
          >
            帖子审核
          </button>
          <button
            className={`btn ${activeTab === 'comments' ? 'btn-primary' : ''}`}
            onClick={() => { setActiveTab('comments'); clearMessages(); }}
          >
            评论审核
          </button>
          <button
            className={`btn ${activeTab === 'reports' ? 'btn-primary' : ''}`}
            onClick={() => { setActiveTab('reports'); clearMessages(); }}
          >
            举报队列
          </button>
        </div>
      </div>

      {/* 帖子审核 */}
      {activeTab === 'posts' && (
        <>
          {/* 筛选器 */}
          <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
            <div className="filter-bar">
              <span>状态筛选：</span>
              <button
                className={`filter-btn ${filterStatus === '' ? 'filter-active' : ''}`}
                onClick={() => setFilterStatus('')}
              >
                全部
              </button>
              <button
                className={`filter-btn ${filterStatus === 'PUBLISHED' ? 'filter-active' : ''}`}
                onClick={() => setFilterStatus('PUBLISHED')}
              >
                已发布
              </button>
              <button
                className={`filter-btn ${filterStatus === 'DRAFT' ? 'filter-active' : ''}`}
                onClick={() => setFilterStatus('DRAFT')}
              >
                草稿
              </button>
              <button
                className={`filter-btn ${filterStatus === 'DELETED' ? 'filter-active' : ''}`}
                onClick={() => setFilterStatus('DELETED')}
              >
                已删除
              </button>
            </div>
          </div>

          {postsLoading ? (
            <div className="loading-container">
              <div className="spinner spinner-lg"></div>
              <span>加载中...</span>
            </div>
          ) : (
            <div className="table-container">
              <div className="card">
                <p style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>共 {posts.length} 条帖子</p>
                {posts.length === 0 ? (
                  <div className="empty-state">暂无帖子</div>
                ) : (
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>标题</th>
                        <th>作者</th>
                        <th className="hide-mobile">版块</th>
                        <th>状态</th>
                        <th className="hide-mobile">浏览</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {posts.map((post) => (
                        <tr key={post.id}>
                          <td style={{ maxWidth: '300px' }}>
                            <div style={{ fontWeight: '500', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {post.title}
                            </div>
                            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                              {new Date(post.createdAt).toLocaleDateString()}
                            </div>
                          </td>
                          <td>{post.authorUsername}</td>
                          <td className="hide-mobile">{post.categoryName || '-'}</td>
                          <td>
                            <span style={{
                              display: 'inline-block',
                              padding: '0.25rem 0.5rem',
                              borderRadius: 'var(--radius-sm)',
                              background: getStatusColor(post.status || 'PUBLISHED'),
                              color: '#fff',
                              fontSize: '0.85rem',
                            }}>
                              {getStatusLabel(post.status || 'PUBLISHED')}
                            </span>
                          </td>
                          <td className="hide-mobile">{post.viewCount}</td>
                          <td>
                            <a
                              href={`/posts/${post.id}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              style={{ marginRight: '0.5rem' }}
                            >
                              查看
                            </a>
                            {post.status === 'DELETED' ? (
                              <button
                                className="btn btn-success btn-sm"
                                disabled={statusMutation.isPending}
                                onClick={() => {
                                  if (confirm(`确定要恢复帖子「${post.title}」吗？`)) {
                                    statusMutation.mutate({ postId: post.id, status: 'PUBLISHED' });
                                  }
                                }}
                              >
                                恢复
                              </button>
                            ) : (
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() => handleDeletePost(post.id, post.title)}
                              >
                                删除
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {/* 评论审核 */}
      {activeTab === 'comments' && (
        <>
          {commentsLoading ? (
            <div className="loading-container">
              <div className="spinner spinner-lg"></div>
              <span>加载中...</span>
            </div>
          ) : (
            <div className="table-container">
              <div className="card">
                <p style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>共 {comments.length} 条评论</p>
                {comments.length === 0 ? (
                  <div className="empty-state">暂无评论</div>
                ) : (
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>内容</th>
                        <th>作者</th>
                        <th className="hide-mobile">所属帖子</th>
                        <th className="hide-mobile">时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {comments.map((comment) => (
                        <tr key={comment.id}>
                          <td style={{ maxWidth: '350px' }}>
                            <div style={{
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                              color: comment.status === 'DELETED' ? '#999' : 'inherit'
                            }}>
                              {comment.content}
                            </div>
                          </td>
                          <td>{comment.authorUsername}</td>
                          <td className="hide-mobile" style={{ maxWidth: '200px' }}>
                            <a
                              href={`/posts/${comment.postId}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              style={{
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                display: 'block'
                              }}
                            >
                              {comment.postTitle}
                            </a>
                          </td>
                          <td className="hide-mobile" style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                            {new Date(comment.createdAt).toLocaleDateString()}
                          </td>
                          <td>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDeleteComment(comment.id, comment.content)}
                              disabled={deleteCommentMutation.isPending}
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
            </div>
          )}
        </>
      )}

      {/* 举报队列 */}
      {activeTab === 'reports' && (
        <>
          {reportsLoading ? (
            <div className="loading-container">
              <div className="spinner spinner-lg"></div>
              <span>加载中...</span>
            </div>
          ) : (
            <div className="table-container">
              <div className="card">
                <p style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>共 {reports.length} 条举报记录</p>
                {reports.length === 0 ? (
                  <div className="empty-state">暂无举报记录</div>
                ) : (
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>被举报内容</th>
                        <th>类型</th>
                        <th>举报人</th>
                        <th>举报原因/详情</th>
                        <th>状态</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {reports.map((report: any) => (
                        <tr key={report.id}>
                          <td style={{ maxWidth: '300px' }}>
                            <div style={{ fontWeight: '500' }}>
                              {report.targetType === 'POST' ? '帖子' : '评论'} ID：
                              {(() => {
                                const postId = report.targetType === 'POST'
                                  ? report.targetId
                                  : commentPostMap.get(report.targetId);
                                return postId ? (
                                  <a
                                    href={`/posts/${postId}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                  >
                                    #{report.targetId}
                                  </a>
                                ) : (
                                  <span>#{report.targetId}</span>
                                );
                              })()}
                            </div>
                            <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
                              举报时间：{new Date(report.createdAt).toLocaleString('zh-CN')}
                            </div>
                          </td>
                          <td>
                            <span style={{
                              padding: '0.2rem 0.5rem',
                              borderRadius: '4px',
                              fontSize: '0.8rem',
                              backgroundColor: report.targetType === 'POST' ? '#3498db' : '#e67e22',
                              color: 'white'
                            }}>
                              {report.targetType === 'POST' ? '帖子' : '评论'}
                            </span>
                          </td>
                          <td>用户 #{report.reporterId}</td>
                          <td>
                            <div style={{ fontWeight: 'bold' }}>{report.reason}</div>
                          </td>
                          <td>
                            <span style={{
                              padding: '0.25rem 0.5rem',
                              borderRadius: '4px',
                              fontSize: '0.82rem',
                              fontWeight: 'bold',
                              color: 'white',
                              backgroundColor: report.status === 'PENDING' ? '#f1c40f' : report.status === 'RESOLVED' ? '#2ecc71' : '#95a5a6'
                            }}>
                              {report.status === 'PENDING' ? '待审核' : report.status === 'RESOLVED' ? '已处理' : '已忽略'}
                            </span>
                          </td>
                          <td>
                            {report.status === 'PENDING' ? (
                              <div style={{ display: 'flex', gap: '0.5rem' }}>
                                <button
                                  className="btn btn-danger btn-sm"
                                  onClick={() => handleResolveReport(report)}
                                  disabled={handleReportMutation.isPending}
                                >
                                  删除内容
                                </button>
                                <button
                                  className="btn btn-sm"
                                  onClick={() => handleDismissReport(report.id)}
                                  disabled={handleReportMutation.isPending}
                                  style={{ border: '1px solid var(--color-border)' }}
                                >
                                  忽略
                                </button>
                              </div>
                            ) : (
                              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>已归档</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
