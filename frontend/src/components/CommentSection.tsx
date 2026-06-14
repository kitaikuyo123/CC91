import { useState, type FormEvent } from 'react';
import SafeLink from './SafeLink';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import {
  getCommentsByPostId,
  createComment,
  replyToComment,
  deleteComment,
  updateComment
} from '../api/comment';
import type { Comment } from '../api/comment';
import { submitReport } from '../api/report';
import { queryKeys } from '../lib/queryKeys';
import PostCard from './PostCard';
import ReportDialog from './ReportDialog';

interface CommentSectionProps {
  postId: number;
  commentCount?: number;
  topicAuthorUsername?: string;
  onQuoteTriggered?: (username: string, content: string) => void;
}

function CommentReplyForm({ commentId, onReplySubmitted, isPending, onCancel, placeholder }: {
  commentId: number;
  onReplySubmitted: (commentId: number, content: string) => void;
  isPending: boolean;
  onCancel: () => void;
  placeholder?: string;
}) {
  const [content, setContent] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    onReplySubmitted(commentId, content);
    setContent('');
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: '0.75rem', width: '100%' }}>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder || '写下你的回复...'}
        className="form-control"
        style={{
          width: '100%',
          minHeight: '70px',
          padding: '0.5rem',
          border: '1px solid var(--border-color)',
          backgroundColor: 'var(--quote-bg)',
          color: 'var(--text-main)',
          borderRadius: 'var(--cc98-radius)',
          fontFamily: 'inherit',
          resize: 'vertical',
          fontSize: '0.85rem'
        }}
        disabled={isPending}
      />
      <div style={{ marginTop: '0.4rem', display: 'flex', gap: '0.4rem' }}>
        <button
          type="submit"
          className="btn btn-primary"
          style={{ fontSize: '0.78rem', padding: '0.2rem 0.8rem' }}
          disabled={isPending || !content.trim()}
        >
          {isPending ? '发送中...' : '发送回复'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="btn"
          style={{ fontSize: '0.78rem', padding: '0.2rem 0.8rem', border: '1px solid var(--border-color)' }}
        >
          取消
        </button>
      </div>
    </form>
  );
}

const MAX_REPLY_DEPTH = 5;

/**
 * 递归回复项组件
 */
