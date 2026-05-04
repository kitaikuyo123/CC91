import { useState, useEffect, type FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  getCommentsByPostId,
  createComment,
  replyToComment,
  deleteComment,
  type Comment
} from '../api/comment';

interface CommentSectionProps {
  postId: number;
}

/**
 * 单个评论组件（递归渲染嵌套回复）
 */
function CommentItem({ comment, postId, currentUser, onDelete, onReply }: {
  comment: Comment;
  postId: number;
  currentUser: string | null;
  onDelete: (commentId: number) => void;
  onReply: (commentId: number, content: string) => void;
}) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyContent, setReplyContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmitReply = async (e: FormEvent) => {
    e.preventDefault();
    if (!replyContent.trim()) return;

    setIsSubmitting(true);
    try {
      await onReply(comment.id, replyContent);
      setReplyContent('');
      setShowReplyForm(false);
    } finally {
      setIsSubmitting(false);
    }
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
              className="btn"
              style={{ padding: '0.25rem 0.75rem', fontSize: '0.85rem', background: '#e74c3c', color: 'white' }}
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
              disabled={isSubmitting || !replyContent.trim()}
            >
              {isSubmitting ? '发送中...' : '发送回复'}
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
export default function CommentSection({ postId }: CommentSectionProps) {
  const { user: currentUser, isAuthenticated } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newComment, setNewComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 加载评论列表
  useEffect(() => {
    const fetchComments = async () => {
      try {
        setLoading(true);
        const data = await getCommentsByPostId(postId);
        setComments(data);
        setError('');
      } catch (err: any) {
        setError(err.response?.data?.message || '加载评论失败');
      } finally {
        setLoading(false);
      }
    };

    fetchComments();
  }, [postId]);

  // 发表新评论
  const handleSubmitComment = async (e: FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setIsSubmitting(true);
    try {
      const created = await createComment(postId, { content: newComment });
      setComments([...comments, created]);
      setNewComment('');
    } catch (err: any) {
      setError(err.response?.data?.message || '发表评论失败');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 回复评论
  const handleReply = async (commentId: number, content: string) => {
    try {
      const created = await replyToComment(commentId, { content });

      // 递归更新评论树中的回复
      const updateReplies = (commentList: Comment[]): Comment[] => {
        return commentList.map(comment => {
          if (comment.id === commentId) {
            return {
              ...comment,
              replies: [...comment.replies, created]
            };
          }
          if (comment.replies.length > 0) {
            return {
              ...comment,
              replies: updateReplies(comment.replies)
            };
          }
          return comment;
        });
      };

      setComments(updateReplies(comments));
    } catch (err: any) {
      setError(err.response?.data?.message || '回复失败');
    }
  };

  // 删除评论
  const handleDelete = async (commentId: number) => {
    if (!confirm('确定要删除这条评论吗？')) return;

    try {
      await deleteComment(commentId);

      // 递归从评论树中移除评论
      const removeComment = (commentList: Comment[]): Comment[] => {
        return commentList
          .filter(comment => comment.id !== commentId)
          .map(comment => ({
            ...comment,
            replies: removeComment(comment.replies)
          }));
      };

      setComments(removeComment(comments));
    } catch (err: any) {
      setError(err.response?.data?.message || '删除评论失败');
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '2rem' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '0.5rem', color: '#888' }}>加载评论中...</p>
      </div>
    );
  }

  return (
    <div className="comment-section" style={{ marginTop: '2rem' }}>
      <h2 style={{ marginBottom: '1rem', fontSize: '1.5rem' }}>评论 ({comments.length})</h2>

      {/* 错误提示 */}
      {error && (
        <div className="error-message" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {/* 发表评论表单 */}
      {isAuthenticated && (
        <form onSubmit={handleSubmitComment} style={{ marginBottom: '1.5rem' }}>
          <textarea
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
            disabled={isSubmitting}
          />
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSubmitting || !newComment.trim()}
          >
            {isSubmitting ? '发表中...' : '发表评论'}
          </button>
        </form>
      )}

      {!isAuthenticated && (
        <div className="card" style={{ padding: '1rem', textAlign: 'center', color: '#888', marginBottom: '1.5rem' }}>
          请<a href="/login" style={{ color: '#3498db' }}>登录</a>后发表评论
        </div>
      )}

      {/* 评论列表 */}
      {comments.length === 0 ? (
        <div className="card" style={{ padding: '2rem', textAlign: 'center', color: '#888' }}>
          还没有评论，快来抢沙发吧！
        </div>
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
            />
          ))}
        </div>
      )}
    </div>
  );
}
