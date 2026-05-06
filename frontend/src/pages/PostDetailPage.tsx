import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getPostById, deletePost, type Post } from '../api/post';
import CommentSection from '../components/CommentSection';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 帖子详情页面
 */
export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const postId = id ? parseInt(id, 10) : 0;

  // 使用 React Query 获取帖子
  const { data: post, isLoading, error } = useQuery({
    queryKey: queryKeys.posts.detail(postId),
    queryFn: () => getPostById(postId),
    enabled: postId > 0,
  });

  // 删除帖子的 mutation
  const deleteMutation = useMutation({
    mutationFn: deletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      navigate('/posts');
    },
    onError: (err: any) => {
      setIsDeleting(false);
      setDeleteError(err.response?.data?.message || '删除失败');
    },
  });

  const handleDelete = async () => {
    if (!post) return;
    if (!confirm('确定要删除这篇帖子吗？此操作不可恢复。')) return;

    setIsDeleting(true);
    deleteMutation.mutate(post.id);
  };

  const handleEdit = () => {
    navigate(`/posts/${postId}/edit`);
  };

  const isAuthor = currentUser?.username === post?.authorUsername;

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message" role="alert">{(error as any)?.response?.data?.message || '帖子不存在'}</div>
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
        &larr; 返回列表
      </button>

      {/* 帖子内容 */}
      <article className="card" style={{ padding: '2rem' }}>
        {/* 操作按钮（仅作者可见） */}
        {isAuthor && (
          <div style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                onClick={handleEdit}
                className="btn btn-primary"
              >
                编辑
              </button>
              <button
                onClick={handleDelete}
                className="btn btn-danger"
                disabled={isDeleting}
              >
                {isDeleting ? '删除中...' : '删除'}
              </button>
            </div>
            {deleteError && (
              <div className="error-message" role="alert" style={{ marginTop: '0.5rem' }}>
                {deleteError}
              </div>
            )}
          </div>
        )}

        {/* 帖子标题 */}
        <h1 style={{ marginBottom: '1rem', fontSize: '2rem' }}>
          {post.title}
        </h1>

        {/* 帖子元信息 */}
        <div style={{
          padding: '1rem 0',
          borderBottom: '1px solid var(--color-border)',
          marginBottom: '1.5rem'
        }}>
          <div className="post-meta" style={{ fontSize: '0.95rem' }}>
            <span>
              作者: <strong style={{ color: 'var(--color-text-primary)' }}>{post.authorUsername}</strong>
            </span>
            <span>浏览: {post.viewCount}</span>
            <span>评论: {post.commentCount}</span>
            <span>发布于 <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time></span>
            {post.updatedAt !== post.createdAt && (
              <span>更新于 <time dateTime={post.updatedAt}>{formatDate(post.updatedAt)}</time></span>
            )}
          </div>
        </div>

        {/* 帖子正文 */}
        <div style={{
          lineHeight: '1.8',
          fontSize: '1.05rem',
          color: 'var(--color-text-primary)',
          whiteSpace: 'pre-wrap'
        }}>
          {post.content}
        </div>
      </article>

      {/* 评论区 */}
      <CommentSection postId={postId} commentCount={post.commentCount} />
    </div>
  );
}
