import { useAuth } from '../context/AuthContext';

/**
 * 用户面板页面
 */
export default function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div className="container" style={{ padding: '2rem' }}>
      <h1>用户面板</h1>
      <div style={{ marginTop: '1.5rem' }}>
        <p>欢迎回来, <strong>{user?.username}</strong>!</p>
        <button
          onClick={logout}
          className="btn btn-danger"
          style={{ marginTop: '1rem' }}
        >
          退出登录
        </button>
      </div>
    </div>
  );
}
