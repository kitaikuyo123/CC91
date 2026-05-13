import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPostList, getPostsByCategory } from '../api/post';
import { getCategories } from '../api/category';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 帖子列表页面 - 显示已发布帖子，支持板块筛选
 */
export default function PostListPage() {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState(0);
  const [categoryFilter, setCategoryFilter] = useState<number | undefined>(undefined);
  const pageSize = 10;

  // 获取版块列表
  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  // 根据是否选中版块调用不同接口
  const { data: postsData, isLoading, error } = useQuery({
    queryKey:
      categoryFilter != null
        ? queryKeys.posts.byCategory(categoryFilter, currentPage, pageSize)
        : queryKeys.posts.list({ page: currentPage, size: pageSize, status: 'PUBLISHED' }),
    queryFn: () =>
      categoryFilter != null
        ? getPostsByCategory(categoryFilter, currentPage, pageSize)
        : getPostList(currentPage, pageSize, 'PUBLISHED'),
  });

  const posts = postsData?.content || [];
  const totalPages = postsData?.totalPages || 0;
  const totalElements = postsData?.totalElements || 0;

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
      window.scrollTo(0, 0);
    }
  };

  const handleCategoryFilterChange = (categoryId: number | undefined) => {
    setCategoryFilter(categoryId);
    setCurrentPage(0);
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message || '加载帖子列表失败';
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message" role="alert">{errorMessage}</div>
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

      {/* 版块筛选器 */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem' }}>
        <div className="filter-bar" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 500 }}>版块筛选：</span>
          <button
            className={`filter-btn ${categoryFilter === undefined ? 'filter-active' : ''}`}
            onClick={() => handleCategoryFilterChange(undefined)}
          >
            全部版块
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`filter-btn ${categoryFilter === cat.id ? 'filter-active' : ''}`}
              onClick={() => handleCategoryFilterChange(cat.id)}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* 帖子统计 */}
      <div style={{ marginBottom: '1.5rem', color: 'var(--color-text-muted)' }}>
        共 {totalElements} 篇帖子
      </div>

      {/* 帖子列表 */}
      {posts.length === 0 ? (
        <div className="card empty-state">
          <p>暂无符合条件的帖子</p>
          <button
            onClick={() => navigate('/posts/new')}
            className="btn btn-primary"
            style={{ marginTop: '1rem' }}
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
                className="card post-item"
                role="link"
                tabIndex={0}
                onClick={() => navigate(`/posts/${post.id}`)}
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/posts/${post.id}`); } }}
                aria-label={`帖子: ${post.title}`}
              >
                {/* 帖子标题 */}
                <h2 style={{ marginBottom: '0.75rem', fontSize: '1.3rem' }}>
                  {post.title}
                </h2>

                {/* 帖子元信息 */}
                <div className="post-meta" style={{ marginBottom: '0.75rem', fontSize: '0.9rem' }}>
                  <span>
                    作者: <strong style={{ color: 'var(--color-text-primary)' }}>{post.authorUsername}</strong>
                  </span>
                  {post.categoryName && (
                    <span>版块: {post.categoryName}</span>
                  )}
                  <span>浏览: {post.viewCount}</span>
                    <span>评论: {post.commentCount}</span>
                  <span><time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time></span>
                </div>

                {/* 帖子摘要 */}
                <p style={{
                  color: 'var(--color-text-secondary)',
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
            <nav className="pagination" aria-label="帖子分页">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="btn"
                aria-label="上一页"
              >
                上一页
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                {Array.from({ length: totalPages }, (_, i) => {
                  if (
                    i === 0 ||
                    i === totalPages - 1 ||
                    (i >= currentPage - 1 && i <= currentPage + 1)
                  ) {
                    return (
                      <button
                        key={i}
                        onClick={() => handlePageChange(i)}
                        className={`btn ${i === currentPage ? 'btn-active' : ''}`}
                        aria-label={`第 ${i + 1} 页`}
                        aria-current={i === currentPage ? 'page' : undefined}
                      >
                        {i + 1}
                      </button>
                    );
                  } else if (
                    i === currentPage - 2 ||
                    i === currentPage + 2
                  ) {
                    return <span key={i} className="pagination-info" aria-hidden="true">...</span>;
                  }
                  return null;
                })}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="btn"
                aria-label="下一页"
              >
                下一页
              </button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
