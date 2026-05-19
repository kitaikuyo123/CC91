import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { createPost, updatePost, getPostById, type CreatePostRequest } from '../api/post';
import { getCategories } from '../api/category';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';

/**
 * CC98 风格创建/编辑帖子页面
 */
export default function CreatePostPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const location = useLocation();

  const draftId = searchParams.get('draftId');
  const isDraftMode = !!draftId;

  // Retrieve categoryId passed in from Board Page
  const initialCategoryId = location.state?.categoryId;

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>(initialCategoryId);
  const [error, setError] = useState('');

  // 记录最近一次操作的状态
  const lastStatusRef = useRef<string>('PUBLISHED');

  // 获取版块列表
  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  // 如果是草稿编辑模式，加载草稿数据
  const { data: draftPost } = useQuery({
    queryKey: queryKeys.posts.detail(Number(draftId)),
    queryFn: () => getPostById(Number(draftId)),
    enabled: isDraftMode,
  });

  // 草稿加载完成后填充表单
  useEffect(() => {
    if (draftPost) {
      setTitle(draftPost.title || '');
      setContent(draftPost.content || '');
      setCategoryId(draftPost.categoryId);
    }
  }, [draftPost]);

  // 认证状态检查
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // 创建/编辑帖子的 mutation
  const createMutation = useMutation({
    mutationFn: isDraftMode
      ? (data: CreatePostRequest) => updatePost(Number(draftId), data)
      : createPost,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.users.mePosts() });
      queryClient.invalidateQueries({ queryKey: queryKeys.users.meDrafts() });
      if (lastStatusRef.current === 'DRAFT') {
        navigate('/dashboard');
      } else {
        navigate(`/posts/${data.id}`);
      }
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || (isDraftMode ? '更新帖子失败' : '创建帖子失败'));
    },
  });

  if (!isAuthenticated) {
    return null;
  }

  const buildPostData = (status: string): CreatePostRequest => ({
    title: title.trim(),
    content: content.trim(),
    ...(categoryId ? { categoryId } : {}),
    status,
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (!categoryId) {
      setError('请选择讨论版块');
      return;
    }
    if (!title.trim()) {
      setError('帖子标题不能为空');
      return;
    }
    if (title.length > 200) {
      setError('标题长度不能超过200个字符');
      return;
    }
    if (!content.trim()) {
      setError('帖子内容不能为空');
      return;
    }

    setError('');
    lastStatusRef.current = 'PUBLISHED';
    createMutation.mutate(buildPostData('PUBLISHED'));
  };

  const handleSaveDraft = () => {
    if (!categoryId) {
      setError('请选择讨论版块');
      return;
    }
    if (!title.trim() && !content.trim()) {
      setError('标题或内容至少填写一项');
      return;
    }
    if (title.length > 200) {
      setError('标题长度不能超过200个字符');
      return;
    }

    setError('');
    lastStatusRef.current = 'DRAFT';
    createMutation.mutate(buildPostData('DRAFT'));
  };

  return (
    <div className="cc98-editor-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '版面列表', href: '/' },
          { label: isDraftMode ? '编辑草稿' : '发表新主题帖' }
        ]} 
      />

      {/* 2. 编辑卡片 */}
      <div className="cc98-editor-card">
        <div className="cc98-editor-title-bar">
          <i className="fa fa-pencil-square-o"></i> {isDraftMode ? '编辑草稿主题' : '撰写新主题帖'}
        </div>

        {error && (
          <div className="cc98-error-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-exclamation-circle"></i> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ padding: '1.5rem' }}>
          {/* 版块选择 */}
          <div className="cc98-form-group">
            <label htmlFor="category">
              选择版面 <span style={{ color: '#fb6165' }}>*</span>
            </label>
            <select
              id="category"
              value={categoryId ?? ''}
              onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
              disabled={createMutation.isPending}
              className="cc98-form-control"
            >
              <option value="">-- 请选择版面 --</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

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
              disabled={createMutation.isPending}
              placeholder="请输入清晰、简洁的主题帖标题..."
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
              disabled={createMutation.isPending}
              placeholder="请在这里撰写你的正文内容，注意遵守论坛规范..."
              className="cc98-form-control text-area"
            />
          </div>

          {/* 操作按钮 */}
          <div className="cc98-editor-actions">
            <button
              type="submit"
              className="cc98-btn btn-publish"
              disabled={createMutation.isPending}
            >
              <i className="fa fa-paper-plane"></i> {createMutation.isPending && lastStatusRef.current === 'PUBLISHED' ? '发布中...' : (isDraftMode ? '更新并发布' : '公开发布主题')}
            </button>
            
            <button
              type="button"
              onClick={handleSaveDraft}
              className="cc98-btn btn-draft"
              disabled={createMutation.isPending}
            >
              <i className="fa fa-floppy-o"></i> {createMutation.isPending && lastStatusRef.current === 'DRAFT' ? '保存中...' : (isDraftMode ? '更新草稿' : '存为草稿')}
            </button>
            
            <button
              type="button"
              onClick={() => {
                if (categoryId) {
                  navigate(`/category/${categoryId}`);
                } else {
                  navigate('/');
                }
              }}
              className="cc98-btn btn-cancel"
              disabled={createMutation.isPending}
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
          flex-wrap: wrap;
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

        .btn-draft {
          background-color: #f39c12;
          color: white;
          flex: 1;
          min-width: 150px;
          box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
        }

        .btn-draft:hover:not(:disabled) {
          background-color: #e67e22;
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
