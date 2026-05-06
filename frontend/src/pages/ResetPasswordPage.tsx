import { useState, type FormEvent, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/auth';

/**
 * 重置密码页面组件
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
      <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
        <div className="card">
          <div style={{ textAlign: 'center', padding: '2rem 0' }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>✅</div>
            <h2>密码重置成功</h2>
            <p style={{ color: '#666', marginTop: '1rem' }}>
              您的密码已成功重置，现在可以使用新密码登录。
            </p>
            <button
              className="btn btn-primary"
              onClick={() => navigate('/login')}
              style={{ marginTop: '1.5rem' }}
            >
              前往登录
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>重置密码</h1>

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

          <div className="form-group">
            <label htmlFor="code">验证码</label>
            <input
              id="code"
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              disabled={isLoading}
              placeholder="请输入邮件中的验证码"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="newPassword">新密码</label>
            <input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请输入新密码（至少6位）"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">确认密码</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isLoading}
              placeholder="请再次输入新密码"
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
            {isLoading ? <span className="spinner"></span> : '重置密码'}
          </button>
        </form>

        <p style={{ marginTop: '1rem', textAlign: 'center' }}>
          <a href="/login" style={{ color: '#3498db' }}>
            返回登录
          </a>
        </p>
      </div>
    </div>
  );
}
