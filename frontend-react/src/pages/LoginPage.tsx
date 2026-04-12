import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

/**
 * 登录页面组件
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await client.post('/auth/login', {
        username,
        password,
      });

      const { accessToken } = response.data;
      login(username, accessToken);
      navigate('/dashboard');
    } catch (err: any) {
      const message = err.response?.data?.message || '登录失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>用户登录</h1>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">用户名</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isLoading}
              placeholder="请输入用户名"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">密码</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请输入密码"
              required
            />
          </div>

          {error && (
            <div className="error-message" style={{ marginBottom: '1rem' }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%' }}
            disabled={isLoading}
          >
            {isLoading ? <span className="spinner"></span> : '登录'}
          </button>
        </form>

        <p style={{ marginTop: '1rem', textAlign: 'center' }}>
          还没有账号？{' '}
          <a href="/register" style={{ color: '#3498db' }}>
            立即注册
          </a>
        </p>
      </div>
    </div>
  );
}
