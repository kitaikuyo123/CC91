import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getNotifications, getUnreadCount } from '../api/notification';
import { getMyComments, getMyPosts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 用户 Dashboard 页面 - 展示我的帖子、我的评论、我的通知
 */
export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // 获取最近通知
  const { data: recentNotifications = [], isLoading: notificationsLoading } = useQuery({
    queryKey: queryKeys.notifications.list(0, 5),
    queryFn: () => getNotifications(0, 5),
    enabled: !!user,
  });

  // 获取未读数
  const { data: unreadCount = 0 } = useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: () => getUnreadCount(),
    enabled: !!user,
  });

  // 获取我的帖子
  const {
    data: myPosts = [],
    isLoading: myPostsLoading,
    error: myPostsError,
  } = useQuery({
    queryKey: queryKeys.users.mePosts(),
    queryFn: () => getMyPosts(),
    enabled: !!user,
  });

  // 获取我的评论
  const {
    data: myComments = [],
    isLoading: myCommentsLoading,
    error: myCommentsError,
  } = useQuery({
    queryKey: queryKeys.users.meComments(),
    queryFn: () => getMyComments(),
    enabled: !!user,
  });

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1>欢迎回来, <span style={{ color: '#3498db' }}>{user?.username}</span>!</h1>
        <p style={{ color: '#666' }}>这是您的个人中心</p>
      </div>

      {/* 快捷操作 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div
          className="card post-item"
          role="button"
          tabIndex={0}
          onClick={() => navigate('/posts/new')}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate('/posts/new'); } }}
          style={{ textAlign: 'center', padding: '1.5rem' }}
          aria-label="发布新帖"
        >
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }} aria-hidden="true">&#x270F;&#xFE0F;</div>
          <div style={{ fontWeight: '500' }}>发帖</div>
        </div>

        <div
          className="card post-item"
          role="button"
          tabIndex={0}
          onClick={() => navigate('/notifications')}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate('/notifications'); } }}
          style={{ textAlign: 'center', padding: '1.5rem', position: 'relative' }}
          aria-label={`通知${unreadCount > 0 ? `，${unreadCount}条未读` : ''}`}
        >
          {unreadCount > 0 && (
            <span className="badge badge-danger" style={{
              position: 'absolute',
              top: '10px',
              right: '10px',
              borderRadius: 'var(--radius-full)',
              minWidth: '20px',
              height: '20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '0.75rem'
            }}>
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }} aria-hidden="true">&#x1F514;</div>
          <div style={{ fontWeight: '500' }}>通知</div>
        </div>

        <div
          className="card post-item"
          role="button"
          tabIndex={0}
          onClick={() => navigate(`/profile/${user?.username}`)}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/profile/${user?.username}`); } }}
          style={{ textAlign: 'center', padding: '1.5rem' }}
          aria-label="查看我的资料"
        >
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }} aria-hidden="true">&#x1F464;</div>
          <div style={{ fontWeight: '500' }}>我的资料</div>
        </div>

        {user?.role === 'ADMIN' && (
          <div
            className="card post-item"
            role="button"
            tabIndex={0}
            onClick={() => navigate('/admin')}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate('/admin'); } }}
            style={{ textAlign: 'center', padding: '1.5rem', background: 'var(--color-header-bg)', color: '#fff' }}
            aria-label="管理后台"
          >
            <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }} aria-hidden="true">&#x2699;&#xFE0F;</div>
            <div style={{ fontWeight: '500' }}>管理后台</div>
          </div>
        )}
      </div>

      <div className="home-content-grid">
        {/* 左侧：说明区域 */}
        <div>
          <div className="card" style={{ padding: '1.5rem', marginBottom: '1rem' }}>
            <h2 style={{ marginBottom: '1rem' }}>📝 我的帖子</h2>

            {myPostsLoading ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>加载中...</div>
            ) : myPostsError ? (
              <div className="error-message" role="alert">
                {(myPostsError as any)?.response?.data?.message || '加载失败'}
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

            <button
              className="btn"
              onClick={() => navigate('/posts')}
              style={{ width: '100%', marginTop: '1rem' }}
            >
              查看全部帖子
            </button>
          </div>

          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 style={{ marginBottom: '1rem' }}>💬 我的评论</h2>
            {myCommentsLoading ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>加载中...</div>
            ) : myCommentsError ? (
              <div className="error-message" role="alert">
                {(myCommentsError as any)?.response?.data?.message || '加载失败'}
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
                    <div style={{ fontSize: '0.9rem', color: '#333', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
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

        {/* 右侧：最新通知 */}
        <div>
          <div className="card" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h2 style={{ margin: 0 }}>🔔 最新通知</h2>
              {unreadCount > 0 && (
                <span style={{
                  background: '#e74c3c',
                  color: '#fff',
                  padding: '0.25rem 0.5rem',
                  borderRadius: '10px',
                  fontSize: '0.8rem'
                }}>
                  {unreadCount} 条未读
                </span>
              )}
            </div>

            {notificationsLoading ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>加载中...</div>
            ) : recentNotifications.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>暂无通知</div>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0 }}>
                {recentNotifications.map((notif) => (
                  <li
                    key={notif.id}
                    style={{
                      padding: '0.75rem 0',
                      borderBottom: '1px solid #eee',
                      cursor: 'pointer'
                    }}
                    onClick={() => {
                      if (notif.relatedId) {
                        navigate(`/posts/${notif.relatedId}`);
                      }
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <span style={{ marginRight: '0.5rem' }}>
                        {notif.type === 'REPLY' ? '💬' : '🔔'}
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{
                          fontWeight: notif.isRead ? 'normal' : '600',
                          fontSize: '0.9rem',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          {notif.title}
                        </div>
                        <div style={{
                          fontSize: '0.8rem',
                          color: '#999',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          {notif.content}
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <button
              className="btn"
              onClick={() => navigate('/notifications')}
              style={{ width: '100%', marginTop: '1rem' }}
            >
              查看全部通知
            </button>
          </div>
        </div>
      </div>

      {/* 退出登录 */}
      <div style={{ marginTop: '2rem', textAlign: 'center' }}>
        <button
          onClick={logout}
          className="btn btn-danger"
        >
          退出登录
        </button>
      </div>
    </div>
  );
}
