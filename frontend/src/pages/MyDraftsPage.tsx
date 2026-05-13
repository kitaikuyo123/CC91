import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyDrafts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 我的草稿页面 - 展示当前用户全部草稿
 */
export default function MyDraftsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myDrafts = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.meDrafts(),
    queryFn: () => getMyDrafts(),
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

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>📄 我的草稿</h1>
      </div>

      <div style={{ marginBottom: '1.5rem', color: 'var(--color-text-muted)' }}>
        共 {myDrafts.length} 篇草稿
      </div>

      {myDrafts.length === 0 ? (
        <div className="card empty-state">
          <p>暂无草稿</p>
        </div>
      ) : (
        <div className="posts-list">
          {myDrafts.map((draft) => (
            <div
              key={draft.id}
              className="card post-item"
              role="link"
              tabIndex={0}
              onClick={() => navigate(`/posts/new?draftId=${draft.id}`)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(`/posts/new?draftId=${draft.id}`);
                }
              }}
              aria-label={`编辑草稿: ${draft.title}`}
            >
              <h2 style={{ marginBottom: '0.75rem', fontSize: '1.3rem' }}>
                {draft.title || '(无标题)'}
                <span className="badge badge-warning" style={{ marginLeft: '0.5rem', verticalAlign: 'middle' }}>
                  草稿
                </span>
              </h2>

              <div className="post-meta" style={{ marginBottom: '0.75rem', fontSize: '0.9rem' }}>
                {draft.categoryName && <span>版块: {draft.categoryName}</span>}
                <span><time dateTime={draft.createdAt}>{formatDate(draft.createdAt)}</time></span>
              </div>

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
                {draft.content || '(空内容)'}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
