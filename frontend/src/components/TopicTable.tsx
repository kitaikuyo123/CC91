import { Link, useNavigate } from 'react-router-dom';
import type { Post } from '../api/post';

interface TopicTableProps {
  posts?: Post[];
}

/**
 * CC98 经典主题帖列表表格组件 (绑定目标项目的 Post 实体数据)
 */
export default function TopicTable({ posts = [] }: TopicTableProps) {
  const navigate = useNavigate();
  const safePosts = posts || [];

  const handleRowClick = (postId: number, e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    // Don't trigger row navigation if clicking on specific links (like author profile or category)
    if (
      target.closest('.cc98-author-link') ||
      target.closest('.cc98-last-author') ||
      target.closest('.cc98-topic-table-category')
    ) {
      return;
    }
    navigate(`/posts/${postId}`);
  };

  if (safePosts.length === 0) {
    return (
      <div className="cc98-empty-table">
        <i className="fa fa-folder-open-o" style={{ fontSize: '2.5rem', display: 'block', marginBottom: '1rem', color: 'var(--text-muted)' }}></i>
        暂无帖子
      </div>
    );
  }

  // 格式化时间显示 (e.g. 刚刚，几分钟前，或者正常日期)
  const formatTime = (timeStr: string) => {
    try {
      const now = new Date();
      const date = new Date(timeStr.replace(/-/g, '/')); // 兼容 Safari
      const diffMs = now.getTime() - date.getTime();
      
      const diffMins = Math.floor(diffMs / (1000 * 60));
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      
      if (diffMins < 1) return '刚刚';
      if (diffMins < 60) return `${diffMins} 分钟前`;
      if (diffHours < 24) return `${diffHours} 小时前`;
      
      return timeStr.split('T')[0] || timeStr;
    } catch {
      return timeStr;
    }
  };

  return (
    <table className="cc98-topic-list">
      <thead className="cc98-tl-header">
        <tr>
          <th className="cc98-tl-cell cc98-tl-status" style={{ width: '45px' }}></th>
          <th className="cc98-tl-cell cc98-tl-title">主题标题</th>
          <th className="cc98-tl-cell cc98-tl-author" style={{ width: '130px' }}>作者</th>
          <th className="cc98-tl-cell cc98-tl-stats" style={{ width: '120px' }}>回复/点击</th>
          <th className="cc98-tl-cell cc98-tl-last" style={{ width: '180px' }}>最后发表</th>
        </tr>
      </thead>
      <tbody>
        {safePosts.map((post) => {
          const isSticky = post.status === 'STICKY';
          const replyCount = post.commentCount ?? 0;
          const viewCount = post.viewCount;

          return (
            <tr 
              key={post.id} 
              className={`cc98-tl-row ${isSticky ? 'sticky-row' : ''} card`}
              onClick={(e) => handleRowClick(post.id, e)}
              style={{ cursor: 'pointer' }}
            >
              {/* 1. Status Icon */}
              <td className="cc98-tl-cell cc98-tl-status">
                {isSticky ? (
                  <i className="fa fa-thumb-tack sticky" title="置顶主题"></i>
                ) : replyCount >= 10 ? (
                  <i className="fa fa-fire hot" title="热门讨论"></i>
                ) : (
                  <i className="fa fa-file-text-o normal" title="普通主题"></i>
                )}
              </td>

              {/* 2. Title with Category Tag */}
              <td className="cc98-tl-cell cc98-tl-title-cell">
                <div className="cc98-title-container">
                  {post.categoryId && post.categoryName && (
                    <Link 
                      to={`/category/${post.categoryId}`} 
                      className="cc98-topic-table-category"
                    >
                      [{post.categoryName}]
                    </Link>
                  )}

                  <Link 
                    to={`/posts/${post.id}`} 
                    className={`cc98-topic-link ${isSticky ? 'sticky-title' : ''}`}
                  >
                    {post.title}
                  </Link>
                  {replyCount >= 15 && (
                    <span className="cc98-hot-spark">HOT</span>
                  )}
                </div>
              </td>

              {/* 3. Author info */}
              <td className="cc98-tl-cell cc98-tl-author">
                <Link to={`/profile/${post.authorUsername}`} className="cc98-author-link">
                  {post.authorUsername}
                </Link>
                <div className="cc98-author-time">
                  {post.createdAt.split('T')[0]}
                </div>
              </td>

              {/* 4. Stats badges */}
              <td className="cc98-tl-cell cc98-tl-stats">
                <span className="cc98-stat-badge cc98-stat-replies" title="回复数">
                  {replyCount}
                </span>
                <span className="cc98-stat-divider">/</span>
                <span className="cc98-stat-badge cc98-stat-views" title="阅读量">
                  {viewCount}
                </span>
              </td>

              {/* 5. Last reply info */}
              <td className="cc98-tl-cell cc98-tl-last">
                <div className="cc98-last-by">
                  <Link to={`/profile/${post.authorUsername}`} className="cc98-last-author">
                    {post.authorUsername + '\u200B'}
                  </Link>
                </div>
                <div className="cc98-last-time">
                  {formatTime(post.updatedAt || post.createdAt)}
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>

      <style>{`
        .cc98-topic-list {
          width: 100%;
          border-collapse: collapse;
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          overflow: hidden;
          margin-bottom: 1.5rem;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
        }
        
        .cc98-tl-header {
          background-color: var(--primary-color);
          color: white;
        }
        
        .cc98-tl-header th {
          padding: 0.8rem;
          text-align: left;
          font-size: 0.88rem;
          font-weight: bold;
          border-bottom: 2px solid var(--accent-color);
        }
        
        .theme-dark .cc98-tl-header th {
          border-bottom-color: var(--border-color);
        }

        .cc98-tl-row {
          border-bottom: 1px solid var(--border-color);
          transition: var(--cc98-transition);
        }
        
        .cc98-tl-row:last-child {
          border-bottom: none;
        }

        .cc98-tl-row:hover {
          background-color: var(--quote-bg);
        }
        
        .sticky-row {
          background-color: rgba(218, 180, 77, 0.05);
        }

        .cc98-tl-cell {
          padding: 0.75rem 0.8rem;
          font-size: 0.92rem;
          vertical-align: middle;
        }

        .cc98-tl-status {
          text-align: center;
          color: var(--text-muted);
        }

        .cc98-tl-status i {
          font-size: 1.1rem;
        }

        .cc98-tl-status i.sticky {
          color: #fb6165;
        }

        .cc98-tl-status i.hot {
          color: var(--accent-color);
        }

        .cc98-tl-status i.normal {
          opacity: 0.6;
        }

        .cc98-title-container {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.25rem;
        }

        .cc98-topic-table-category {
          color: var(--primary-color);
          font-weight: bold;
          text-decoration: none;
          margin-right: 0.25rem;
          transition: var(--cc98-transition);
        }

        .cc98-topic-table-category:hover {
          color: var(--link-color);
          text-decoration: underline;
        }

        .cc98-topic-link {
          color: var(--text-main);
          text-decoration: none;
          font-weight: 500;
          transition: var(--cc98-transition);
        }

        .cc98-topic-link:hover {
          color: var(--link-color);
          text-decoration: underline;
        }

        .sticky-title {
          font-weight: bold;
          color: #fb6165 !important;
        }
        
        .theme-dark .sticky-title {
          color: #ff8084 !important;
        }

        .cc98-hot-spark {
          background-color: #fb6165;
          color: white;
          font-size: 0.65rem;
          font-weight: 900;
          padding: 0.05rem 0.25rem;
          border-radius: 3px;
          margin-left: 0.4rem;
          animation: sparkPulse 1.5s infinite ease-in-out;
        }

        @keyframes sparkPulse {
          0% { transform: scale(1); opacity: 0.8; }
          50% { transform: scale(1.1); opacity: 1; }
          100% { transform: scale(1); opacity: 0.8; }
        }

        .cc98-author-link {
          color: var(--text-main);
          font-weight: bold;
          font-size: 0.88rem;
        }

        .cc98-author-link:hover {
          color: var(--link-color);
        }

        .cc98-author-time {
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-top: 0.15rem;
        }

        .cc98-tl-stats {
          text-align: center;
          white-space: nowrap;
        }

        .cc98-stat-badge {
          display: inline-block;
          padding: 0.15rem 0.4rem;
          border-radius: 10px;
          font-size: 0.78rem;
          font-weight: bold;
        }

        .cc98-stat-views {
          background-color: #e0f2fe;
          color: #0369a1;
        }
        
        .theme-dark .cc98-stat-views {
          background-color: #1e3a8a;
          color: #93c5fd;
        }

        .cc98-stat-replies {
          background-color: #f0fdf4;
          color: #166534;
        }
        
        .theme-dark .cc98-stat-replies {
          background-color: #064e3b;
          color: #6ee7b7;
        }

        .cc98-stat-divider {
          color: var(--border-color);
          margin: 0 0.25rem;
          font-size: 0.8rem;
        }

        .cc98-last-by {
          font-weight: bold;
        }

        .cc98-last-author {
          color: var(--text-main);
          font-size: 0.85rem;
        }

        .cc98-last-author:hover {
          color: var(--link-color);
        }

        .cc98-last-time {
          font-size: 0.78rem;
          color: var(--text-muted);
          margin-top: 0.15rem;
        }

        .cc98-empty-table {
          padding: 3rem 1rem;
          text-align: center;
          color: var(--text-muted);
          background: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
        }
      `}</style>
    </table>
  );
}