function ReplyItem({
  reply,
  depth,
  activeReplyCommentId,
  onToggleReply,
  onReplySubmitted,
  replyPending,
  onDelete,
  onReport,
  onEdit,
  editingCommentId,
  editContent,
  onEditContentChange,
  onSaveEdit,
  onCancelEdit,
  currentUser,
  isAuthenticated,
}: {
  reply: Comment;
  depth: number;
  activeReplyCommentId: number | null;
  onToggleReply: (id: number) => void;
  onReplySubmitted: (commentId: number, content: string) => void;
  replyPending: boolean;
  onDelete: (id: number) => void;
  onReport: (id: number) => void;
  onEdit: (id: number, content: string) => void;
  editingCommentId: number | null;
  editContent: string;
  onEditContentChange: (v: string) => void;
  onSaveEdit: (id: number) => void;
  onCancelEdit: () => void;
  currentUser: any;
  isAuthenticated: boolean;
}) {
  const isEditing = editingCommentId === reply.id;
  const isReplyOpen = activeReplyCommentId === reply.id;

  return (
    <div className="cc98-nested-reply-item">
      <div className="cc98-nested-reply-meta">
        <span>
          <i className="fa fa-comments-o"></i> <strong>{reply.authorUsername}</strong>
        </span>
        <span className="time">{new Date(reply.createdAt).toLocaleString('zh-CN')}</span>
      </div>

      {isEditing ? (
        <div style={{ marginTop: '0.3rem' }}>
          <textarea
            value={editContent}
            onChange={(e) => onEditContentChange(e.target.value)}
            className="form-control"
            style={{
              width: '100%', minHeight: '60px', padding: '0.4rem',
              fontSize: '0.85rem', resize: 'vertical',
              border: '1px solid var(--border-color)',
              backgroundColor: 'var(--quote-bg)',
              color: 'var(--text-main)',
              borderRadius: 'var(--cc98-radius)',
            }}
          />
          <div style={{ marginTop: '0.3rem', display: 'flex', gap: '0.3rem' }}>
            <button className="btn btn-primary" style={{ fontSize: '0.78rem', padding: '0.2rem 0.6rem' }} onClick={() => onSaveEdit(reply.id)}>保存</button>
            <button className="btn" style={{ fontSize: '0.78rem', padding: '0.2rem 0.6rem', border: '1px solid var(--border-color)' }} onClick={onCancelEdit}>取消</button>
          </div>
        </div>
      ) : (
        <div className="cc98-nested-reply-content">{reply.content}</div>
      )}

      <div style={{ marginTop: '0.2rem', display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', flexWrap: 'wrap' }}>
        {isAuthenticated && (
          <button onClick={() => onToggleReply(reply.id)} className="cc98-nested-trigger-btn" style={{ marginBottom: isReplyOpen ? '0.3rem' : '0' }}>
            <i className="fa fa-reply"></i> 回复
          </button>
        )}
        {isAuthenticated && (
          <button onClick={() => onReport(reply.id)} className="cc98-nested-delete-btn" style={{ color: '#fb6165' }}>
            举报
          </button>
        )}
        {currentUser?.username === reply.authorUsername && !isEditing && (
          <>
            <button onClick={() => onEdit(reply.id, reply.content)} className="cc98-nested-delete-btn">
              编辑
            </button>
            <button onClick={() => onDelete(reply.id)} className="cc98-nested-delete-btn">
              删除
            </button>
          </>
        )}
      </div>

      {isReplyOpen && (
        <CommentReplyForm
          commentId={reply.id}
          onReplySubmitted={onReplySubmitted}
          isPending={replyPending}
          onCancel={() => onToggleReply(reply.id)}
          placeholder={`回复 @${reply.authorUsername}...`}
        />
      )}

      {/* 递归渲染子回复 */}
      {reply.replies && reply.replies.length > 0 && depth < MAX_REPLY_DEPTH && (
        <div className="cc98-nested-replies-block" style={{ marginTop: '0.5rem' }}>
          {reply.replies.map((child) => (
            <ReplyItem
              key={child.id}
              reply={child}
              depth={depth + 1}
              activeReplyCommentId={activeReplyCommentId}
              onToggleReply={onToggleReply}
              onReplySubmitted={onReplySubmitted}
              replyPending={replyPending}
              onDelete={onDelete}
              onReport={onReport}
              onEdit={onEdit}
              editingCommentId={editingCommentId}
              editContent={editContent}
              onEditContentChange={onEditContentChange}
              onSaveEdit={onSaveEdit}
              onCancelEdit={onCancelEdit}
              currentUser={currentUser}
              isAuthenticated={isAuthenticated}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * CC98 风格评论/回帖区域组件
 */
export default function CommentSection({
  postId,
  commentCount,
  topicAuthorUsername = '',
  onQuoteTriggered
}: CommentSectionProps) {
  const { user: currentUser, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');

  const [activeReplyCommentId, setActiveReplyCommentId] = useState<number | null>(null);
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');
  const [isReportOpen, setIsReportOpen] = useState(false);
  const [reportTargetId, setReportTargetId] = useState<number | null>(null);

  const { data: comments = [], isLoading, error: queryError } = useQuery({
    queryKey: queryKeys.comments.byPost(postId),
    queryFn: () => getCommentsByPostId(postId),
  });

  const createCommentMutation = useMutation({
    mutationFn: ({ postId, content }: { postId: number; content: string }) =>
      createComment(postId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setNewComment('');
      setError('');
    },
    onError: (err: any) => setError(err.response?.data?.message || '发表评论失败'),
  });

  const replyCommentMutation = useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      replyToComment(commentId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setActiveReplyCommentId(null);
      setError('');
    },
    onError: (err: any) => setError(err.response?.data?.message || '回复楼层失败'),
  });

  const deleteCommentMutation = useMutation({
    mutationFn: deleteComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setError('');
    },
    onError: (err: any) => setError(err.response?.data?.message || '删除楼层失败'),
  });

  const updateCommentMutation = useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      updateComment(commentId, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      setEditingCommentId(null);
      setEditContent('');
      setError('');
    },
    onError: (err: any) => setError(err.response?.data?.message || '修改评论失败'),
  });

  const submitReportMutation = useMutation({
    mutationFn: (data: { contentType: 'POST' | 'COMMENT'; contentId: number; reason: string; description?: string }) =>
      submitReport(data),
    onSuccess: () => {
      alert('举报提交成功，感谢您的配合！');
      setIsReportOpen(false);
    },
  });

  const handleToggleReply = (commentId: number) => {
    setActiveReplyCommentId(prev => prev === commentId ? null : commentId);
  };

  const handleReplySubmitted = (commentId: number, content: string) => {
    replyCommentMutation.mutate({ commentId, content });
  };

  const handleStartEdit = (commentId: number, currentContent: string) => {
    setEditingCommentId(commentId);
    setEditContent(currentContent);
  };

  const handleSaveEdit = (commentId: number) => {
    if (!editContent.trim()) return;
    updateCommentMutation.mutate({ commentId, content: editContent.trim() });
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditContent('');
  };

  const handleDeleteComment = (commentId: number) => {
    if (!confirm('确定要删除这条评论吗？')) return;
    deleteCommentMutation.mutate(commentId);
  };

  const handleOpenReport = (commentId: number) => {
    if (!isAuthenticated) { alert('请先登录！'); return; }
    setReportTargetId(commentId);
    setIsReportOpen(true);
  };

  const handleReportSubmit = async (reason: string, description: string) => {
    if (reportTargetId === null) return;
    await submitReportMutation.mutateAsync({
      contentType: 'COMMENT', contentId: reportTargetId, reason, description
    });
  };

  const handleSubmitComment = (e: FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    createCommentMutation.mutate({ postId, content: newComment });
  };

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>加载评论中...</p>
      </div>
    );
  }

  if (queryError) {
    return (
      <div style={{ marginTop: '2rem' }}>
        <div className="cc98-error-box">
          {(queryError as any)?.response?.data?.message || '加载评论失败'}
        </div>
      </div>
    );
  }

  // 共享 props 传给所有 ReplyItem
  const replyItemProps = {
    activeReplyCommentId,
    onToggleReply: handleToggleReply,
    onReplySubmitted: handleReplySubmitted,
    replyPending: replyCommentMutation.isPending,
    onDelete: handleDeleteComment,
    onReport: handleOpenReport,
    onEdit: handleStartEdit,
    editingCommentId,
    editContent,
    onEditContentChange: setEditContent,
    onSaveEdit: handleSaveEdit,
    onCancelEdit: handleCancelEdit,
    currentUser,
    isAuthenticated,
  };

  return (
    <div className="cc98-comments-section" style={{ marginTop: '2.5rem' }}>
      <div className="cc98-comments-title-bar">
        <i className="fa fa-comments"></i> 评论 ({commentCount ?? comments.length})
      </div>

      {error && (
        <div className="cc98-error-box" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {comments.length === 0 ? (
        <div className="cc98-empty-comments">
          <i className="fa fa-coffee" style={{ fontSize: '2.2rem', opacity: 0.4, display: 'block', marginBottom: '0.5rem' }}></i>
          还没有评论，快来抢沙发吧！
        </div>
      ) : (
        <div className="cc98-comments-list">
          {comments.map((comment, index) => {
            const floorNum = index + 2;
            return (
              <PostCard
                key={comment.id}
                id={comment.id}
                authorUsername={comment.authorUsername}
                authorAvatarUrl={comment.authorAvatarUrl}
                floor={floorNum}
                content={comment.content}
                createdAt={comment.createdAt}
                isTopicAuthor={comment.authorUsername === topicAuthorUsername}
                currentUserCanModify={currentUser?.username === comment.authorUsername}
                onDelete={() => handleDeleteComment(comment.id)}
                onQuote={onQuoteTriggered}
                onEdit={() => handleStartEdit(comment.id, comment.content)}
                isEditing={editingCommentId === comment.id}
                editContent={editContent}
                onEditContentChange={setEditContent}
                onSaveEdit={() => handleSaveEdit(comment.id)}
                onCancelEdit={handleCancelEdit}
                onReport={() => handleOpenReport(comment.id)}
              >
                {/* 嵌套的回复列表（递归渲染） */}
                {comment.replies && comment.replies.length > 0 && (
                  <div className="cc98-nested-replies-block">
                    {comment.replies.map((reply) => (
                      <ReplyItem
                        key={reply.id}
                        reply={reply}
                        depth={1}
                        {...replyItemProps}
                      />
                    ))}
                  </div>
                )}

                {/* 根评论回复按钮 */}
                {isAuthenticated && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <button
                      onClick={() => handleToggleReply(comment.id)}
                      className="cc98-nested-trigger-btn"
                      style={{ marginBottom: activeReplyCommentId === comment.id ? '0.5rem' : '0' }}
                    >
                      <i className="fa fa-reply"></i> 回复
                    </button>
                    {activeReplyCommentId === comment.id && (
                      <CommentReplyForm
                        commentId={comment.id}
                        onReplySubmitted={handleReplySubmitted}
                        isPending={replyCommentMutation.isPending}
                        onCancel={() => setActiveReplyCommentId(null)}
                        placeholder={`回复 @${comment.authorUsername}...`}
                      />
                    )}
                  </div>
                )}
              </PostCard>
            );
          })}
        </div>
      )}

      {/* 底部回帖编辑器 */}
      {isAuthenticated ? (
        <div className="cc98-floor-reply-box" style={{ marginTop: '2.5rem' }}>
          <div className="cc98-editor-header">
            <i className="fa fa-pencil"></i> 快速发表回复楼层
          </div>
          <form onSubmit={handleSubmitComment} style={{ padding: '1.25rem' }}>
            <textarea
              id="new-comment"
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="发表你的看法..."
              className="form-control"
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
              disabled={createCommentMutation.isPending}
            />
            <button
              type="submit"
              className="cc98-reply-submit-btn"
              disabled={createCommentMutation.isPending || !newComment.trim()}
            >
              {createCommentMutation.isPending ? '发表中...' : '发表评论'}
            </button>
          </form>
        </div>
      ) : (
        <div className="cc98-login-tip-box">
          请在右侧导航栏或点击 <SafeLink to="/login" style={{ color: 'var(--primary-color)', fontWeight: 'bold' }}>登录</SafeLink> 后发表讨论楼层哦 💖
        </div>
      )}

      {/* 举报弹窗 */}
      <ReportDialog
        isOpen={isReportOpen}
        onClose={() => setIsReportOpen(false)}
        onSubmit={handleReportSubmit}
        isSubmitting={submitReportMutation.isPending}
        contentType="COMMENT"
      />

      <style>{`
        .cc98-comments-title-bar {
          background-color: var(--primary-color);
          color: white;
          padding: 0.75rem 1.25rem;
          font-weight: bold;
          font-size: 0.95rem;
          border-radius: var(--cc98-radius) var(--cc98-radius) 0 0;
          border-bottom: 2px solid var(--accent-color);
          margin-bottom: 1.25rem;
        }

        .theme-dark .cc98-comments-title-bar {
          border-bottom-color: var(--border-color);
        }

        .cc98-empty-comments {
          padding: 3.5rem 1rem;
          text-align: center;
          color: var(--text-muted);
          background-color: var(--card-bg);
          border: 1px dashed var(--border-color);
          border-radius: var(--cc98-radius);
          font-size: 0.92rem;
        }

        .cc98-error-box {
          background-color: rgba(251, 97, 101, 0.1);
          color: #fb6165;
          padding: 0.75rem 1.25rem;
          border-radius: var(--cc98-radius);
          border: 1px solid rgba(251, 97, 101, 0.2);
          font-size: 0.9rem;
        }

        .cc98-nested-replies-block {
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          border-left: 4px solid var(--primary-color);
          border-radius: var(--cc98-radius);
          padding: 0.6rem 0.8rem;
          margin-top: 0.5rem;
          margin-bottom: 0.5rem;
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
        }

        .cc98-nested-reply-item {
          border-bottom: 1px dashed var(--border-color);
          padding-bottom: 0.4rem;
        }

        .cc98-nested-reply-item:last-child {
          border-bottom: none;
          padding-bottom: 0;
        }

        .cc98-nested-reply-meta {
          display: flex;
          justify-content: space-between;
          font-size: 0.78rem;
          color: var(--text-muted);
          margin-bottom: 0.2rem;
        }

        .cc98-nested-reply-content {
          font-size: 0.85rem;
          color: var(--text-main);
          line-height: 1.45;
          word-break: break-all;
        }

        .cc98-nested-delete-btn {
          background: none;
          border: none;
          color: #fb6165;
          cursor: pointer;
          font-size: 0.75rem;
          padding: 0;
        }

        .cc98-nested-delete-btn:hover {
          text-decoration: underline;
        }

        .cc98-nested-trigger-btn {
          background: none;
          border: 1px solid var(--border-color);
          color: var(--text-muted);
          font-size: 0.78rem;
          padding: 0.2rem 0.6rem;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          transition: var(--cc98-transition);
        }

        .cc98-nested-trigger-btn:hover {
          color: var(--primary-color);
          border-color: var(--primary-color);
          background-color: var(--quote-bg);
        }

        .cc98-floor-reply-box {
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-editor-header {
          background-color: var(--primary-color);
          color: white;
          padding: 0.75rem 1.25rem;
          font-weight: bold;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          gap: 0.5rem;
          border-bottom: 2px solid var(--accent-color);
        }

        .theme-dark .cc98-editor-header {
          border-bottom-color: var(--border-color);
        }

        .cc98-reply-submit-btn {
          background-color: var(--primary-color);
          color: white;
          border: none;
          padding: 0.5rem 1.75rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.9rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .cc98-reply-submit-btn:hover:not(:disabled) {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-1px);
        }

        .cc98-reply-submit-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .cc98-login-tip-box {
          padding: 1.5rem;
          text-align: center;
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          color: var(--text-muted);
          font-size: 0.9rem;
          font-weight: 500;
        }
      `}</style>
    </div>
  );
}
