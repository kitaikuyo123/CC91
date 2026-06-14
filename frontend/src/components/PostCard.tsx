import React, { useState } from 'react';
import SafeLink from './SafeLink';
import { sanitizeHtml, escapeHtml } from '../utils/sanitize';
import catAvatar from '../assets/cc98_avatar_cat.png';

interface PostCardProps {
  id: number;
  authorUsername: string;
  authorAvatarUrl?: string | null;
  floor: number | string;
  content: string;
  createdAt: string;
  updatedAt?: string;
  isTopicAuthor?: boolean;
  onQuote?: (username: string, content: string) => void;
  onDelete?: () => void;
  onEdit?: () => void;
  isDeleting?: boolean;
  currentUserCanModify?: boolean;
  children?: React.ReactNode;
  
  // New props for Likes and Bookmarks
  likeCount?: number;
  isLikedByCurrentUser?: boolean;
  isBookmarkedByCurrentUser?: boolean;
  onToggleLike?: () => void;
  onToggleBookmark?: () => void;

  // New props for Editing and Reporting
  isEditing?: boolean;
  editContent?: string;
  onEditContentChange?: (val: string) => void;
  onSaveEdit?: () => void;
  onCancelEdit?: () => void;
  onReport?: () => void;
}

/**
 * CC98 经典帖子楼层双栏卡片组件 (绑定真实 API 数据)
 */
