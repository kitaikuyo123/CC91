import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getNotifications, getUnreadCount } from '../api/notification';
import { getMyComments, getMyPosts, getMyDrafts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';

/**
 * CC98 风格用户 Dashboard 个人中心页面
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

  // 获取我的草稿（仅最新一条）
  const { data: myDrafts = [] } = useQuery({
    queryKey: queryKeys.users.meDrafts(),
    queryFn: () => getMyDrafts(),
    enabled: !!user,
  });

  const formatTime = (timeStr: string) => {
    return timeStr.replace('T', ' ').split('.')[0] || timeStr;
  };

  return (
    <div className="cc98-dashboard-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '论坛首页', href: '/' },
          { label: '个人中心' }
        ]} 
      />

      {/* 2. 欢迎卡片 */}
      <div className="cc98-welcome-banner">
        <div className="banner-details">
          <h1>
            欢迎回来，<span className="username-highlight">{user?.username}</span>！
          </h1>
          <p>这是您的个人控制面板。在这里您可以管理主题帖、追踪回帖通知和编辑草稿。</p>
        </div>
        <div className="banner-actions">
          <button onClick={logout} className="cc98-logout-btn">
            <i className="fa fa-sign-out"></i> 安全退出
          </button>
        </div>
      </div>

      {/* 3. 快捷卡片网格 */}
      <div className="cc98-quick-actions-grid">
        <div className="cc98-quick-card" onClick={() => navigate('/posts/new')}>
          <div className="quick-icon"><i className="fa fa-pencil-square-o"></i></div>
          <div className="quick-title">发表新帖</div>
          <div className="quick-desc">发布讨论帖至各板块</div>
        </div>

        <div className="cc98-quick-card" onClick={() => navigate('/notifications')}>
          <div className="quick-icon" style={{ position: 'relative' }}>
            <i className="fa fa-bell-o"></i>
            {unreadCount > 0 && (
              <span className="badge-danger-dot">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </div>
          <div className="quick-title">消息通知</div>
          <div className="quick-desc">查看收到的回复和私信</div>
        </div>

        <div className="cc98-quick-card" onClick={() => navigate(`/profile/${user?.username}`)}>
          <div className="quick-icon"><i className="fa fa-user-circle-o"></i></div>
          <div className="quick-title">我的资料</div>
          <div className="quick-desc">编辑个人信息与头像</div>
        </div>

        {user?.role === 'ADMIN' && (
          <div className="cc98-quick-card admin-card" onClick={() => navigate('/admin')}>
            <div className="quick-icon"><i className="fa fa-cogs"></i></div>
            <div className="quick-title">管理后台</div>
            <div className="quick-desc">系统设置与板块管理</div>
          </div>
        )}
      </div>

      {/* 4. 双栏主要内容 */}
      <div className="cc98-dashboard-grid">
        {/* 左栏：我的帖子 & 我的评论 */}
        <div className="cc98-dashboard-main-col">
          {/* 我的帖子 */}
          <div className="cc98-panel-classic col-b">
            <div className="cc98-panel-title">
              <i className="fa fa-file-text-o"></i> 我的主题帖
            </div>
            <div className="cc98-panel-body">
              {myPostsLoading ? (
                <div className="loading-state">载入中...</div>
              ) : myPostsError ? (
                <div className="error-state">获取主题帖列表失败</div>
              ) : myPosts.length === 0 ? (
                <div className="empty-state">您还没有发表过任何主题帖</div>
              ) : (
                <ul className="cc98-dashboard-list">
                  {myPosts.slice(0, 5).map((post) => (
                    <li key={post.id} onClick={() => navigate(`/posts/${post.id}`)}>
                      <div className="item-title">{post.title}</div>
                      <div className="item-meta">
                        <span>发表于 {formatTime(post.createdAt)}</span>
                        <span>·</span>
                        <span>{post.commentCount ?? 0} 条评论</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <button className="cc98-panel-btn" onClick={() => navigate('/dashboard/posts')}>
                查看全部我的帖子
              </button>
            </div>
          </div>

          {/* 我的评论 */}
          <div className="cc98-panel-classic col-a">
            <div className="cc98-panel-title">
              <i className="fa fa-comments-o"></i> 我的回复评论
            </div>
            <div className="cc98-panel-body">
              {myCommentsLoading ? (
                <div className="loading-state">载入中...</div>
              ) : myCommentsError ? (
                <div className="error-state">获取回复列表失败</div>
              ) : myComments.length === 0 ? (
                <div className="empty-state">您还没有发表过任何回复</div>
              ) : (
                <ul className="cc98-dashboard-list">
                  {myComments.slice(0, 5).map((comment) => (
                    <li key={comment.id} onClick={() => navigate(`/posts/${comment.postId}`)}>
                      <div className="item-title comment-text">{comment.content}</div>
                      <div className="item-meta">
                        <span>回复于 {formatTime(comment.createdAt)}</span>
                        <span>·</span>
                        <span className="source-topic">来自主题：{comment.postTitle}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <button className="cc98-panel-btn" onClick={() => navigate('/dashboard/comments')}>
                查看所有我的回复
              </button>
            </div>
          </div>
        </div>

        {/* 右栏：最新通知 & 草稿箱 */}
        <div className="cc98-dashboard-side-col">
          {/* 最新通知 */}
          <div className="cc98-panel-classic col-red">
            <div className="cc98-panel-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span><i className="fa fa-bell-o"></i> 最新消息通知</span>
              {unreadCount > 0 && <span className="title-unread-badge">{unreadCount} 未读</span>}
            </div>
            <div className="cc98-panel-body">
              {notificationsLoading ? (
                <div className="loading-state">载入中...</div>
              ) : recentNotifications.length === 0 ? (
                <div className="empty-state">暂无新通知消息</div>
              ) : (
                <ul className="cc98-dashboard-list notif-list">
                  {recentNotifications.map((notif) => (
                    <li 
                      key={notif.id} 
                      className={notif.isRead ? '' : 'unread-item'}
                      onClick={() => notif.relatedId && navigate(`/posts/${notif.relatedId}`)}
                    >
                      <div className="notif-header">
                        <span className="notif-indicator">{notif.type === 'REPLY' ? '💬 回复' : '🔔 系统'}</span>
                        <span className="time">{formatTime(notif.createdAt)}</span>
                      </div>
                      <div className="item-title">{notif.title}</div>
                      <div className="item-desc">{notif.content}</div>
                    </li>
                  ))}
                </ul>
              )}
              <button className="cc98-panel-btn" onClick={() => navigate('/notifications')}>
                查看所有通知消息
              </button>
            </div>
          </div>

          {/* 草稿 */}
          <div className="cc98-panel-classic col-orange">
            <div className="cc98-panel-title">
              <i className="fa fa-pencil-square-o"></i> 草稿箱
            </div>
            <div className="cc98-panel-body">
              {myDrafts.length === 0 ? (
                <div className="empty-state">草稿箱空空如也</div>
              ) : (
                <ul className="cc98-dashboard-list draft-list">
                  {myDrafts.slice(0, 2).map((draft) => (
                    <li key={draft.id} onClick={() => navigate(`/posts/new?draftId=${draft.id}`)}>
                      <div className="item-title">
                        <span className="draft-tag">草稿</span>
                        {draft.title || '(暂无标题)'}
                      </div>
                      <div className="item-meta">
                        <span>保存于 {formatTime(draft.createdAt)}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <button className="cc98-panel-btn" onClick={() => navigate('/dashboard/drafts')}>
                打开我的草稿箱
              </button>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .cc98-welcome-banner {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-left: 6px solid var(--primary-color);
          border-radius: var(--cc98-radius);
          padding: 1.5rem;
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 2rem;
          box-shadow: var(--cc98-shadow);
          gap: 1.5rem;
        }

        .cc98-welcome-banner h1 {
          font-size: 1.35rem;
          font-weight: bold;
          margin: 0 0 0.5rem 0;
          color: var(--text-main);
        }

        .cc98-welcome-banner p {
          font-size: 0.88rem;
          color: var(--text-muted);
          margin: 0;
        }

        .username-highlight {
          color: var(--primary-color);
          font-weight: 800;
        }

        .cc98-logout-btn {
          background-color: transparent;
          border: 1px solid var(--border-color);
          color: #fb6165;
          padding: 0.45rem 1.25rem;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          font-weight: bold;
          font-size: 0.85rem;
          transition: var(--cc98-transition);
        }

        .cc98-logout-btn:hover {
          background-color: rgba(251, 97, 101, 0.1);
          border-color: #fb6165;
        }

        /* Quick cards */
        .cc98-quick-actions-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
          gap: 1.25rem;
          margin-bottom: 2.5rem;
        }

        .cc98-quick-card {
          background: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 1.5rem;
          text-align: center;
          cursor: pointer;
          transition: var(--cc98-transition);
          box-shadow: var(--cc98-shadow);
        }

        .cc98-quick-card:hover {
          transform: translateY(-4px);
          border-color: var(--primary-color);
        }

        .quick-icon {
          font-size: 2.2rem;
          color: var(--primary-color);
          margin-bottom: 0.75rem;
          display: inline-block;
        }

        .badge-danger-dot {
          position: absolute;
          top: -3px;
          right: -8px;
          background-color: #fb6165;
          color: white;
          font-size: 0.7rem;
          font-weight: bold;
          border-radius: var(--cc98-radius-pill);
          padding: 0.1rem 0.35rem;
          border: 2px solid var(--card-bg);
        }

        .quick-title {
          font-weight: bold;
          font-size: 1.05rem;
          color: var(--text-main);
          margin-bottom: 0.25rem;
        }

        .quick-desc {
          font-size: 0.78rem;
          color: var(--text-muted);
        }

        .cc98-quick-card.admin-card {
          border-top: 4px solid var(--accent-color);
        }

        /* Two column layout */
        .cc98-dashboard-grid {
          display: flex;
          gap: 1.75rem;
          align-items: flex-start;
        }

        .cc98-dashboard-main-col {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 2rem;
        }

        .cc98-dashboard-side-col {
          width: 340px;
          flex-shrink: 0;
          display: flex;
          flex-direction: column;
          gap: 2rem;
        }

        @media (max-width: 992px) {
          .cc98-dashboard-grid {
            flex-direction: column;
          }
          .cc98-dashboard-side-col {
            width: 100%;
          }
        }

        /* Panels styling */
        .cc98-panel-classic {
          border: 1px solid var(--border-color);
          border-radius: 4px;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-panel-classic.col-a { border-top: 6px solid var(--cc98-alt-color-a); }
        .cc98-panel-classic.col-b { border-top: 6px solid var(--cc98-alt-color-b); }
        .cc98-panel-classic.col-orange { border-top: 6px solid #f39c12; }
        .cc98-panel-classic.col-red { border-top: 6px solid #fb6165; }

        .cc98-panel-title {
          padding: 0.8rem 1.25rem;
          font-weight: bold;
          font-size: 0.95rem;
          color: var(--text-main);
          border-bottom: 1px solid var(--border-color);
          background-color: var(--quote-bg);
        }

        .title-unread-badge {
          background-color: #fb6165;
          color: white;
          font-size: 0.72rem;
          font-weight: bold;
          padding: 0.1rem 0.4rem;
          border-radius: 4px;
        }

        .cc98-panel-body {
          padding: 0.5rem 0;
        }

        .cc98-dashboard-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .cc98-dashboard-list li {
          padding: 0.8rem 1.5rem;
          border-bottom: 1px dashed var(--border-color);
          cursor: pointer;
          transition: var(--cc98-transition);
        }

        .cc98-dashboard-list li:hover {
          background-color: var(--quote-bg);
        }

        .item-title {
          font-weight: 500;
          font-size: 0.92rem;
          color: var(--text-main);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .item-title.comment-text {
          font-weight: normal;
          color: var(--text-main);
        }

        .item-meta {
          font-size: 0.78rem;
          color: var(--text-muted);
          display: flex;
          gap: 0.4rem;
          margin-top: 0.25rem;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .source-topic {
          font-weight: bold;
        }

        .cc98-panel-btn {
          width: calc(100% - 3rem);
          margin: 1rem 1.5rem;
          padding: 0.5rem;
          background-color: transparent;
          border: 1px solid var(--border-color);
          color: var(--text-muted);
          border-radius: 4px;
          font-size: 0.85rem;
          font-weight: bold;
          cursor: pointer;
          transition: var(--cc98-transition);
        }

        .cc98-panel-btn:hover {
          color: var(--primary-color);
          border-color: var(--primary-color);
          background-color: var(--quote-bg);
        }

        /* Notif list specifics */
        .notif-list li {
          border-bottom: 1px solid var(--border-color);
        }
        
        .unread-item {
          background-color: rgba(251, 97, 101, 0.04);
        }

        .notif-header {
          display: flex;
          justify-content: space-between;
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-bottom: 0.2rem;
        }

        .notif-indicator {
          font-weight: bold;
          color: var(--primary-color);
        }

        .unread-item .notif-indicator {
          color: #fb6165;
        }

        .item-desc {
          font-size: 0.8rem;
          color: var(--text-muted);
          margin-top: 0.15rem;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        /* Draft list specials */
        .draft-list li {
          border-bottom: 1px dashed var(--border-color);
        }

        .draft-tag {
          background-color: #f39c12;
          color: white;
          font-size: 0.7rem;
          font-weight: bold;
          padding: 0.05rem 0.3rem;
          border-radius: 3px;
          margin-right: 0.4rem;
        }

        .loading-state, .error-state, .empty-state {
          padding: 2.5rem 1.5rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.88rem;
        }
      `}</style>
    </div>
  );
}
