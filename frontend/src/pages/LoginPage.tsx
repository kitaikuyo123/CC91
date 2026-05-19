import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

/**
 * CC98 风格用户登录页面
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

      const { accessToken, refreshToken } = response.data;

      // Store tokens FIRST so the axios interceptor can use them
      localStorage.setItem('access_token', accessToken);
      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
      }

      // Now fetch user role
      let userRole = 'USER';
      try {
        const userResponse = await client.get(`/users/${username}`);
        userRole = userResponse.data.role || 'USER';
      } catch (err) {
        console.error('Failed to fetch user role', err);
      }

      login(username, accessToken, userRole);
      navigate(`/profile/${username}`);
    } catch (err: any) {
      const message = err.response?.data?.message || '登录失败，请检查用户名或密码。';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="cc98-auth-page container">
      <div className="cc98-auth-card">
        <div className="cc98-auth-header">
          <i className="fa fa-sign-in"></i> User Login
        </div>

        {error && (
          <div className="cc98-error-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-exclamation-circle"></i> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="cc98-auth-form">
          {/* Username */}
          <div className="cc98-form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isLoading}
              placeholder="请输入您的账号用户名"
              className="cc98-form-control"
              required
              autoFocus
            />
          </div>

          {/* Password */}
          <div className="cc98-form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请输入登录密码"
              className="cc98-form-control"
              required
            />
          </div>

          {/* Login Button */}
          <button
            type="submit"
            className="cc98-btn btn-publish btn-block"
            disabled={isLoading}
            style={{ width: '100%', marginTop: '1.5rem' }}
          >
            {isLoading ? <i className="fa fa-spinner fa-spin"></i> : 'Login'}
          </button>
        </form>

        <div className="cc98-auth-footer">
          <p>
            还没有账号？ <Link to="/register" className="auth-link">立即注册</Link>
          </p>
          <p style={{ marginTop: '0.4rem' }}>
            <Link to="/forgot-password" className="auth-link text-muted">忘记密码？</Link>
          </p>
        </div>
      </div>

      <style>{`
        .cc98-auth-page {
          max-width: 420px;
          margin-top: 4rem;
          margin-bottom: 4rem;
        }

        .cc98-auth-card {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-auth-header {
          background-color: var(--primary-color);
          color: white;
          padding: 1rem 1.5rem;
          font-weight: bold;
          font-size: 1.1rem;
          text-align: center;
          border-bottom: 2px solid var(--accent-color);
        }

        .theme-dark .cc98-auth-header {
          border-bottom-color: var(--border-color);
        }

        .cc98-auth-form {
          padding: 1.75rem 1.5rem 1rem 1.5rem;
        }

        .cc98-form-group {
          margin-bottom: 1.25rem;
        }

        .cc98-form-group label {
          display: block;
          margin-bottom: 0.4rem;
          font-weight: bold;
          font-size: 0.88rem;
          color: var(--text-main);
        }

        .cc98-form-control {
          width: 100%;
          padding: 0.7rem 0.85rem;
          border: 1px solid var(--border-color);
          background-color: var(--card-bg);
          color: var(--text-main);
          border-radius: 4px;
          font-size: 0.92rem;
          font-family: inherit;
          transition: var(--cc98-transition);
        }

        .cc98-form-control:focus {
          border-color: var(--primary-color);
          outline: none;
          box-shadow: 0 0 0 3px rgba(84, 110, 122, 0.15);
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
          box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
        }

        .btn-publish:hover:not(:disabled) {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-1px);
        }

        .btn-publish:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .cc98-error-box {
          background-color: rgba(251, 97, 101, 0.1);
          color: #fb6165;
          padding: 0.7rem 1rem;
          border-radius: 4px;
          border: 1px solid rgba(251, 97, 101, 0.2);
          font-size: 0.85rem;
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }

        .cc98-auth-footer {
          padding: 0.5rem 1.5rem 1.75rem 1.5rem;
          text-align: center;
          font-size: 0.85rem;
        }

        .auth-link {
          color: var(--primary-color);
          font-weight: bold;
          text-decoration: none;
        }

        .auth-link:hover {
          text-decoration: underline;
        }

        .auth-link.text-muted {
          color: var(--text-muted);
          font-weight: normal;
        }
      `}</style>
    </div>
  );
}
