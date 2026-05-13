import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyPosts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 我的帖子页面
 */
export default function MyPostsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myPosts = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.mePosts(),
    queryFn: () => getMyPosts(),
    enabled: !!user,
  });

  return (
    <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
      <button onClick={() => navigate('/dashboard')} className="btn" style={{ marginBottom: '1rem' }}>
        &larr; 返回 Dashboard
      </button>

      <div className="card" style={{ padding: '1.5rem' }}>
        <h1 style={{ marginBottom: '1rem' }}>📝 我的帖子</h1>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>加载中...</div>
        ) : error ? (
          <div className="error-message" role="alert">
            {(error as any)?.response?.data?.message || '加载失败'}
          </div>
        ) : myPosts.length === 0 ? (
          <div className="empty-state">你还没有发布帖子</div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {myPosts.map((post) => (
              <li
                key={post.id}
                className="post-item"
                role="button"
                tabIndex={0}
                onClick={() => navigate(`/posts/${post.id}`)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    navigate(`/posts/${post.id}`);
                  }
                }}
                style={{
                  padding: '0.75rem 0',
                  borderBottom: '1px solid #eee',
                  cursor: 'pointer'
                }}
                aria-label={`查看帖子：${post.title}`}
              >
                <div style={{ fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {post.title}
                </div>
                <div style={{ fontSize: '0.85rem', color: '#999', display: 'flex', gap: '0.75rem', marginTop: '0.25rem' }}>
                  <span><time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time></span>
                  {post.status && <span>{post.status}</span>}
                  <span>评论 {post.commentCount ?? 0}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
