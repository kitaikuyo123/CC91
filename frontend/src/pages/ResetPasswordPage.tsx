import { useState, type FormEvent, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../api/auth';

/**
 * CC98 风格重置密码页面组件
 */
export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const emailFromParams = searchParams.get('email') || '';

  const [email, setEmail] = useState(emailFromParams);
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (emailFromParams) {
      setEmail(emailFromParams);
    }
  }, [emailFromParams]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (newPassword !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    if (newPassword.length < 6) {
      setError('密码长度至少为6位');
      return;
    }

    setIsLoading(true);

    try {
      await resetPassword(email, code, newPassword);
      setSuccess(true);
    } catch (err: any) {
      const message = err.response?.data?.message || '重置密码失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="cc98-auth-page container">
        <div className="cc98-auth-card">
          <h2 className="cc98-auth-header">
            <i className="fa fa-check-circle-o"></i> 密码重置成功
          </h2>
          <div style={{ textAlign: 'center', padding: '2.5rem 1.5rem' }}>
            <div style={{ fontSize: '3rem', color: '#2ecc71', marginBottom: '1rem' }} aria-hidden="true">
              <i className="fa fa-smile-o"></i>
            </div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 'bold', color: 'var(--text-main)' }}>您的密码已成功重置</h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.75rem', fontSize: '0.9rem', lineHeight: '1.6' }}>
              重置成功，您现在可以使用刚才设置的新密码重新登录论坛。
            </p>
            <button
              className="cc98-btn btn-publish"
              onClick={() => navigate('/login')}
              style={{ marginTop: '1.75rem', width: '100%' }}
            >
              前往登录
            </button>
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
          .cc98-btn {
            padding: 0.7rem 1.5rem;
            font-size: 0.92rem;
            font-weight: bold;
            border-radius: var(--cc98-radius-pill);
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: none;
          }
          .btn-publish {
            background-color: var(--primary-color);
            color: white;
            box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
          }
          .btn-publish:hover {
            background-color: var(--accent-color);
            color: #333;
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className="cc98-auth-page container">
      <div className="cc98-auth-card">
        <h2 className="cc98-auth-header">
          <i className="fa fa-key"></i> 重置密码
        </h2>

        {error && (
          <div className="cc98-error-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-exclamation-circle"></i> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="cc98-auth-form">
          {/* 邮箱 */}
          <div className="cc98-form-group">
            <label htmlFor="email">邮箱</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isLoading}
              placeholder="请输入您的账号绑定邮箱"
              className="cc98-form-control"
              required
            />
          </div>

          {/* 验证码 */}
          <div className="cc98-form-group">
            <label htmlFor="code">验证码</label>
            <input
              id="code"
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              disabled={isLoading}
              placeholder="请输入您收到的邮箱验证码"
              className="cc98-form-control"
              required
            />
          </div>

          {/* 新密码 */}
          <div className="cc98-form-group">
            <label htmlFor="newPassword">新密码</label>
            <input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请输入新密码 (至少 6 位)"
              className="cc98-form-control"
              required
            />
          </div>

          {/* 确认密码 */}
          <div className="cc98-form-group">
            <label htmlFor="confirmPassword">确认密码</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请再次输入您的新密码"
              className="cc98-form-control"
              required
            />
          </div>

          <button
            type="submit"
            className="cc98-btn btn-publish btn-block"
            disabled={isLoading}
            style={{ width: '100%', marginTop: '1.5rem' }}
          >
            {isLoading ? <i className="fa fa-spinner fa-spin"></i> : '重置密码'}
          </button>
        </form>

        <div className="cc98-auth-footer">
          <Link to="/login" className="auth-link">返回登录</Link>
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
      `}</style>
    </div>
  );
}
