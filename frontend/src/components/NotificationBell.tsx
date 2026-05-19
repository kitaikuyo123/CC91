import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getUnreadCount, getNotifications, markAsRead, type Notification } from '../api/notification';
import { queryKeys } from '../lib/queryKeys';

/**
 * 通知铃铛组件 - 显示在 Header 中，重塑为 CC98 视觉风格
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
    },
  });

  const handleBellClick = (e: React.MouseEvent) => {
    e.preventDefault();
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
    <div className="cc98-bell-wrapper">
      <button
        onClick={handleBellClick}
        className="cc98-bell-trigger"
        aria-label={`通知${unreadCount > 0 ? `，${unreadCount}条未读` : ''}`}
        aria-expanded={showDropdown}
        aria-haspopup="true"
      >
        <i className="fa fa-bell-o"></i>
        {unreadCount > 0 && (
          <span className="cc98-bell-dot">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {showDropdown && (
        <>
          <div className="cc98-bell-overlay" style={{ position: 'fixed' }} onClick={() => setShowDropdown(false)} />
          <div className="cc98-bell-dropdown">
            <div className="cc98-bell-header">
              <span className="title">通知</span>
              <button
                onClick={() => {
                  setShowDropdown(false);
                  navigate('/notifications');
                }}
                className="view-all-btn"
              >
                查看全部
              </button>
            </div>

            <div className="cc98-bell-list">
              {recentNotifications.length === 0 ? (
                <div className="cc98-bell-empty">
                  暂无通知
                </div>
              ) : (
                recentNotifications.map((notif) => (
                  <div
                    key={notif.id}
                    onClick={() => handleNotificationClick(notif)}
                    className={`cc98-bell-item ${notif.isRead ? 'read' : 'unread'}`}
                  >
                    <div className="cc98-bell-item-content">
                      <span className="icon">
                        {notif.type === 'REPLY' ? '💬' : notif.type === 'MENTION' ? '@' : '🔔'}
                      </span>
                      <div className="details">
                        <div className="title-text">{notif.title}</div>
                        <div className="summary-text">{notif.content}</div>
                        <div className="time-text">
                          {new Date(notif.createdAt).toLocaleString()}
                        </div>
                      </div>
                      {!notif.isRead && <span className="red-dot-indicator" />}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}

      <style>{`
        .cc98-bell-wrapper {
          position: relative;
          display: inline-flex;
          align-items: center;
        }
        .cc98-bell-trigger {
          background: none;
          border: none;
          color: rgba(255, 255, 255, 0.9);
          font-size: 1.2rem;
          cursor: pointer;
          padding: 0.25rem;
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: var(--cc98-transition);
        }
        .cc98-bell-trigger:hover {
          color: white;
          text-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
        }
        .cc98-bell-dot {
          position: absolute;
          top: -2px;
          right: -4px;
          background-color: #fb6165;
          color: white;
          font-size: 0.65rem;
          font-weight: bold;
          border-radius: 10px;
          min-width: 14px;
          height: 14px;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0 3px;
          border: 1px solid var(--primary-color);
        }
        .cc98-bell-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          z-index: 998;
          cursor: default;
        }
        .cc98-bell-dropdown {
          position: absolute;
          top: calc(100% + 10px);
          right: -50px;
          width: 290px;
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          z-index: 999;
          overflow: hidden;
          animation: slideDownFade 0.15s ease-out;
        }
        .cc98-bell-header {
          padding: 0.75rem 1rem;
          border-bottom: 1px solid var(--border-color);
          display: flex;
          justify-content: space-between;
          align-items: center;
          background-color: var(--quote-bg);
        }
        .cc98-bell-header .title {
          font-weight: bold;
          font-size: 0.88rem;
          color: var(--primary-text);
        }
        .cc98-bell-header .view-all-btn {
          background: none;
          border: none;
          color: var(--link-color);
          cursor: pointer;
          font-size: 0.8rem;
          font-weight: bold;
        }
        .cc98-bell-header .view-all-btn:hover {
          text-decoration: underline;
        }
        .cc98-bell-list {
          max-height: 320px;
          overflow-y: auto;
        }
        .cc98-bell-empty {
          padding: 2rem 1rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.85rem;
        }
        .cc98-bell-item {
          padding: 0.75rem 1rem;
          border-bottom: 1px solid var(--border-color);
          cursor: pointer;
          transition: var(--cc98-transition);
        }
        .cc98-bell-item:last-child {
          border-bottom: none;
        }
        .cc98-bell-item:hover {
          background-color: var(--quote-bg);
        }
        .cc98-bell-item.unread {
          background-color: rgba(57, 70, 118, 0.03);
        }
        .cc98-bell-item-content {
          display: flex;
          align-items: flex-start;
          gap: 0.6rem;
          position: relative;
        }
        .cc98-bell-item-content .icon {
          font-size: 1rem;
          flex-shrink: 0;
          margin-top: 0.1rem;
        }
        .cc98-bell-item-content .details {
          flex: 1;
          min-width: 0;
        }
        .cc98-bell-item-content .title-text {
          font-weight: bold;
          font-size: 0.85rem;
          color: var(--text-main);
          margin-bottom: 0.15rem;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .cc98-bell-item.unread .title-text {
          color: var(--primary-text);
        }
        .cc98-bell-item-content .summary-text {
          color: var(--text-muted);
          font-size: 0.78rem;
          margin-bottom: 0.25rem;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .cc98-bell-item-content .time-text {
          font-size: 0.7rem;
          color: var(--text-muted);
          opacity: 0.8;
        }
        .cc98-bell-item-content .red-dot-indicator {
          width: 6px;
          height: 6px;
          background-color: #fb6165;
          border-radius: 50%;
          flex-shrink: 0;
          align-self: center;
        }
      `}</style>
    </div>
  );
}
