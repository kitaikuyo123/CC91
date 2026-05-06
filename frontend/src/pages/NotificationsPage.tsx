import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type Notification } from '../api/notification';
import { queryKeys } from '../lib/queryKeys';

/**
 * 通知页面组件
 */
export default function NotificationsPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  // 获取通知列表
  const { data: notifications = [], isLoading } = useQuery({
    queryKey: queryKeys.notifications.list(0, 50),
    queryFn: () => getNotifications(0, 50),
    enabled: isAuthenticated,
  });

  // 获取未读数
  const { data: unreadCount = 0 } = useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: getUnreadCount,
    enabled: isAuthenticated,
  });

  // 标记已读 mutation
  const markAsReadMutation = useMutation({
    mutationFn: markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list(0, 50) });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount() });
    },
  });

  // 标记全部已读 mutation
  const markAllAsReadMutation = useMutation({
    mutationFn: markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list(0, 50) });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount() });
    },
  });

  const handleNotificationClick = (notif: Notification) => {
    if (!notif.isRead) {
      markAsReadMutation.mutate(notif.id);
    }
    if (notif.relatedId) {
      navigate(`/posts/${notif.relatedId}`);
    }
  };

  const handleMarkAllRead = () => {
    markAllAsReadMutation.mutate();
  };

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="card" style={{ textAlign: 'center', padding: '2rem' }}>
          <p>请先登录查看通知</p>
          <button className="btn btn-primary" onClick={() => navigate('/login')}>
            前往登录
          </button>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return <div className="container" style={{ marginTop: '2rem' }}>加载中...</div>;
  }

  return (
    <div className="container" style={{ marginTop: '2rem', maxWidth: '800px' }}>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h1>通知</h1>
          {unreadCount > 0 && (
            <button
              className="btn"
              onClick={handleMarkAllRead}
              disabled={markAllAsReadMutation.isPending}
            >
              {markAllAsReadMutation.isPending ? '处理中...' : '全部标记为已读'}
            </button>
          )}
        </div>

        {notifications.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
            暂无通知
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {notifications.map((notif) => (
              <li
                key={notif.id}
                onClick={() => handleNotificationClick(notif)}
                style={{
                  padding: '1rem',
                  borderBottom: '1px solid #eee',
                  cursor: 'pointer',
                  backgroundColor: notif.isRead ? 'transparent' : '#f8f9fa',
                  transition: 'background 0.2s'
                }}
                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = notif.isRead ? '#f8f9fa' : '#e9ecef'}
                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = notif.isRead ? 'transparent' : '#f8f9fa'}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                  <span style={{
                    fontSize: '1.5rem',
                    marginRight: '0.75rem',
                    flexShrink: 0
                  }}>
                    {notif.type === 'REPLY' ? '💬' : notif.type === 'MENTION' ? '@' : '🔔'}
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: notif.isRead ? 'normal' : '600', marginBottom: '0.25rem' }}>
                      {notif.title}
                    </div>
                    <div style={{ color: '#666', fontSize: '0.9rem', marginBottom: '0.25rem' }}>
                      {notif.content}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#999' }}>
                      {new Date(notif.createdAt).toLocaleString()}
                    </div>
                  </div>
                  {!notif.isRead && (
                    <span style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      backgroundColor: '#e74c3c',
                      flexShrink: 0,
                      marginLeft: '0.5rem'
                    }} />
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
