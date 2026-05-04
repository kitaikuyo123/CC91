import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getPostById, deletePost, type Post } from '../api/post';
import CommentSection from '../components/CommentSection';

/**
 * 帖子详情页面
 */
export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);

  const postId = id ? parseInt(id, 10) : 0;

  useEffect(() => {
    const fetchPost = async () => {
      if (!postId) return;

      try {
        setLoading(true);
        const data = await getPostById(postId);
        setPost(data);
        setError('');
      } catch (err: any) {
        const errorMsg = err.response?.data?.message || '加载帖子失败';
        setError(errorMsg);
        if (err.response?.status === 404) {
          setError('帖子不存在');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchPost();
  }, [postId]);

  const handleDelete = async () => {
    if (!post) return;
    if (!confirm('确定要删除这篇帖子吗？此操作不可恢复。')) return;

    setIsDeleting(true);
    try {
      await deletePost(post.id);
      navigate('/posts');
    } catch (err: any) {
      setError(err.response?.data?.message || '删除失败');
      setIsDeleting(false);
    }
  };

  const handleEdit = () => {
    navigate(`/posts/${postId}/edit`);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isAuthor = currentUser?.username === post?.authorUsername;

  if (loading) {
    return (
      <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem' }}>加载中...</p>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message">{error || '帖子不存在'}</div>
        <button
          onClick={() => navigate('/posts')}
          className="btn btn-primary"
          style={{ marginTop: '1rem' }}
        >
          返回列表
        </button>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '900px', padding: '2rem' }}>
      {/* 返回按钮 */}
      <button
        onClick={() => navigate('/posts')}
        className="btn"
        style={{ marginBottom: '1rem' }}
      >
        ← 返回列表
      </button>

      {/* 帖子内容 */}
      <div className="card" style={{ padding: '2rem' }}>
        {/* 操作按钮（仅作者可见） */}
        {isAuthor && (
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
            <button
              onClick={handleEdit}
              className="btn btn-primary"
            >
              编辑
            </button>
            <button
              onClick={handleDelete}
              className="btn"
              style={{ background: '#e74c3c', color: 'white' }}
              disabled={isDeleting}
            >
              {isDeleting ? '删除中...' : '删除'}
            </button>
          </div>
        )}

        {/* 帖子标题 */}
        <h1 style={{ marginBottom: '1rem', fontSize: '2rem' }}>
          {post.title}
        </h1>

        {/* 帖子元信息 */}
        <div style={{
          padding: '1rem 0',
          borderBottom: '1px solid #e0e0e0',
          marginBottom: '1.5rem'
        }}>
          <div style={{ display: 'flex', gap: '1.5rem', fontSize: '0.95rem', color: '#666' }}>
            <span>
              作者: <strong style={{ color: '#333' }}>{post.authorUsername}</strong>
            </span>
            <span>浏览: {post.viewCount}</span>
            <span>发布于 {formatDate(post.createdAt)}</span>
            {post.updatedAt !== post.createdAt && (
              <span>更新于 {formatDate(post.updatedAt)}</span>
            )}
          </div>
        </div>

        {/* 帖子正文 */}
        <div style={{
          lineHeight: '1.8',
          fontSize: '1.05rem',
          color: '#333',
          whiteSpace: 'pre-wrap'
        }}>
          {post.content}
        </div>
      </div>

      {/* 评论区 */}
      <CommentSection postId={postId} />
    </div>
  );
}
