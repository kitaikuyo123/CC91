import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyComments } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 我的评论页面
 */
export default function MyCommentsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myComments = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.meComments(),
    queryFn: () => getMyComments(),
    enabled: !!user,
  });

  return (
    <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
      <button onClick={() => navigate('/dashboard')} className="btn" style={{ marginBottom: '1rem' }}>
        &larr; 返回 Dashboard
      </button>

      <div className="card" style={{ padding: '1.5rem' }}>
        <h1 style={{ marginBottom: '1rem' }}>💬 我的评论</h1>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>加载中...</div>
        ) : error ? (
          <div className="error-message" role="alert">
            {(error as any)?.response?.data?.message || '加载失败'}
          </div>
        ) : myComments.length === 0 ? (
          <div className="empty-state">你还没有发表过评论</div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {myComments.map((c) => (
              <li
                key={c.id}
                role="button"
                tabIndex={0}
                onClick={() => navigate(`/posts/${c.postId}`)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    navigate(`/posts/${c.postId}`);
                  }
                }}
                style={{
                  padding: '0.75rem 0',
                  borderBottom: '1px solid #eee',
                  cursor: 'pointer'
                }}
                aria-label={`查看评论对应帖子：${c.postTitle}`}
              >
                <div style={{ fontSize: '0.95rem', color: '#333', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {c.content}
                </div>
                <div style={{ fontSize: '0.85rem', color: '#999', marginTop: '0.25rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  <span style={{ marginRight: '0.5rem' }}>
                    <time dateTime={c.createdAt}>{formatDate(c.createdAt)}</time>
                  </span>
                  <span>来自：{c.postTitle}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
