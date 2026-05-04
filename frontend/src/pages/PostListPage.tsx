import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPostList, type Post, type PageResponse } from '../api/post';

/**
 * 帖子列表页面
 */
export default function PostListPage() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        setLoading(true);
        const data: PageResponse<Post> = await getPostList(currentPage, pageSize);
        setPosts(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setError('');
      } catch (err: any) {
        setError(err.response?.data?.message || '加载帖子列表失败');
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [currentPage]);

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

  if (loading) {
    return (
      <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem' }}>加载中...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message">{error}</div>
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

      {/* 帖子统计 */}
      <div style={{ marginBottom: '1.5rem', color: '#888' }}>
        共 {totalElements} 篇帖子
      </div>

      {/* 帖子列表 */}
      {posts.length === 0 ? (
        <div className="card" style={{ padding: '2rem', textAlign: 'center' }}>
          <p style={{ color: '#888', marginBottom: '1rem' }}>还没有帖子</p>
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
                onClick={() => navigate(`/posts/${post.id}`)}
              >
                {/* 帖子标题 */}
                <h2 style={{ marginBottom: '0.75rem', fontSize: '1.3rem' }}>
                  {post.title}
                </h2>

                {/* 帖子元信息 */}
                <div style={{ display: 'flex', gap: '1rem', fontSize: '0.9rem', color: '#888', marginBottom: '0.75rem' }}>
                  <span>
                    作者: <strong style={{ color: '#333' }}>{post.authorUsername}</strong>
                  </span>
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