export default function PostCard({
  id,
  authorUsername,
  authorAvatarUrl,
  floor,
  content,
  createdAt,
  updatedAt,
  isTopicAuthor = false,
  onQuote,
  onDelete,
  onEdit,
  isDeleting = false,
  currentUserCanModify = false,
  children,
  likeCount,
  isLikedByCurrentUser,
  isBookmarkedByCurrentUser,
  onToggleLike,
  onToggleBookmark,
  isEditing = false,
  editContent = '',
  onEditContentChange,
  onSaveEdit,
  onCancelEdit,
  onReport
}: PostCardProps) {
  // Deterministic user stats based on username hash
  const avatar = authorAvatarUrl || catAvatar;

  const hasLikeApi = onToggleLike !== undefined;
  const likes = likeCount ?? 0;
  const liked = !!isLikedByCurrentUser;

  const handleLike = () => {
    if (hasLikeApi) {
      onToggleLike?.();
    }
  };

  // 格式化发帖内容，支持简单的 [quote] BBCode 标签解析
  // 所有用户输入均经过 HTML 转义 + DOMPurify 双重净化，防止 XSS
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
      // escapeHtml 二次防护：确保用户名不会被注入 HTML
      const userName = userMatch ? escapeHtml(userMatch[1]) : '论坛会员';
      const rawContent = userMatch
        ? quoteContent.replace(/^([\w\d\u4e00-\u9fa5_-]+)\s+说道：\n?/, '')
        : quoteContent;

      return `
        <div class="cc98-post-quote">
          <div style="font-weight: bold; margin-bottom: 0.4rem; color: var(--primary-text); display: flex; justify-content: space-between; font-size: 0.82rem;">
            <span><i class="fa fa-quote-left"></i> 引用自用户 @${userName} 的发言：</span>
          </div>
          <div style="padding-top: 0.4rem; border-top: 1px dashed var(--border-color); font-style: italic;">
            ${rawContent}
          </div>
        </div>
      `;
    });

    return { __html: sanitizeHtml(html) };
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
          </div>
        </div>

      </div>

      {/* 2. Right Content Area */}
      <div className="cc98-post-content-area">
        {/* Meta Row (Floor, Time, Modify/Delete actions) */}
        <div className="cc98-post-meta-row">
          <div className="cc98-post-time">
            <span>发布于 {new Date(createdAt).toLocaleString('zh-CN')}</span>
            {updatedAt && updatedAt !== createdAt && (
              <span style={{ marginLeft: '1rem' }}>更新于 {new Date(updatedAt).toLocaleString('zh-CN')}</span>
            )}
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
          {isEditing ? (
            <div style={{ marginTop: '0.5rem', width: '100%' }}>
              <textarea
                value={editContent}
                onChange={(e) => onEditContentChange?.(e.target.value)}
                className="cc98-form-control"
                style={{
                  width: '100%',
                  minHeight: '120px',
                  padding: '0.75rem',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'var(--card-bg)',
                  color: 'var(--text-main)',
                  borderRadius: 'var(--cc98-radius)',
                  fontFamily: 'inherit',
                  fontSize: '0.95rem',
                  resize: 'vertical',
                  marginBottom: '0.75rem'
                }}
              />
              <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <button
                  type="button"
                  onClick={onSaveEdit}
                  className="cc98-edit-save-btn"
                  disabled={!editContent?.trim()}
                >
                  保存
                </button>
                <button
                  type="button"
                  onClick={onCancelEdit}
                  className="cc98-edit-cancel-btn"
                >
                  取消
                </button>
              </div>
            </div>
          ) : (
            <div
              className="cc98-post-body"
              dangerouslySetInnerHTML={parsePostContent(content)}
            />
          )}
        </div>

        {/* Nested Comments/Replies Slot */}
        {children}


        {/* Actions (Like, Dislike, Quote Reply, Edit/Delete if authorized) */}
        {!isEditing && (
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
            {onToggleBookmark && (
              <div
                className={`cc98-action-item ${isBookmarkedByCurrentUser ? 'active favorited' : ''}`}
                onClick={onToggleBookmark}
                title={isBookmarkedByCurrentUser ? '从收藏夹中移除' : '收藏此贴'}
              >
                <i className={`fa ${isBookmarkedByCurrentUser ? 'fa-star' : 'fa-star-o'}`}></i> {isBookmarkedByCurrentUser ? '已收藏' : '收藏'}
              </div>
            )}
            {onQuote && (
              <div
                className="cc98-action-item"
                onClick={() => onQuote(authorUsername, content)}
                title="引用本楼内容发表回复"
              >
                <i className="fa fa-reply"></i> 引用
              </div>
            )}
            {onReport && (
              <div
                className="cc98-action-item report-btn"
                onClick={onReport}
                title="举报本楼发言"
                style={{ color: '#fb6165' }}
              >
                <i className="fa fa-flag"></i> 举报
              </div>
            )}
          </div>
        )}
      </div>

      <style>{`
        .cc98-edit-save-btn {
          background-color: var(--primary-color);
          color: white;
          border: none;
          padding: 0.4rem 1.25rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.85rem;
          cursor: pointer;
          transition: var(--cc98-transition);
        }
        .cc98-edit-save-btn:hover:not(:disabled) {
          background-color: var(--accent-color);
          color: #333;
        }
        .cc98-edit-save-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
        .cc98-edit-cancel-btn {
          background-color: transparent;
          color: var(--text-muted);
          border: 1px solid var(--border-color);
          padding: 0.4rem 1.25rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.85rem;
          cursor: pointer;
          transition: var(--cc98-transition);
        }
        .cc98-edit-cancel-btn:hover {
          background-color: var(--quote-bg);
          color: var(--text-main);
        }
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
          padding: 1.25rem 1.0rem;
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

        .cc98-action-item.favorited {
          color: var(--accent-color);
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

        /* ========= PostCard 响应式适配 ========= */
        @media (max-width: 768px) {
          .cc98-post-card {
            flex-direction: column;
          }

          .cc98-post-sidebar {
            width: 100%;
            flex-direction: row;
            align-items: center;
            gap: 0.75rem;
            padding: 0.6rem 1rem;
            border-right: none;
            border-bottom: 2px solid rgba(255, 255, 255, 0.15);
            border-top-left-radius: calc(var(--cc98-radius) - 1px);
            border-top-right-radius: calc(var(--cc98-radius) - 1px);
            border-bottom-left-radius: 0;
          }

          .cc98-post-sidebar-left {
            flex-direction: row;
            align-items: center;
            gap: 0.5rem;
            width: auto;
            flex: 1;
          }

          .cc98-post-sidebar-right {
            flex-direction: row;
            align-items: center;
            gap: 0.5rem;
            width: auto;
            flex-shrink: 0;
          }

          .cc98-post-user-name {
            font-size: 0.9rem;
            margin-bottom: 0;
            max-width: 110px;
          }

          .cc98-post-avatar {
            width: 36px;
            height: 36px;
          }

          .cc98-user-stats {
            display: none;
          }

          .cc98-sidebar-buttons {
            display: none;
          }

          .cc98-post-content-area {
            padding: 0.75rem;
          }

          .cc98-post-meta-row {
            flex-direction: column;
            align-items: flex-start;
            gap: 0.25rem;
          }

          .cc98-post-time {
            font-size: 0.72rem;
          }

          .cc98-post-body {
            font-size: 0.88rem;
          }

          .cc98-post-actions {
            flex-wrap: wrap;
            gap: 0.75rem;
            justify-content: flex-start;
          }
        }
      `}</style>
    </div>
  );
}
