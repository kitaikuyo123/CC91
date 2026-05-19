import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type Notification } from '../api/notification';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';

/**
 * CC98 风格的通知消息页面组件
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

  const formatTime = (timeStr: string) => {
    return timeStr.replace('T', ' ').split('.')[0] || timeStr;
  };

  if (!isAuthenticated) {
    return (
      <div className="container" style={{ marginTop: '3rem', maxWidth: '600px' }}>
        <div className="cc98-login-prompt">
          <i className="fa fa-lock" style={{ fontSize: '3rem', color: 'var(--primary-color)', marginBottom: '1rem' }}></i>
          <h2>请先登录查看通知</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>查看回帖通知与提及消息，请先验证您的身份。</p>
          <button className="btn btn-primary" onClick={() => navigate('/login')}>
            前往登录
          </button>
        </div>
        <style>{`
          .cc98-login-prompt {
            background-color: var(--card-bg);
            border: 1px solid var(--border-color);
            padding: 3.5rem 2rem;
            text-align: center;
            border-radius: var(--cc98-radius);
            box-shadow: var(--cc98-shadow);
          }
        `}</style>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>加载中...</p>
      </div>
    );
  }

  return (
    <div className="cc98-notifications-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '首页', href: '/' },
          { label: '个人中心', href: '/dashboard' },
          { label: '消息通知' }
        ]} 
      />

      {/* 2. 主体通知面板 */}
      <div className="cc98-panel-classic col-red">
        <div className="cc98-panel-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>
            <i className="fa fa-bell-o"></i> 通知
            {unreadCount > 0 && <span className="title-unread-badge" style={{ marginLeft: '0.5rem' }}>{unreadCount} 条新未读</span>}
          </span>
          {unreadCount > 0 && (
            <button
              className="cc98-mark-all-btn"
              onClick={handleMarkAllRead}
              disabled={markAllAsReadMutation.isPending}
            >
              <i className="fa fa-check-square-o"></i> {markAllAsReadMutation.isPending ? '标记中...' : '全部标记为已读'}
            </button>
          )}
        </div>

        <div className="cc98-panel-body" style={{ padding: 0 }}>
          {notifications.length === 0 ? (
            <div className="cc98-notif-empty">
              暂无通知
            </div>
          ) : (
            <ul className="cc98-notif-list">
              {notifications.map((notif) => (
                <li
                  key={notif.id}
                  className={notif.isRead ? 'read-item' : 'unread-item'}
                  onClick={() => handleNotificationClick(notif)}
                >
                  <div className="notif-item-header">
                    <span className="notif-type-tag">
                      {notif.type === 'REPLY' ? (
                        <><i className="fa fa-commenting-o"></i> 回帖</>
                      ) : notif.type === 'MENTION' ? (
                        <><i className="fa fa-at"></i> 提及</>
                      ) : (
                        <><i className="fa fa-info-circle"></i> 系统</>
                      )}
                    </span>
                    <span className="notif-time">{formatTime(notif.createdAt)}</span>
                  </div>

                  <h3 className="notif-item-title">{notif.title}</h3>
                  <div className="notif-item-body">{notif.content}</div>

                  {!notif.isRead && (
                    <span className="notif-unread-dot" title="未读消息" />
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <style>{`
        .cc98-panel-classic {
          border: 1px solid var(--border-color);
          border-radius: 4px;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-panel-classic.col-red {
          border-top: 6px solid #fb6165;
        }

        .cc98-panel-title {
          padding: 0.85rem 1.5rem;
          font-weight: bold;
          font-size: 1.05rem;
          color: var(--text-main);
          border-bottom: 1px solid var(--border-color);
          background-color: var(--quote-bg);
        }

        .title-unread-badge {
          background-color: #fb6165;
          color: white;
          font-size: 0.72rem;
          font-weight: bold;
          padding: 0.1rem 0.46rem;
          border-radius: var(--cc98-radius-pill);
        }

        .cc98-mark-all-btn {
          background-color: transparent;
          border: 1px solid var(--border-color);
          color: var(--text-main);
          font-size: 0.78rem;
          font-weight: bold;
          padding: 0.25rem 0.75rem;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          transition: var(--cc98-transition);
        }

        .cc98-mark-all-btn:hover {
          color: var(--primary-color);
          border-color: var(--primary-color);
          background-color: var(--quote-bg);
        }

        .cc98-notif-empty {
          padding: 4.5rem 1.5rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.95rem;
        }

        .cc98-notif-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .cc98-notif-list li {
          padding: 1.25rem 1.5rem;
          border-bottom: 1px solid var(--border-color);
          cursor: pointer;
          position: relative;
          transition: var(--cc98-transition);
        }

        .cc98-notif-list li:hover {
          background-color: var(--quote-bg);
        }

        .cc98-notif-list li.unread-item {
          background-color: rgba(251, 97, 101, 0.04);
        }

        .cc98-notif-list li.unread-item:hover {
          background-color: rgba(251, 97, 101, 0.08);
        }

        .notif-item-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 0.5rem;
        }

        .notif-type-tag {
          font-size: 0.75rem;
          font-weight: bold;
          color: var(--primary-color);
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          padding: 0.1rem 0.5rem;
          border-radius: 3px;
        }

        .unread-item .notif-type-tag {
          color: #fb6165;
          border-color: rgba(251, 97, 101, 0.2);
          background-color: rgba(251, 97, 101, 0.05);
        }

        .notif-time {
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .notif-item-title {
          font-size: 0.95rem;
          font-weight: bold;
          margin: 0 0 0.35rem 0;
          color: var(--text-main);
        }

        .unread-item .notif-item-title {
          color: var(--primary-text);
        }

        .notif-item-body {
          font-size: 0.88rem;
          color: var(--text-muted);
          line-height: 1.5;
        }

        .unread-item .notif-item-body {
          color: var(--text-main);
        }

        .notif-unread-dot {
          position: absolute;
          top: 1.4rem;
          right: 1.5rem;
          width: 8px;
          height: 8px;
          background-color: #fb6165;
          border-radius: 50%;
        }
      `}</style>
    </div>
  );
}
