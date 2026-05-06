import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getNotifications, type Notification } from '../api/notification';
import { queryKeys } from '../lib/queryKeys';

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
    queryFn: () => import('../api/notification').then(m => m.getUnreadCount()),
    enabled: !!user,
  });

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1>欢迎回来, <span style={{ color: '#3498db' }}>{user?.username}</span>!</h1>
        <p style={{ color: '#666' }}>这是您的个人中心</p>
      </div>

      {/* 快捷操作 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div
          className="card"
          onClick={() => navigate('/posts/new')}
          style={{ cursor: 'pointer', textAlign: 'center', padding: '1.5rem' }}
        >
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>✏️</div>
          <div style={{ fontWeight: '500' }}>发帖</div>
        </div>

        <div
          className="card"
          onClick={() => navigate('/notifications')}
          style={{ cursor: 'pointer', textAlign: 'center', padding: '1.5rem', position: 'relative' }}
        >
          {unreadCount > 0 && (
            <span style={{
              position: 'absolute',
              top: '10px',
              right: '10px',
              background: '#e74c3c',
              color: '#fff',
              borderRadius: '50%',
              width: '20px',
              height: '20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '0.75rem'
            }}>
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🔔</div>
          <div style={{ fontWeight: '500' }}>通知</div>
        </div>

        <div
          className="card"
          onClick={() => navigate(`/profile/${user?.username}`)}
          style={{ cursor: 'pointer', textAlign: 'center', padding: '1.5rem' }}
        >
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>👤</div>
          <div style={{ fontWeight: '500' }}>我的资料</div>
        </div>

        {user?.role === 'ADMIN' && (
          <div
            className="card"
            onClick={() => navigate('/admin')}
            style={{ cursor: 'pointer', textAlign: 'center', padding: '1.5rem', background: '#2c3e50', color: '#fff' }}
          >
            <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>⚙️</div>
            <div style={{ fontWeight: '500' }}>管理后台</div>
          </div>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        {/* 左侧：说明区域 */}
        <div>
          <div className="card" style={{ padding: '1.5rem', marginBottom: '1rem' }}>
            <h2 style={{ marginBottom: '1rem' }}>📝 我的帖子</h2>
            <p style={{ color: '#666' }}>查看和管理您发布的所有帖子</p>
            <button
              className="btn btn-primary"
              onClick={() => navigate('/posts')}
              style={{ marginTop: '1rem' }}
            >
              查看帖子列表
            </button>
          </div>

          <div className="card" style={{ padding: '1.5rem' }}>
            <h2 style={{ marginBottom: '1rem' }}>💬 我的评论</h2>
            <p style={{ color: '#666' }}>查看您发表的评论和回复</p>
            <p style={{ fontSize: '0.9rem', color: '#999', marginTop: '1rem' }}>
              此功能正在开发中...
            </p>
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
          className="btn"
          style={{ background: '#e74c3c', color: '#fff' }}
        >
          退出登录
        </button>
      </div>
    </div>
  );
}
