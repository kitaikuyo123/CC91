import React, { useState } from 'react';
import SafeLink from './SafeLink';
import catAvatar from '../assets/cc98_avatar_cat.png';
import studentAvatar from '../assets/cc98_avatar_student.png';

interface PostCardProps {
  id: number;
  authorUsername: string;
  floor: number | string;
  content: string;
  createdAt: string;
  isTopicAuthor?: boolean;
  onQuote?: (username: string, content: string) => void;
  onDelete?: () => void;
  onEdit?: () => void;
  isDeleting?: boolean;
  currentUserCanModify?: boolean;
  children?: React.ReactNode;
}

// Generate deterministic hash code for user stats
function hashCode(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return Math.abs(hash);
}

/**
 * CC98 经典帖子楼层双栏卡片组件 (绑定真实 API 数据)
 */
export default function PostCard({
  id,
  authorUsername,
  floor,
  content,
  createdAt,
  isTopicAuthor = false,
  onQuote,
  onDelete,
  onEdit,
  isDeleting = false,
  currentUserCanModify = false,
  children
}: PostCardProps) {
  // Deterministic user stats based on username hash
  const hash = hashCode(authorUsername || 'anon');
  const postCount = (hash % 850) + 18;
  const fanCount = hash % 95;
  const reputation = (hash % 45) + 3;
  const gender = hash % 2 === 0 ? 'male' : 'female';
  const avatar = hash % 3 === 0 ? studentAvatar : catAvatar;
  const level = hash % 3 === 0 ? '晨光曦微' : hash % 3 === 1 ? '求是之子' : '风云巨擘';
  const signature = hash % 2 === 0 ? '行百里者半九十，心之所向素履以往。' : '浙大求是人，纵横天地间！ 🌟';

  // Support local likes/dislikes since the real backend doesn't save them
  const [likes, setLikes] = useState((hash % 12) + 1);
  const [dislikes, setDislikes] = useState(hash % 3);
  const [liked, setLiked] = useState(false);
  const [disliked, setDisliked] = useState(false);

  const handleLike = () => {
    if (liked) {
      setLikes(l => l - 1);
      setLiked(false);
    } else {
      setLikes(l => l + 1);
      setLiked(true);
      if (disliked) {
        setDislikes(d => d - 1);
        setDisliked(false);
      }
    }
  };

  const handleDislike = () => {
    if (disliked) {
      setDislikes(d => d - 1);
      setDisliked(false);
    } else {
      setDislikes(d => d + 1);
      setDisliked(true);
      if (liked) {
        setLikes(l => l - 1);
        setLiked(false);
      }
    }
  };

  // 格式化发帖内容，支持简单的 [quote] BBCode 标签解析
  const parsePostContent = (text: string) => {
    if (!text) return { __html: '' };

    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br />');

    // 正则解析 [quote]...[/quote]
    const quoteRegex = /\[quote\]([\s\S]*?)\[\/quote\]/gi;
    html = html.replace(quoteRegex, (_, quoteContent) => {
      const userMatch = quoteContent.match(/^([\w\d\u4e00-\u9fa5_-]+)\s+说道：/);
      const userName = userMatch ? userMatch[1] : '论坛会员';
      const cleanContent = userMatch
        ? quoteContent.replace(/^([\w\d\u4e00-\u9fa5_-]+)\s+说道：\n?/, '')
        : quoteContent;

      return `
        <div class="cc98-post-quote">
          <div style="font-weight: bold; margin-bottom: 0.4rem; color: var(--primary-text); display: flex; justify-content: space-between; font-size: 0.82rem;">
            <span><i class="fa fa-quote-left"></i> 引用自用户 @${userName} 的发言：</span>
          </div>
          <div style="padding-top: 0.4rem; border-top: 1px dashed var(--border-color); font-style: italic;">
            ${cleanContent}
          </div>
        </div>
      `;
    });

    return { __html: html };
  };

  return (
    <div className="cc98-post-card" id={`post-${id}`}>
      {/* 1. Left User Sidebar */}
      <div className="cc98-post-sidebar">
        {/* Left Column: Username & Stats */}
        <div className="cc98-post-sidebar-left">
          <div className="cc98-post-user-name" title={authorUsername}>
            <SafeLink to={`/profile/${authorUsername}`} style={{ color: 'white', textDecoration: 'none' }}>
              {authorUsername}
            </SafeLink>
          </div>
          
          <div className="cc98-user-level-badge">{level}</div>

          <div className="cc98-user-stats">
            <div className="cc98-stat-line">
              <span className="lbl">帖数</span>
              <span className="val">{postCount}</span>
            </div>
            <div className="cc98-stat-line">
              <span className="lbl">粉丝</span>
              <span className="val">{fanCount}</span>
            </div>
            <div className="cc98-stat-line">
              <span className="lbl">威望</span>
              <span className="val">{reputation}</span>
            </div>
          </div>
        </div>

        {/* Right Column: Avatar and Actions */}
        <div className="cc98-post-sidebar-right">
          <div className="cc98-post-avatar-wrap">
            <img
              src={avatar}
              alt={authorUsername}
              className="cc98-post-avatar"
            />
          </div>

          <div className="cc98-sidebar-buttons">
            <button className="cc98-sidebar-btn" onClick={() => alert(`关注了 ${authorUsername}`)}>
              关注
            </button>
            <button className="cc98-sidebar-btn" onClick={() => alert(`准备私信给 ${authorUsername}`)}>
              私信
            </button>
          </div>
        </div>

        {/* Gender Badge */}
        {gender === 'male' ? (
          <span className="cc98-gender-badge male" title="男生">
            <i className="fa fa-mars"></i>
          </span>
        ) : (
          <span className="cc98-gender-badge female" title="女生">
            <i className="fa fa-venus"></i>
          </span>
        )}
      </div>

      {/* 2. Right Content Area */}
      <div className="cc98-post-content-area">
        {/* Meta Row (Floor, Time, Modify/Delete actions) */}
        <div className="cc98-post-meta-row">
          <div className="cc98-post-time">
            发表于 {new Date(createdAt).toLocaleString('zh-CN')}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            {isTopicAuthor && (
              <span className="cc98-post-floor-indicator">
                楼主
              </span>
            )}
            <span className="cc98-post-floor-number">
              {typeof floor === 'number' ? `${floor} 楼` : floor}
            </span>
          </div>
        </div>

        {/* Body Content */}
        <div className="cc98-post-body-container">
          <div
            className="cc98-post-body"
            dangerouslySetInnerHTML={parsePostContent(content)}
          />
        </div>

        {/* Nested Comments/Replies Slot */}
        {children}

        {/* Signature */}
        {signature && (
          <div className="cc98-post-signature-container">
            <hr className="cc98-post-signature-divider" />
            <div className="cc98-post-signature">
              {signature}
            </div>
          </div>
        )}

        {/* Actions (Like, Dislike, Quote Reply, Edit/Delete if authorized) */}
        <div className="cc98-post-actions">
          {currentUserCanModify && (
            <div style={{ marginRight: 'auto', display: 'flex', gap: '0.75rem' }}>
              {onEdit && (
                <button onClick={onEdit} className="cc98-action-btn-link edit">
                  <i className="fa fa-pencil-square-o"></i> 编辑
                </button>
              )}
              {onDelete && (
                <button onClick={onDelete} disabled={isDeleting} className="cc98-action-btn-link delete">
                  <i className="fa fa-trash-o"></i> {isDeleting ? '删除中...' : '删除'}
                </button>
              )}
            </div>
          )}

          <div
            className={`cc98-action-item ${liked ? 'active liked' : ''}`}
            onClick={handleLike}
            title="赞同此楼发言"
          >
            <i className={`fa ${liked ? 'fa-thumbs-up' : 'fa-thumbs-o-up'}`}></i> 赞 ({likes})
          </div>
          <div
            className={`cc98-action-item ${disliked ? 'active disliked' : ''}`}
            onClick={handleDislike}
            title="不赞同此楼发言"
          >
            <i className={`fa ${disliked ? 'fa-thumbs-down' : 'fa-thumbs-o-down'}`}></i> 踩 ({dislikes})
          </div>
          {onQuote && (
            <div
              className="cc98-action-item"
              onClick={() => onQuote(authorUsername, content)}
              title="引用本楼内容发表回复"
            >
              <i className="fa fa-reply"></i> 引用
            </div>
          )}
        </div>
      </div>

      <style>{`
        .cc98-post-card {
          display: flex;
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          background-color: var(--card-bg);
          overflow: visible;
          margin-bottom: 1.5rem;
          box-shadow: var(--cc98-shadow);
        }
        
        .cc98-post-sidebar {
          width: 175px;
          background-color: var(--primary-color);
          color: rgba(255, 255, 255, 0.9);
          padding: 1.25rem 0.5rem;
          display: flex;
          flex-direction: row;
          justify-content: space-between;
          align-items: flex-start;
          position: relative;
          flex-shrink: 0;
          transition: var(--cc98-transition);
          border-top-left-radius: calc(var(--cc98-radius) - 1px);
          border-bottom-left-radius: calc(var(--cc98-radius) - 1px);
        }
        
        .theme-dark .cc98-post-sidebar {
          background-color: #1e1e24;
          border-right: 1px solid var(--border-color);
        }

        .cc98-post-sidebar-left {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          justify-content: flex-start;
          text-align: left;
          width: 82px;
        }

        .cc98-post-sidebar-right {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: flex-start;
          flex-shrink: 0;
          width: 72px;
        }

        .cc98-post-user-name {
          font-weight: bold;
          font-size: 1.05rem;
          margin-bottom: 0.15rem;
          color: white;
          display: block;
          max-width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          text-align: left;
        }

        .cc98-user-level-badge {
          background-color: rgba(255, 255, 255, 0.2);
          color: white;
          font-size: 0.65rem;
          padding: 0.05rem 0.35rem;
          border-radius: 4px;
          margin-bottom: 0.5rem;
          max-width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .cc98-gender-badge {
          position: absolute;
          top: 0.5rem;
          right: 0.2rem;
          font-size: 0.8rem;
          width: 1.3rem;
          height: 1.3rem;
          border: 1.5px solid var(--primary-color);
          border-radius: 50%;
          background-color: var(--primary-color);
          display: inline-flex;
          align-items: center;
          justify-content: center;
          z-index: 2;
        }

        .theme-dark .cc98-gender-badge {
          border-color: #1e1e24;
          background-color: #1e1e24;
        }

        .cc98-gender-badge i {
          color: white !important;
        }

        .cc98-post-avatar-wrap {
          position: relative;
          display: flex;
          justify-content: center;
        }

        .cc98-post-avatar {
          width: 54px;
          height: 54px;
          border-radius: 50%;
          border: 2px solid rgba(255, 255, 255, 0.85);
          background-color: white;
          object-fit: cover;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
        }

        .cc98-user-stats {
          width: 100%;
          font-size: 0.75rem;
          color: rgba(255, 255, 255, 0.9);
          display: flex;
          flex-direction: column;
          gap: 0.15rem;
          text-align: left;
        }
        
        .cc98-stat-line {
          display: flex;
          gap: 0.3rem;
        }

        .cc98-stat-line .lbl {
          color: rgba(255, 255, 255, 0.7);
        }

        .cc98-stat-line .val {
          color: white;
          font-weight: bold;
        }

        .cc98-sidebar-buttons {
          display: flex;
          gap: 0.2rem;
          width: 100%;
          margin-top: 0.5rem;
        }

        .cc98-sidebar-btn {
          flex: 1;
          padding: 0.1rem 0;
          font-size: 0.6rem;
          font-weight: bold;
          color: white;
          background: transparent;
          border: 1px solid rgba(255, 255, 255, 0.5);
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          transition: var(--cc98-transition);
          white-space: nowrap;
          text-align: center;
        }

        .cc98-sidebar-btn:hover {
          background: rgba(255, 255, 255, 0.15);
          border-color: white;
        }

        .cc98-post-content-area {
          flex: 1;
          padding: 1.25rem 1.5rem;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          position: relative;
          min-width: 0;
        }

        .cc98-post-meta-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 0.8rem;
          color: var(--text-muted);
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 0.5rem;
          margin-bottom: 1rem;
        }

        .cc98-post-floor-indicator {
          background-color: var(--primary-color);
          color: white;
          font-size: 0.75rem;
          font-weight: bold;
          padding: 0.1rem 0.4rem;
          border-radius: 4px;
          user-select: none;
        }

        .cc98-post-floor-number {
          color: var(--text-muted);
          font-weight: bold;
          font-size: 0.8rem;
        }

        .cc98-post-body-container {
          flex: 1;
          display: flex;
          flex-direction: column;
        }

        .cc98-post-body {
          font-size: 0.95rem;
          line-height: 1.65;
          color: var(--text-main);
          white-space: normal;
          word-break: break-all;
          margin-bottom: 1rem;
        }

        .cc98-post-signature-container {
          margin-top: auto;
        }

        .cc98-post-signature-divider {
          border: none;
          border-top: 1px dashed var(--border-color);
          margin: 1.25rem 0 0.5rem 0;
        }

        .cc98-post-signature {
          font-size: 0.78rem;
          color: var(--text-muted);
          font-style: italic;
          opacity: 0.8;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .cc98-post-actions {
          display: flex;
          justify-content: flex-end;
          gap: 1.25rem;
          margin-top: 1rem;
          padding-top: 0.6rem;
          border-top: 1px solid var(--border-color);
          align-items: center;
        }

        .cc98-action-btn-link {
          background: none;
          border: none;
          color: var(--text-muted);
          font-size: 0.8rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          padding: 0;
        }

        .cc98-action-btn-link:hover {
          color: var(--primary-text);
        }

        .cc98-action-btn-link.delete:hover {
          color: #fb6165;
        }

        .cc98-action-item {
          display: flex;
          align-items: center;
          gap: 0.3rem;
          font-size: 0.8rem;
          color: var(--text-muted);
          cursor: pointer;
          transition: var(--cc98-transition);
          user-select: none;
        }

        .cc98-action-item:hover {
          color: var(--primary-text);
        }

        .cc98-action-item.liked {
          color: var(--primary-color);
        }

        .cc98-action-item.disliked {
          color: #fb6165;
        }

        .cc98-post-quote {
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          border-left: 4px solid var(--primary-color);
          border-radius: var(--cc98-radius);
          padding: 0.75rem 1rem;
          margin-bottom: 1rem;
          font-size: 0.88rem;
          color: var(--text-main);
        }
      `}</style>
    </div>
  );
}
