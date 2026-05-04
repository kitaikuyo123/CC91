import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { createPost, type CreatePostRequest } from '../api/post';

/**
 * 创建帖子页面
 */
export default function CreatePostPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 检查登录状态
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, navigate]);

  if (!isAuthenticated) {
    return null;
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

    setIsSubmitting(true);
    setError('');

    try {
      const postData: CreatePostRequest = {
        title: title.trim(),
        content: content.trim()
      };

      const createdPost = await createPost(postData);
      navigate(`/posts/${createdPost.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || '创建帖子失败');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '800px', padding: '2rem' }}>
      <div className="card" style={{ padding: '2rem' }}>
        <h1 style={{ marginBottom: '1.5rem' }}>发布新帖</h1>

        {error && (
          <div className="error-message" style={{ marginBottom: '1rem' }}>
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
              disabled={isSubmitting}
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
              disabled={isSubmitting}
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
              disabled={isSubmitting}
            >
              {isSubmitting ? '发布中...' : '发布帖子'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/posts')}
              className="btn"
              style={{ padding: '0.75rem 1.5rem' }}
              disabled={isSubmitting}
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
