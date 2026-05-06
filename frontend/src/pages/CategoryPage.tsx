import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategoryById } from '../api/category';
import { getPostsByCategory, type Post } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 版块页面组件
 */
export default function CategoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const categoryId = id ? parseInt(id) : 0;

  // 使用 React Query 获取版块信息
  const { data: category, isLoading: categoryLoading, error: categoryError } = useQuery({
    queryKey: queryKeys.categories.detail(categoryId),
    queryFn: () => getCategoryById(categoryId),
    enabled: categoryId > 0,
  });

  // 使用 React Query 获取版块帖子
  const { data: postsData, isLoading: postsLoading, error: postsError } = useQuery({
    queryKey: queryKeys.posts.byCategory(categoryId, page, 10),
    queryFn: () => getPostsByCategory(categoryId, page, 10),
    enabled: categoryId > 0,
  });

  const posts = postsData?.content || [];
  const totalPages = postsData?.totalPages || 0;
  const loading = categoryLoading || postsLoading;
  const error = categoryError || postsError;

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  if (error || !category) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="error-message" role="alert">{(error as any)?.response?.data?.message || '加载失败'}</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ marginTop: '2rem' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1>{category.name}</h1>
        {category.description && (
          <p style={{ color: 'var(--color-text-muted)' }}>{category.description}</p>
        )}
      </div>

      {posts.length === 0 ? (
        <div className="empty-state">暂无帖子</div>
      ) : (
        <>
          <div className="post-list">
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
                <h3 style={{ marginBottom: '0.5rem' }}>{post.title}</h3>
                <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>
                  {post.content ? `${post.content.substring(0, 150)}...` : ''}
                </p>
                <div className="post-meta" style={{ marginTop: '0.5rem' }}>
                  <span>作者: {post.authorUsername}</span>
                  <span>
                    <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                  </span>
                  <span>浏览: {post.viewCount}</span>
                  <span>评论: {post.commentCount}</span>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <nav className="pagination" aria-label="版块帖子分页">
              <button
                className="btn"
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
                aria-label="上一页"
              >
                上一页
              </button>
              <span className="pagination-info">
                第 {page + 1} / {totalPages} 页
              </span>
              <button
                className="btn"
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= totalPages - 1}
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
