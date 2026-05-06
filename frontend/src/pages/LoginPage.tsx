import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';

/**
 * Login page component
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

      // B-09 fix: Store tokens FIRST so the axios interceptor can use them
      localStorage.setItem('access_token', accessToken);
      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
      }

      // Now fetch user role - token is available for the interceptor
      let userRole = 'USER';
      try {
        const userResponse = await client.get(`/users/${username}`);
        userRole = userResponse.data.role || 'USER';
      } catch (err) {
        // If user info fetch fails, default to regular user
        console.error('Failed to fetch user role', err);
      }

      login(username, accessToken, userRole);
      navigate(`/profile/${username}`);
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Login failed, please try again';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>User Login</h1>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isLoading}
              placeholder="Enter your username"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
              placeholder="Enter your password"
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
            {isLoading ? <span className="spinner" aria-hidden="true"></span> : 'Login'}
          </button>
        </form>

        <p style={{ marginTop: '1rem', textAlign: 'center' }}>
          Don't have an account?{' '}
          <Link to="/register">
            Register now
          </Link>
        </p>

        <p style={{ marginTop: '0.5rem', textAlign: 'center' }}>
          <Link to="/forgot-password">
            Forgot password?
          </Link>
        </p>
      </div>
    </div>
  );
}
