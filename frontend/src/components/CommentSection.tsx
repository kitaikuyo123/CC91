import { useState, type FormEvent } from 'react';
import SafeLink from './SafeLink';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import {
  getCommentsByPostId,
  createComment,
  replyToComment,
  deleteComment
} from '../api/comment';
import { queryKeys } from '../lib/queryKeys';
import PostCard from './PostCard';

interface CommentSectionProps {
  postId: number;
  commentCount?: number;
  topicAuthorUsername?: string;
  onQuoteTriggered?: (username: string, content: string) => void;
}

interface CommentReplyFormProps {
  commentId: number;
  onReplySubmitted: (commentId: number, content: string) => void;
  isPending: boolean;
  onCancel: () => void;
}

function CommentReplyForm({ commentId, onReplySubmitted, isPending, onCancel }: CommentReplyFormProps) {
  const [content, setContent] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    onReplySubmitted(commentId, content);
    setContent('');
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: '1rem', width: '100%' }}>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="写下你的回复..."
        className="form-control"
        style={{
          width: '100%',
          minHeight: '80px',
          padding: '0.6rem',
          border: '1px solid var(--border-color)',
          backgroundColor: 'var(--quote-bg)',
          color: 'var(--text-main)',
          borderRadius: 'var(--cc98-radius)',
          fontFamily: 'inherit',
          resize: 'vertical',
          fontSize: '0.9rem'
        }}
        disabled={isPending}
      />
      <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem' }}>
        <button
          type="submit"
          className="btn btn-primary"
          style={{ fontSize: '0.82rem', padding: '0.3rem 1rem' }}
          disabled={isPending || !content.trim()}
        >
          {isPending ? '发送中...' : '发送回复'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="btn"
          style={{ fontSize: '0.82rem', padding: '0.3rem 1rem', border: '1px solid var(--border-color)' }}
        >
          取消
        </button>
      </div>
    </form>
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
  
  // Track which comment has an open reply form
  const [activeReplyCommentId, setActiveReplyCommentId] = useState<number | null>(null);

  // 获取评论列表
  const { data: comments = [], isLoading, error: queryError } = useQuery({
    queryKey: queryKeys.comments.byPost(postId),
    queryFn: () => getCommentsByPostId(postId),
  });

  // 创建评论 (发表新楼层)
  const createCommentMutation = useMutation({
    mutationFn: ({ postId, content }: { postId: number; content: string }) =>
      createComment(postId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setNewComment('');
      setError('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '发表评论失败');
    },
  });

  // 回复评论
  const replyCommentMutation = useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      replyToComment(commentId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setActiveReplyCommentId(null);
      setError('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '回复楼层失败');
    },
  });

  // 删除评论
  const deleteCommentMutation = useMutation({
    mutationFn: deleteComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setError('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除楼层失败');
    },
  });

  // 发表新评论
  const handleSubmitComment = (e: FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    createCommentMutation.mutate({ postId, content: newComment });
  };

  // 提交子楼层回复
  const handleReplySubmitted = (commentId: number, content: string) => {
    replyCommentMutation.mutate({ commentId, content });
  };

  // 删除某楼层评论
  const handleDeleteComment = (commentId: number) => {
    if (!confirm('确定要删除这条评论吗？')) return;
    deleteCommentMutation.mutate(commentId);
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

      {/* 楼层列表 */}
      {comments.length === 0 ? (
        <div className="cc98-empty-comments">
          <i className="fa fa-coffee" style={{ fontSize: '2.2rem', opacity: 0.4, display: 'block', marginBottom: '0.5rem' }}></i>
          还没有评论，快来抢沙发吧！
        </div>
      ) : (
        <div className="cc98-comments-list">
          {comments.map((comment, index) => {
            const floorNum = index + 2; // Floor 1 is the main post
            return (
              <PostCard
                key={comment.id}
                id={comment.id}
                authorUsername={comment.authorUsername}
                floor={floorNum}
                content={comment.content}
                createdAt={comment.createdAt}
                isTopicAuthor={comment.authorUsername === topicAuthorUsername}
                currentUserCanModify={currentUser?.username === comment.authorUsername}
                onDelete={() => handleDeleteComment(comment.id)}
                onQuote={onQuoteTriggered}
              >
                {/* 嵌套的回复列表 (引用样式) */}
                {comment.replies && comment.replies.length > 0 && (
                  <div className="cc98-nested-replies-block">
                    {comment.replies.map((reply) => (
                      <div key={reply.id} className="cc98-nested-reply-item">
                        <div className="cc98-nested-reply-meta">
                          <span>
                            <i className="fa fa-comments-o"></i> <strong>{reply.authorUsername}</strong> 回复：
                          </span>
                          <span className="time">{new Date(reply.createdAt).toLocaleString('zh-CN')}</span>
                        </div>
                        <div className="cc98-nested-reply-content">{reply.content}</div>
                        {currentUser?.username === reply.authorUsername && (
                          <div style={{ textAlign: 'right', marginTop: '0.2rem' }}>
                            <button
                              onClick={() => handleDeleteComment(reply.id)}
                              className="cc98-nested-delete-btn"
                            >
                              删除
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                {/* 行内回复切换控制 */}
                {isAuthenticated && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <button
                      onClick={() => setActiveReplyCommentId(prev => prev === comment.id ? null : comment.id)}
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
                      />
                    )}
                  </div>
                )}
              </PostCard>
            );
          })}
        </div>
      )}

      {/* 楼层底部回帖编辑器 */}
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
          padding: 0.75rem 1rem;
          margin-top: 1rem;
          margin-bottom: 0.75rem;
          display: flex;
          flex-direction: column;
          gap: 0.6rem;
        }

        .cc98-nested-reply-item {
          border-bottom: 1px dashed var(--border-color);
          padding-bottom: 0.5rem;
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
          margin-bottom: 0.25rem;
        }

        .cc98-nested-reply-content {
          font-size: 0.88rem;
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
