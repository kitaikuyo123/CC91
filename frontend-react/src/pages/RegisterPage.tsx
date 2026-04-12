import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

/**
 * 注册页面组件
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
    <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>
          {step === 'register' ? '用户注册' : '邮箱验证'}
        </h1>

        {error && (
          <div className="error-message" style={{ marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {success && (
          <div className="success-message" style={{ marginBottom: '1rem' }}>
            {success}
          </div>
        )}

        {step === 'register' ? (
          <form onSubmit={handleRegister}>
            <div className="form-group">
              <label htmlFor="reg-username">用户名</label>
              <input
                id="reg-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
                placeholder="3-20个字符"
                minLength={3}
                maxLength={20}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="reg-email">邮箱</label>
              <input
                id="reg-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={isLoading}
                placeholder="请输入邮箱"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="reg-password">密码</label>
              <input
                id="reg-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
                placeholder="至少6个字符"
                minLength={6}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="reg-confirm-password">确认密码</label>
              <input
                id="reg-confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                disabled={isLoading}
                placeholder="再次输入密码"
                minLength={6}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={isLoading}
            >
              {isLoading ? <span className="spinner"></span> : '发送验证码'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerify}>
            <p style={{ marginBottom: '1rem' }}>
              验证码已发送至 <strong>{email}</strong>
            </p>

            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
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
                  style={{
                    width: '45px',
                    height: '50px',
                    textAlign: 'center',
                    fontSize: '1.25rem',
                    fontWeight: 'bold',
                  }}
                  required
                />
              ))}
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%' }}
              disabled={isLoading || verificationCode.length !== 6}
            >
              {isLoading ? <span className="spinner"></span> : '验证'}
            </button>

            <button
              type="button"
              onClick={handleResendCode}
              className="btn btn-secondary"
              style={{ width: '100%', marginTop: '0.5rem' }}
              disabled={isLoading || countdown > 0}
            >
              {countdown > 0 ? `${countdown}秒后可重发` : '重新发送验证码'}
            </button>
          </form>
        )}

        <p style={{ marginTop: '1rem', textAlign: 'center' }}>
          已有账号？{' '}
          <a href="/login" style={{ color: '#3498db' }}>
            立即登录
          </a>
        </p>
      </div>
    </div>
  );
}
