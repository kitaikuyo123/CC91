import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPostList, type Post } from '../api/post';
import { queryKeys } from '../lib/queryKeys';

/**
 * 帖子列表页面 - 支持状态筛选
 */
export default function PostListPage() {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PUBLISHED' | 'DRAFT'>('PUBLISHED');
  const pageSize = 10;

  // 使用 React Query 获取帖子列表
  const statusParam = statusFilter === 'ALL' ? undefined : statusFilter;

  const { data: postsData, isLoading, error } = useQuery({
    queryKey: queryKeys.posts.list({ page: currentPage, size: pageSize, status: statusParam }),
    queryFn: () => getPostList(currentPage, pageSize, statusParam),
  });

  const posts = postsData?.content || [];
  const totalPages = postsData?.totalPages || 0;
  const totalElements = postsData?.totalElements || 0;

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
      window.scrollTo(0, 0);
    }
  };

  const handleStatusFilterChange = (newStatus: 'ALL' | 'PUBLISHED' | 'DRAFT') => {
    setStatusFilter(newStatus);
    setCurrentPage(0);
  };

  if (isLoading) {
    return (
      <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem' }}>加载中...</p>
      </div>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message || '加载帖子列表失败';
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message">{errorMessage}</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
      {/* 页面标题 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>帖子列表</h1>
        <button
          onClick={() => navigate('/posts/new')}
          className="btn btn-primary"
        >
          发布新帖
        </button>
      </div>

      {/* 状态筛选器 */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <span>状态筛选：</span>
          <button
            className="btn"
            onClick={() => handleStatusFilterChange('ALL')}
            style={{
              background: statusFilter === 'ALL' ? '#3498db' : '#95a5a6',
              color: '#fff',
              padding: '0.5rem 1rem',
              fontSize: '0.9rem'
            }}
          >
            全部
          </button>
          <button
            className="btn"
            onClick={() => handleStatusFilterChange('PUBLISHED')}
            style={{
              background: statusFilter === 'PUBLISHED' ? '#3498db' : '#95a5a6',
              color: '#fff',
              padding: '0.5rem 1rem',
              fontSize: '0.9rem'
            }}
          >
            已发布
          </button>
          <button
            className="btn"
            onClick={() => handleStatusFilterChange('DRAFT')}
            style={{
              background: statusFilter === 'DRAFT' ? '#3498db' : '#95a5a6',
              color: '#fff',
              padding: '0.5rem 1rem',
              fontSize: '0.9rem'
            }}
          >
            草稿
          </button>
        </div>
      </div>

      {/* 帖子统计 */}
      <div style={{ marginBottom: '1.5rem', color: '#888' }}>
        共 {totalElements} 篇帖子
      </div>

      {/* 帖子列表 */}
      {posts.length === 0 ? (
        <div className="card" style={{ padding: '2rem', textAlign: 'center' }}>
          <p style={{ color: '#888', marginBottom: '1rem' }}>暂无符合条件的帖子</p>
          <button
            onClick={() => navigate('/posts/new')}
            className="btn btn-primary"
          >
            发布第一篇帖子
          </button>
        </div>
      ) : (
        <>
          <div className="posts-list">
            {posts.map((post) => (
              <div
                key={post.id}
                className="card"
                style={{ padding: '1.5rem', marginBottom: '1rem', cursor: 'pointer' }}
                onClick={() => window.open(`/posts/${post.id}`, '_blank')}
              >
                {/* 帖子标题 */}
                <h2 style={{ marginBottom: '0.75rem', fontSize: '1.3rem' }}>
                  {post.title}
                  <span style={{
                    background: post.status === 'DRAFT' ? '#f39c12' : '#27ae60',
                    color: '#fff',
                    padding: '0.15rem 0.5rem',
                    borderRadius: '3px',
                    fontSize: '0.75rem',
                    marginLeft: '0.5rem',
                    verticalAlign: 'middle'
                  }}>
                    {post.status === 'DRAFT' ? '草稿' : '已发布'}
                  </span>
                </h2>

                {/* 帖子元信息 */}
                <div style={{ display: 'flex', gap: '1rem', fontSize: '0.9rem', color: '#888', marginBottom: '0.75rem' }}>
                  <span>
                    作者: <strong style={{ color: '#333' }}>{post.authorUsername}</strong>
                  </span>
                  {post.categoryName && (
                    <span>版块: {post.categoryName}</span>
                  )}
                  <span>浏览: {post.viewCount}</span>
                  <span>{formatDate(post.createdAt)}</span>
                </div>

                {/* 帖子摘要 */}
                <p style={{
                  color: '#666',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  display: '-webkit-box',
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: 'vertical',
                  lineHeight: '1.5'
                }}>
                  {post.content}
                </p>
              </div>
            ))}
          </div>

          {/* 分页 */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '2rem' }}>
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="btn"
                style={{ minWidth: '80px' }}
              >
                上一页
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                {Array.from({ length: totalPages }, (_, i) => {
                  // 显示当前页附近页码
                  if (
                    i === 0 ||
                    i === totalPages - 1 ||
                    (i >= currentPage - 1 && i <= currentPage + 1)
                  ) {
                    return (
                      <button
                        key={i}
                        onClick={() => handlePageChange(i)}
                        className="btn"
                        style={{
                          minWidth: '40px',
                          background: i === currentPage ? '#3498db' : undefined,
                          color: i === currentPage ? 'white' : undefined
                        }}
                      >
                        {i + 1}
                      </button>
                    );
                  } else if (
                    i === currentPage - 2 ||
                    i === currentPage + 2
                  ) {
                    return <span key={i} style={{ padding: '0 0.5rem', color: '#888' }}>...</span>;
                  }
                  return null;
                })}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="btn"
                style={{ minWidth: '80px' }}
              >
                下一页
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
