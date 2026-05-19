import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getPostById, updatePost, type UpdatePostRequest } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';

/**
 * CC98 风格编辑帖子页面
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

  // 获取帖子详情
  const { data: post, isLoading, error: fetchError } = useQuery({
    queryKey: queryKeys.posts.detail(postId),
    queryFn: () => getPostById(postId),
    enabled: postId > 0,
  });

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

  // 认证检查
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
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>正在载入主题帖数据...</p>
      </div>
    );
  }

  if (fetchError && !post) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-editor-card" style={{ padding: '3rem 1.5rem', textAlign: 'center' }}>
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>加载帖子出错</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>
            {(fetchError as any)?.response?.data?.message || '帖子不存在或您没有权限查看。'}
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
      </div>
    );
  }

  const isAuthor = currentUser?.username === post?.authorUsername;
  if (!isAuthor) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-editor-card" style={{ padding: '3rem 1.5rem', textAlign: 'center' }}>
          <i className="fa fa-ban" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>越权访问警告</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>你没有权限编辑这篇帖子，只有原作者可以编辑。</p>
          <button className="btn btn-primary" onClick={() => navigate(`/posts/${postId}`)}>
            返回帖子
          </button>
        </div>
      </div>
    );
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('主题标题不能为空');
      return;
    }
    if (title.length > 200) {
      setError('标题长度不能超过200个字符');
      return;
    }
    if (!content.trim()) {
      setError('主题正文不能为空');
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
    <div className="cc98-editor-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '版面列表', href: '/' },
          { label: post?.categoryName || '讨论板块', href: `/category/${post?.categoryId}` },
          { label: '编辑主题帖' }
        ]} 
      />

      {/* 2. 编辑卡片 */}
      <div className="cc98-editor-card">
        <div className="cc98-editor-title-bar">
          <i className="fa fa-pencil-square-o"></i> 修改主题帖内容
        </div>

        {error && (
          <div className="cc98-error-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-exclamation-circle"></i> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ padding: '1.5rem' }}>
          {/* 标题 */}
          <div className="cc98-form-group">
            <label htmlFor="title">
              主题标题 <span style={{ color: '#fb6165' }}>*</span>
              <span className="count-hint">({title.length}/200)</span>
            </label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="请输入新的标题..."
              className="cc98-form-control"
              maxLength={200}
              autoFocus
            />
          </div>

          {/* 内容 */}
          <div className="cc98-form-group">
            <label htmlFor="content">
              主题正文 <span style={{ color: '#fb6165' }}>*</span>
            </label>
            <textarea
              id="content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="请输入新的正文内容..."
              className="cc98-form-control text-area"
            />
          </div>

          {/* 操作按钮 */}
          <div className="cc98-editor-actions">
            <button
              type="submit"
              className="cc98-btn btn-publish"
              disabled={updateMutation.isPending}
            >
              <i className="fa fa-check"></i> {updateMutation.isPending ? '正在保存...' : '保存修改内容'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/posts/${postId}`)}
              className="cc98-btn btn-cancel"
              disabled={updateMutation.isPending}
            >
              取消
            </button>
          </div>
        </form>
      </div>

      <style>{`
        .cc98-editor-card {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-editor-title-bar {
          background-color: var(--primary-color);
          color: white;
          padding: 0.85rem 1.5rem;
          font-weight: bold;
          font-size: 1.05rem;
          border-bottom: 2px solid var(--accent-color);
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .theme-dark .cc98-editor-title-bar {
          border-bottom-color: var(--border-color);
        }

        .cc98-error-box {
          background-color: rgba(251, 97, 101, 0.1);
          color: #fb6165;
          padding: 0.75rem 1.25rem;
          border-radius: var(--cc98-radius);
          border: 1px solid rgba(251, 97, 101, 0.2);
          font-size: 0.9rem;
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .cc98-form-group {
          margin-bottom: 1.5rem;
        }

        .cc98-form-group label {
          display: block;
          margin-bottom: 0.5rem;
          font-weight: bold;
          font-size: 0.92rem;
          color: var(--text-main);
        }

        .cc98-form-group label .count-hint {
          font-weight: normal;
          color: var(--text-muted);
          font-size: 0.82rem;
          margin-left: 0.5rem;
        }

        .cc98-form-control {
          width: 100%;
          padding: 0.75rem;
          border: 1px solid var(--border-color);
          background-color: var(--card-bg);
          color: var(--text-main);
          border-radius: 4px;
          font-size: 0.95rem;
          font-family: inherit;
          transition: var(--cc98-transition);
        }

        .cc98-form-control:focus {
          border-color: var(--primary-color);
          outline: none;
          box-shadow: 0 0 0 3px rgba(84, 110, 122, 0.15);
        }

        .cc98-form-control.text-area {
          min-height: 320px;
          resize: vertical;
          line-height: 1.6;
        }

        .cc98-editor-actions {
          display: flex;
          gap: 1rem;
          margin-top: 2rem;
        }

        .cc98-btn {
          padding: 0.7rem 1.5rem;
          font-size: 0.92rem;
          font-weight: bold;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          transition: var(--cc98-transition);
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 0.4rem;
          border: none;
        }

        .btn-publish {
          background-color: var(--primary-color);
          color: white;
          flex: 1;
          min-width: 150px;
          box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
        }

        .btn-publish:hover:not(:disabled) {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-1px);
        }

        .btn-cancel {
          background-color: transparent;
          color: var(--text-muted);
          border: 1px solid var(--border-color);
          padding: 0.7rem 2rem;
        }

        .btn-cancel:hover:not(:disabled) {
          background-color: var(--quote-bg);
          color: var(--text-main);
        }

        .cc98-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}
