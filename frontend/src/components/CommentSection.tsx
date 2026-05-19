import { useState, type FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import {
  getCommentsByPostId,
  createComment,
  replyToComment,
  deleteComment,
  type Comment
} from '../api/comment';
import { queryKeys } from '../lib/queryKeys';

interface CommentSectionProps {
  postId: number;
  commentCount?: number;
}

/**
 * 单个评论组件（递归渲染嵌套回复）
 */
function CommentItem({ comment, postId, currentUser, onDelete, onReply, isReplying, isSubmitting }: {
  comment: Comment;
  postId: number;
  currentUser: string | null;
  onDelete: (commentId: number) => void;
  onReply: (commentId: number, content: string) => void;
  isReplying: boolean;
  isSubmitting: boolean;
}) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyContent, setReplyContent] = useState('');

  const handleSubmitReply = async (e: FormEvent) => {
    e.preventDefault();
    if (!replyContent.trim()) return;
    onReply(comment.id, replyContent);
    setReplyContent('');
    setShowReplyForm(false);
  };

  const isAuthor = currentUser === comment.authorUsername;

  return (
    <div style={{ marginBottom: '1rem' }}>
      <div className="card" style={{ padding: '1rem', marginBottom: '0.5rem' }}>
        {/* 评论头部 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
          <div>
            <strong style={{ color: '#333' }}>{comment.authorUsername}</strong>
            <span style={{ marginLeft: '0.5rem', fontSize: '0.85rem', color: '#888' }}>
              {new Date(comment.createdAt).toLocaleString('zh-CN')}
            </span>
          </div>
          {isAuthor && (
            <button
              onClick={() => onDelete(comment.id)}
              className="btn btn-danger btn-sm"
              aria-label={`删除${comment.authorUsername}的评论`}
            >
              删除
            </button>
          )}
        </div>

        {/* 评论内容 */}
        <div style={{ color: '#333', lineHeight: '1.5', marginBottom: '0.5rem' }}>
          {comment.content}
        </div>

        {/* 回复按钮 */}
        {currentUser && (
          <button
            onClick={() => setShowReplyForm(!showReplyForm)}
            className="btn"
            style={{ padding: '0.25rem 0.75rem', fontSize: '0.85rem' }}
          >
            {showReplyForm ? '取消回复' : '回复'}
          </button>
        )}
      </div>

      {/* 回复表单 */}
      {showReplyForm && (
        <form onSubmit={handleSubmitReply} style={{ marginLeft: '1rem', marginBottom: '0.5rem' }}>
          <textarea
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            placeholder="写下你的回复..."
            className="form-control"
            style={{
              width: '100%',
              minHeight: '80px',
              padding: '0.5rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontFamily: 'inherit',
              resize: 'vertical'
            }}
            disabled={isSubmitting}
          />
          <div style={{ marginTop: '0.5rem' }}>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ fontSize: '0.85rem' }}
              disabled={isReplying || !replyContent.trim()}
            >
              {isReplying ? '发送中...' : '发送回复'}
            </button>
          </div>
        </form>
      )}

      {/* 嵌套回复 */}
      {comment.replies && comment.replies.length > 0 && (
        <div style={{ marginLeft: '1.5rem', borderLeft: '2px solid #e0e0e0', paddingLeft: '1rem' }}>
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              postId={postId}
              currentUser={currentUser}
              onDelete={onDelete}
              onReply={onReply}
              isReplying={isReplying}
              isSubmitting={isSubmitting}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * 评论区域组件
 */
export default function CommentSection({ postId, commentCount }: CommentSectionProps) {
  const { user: currentUser, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [newComment, setNewComment] = useState('');
  const [error, setError] = useState('');

  // 使用 React Query 获取评论列表
  const { data: comments = [], isLoading, error: queryError } = useQuery({
    queryKey: queryKeys.comments.byPost(postId),
    queryFn: () => getCommentsByPostId(postId),
  });

  // 创建评论的 mutation
  const createCommentMutation = useMutation({
    mutationFn: ({ postId, content }: { postId: number; content: string }) =>
      createComment(postId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      setNewComment('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '发表评论失败');
    },
  });

  // 回复评论的 mutation
  const replyCommentMutation = useMutation({
    mutationFn: ({ commentId, content }: { commentId: number; content: string }) =>
      replyToComment(commentId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '回复失败');
    },
  });

  // 删除评论的 mutation
  const deleteCommentMutation = useMutation({
    mutationFn: deleteComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.byPost(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除评论失败');
    },
  });

  // 发表新评论
  const handleSubmitComment = (e: FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    createCommentMutation.mutate({ postId, content: newComment });
  };

  // 回复评论
  const handleReply = (commentId: number, content: string) => {
    replyCommentMutation.mutate({ commentId, content });
  };

  // 删除评论
  const handleDelete = (commentId: number) => {
    if (!confirm('确定要删除这条评论吗？')) return;
    deleteCommentMutation.mutate(commentId);
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <span>加载评论中...</span>
      </div>
    );
  }

  // 显示查询错误
  if (queryError) {
    const errorMessage = (queryError as any)?.response?.data?.message || '加载评论失败';
    return (
      <div className="comment-section" style={{ marginTop: '2rem' }}>
        <h2 style={{ marginBottom: '1rem', fontSize: '1.5rem' }}>评论 ({commentCount ?? 0})</h2>
        <div className="error-message" role="alert" style={{ marginBottom: '1rem' }}>
          {errorMessage}
        </div>
      </div>
    );
  }

  return (
    <div className="comment-section" style={{ marginTop: '2rem' }}>
      <h2 style={{ marginBottom: '1rem', fontSize: '1.5rem' }}>评论 ({commentCount ?? comments.length})</h2>

      {/* 错误提示 */}
      {error && (
        <div className="error-message" role="alert" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {/* 发表评论表单 */}
      {isAuthenticated && (
        <form onSubmit={handleSubmitComment} style={{ marginBottom: '1.5rem' }}>
          <label htmlFor="new-comment" className="visually-hidden">发表评论</label>
          <textarea
            id="new-comment"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="发表你的看法..."
            className="form-control"
            style={{
              width: '100%',
              minHeight: '100px',
              padding: '0.75rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontFamily: 'inherit',
              fontSize: '1rem',
              resize: 'vertical',
              marginBottom: '0.5rem'
            }}
            disabled={createCommentMutation.isPending}
          />
          <button
            type="submit"
            className="btn btn-primary"
            disabled={createCommentMutation.isPending || !newComment.trim()}
          >
            {createCommentMutation.isPending ? '发表中...' : '发表评论'}
          </button>
        </form>
      )}

      {!isAuthenticated && (
        <div className="card" style={{ padding: '1rem', textAlign: 'center', color: 'var(--color-text-muted)', marginBottom: '1.5rem' }}>
          请<a href="/login">登录</a>后发表评论
        </div>
      )}

      {/* 评论列表 */}
      {comments.length === 0 ? (
        <div className="empty-state">还没有评论，快来抢沙发吧！</div>
      ) : (
        <div className="comments-list">
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              postId={postId}
              currentUser={currentUser?.username || null}
              onDelete={handleDelete}
              onReply={handleReply}
              isReplying={replyCommentMutation.isPending}
              isSubmitting={createCommentMutation.isPending || replyCommentMutation.isPending || deleteCommentMutation.isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
}
