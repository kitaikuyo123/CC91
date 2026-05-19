import type { Category } from '../api/category';

interface BoardCardProps {
  category: Category;
  onClick?: (id: number) => void;
}

/**
 * CC98 经典热门版块卡片组件
 */
export default function BoardCard({ category, onClick }: BoardCardProps) {
  const { id, name, description } = category;

  // Mock BBS parameters not provided by core Category interface
  const icon = id % 4 === 0 
    ? 'fa-laptop' 
    : id % 4 === 1 
      ? 'fa-heartbeat' 
      : id % 4 === 2 
        ? 'fa-gamepad' 
        : 'fa-comments-o';
  
  // Deterministic mock stats based on category ID
  const topicCount = Math.floor((id * 137 + 42) % 300) + 15;
  const replyCount = topicCount * 3 + Math.floor((id * 29) % 50);

  return (
    <div className="cc98-board-card card" onClick={() => onClick && onClick(id)}>
      {/* 1. Icon Container */}
      <div className="cc98-board-icon">
        <i className={`fa ${icon}`}></i>
      </div>

      {/* 2. Board details */}
      <div className="cc98-board-details">
        <div className="cc98-board-name">{name}</div>
        <div className="cc98-board-desc">{description || '暂无描述。'}</div>
        <div className="cc98-board-stats">
          <span>主题: <strong>{topicCount}</strong></span>
          <span className="bullet-dot">·</span>
          <span>回复: <strong>{replyCount}</strong></span>
        </div>
      </div>

      {/* 3. Small top right badge */}
      <span className="cc98-board-badge" title="主题帖总数">
        {topicCount} 贴
      </span>

      <style>{`
        .cc98-board-card {
          background: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 1.25rem 1.5rem;
          display: flex;
          align-items: center;
          gap: 1.25rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          position: relative;
          box-shadow: var(--cc98-shadow);
          min-width: 0;
        }
        
        .cc98-board-card:hover {
          transform: translateY(-4px) scale(1.02);
          border-color: var(--accent-color);
          box-shadow: rgba(0, 0, 0, 0.08) 0 8px 16px;
        }

        .theme-dark .cc98-board-card:hover {
          border-color: var(--primary-text);
        }

        .cc98-board-icon {
          width: 52px;
          height: 52px;
          border-radius: 10px;
          background-color: var(--quote-bg);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 1.65rem;
          border: 1px solid var(--border-color);
          color: var(--primary-text);
          flex-shrink: 0;
          transition: var(--cc98-transition);
        }
        
        .cc98-board-card:hover .cc98-board-icon {
          background-color: var(--primary-color);
          color: white;
          border-color: var(--primary-color);
        }

        .cc98-board-details {
          flex: 1;
          min-width: 0; /* 防止子元素文本太长破容器 */
        }

        .cc98-board-name {
          font-weight: bold;
          font-size: 1.05rem;
          color: var(--text-main);
          margin-bottom: 0.25rem;
        }

        .cc98-board-desc {
          font-size: 0.8rem;
          color: var(--text-muted);
          margin-bottom: 0.35rem;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .cc98-board-stats {
          font-size: 0.75rem;
          color: var(--text-muted);
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }
        
        .cc98-board-stats strong {
          color: var(--primary-text);
        }

        .bullet-dot {
          color: var(--border-color);
          font-weight: bold;
        }

        .cc98-board-badge {
          position: absolute;
          top: 0.6rem;
          right: 0.6rem;
          font-size: 0.7rem;
          font-weight: bold;
          padding: 0.1rem 0.4rem;
          border-radius: 4px;
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          color: var(--text-muted);
          user-select: none;
        }
      `}</style>
    </div>
  );
}
