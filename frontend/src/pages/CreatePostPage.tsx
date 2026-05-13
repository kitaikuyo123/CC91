import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { createPost, updatePost, getPostById, type CreatePostRequest } from '../api/post';
import { getCategories } from '../api/category';
import { queryKeys } from '../lib/queryKeys';

/**
 * 创建/编辑帖子页面
 * 支持 ?draftId= 参数进入草稿编辑模式
 */
export default function CreatePostPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const draftId = searchParams.get('draftId');
  const isDraftMode = !!draftId;

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);
  const [error, setError] = useState('');

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

  // 使用 useEffect 进行导航检查，避免在 hooks 之前 return
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
      navigate(`/posts/${data.id}`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || (isDraftMode ? '更新帖子失败' : '创建帖子失败'));
    },
  });

  // 如果未认证，不渲染内容
  if (!isAuthenticated) {
    return null;
  }

  const buildPostData = (status: string): CreatePostRequest => ({
    title: title.trim(),
    content: content.trim(),
    ...(categoryId ? { categoryId } : {}),
    status,
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    // 基本验证
    if (!categoryId) {
      setError('请选择版块');
      return;
    }
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
    createMutation.mutate(buildPostData('PUBLISHED'));
  };

  const handleSaveDraft = async () => {
    if (!categoryId) {
      setError('请选择版块');
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
    createMutation.mutate(buildPostData('DRAFT'));
  };

  return (
    <div className="container" style={{ maxWidth: '800px', padding: '2rem' }}>
      <div className="card" style={{ padding: '2rem' }}>
        <h1 style={{ marginBottom: '1.5rem' }}>{isDraftMode ? '编辑草稿' : '发布新帖'}</h1>

        {error && (
          <div className="error-message" role="alert" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* 版块选择 */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label htmlFor="category" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              版块 <span style={{ color: '#e74c3c' }}>*</span>
            </label>
            <select
              id="category"
              value={categoryId ?? ''}
              onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
              disabled={createMutation.isPending}
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #ddd',
                borderRadius: '4px',
                fontSize: '1rem'
              }}
            >
              <option value="">请选择版块</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

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
              disabled={createMutation.isPending}
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
              disabled={createMutation.isPending}
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
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? '发布中...' : (isDraftMode ? '更新并发布' : '发布帖子')}
            </button>
            <button
              type="button"
              onClick={handleSaveDraft}
              className="btn"
              style={{ flex: 1, padding: '0.75rem', background: '#f39c12', color: '#fff' }}
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? '保存中...' : (isDraftMode ? '更新草稿' : '保存草稿')}
            </button>
            <button
              type="button"
              onClick={() => navigate('/posts')}
              className="btn"
              style={{ padding: '0.75rem 1.5rem' }}
              disabled={createMutation.isPending}
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
