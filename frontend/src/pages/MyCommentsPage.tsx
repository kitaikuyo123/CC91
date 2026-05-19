import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyComments } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';

/**
 * CC98 风格的我的回复/评论页面
 */
export default function MyCommentsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myComments = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.meComments(),
    queryFn: () => getMyComments(),
    enabled: !!user,
  });

  const formatTime = (timeStr: string) => {
    return timeStr.replace('T', ' ').split('.')[0] || timeStr;
  };

  return (
    <div className="cc98-my-comments-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '首页', href: '/' },
          { label: '个人中心', href: '/dashboard' },
          { label: '我的回复' }
        ]} 
      />

      {/* 2. 主体 Panel */}
      <div className="cc98-panel-classic col-a">
        <div className="cc98-panel-title">
          <i className="fa fa-comments-o"></i> 我的回复评论历史 ({myComments.length} 条)
        </div>
        <div className="cc98-panel-body" style={{ padding: '0.5rem 0' }}>
          {isLoading ? (
            <div className="loading-state">载入回复历史数据中...</div>
          ) : error ? (
            <div className="error-state">获取回复列表失败</div>
          ) : myComments.length === 0 ? (
            <div className="empty-state">你还没有发表过任何评论回复</div>
          ) : (
            <ul className="cc98-dashboard-list">
              {myComments.map((c) => (
                <li key={c.id} onClick={() => navigate(`/posts/${c.postId}`)}>
                  <div className="item-title comment-text">{c.content}</div>
                  <div className="item-meta">
                    <span>回复时间: {formatTime(c.createdAt)}</span>
                    <span>·</span>
                    <span className="source-topic">所属主题帖: {c.postTitle}</span>
                  </div>
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

        .cc98-panel-classic.col-a {
          border-top: 6px solid var(--cc98-alt-color-a);
        }

        .cc98-panel-title {
          padding: 0.85rem 1.5rem;
          font-weight: bold;
          font-size: 1.05rem;
          color: var(--text-main);
          border-bottom: 1px solid var(--border-color);
          background-color: var(--quote-bg);
        }

        .cc98-panel-body {
          background-color: var(--card-bg);
        }

        .cc98-dashboard-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .cc98-dashboard-list li {
          padding: 1rem 1.5rem;
          border-bottom: 1px dashed var(--border-color);
          cursor: pointer;
          transition: var(--cc98-transition);
        }

        .cc98-dashboard-list li:hover {
          background-color: var(--quote-bg);
        }

        .item-title.comment-text {
          font-weight: normal;
          font-size: 0.95rem;
          color: var(--text-main);
          line-height: 1.5;
        }

        .item-meta {
          font-size: 0.78rem;
          color: var(--text-muted);
          display: flex;
          gap: 0.4rem;
          margin-top: 0.4rem;
        }

        .source-topic {
          font-weight: bold;
          color: var(--primary-color);
        }

        .loading-state, .error-state, .empty-state {
          padding: 3rem 1.5rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.9rem;
        }
      `}</style>
    </div>
  );
}
