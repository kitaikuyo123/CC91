import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getPostById, updatePost, type Post, type UpdatePostRequest } from '../api/post';
import { queryKeys } from '../lib/queryKeys';

/**
 * 编辑帖子页面
 */
export default function EditPostPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const postId = id ? parseInt(id, 10) : 0;

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');

  // 使用 React Query 获取帖子
  const { data: post, isLoading, error: fetchError } = useQuery({
    queryKey: queryKeys.posts.detail(postId),
    queryFn: () => getPostById(postId),
    enabled: postId > 0,
  });

  // React Query v5 移除了 onSuccess，改用 useEffect
  useEffect(() => {
    if (post) {
      setTitle(post.title);
      setContent(post.content);
    }
  }, [post]);

  // 更新帖子的 mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdatePostRequest }) => updatePost(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      navigate(`/posts/${postId}`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '更新帖子失败');
    },
  });

  // 检查登录状态
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (!isAuthenticated) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  if (fetchError && !post) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message" role="alert">{(fetchError as any)?.response?.data?.message || '加载失败'}</div>
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

  // 检查是否是作者
  const isAuthor = currentUser?.username === post?.authorUsername;
  if (!isAuthor) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message">你没有权限编辑这篇帖子</div>
        <button
          onClick={() => navigate(`/posts/${postId}`)}
          className="btn btn-primary"
          style={{ marginTop: '1rem' }}
        >
          返回帖子
        </button>
      </div>
    );
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    // 基本验证
    if (!title.trim()) {
      setError('标题不能为空');
      return;
    }
    if (title.length > 200) {
      setError('标题长度不能超过200个字符');
      return;
    }
    if (!content.trim()) {
      setError('内容不能为空');
      return;
    }

    setError('');

    const updateData: UpdatePostRequest = {
      title: title.trim(),
      content: content.trim()
    };

    updateMutation.mutate({ id: postId, data: updateData });
  };

  return (
    <div className="container" style={{ maxWidth: '800px', padding: '2rem' }}>
      <div className="card" style={{ padding: '2rem' }}>
        <h1 style={{ marginBottom: '1.5rem' }}>编辑帖子</h1>

        {error && (
          <div className="error-message" role="alert" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* 标题输入 */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label htmlFor="title" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              标题 <span style={{ color: '#e74c3c' }}>*</span>
              <span style={{ fontWeight: 'normal', color: '#888', fontSize: '0.9rem' }}>
                ({title.length}/200)
              </span>
            </label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="请输入帖子标题"
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #ddd',
                borderRadius: '4px',
                fontSize: '1rem'
              }}
              maxLength={200}
              autoFocus
            />
          </div>

          {/* 内容输入 */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label htmlFor="content" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              内容 <span style={{ color: '#e74c3c' }}>*</span>
            </label>
            <textarea
              id="content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="请输入帖子内容..."
              style={{
                width: '100%',
                minHeight: '300px',
                padding: '0.75rem',
                border: '1px solid #ddd',
                borderRadius: '4px',
                fontSize: '1rem',
                fontFamily: 'inherit',
                resize: 'vertical'
              }}
            />
          </div>

          {/* 操作按钮 */}
          <div style={{ display: 'flex', gap: '1rem' }}>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 1, padding: '0.75rem' }}
              disabled={updateMutation.isPending}
            >
              {updateMutation.isPending ? '保存中...' : '保存修改'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/posts/${postId}`)}
              className="btn"
              style={{ padding: '0.75rem 1.5rem' }}
              disabled={updateMutation.isPending}
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
