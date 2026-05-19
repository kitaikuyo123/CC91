import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import client from '../api/client';

/**
 * CC98 风格用户注册页面组件
 */
export default function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'register' | 'verify'>('register');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    setIsLoading(true);

    try {
      await client.post('/auth/register', {
        username,
        email,
        password,
      });

      setSuccess('验证码已发送至邮箱，请查收');
      setStep('verify');
      startCountdown();
    } catch (err: any) {
      const message = err.response?.data?.message || '注册失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerify = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await client.post('/auth/verify-email', {
        email,
        code: verificationCode,
      });

      alert('邮箱验证成功！请登录');
      navigate('/login');
    } catch (err: any) {
      const message = err.response?.data?.message || '验证失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (countdown > 0) return;

    setIsLoading(true);
    setError('');

    try {
      await client.post('/auth/register', {
        username,
        email,
        password,
      });

      setSuccess('验证码已重新发送');
      startCountdown();
    } catch (err: any) {
      const message = err.response?.data?.message || '发送失败，请重试';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const startCountdown = () => {
    setCountdown(60);
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleCodeChange = (index: number, value: string) => {
    if (value.length > 1) return;
    const newCode = verificationCode.split('');
    newCode[index] = value;
    setVerificationCode(newCode.join(''));

    // 自动聚焦下一个输入框
    if (value && index < 5) {
      const nextInput = document.getElementById(`code-${index + 1}`) as HTMLInputElement;
      nextInput?.focus();
    }
  };

  return (
    <div className="cc98-auth-page container">
      <div className="cc98-auth-card">
        <div className="cc98-auth-header">
          <i className="fa fa-user-plus"></i> {step === 'register' ? '用户注册' : '邮箱验证'}
        </div>

        {error && (
          <div className="cc98-error-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-exclamation-circle"></i> {error}
          </div>
        )}

        {success && (
          <div className="cc98-success-box" style={{ margin: '1.25rem 1.5rem 0 1.5rem' }}>
            <i className="fa fa-check-circle"></i> {success}
          </div>
        )}

        <div className="cc98-auth-form" style={{ paddingBottom: '1rem' }}>
          {step === 'register' ? (
            <form onSubmit={handleRegister}>
              <div className="cc98-form-group">
                <label htmlFor="reg-username">用户名</label>
                <input
                  id="reg-username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={isLoading}
                  placeholder="3-20 个字符"
                  minLength={3}
                  maxLength={20}
                  className="cc98-form-control"
                  required
                  autoFocus
                />
              </div>

              <div className="cc98-form-group">
                <label htmlFor="reg-email">邮箱</label>
                <input
                  id="reg-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={isLoading}
                  placeholder="请输入您的邮箱"
                  className="cc98-form-control"
                  required
                />
              </div>

              <div className="cc98-form-group">
                <label htmlFor="reg-password">密码</label>
                <input
                  id="reg-password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading}
                  placeholder="至少 6 个字符"
                  minLength={6}
                  className="cc98-form-control"
                  required
                />
              </div>

              <div className="cc98-form-group">
                <label htmlFor="reg-confirm-password">确认密码</label>
                <input
                  id="reg-confirm-password"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  disabled={isLoading}
                  placeholder="请再次输入登录密码"
                  minLength={6}
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
                {isLoading ? <i className="fa fa-spinner fa-spin"></i> : '发送验证码'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerify}>
              <p style={{ marginBottom: '1.25rem', fontSize: '0.88rem', color: 'var(--text-main)', textAlign: 'center' }}>
                验证码已发送至 <strong>{email}</strong>，请填入 6 位验证码：
              </p>

              <div className="verify-code-inputs" role="group" aria-label="验证码输入">
                {[0, 1, 2, 3, 4, 5].map((index) => (
                  <input
                    key={index}
                    id={`code-${index}`}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={verificationCode[index] || ''}
                    onChange={(e) => handleCodeChange(index, e.target.value)}
                    disabled={isLoading}
                    aria-label={`验证码第${index + 1}位`}
                    className="verify-code-box"
                    required
                  />
                ))}
              </div>

              <button
                type="submit"
                className="cc98-btn btn-publish btn-block"
                disabled={isLoading || verificationCode.length !== 6}
                style={{ width: '100%', marginTop: '1.5rem' }}
              >
                {isLoading ? <i className="fa fa-spinner fa-spin"></i> : '验证'}
              </button>

              <button
                type="button"
                onClick={handleResendCode}
                className="cc98-btn btn-cancel btn-block"
                style={{ width: '100%', marginTop: '0.75rem', border: '1px solid var(--border-color)' }}
                disabled={isLoading || countdown > 0}
              >
                {countdown > 0 ? `${countdown} 秒后可重发` : '重新发送邮箱验证码'}
              </button>
            </form>
          )}
        </div>

        <div className="cc98-auth-footer">
          已有账号？ <Link to="/login" className="auth-link">立即登录</Link>
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

        .btn-cancel {
          background-color: transparent;
          color: var(--text-muted);
        }

        .btn-cancel:hover:not(:disabled) {
          background-color: var(--quote-bg);
          color: var(--text-main);
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

        .cc98-success-box {
          background-color: rgba(46, 204, 113, 0.1);
          color: #2ecc71;
          padding: 0.7rem 1rem;
          border-radius: 4px;
          border: 1px solid rgba(46, 204, 113, 0.2);
          font-size: 0.85rem;
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }

        .verify-code-inputs {
          display: flex;
          justify-content: space-between;
          gap: 0.5rem;
          margin-bottom: 1.5rem;
        }

        .verify-code-box {
          width: 46px;
          height: 52px;
          text-align: center;
          font-size: 1.35rem;
          font-weight: bold;
          border: 1px solid var(--border-color);
          background-color: var(--card-bg);
          color: var(--text-main);
          border-radius: 6px;
          transition: var(--cc98-transition);
        }

        .verify-code-box:focus {
          border-color: var(--primary-color);
          outline: none;
          box-shadow: 0 0 0 3px rgba(84, 110, 122, 0.15);
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
