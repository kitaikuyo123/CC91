import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategoryById } from '../api/category';
import { getPostsByCategory, type Post } from '../api/post';
import { queryKeys } from '../lib/queryKeys';

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
    return <div className="container" style={{ marginTop: '2rem' }}>加载中...</div>;
  }

  if (error || !category) {
    return <div className="container" style={{ marginTop: '2rem' }}>
      <div className="error-message">{(error as any)?.response?.data?.message || '加载失败'}</div>
    </div>;
  }

  return (
    <div className="container" style={{ marginTop: '2rem' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1>{category.name}</h1>
        {category.description && (
          <p style={{ color: '#666' }}>{category.description}</p>
        )}
      </div>

      {posts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
          暂无帖子
        </div>
      ) : (
        <>
          <div className="post-list">
            {posts.map((post) => (
              <div
                key={post.id}
                className="card post-item"
                onClick={() => window.open(`/posts/${post.id}`, '_blank')}
                style={{ cursor: 'pointer', marginBottom: '1rem' }}
              >
                <h3 style={{ marginBottom: '0.5rem' }}>{post.title}</h3>
                <p style={{ color: '#666', fontSize: '0.9rem' }}>
                  {post.content.substring(0, 150)}...
                </p>
                <div style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#999' }}>
                  <span>作者: {post.authorUsername}</span>
                  <span style={{ marginLeft: '1rem' }}>
                    {new Date(post.createdAt).toLocaleString()}
                  </span>
                  <span style={{ marginLeft: '1rem' }}>
                    浏览: {post.viewCount}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '2rem' }}>
              <button
                className="btn"
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
                style={{ marginRight: '0.5rem' }}
              >
                上一页
              </button>
              <span style={{ padding: '0.5rem 1rem' }}>
                第 {page + 1} / {totalPages} 页
              </span>
              <button
                className="btn"
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= totalPages - 1}
                style={{ marginLeft: '0.5rem' }}
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
