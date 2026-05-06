import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';

/**
 * 忘记密码页面组件
 */
export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await forgotPassword(email);
      setSuccess(true);
    } catch (err: any) {
      const message = err.response?.data?.message || '发送验证码失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
        <div className="card">
          <div style={{ textAlign: 'center', padding: '2rem 0' }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }} aria-hidden="true">&#x1F4E7;</div>
            <h2>验证码已发送</h2>
            <p style={{ color: 'var(--color-text-muted)', marginTop: '1rem' }}>
              我们已向 {email} 发送了验证码，请查收邮件。
            </p>
            <button
              className="btn btn-primary"
              onClick={() => navigate(`/reset-password?email=${encodeURIComponent(email)}`)}
              style={{ marginTop: '1.5rem' }}
            >
              前往重置密码
            </button>
            <p style={{ marginTop: '1rem' }}>
              <Link to="/login">
                返回登录
              </Link>
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>忘记密码</h1>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">邮箱</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isLoading}
              placeholder="请输入注册邮箱"
              required
            />
          </div>

          {error && (
            <div className="error-message" role="alert" style={{ marginBottom: '1rem' }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={isLoading}
            aria-busy={isLoading}
          >
            {isLoading ? <span className="spinner" aria-hidden="true"></span> : '发送验证码'}
          </button>
        </form>

        <p style={{ marginTop: '1rem', textAlign: 'center' }}>
          <Link to="/login">
            返回登录
          </Link>
        </p>
      </div>
    </div>
  );
}
