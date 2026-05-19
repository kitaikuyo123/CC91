import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getUnreadCount, getNotifications, markAsRead, type Notification } from '../api/notification';
import { queryKeys } from '../lib/queryKeys';

/**
 * 通知铃铛组件 - 显示在 Header 中
 */
export default function NotificationBell() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [showDropdown, setShowDropdown] = useState(false);

  // 获取未读通知数量
  const { data: unreadCount = 0 } = useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: getUnreadCount,
    enabled: isAuthenticated,
    refetchInterval: 30000, // 每30秒刷新
  });

  // 获取最近通知（仅当下拉框展开时）
  const { data: recentNotifications = [] } = useQuery({
    queryKey: queryKeys.notifications.list(0, 5),
    queryFn: () => getNotifications(0, 5),
    enabled: isAuthenticated && showDropdown,
  });

  // 标记已读 mutation
  const markAsReadMutation = useMutation({
    mutationFn: markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount() });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.lists() });
    },
  });

  const handleBellClick = () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setShowDropdown(!showDropdown);
  };

  const handleNotificationClick = (notif: Notification) => {
    setShowDropdown(false);
    if (!notif.isRead) {
      markAsReadMutation.mutate(notif.id);
    }
    if (notif.relatedId) {
      navigate(`/posts/${notif.relatedId}`);
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <button
        onClick={handleBellClick}
        style={{
          background: 'none',
          border: 'none',
          fontSize: '1.2rem',
          cursor: 'pointer',
          padding: '0.25rem',
          position: 'relative',
          color: 'inherit'
        }}
        aria-label={`通知${unreadCount > 0 ? `，${unreadCount}条未读` : ''}`}
        aria-expanded={showDropdown}
        aria-haspopup="true"
      >
        <span aria-hidden="true">&#x1F514;</span>
        {unreadCount > 0 && (
          <span className="badge badge-danger" style={{
            position: 'absolute',
            top: '-2px',
            right: '-2px',
            fontSize: '0.7rem',
            borderRadius: '10px',
            minWidth: '16px',
            textAlign: 'center'
          }}>
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {showDropdown && (
        <>
          <div
            style={{
              position: 'fixed',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              zIndex: 998
            }}
            onClick={() => setShowDropdown(false)}
          />
          <div style={{
            position: 'absolute',
            top: '100%',
            right: 0,
            width: '300px',
            maxHeight: '400px',
            background: '#fff',
            border: '1px solid #ddd',
            borderRadius: '4px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            zIndex: 999,
            overflow: 'hidden'
          }}>
            <div style={{
              padding: '0.75rem 1rem',
              borderBottom: '1px solid #eee',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <span style={{ fontWeight: '600' }}>通知</span>
              <button
                onClick={() => {
                  setShowDropdown(false);
                  navigate('/notifications');
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#3498db',
                  cursor: 'pointer',
                  fontSize: '0.85rem'
                }}
              >
                查看全部
              </button>
            </div>

            <div style={{ maxHeight: '350px', overflowY: 'auto' }}>
              {recentNotifications.length === 0 ? (
                <div style={{
                  padding: '2rem 1rem',
                  textAlign: 'center',
                  color: '#999',
                  fontSize: '0.9rem'
                }}>
                  暂无通知
                </div>
              ) : (
                recentNotifications.map((notif) => (
                  <div
                    key={notif.id}
                    onClick={() => handleNotificationClick(notif)}
                    style={{
                      padding: '0.75rem 1rem',
                      borderBottom: '1px solid #f0f0f0',
                      cursor: 'pointer',
                      backgroundColor: notif.isRead ? 'transparent' : '#f8f9fa'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <span style={{ marginRight: '0.5rem' }}>
                        {notif.type === 'REPLY' ? '💬' : notif.type === 'MENTION' ? '@' : '🔔'}
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{
                          fontWeight: notif.isRead ? 'normal' : '600',
                          fontSize: '0.9rem',
                          marginBottom: '0.25rem',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          {notif.title}
                        </div>
                        <div style={{
                          color: '#666',
                          fontSize: '0.8rem',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}>
                          {notif.content}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: '#999', marginTop: '0.25rem' }}>
                          {new Date(notif.createdAt).toLocaleString()}
                        </div>
                      </div>
                      {!notif.isRead && (
                        <span style={{
                          width: '6px',
                          height: '6px',
                          borderRadius: '50%',
                          backgroundColor: '#e74c3c',
                          flexShrink: 0,
                          marginLeft: '0.5rem'
                        }} />
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
