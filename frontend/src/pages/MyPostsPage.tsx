import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyPosts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 我的帖子页面 - 展示当前用户全部帖子（含已发布与草稿）
 */
export default function MyPostsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myPosts = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.mePosts(),
    queryFn: () => getMyPosts(),
    enabled: !!user,
  });

  if (isLoading) {
    return (
      <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
        <div className="loading-container">
          <div className="spinner spinner-lg"></div>
          <span>加载中...</span>
        </div>
      </div>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message || '加载失败';
    return (
      <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
        <div className="error-message" role="alert">{errorMessage}</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
      <button onClick={() => navigate('/dashboard')} className="btn" style={{ marginBottom: '1rem' }}>
        &larr; 返回 Dashboard
      </button>

      {/* 页面标题 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>📝 我的帖子</h1>
      </div>

      {/* 帖子统计 */}
      <div style={{ marginBottom: '1.5rem', color: 'var(--color-text-muted)' }}>
        共 {myPosts.length} 篇帖子
      </div>

      {myPosts.length === 0 ? (
        <div className="card empty-state">
          <p>你还没有发布帖子</p>
        </div>
      ) : (
        <div className="posts-list">
          {myPosts.map((post) => (
            <div
              key={post.id}
              className="card post-item"
              role="link"
              tabIndex={0}
              onClick={() => navigate(`/posts/${post.id}`)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(`/posts/${post.id}`);
                }
              }}
              aria-label={`帖子: ${post.title}`}
            >
              {/* 帖子标题 */}
              <h2 style={{ marginBottom: '0.75rem', fontSize: '1.3rem' }}>
                {post.title}
              </h2>

              {/* 帖子元信息 */}
              <div className="post-meta" style={{ marginBottom: '0.75rem', fontSize: '0.9rem' }}>
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
                lineHeight: '1.5',
                margin: 0,
              }}>
                {post.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
