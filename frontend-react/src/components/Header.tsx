import { Link } from 'react-router-dom';

interface HeaderProps {
  isLoggedIn?: boolean;
}

/**
 * 页面头部组件
 */
export default function Header({ isLoggedIn = false }: HeaderProps) {
  return (
    <header className="header">
      <div className="container">
        <div className="header-content">
          <Link to="/" className="logo">
            CC91 论坛
          </Link>
          <nav className="nav">
            <Link to="/" className="nav-link">首页</Link>
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className="nav-link">个人中心</Link>
                <button
                  onClick={() => {
                    localStorage.removeItem('access_token');
                    localStorage.removeItem('refresh_token');
                    window.location.href = '/login';
                  }}
                  className="nav-link button-link"
                >
                  退出登录
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="nav-link">登录</Link>
                <Link to="/register" className="nav-link">注册</Link>
              </>
            )}
          </nav>
        </div>
      </div>

      <style>{`
        .header {
          background-color: #2c3e50;
          color: white;
          padding: 1rem 0;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 0 1rem;
        }
        .header-content {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .logo {
          font-size: 1.5rem;
          font-weight: bold;
          color: white;
          text-decoration: none;
        }
        .nav {
          display: flex;
          gap: 1.5rem;
          align-items: center;
        }
        .nav-link {
          color: white;
          text-decoration: none;
          transition: opacity 0.2s;
        }
        .nav-link:hover {
          opacity: 0.8;
        }
        .button-link {
          background: none;
          border: none;
          color: white;
          cursor: pointer;
          font-size: inherit;
        }
      `}</style>
    </header>
  );
}
