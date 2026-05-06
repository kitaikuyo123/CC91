import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useDebounce } from '../hooks';
import NotificationBell from './NotificationBell';

interface HeaderProps {
  isLoggedIn?: boolean;
}

/**
 * 页面头部组件
 */
export default function Header({ isLoggedIn = false }: HeaderProps) {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);

  // 防抖搜索关键词
  const debouncedKeyword = useDebounce(searchKeyword, 300);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      window.location.href = `/search?q=${encodeURIComponent(searchKeyword)}`;
    }
  };

  // 当防抖后的关键词变化时，如果搜索框展开且不为空，自动跳转
  useState(() => {
    if (showSearchInput && debouncedKeyword.trim().length > 2) {
      // 可选：实现实时搜索跳转
    }
  });

  return (
    <header className="header">
      <div className="container">
        <div className="header-content">
          <Link to="/" className="logo">
            CC91 论坛
          </Link>

          {/* 搜索栏 */}
          <div style={{ flex: 1, maxWidth: '400px', margin: '0 2rem' }}>
            {showSearchInput ? (
              <form onSubmit={handleSearchSubmit} style={{ display: 'flex' }}>
                <input
                  type="text"
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  placeholder="搜索帖子..."
                  autoFocus
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    border: 'none',
                    borderRadius: '4px 0 0 4px',
                    fontSize: '0.9rem'
                  }}
                  onBlur={() => {
                    if (!searchKeyword) setShowSearchInput(false);
                  }}
                />
                <button
                  type="submit"
                  style={{
                    padding: '0 1rem',
                    background: '#3498db',
                    border: 'none',
                    color: '#fff',
                    borderRadius: '0 4px 4px 0',
                    cursor: 'pointer'
                  }}
                >
                  🔍
                </button>
              </form>
            ) : (
              <div
                onClick={() => setShowSearchInput(true)}
                style={{
                  padding: '0.5rem',
                  background: 'rgba(255,255,255,0.1)',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  color: 'rgba(255,255,255,0.7)',
                  fontSize: '0.9rem'
                }}
              >
                🔍 搜索帖子...
              </div>
            )}
          </div>

          <nav className="nav">
            <Link to="/" className="nav-link">首页</Link>
            <Link to="/posts" className="nav-link">帖子</Link>
            {isLoggedIn ? (
              <>
                <NotificationBell />
                <Link to="/posts/new" className="nav-link">发帖</Link>
                <Link to="/dashboard" className="nav-link">个人中心</Link>
                <button
                  onClick={() => {
                    localStorage.removeItem('access_token');
                    localStorage.removeItem('refresh_token');
                    localStorage.removeItem('user');
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
          gap: 1rem;
          align-items: center;
        }
        .nav-link {
          color: white;
          text-decoration: none;
          transition: opacity 0.2s;
          white-space: nowrap;
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
