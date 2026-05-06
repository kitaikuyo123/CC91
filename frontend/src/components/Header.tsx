import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { logout as apiLogout } from '../api/auth';
import NotificationBell from './NotificationBell';

interface HeaderProps {
  isLoggedIn?: boolean;
}

/**
 * Page header component
 */
export default function Header({ isLoggedIn = false }: HeaderProps) {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const navigate = useNavigate();
  const auth = useAuth();

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchKeyword)}`);
    }
  };

  const handleLogout = async () => {
    try {
      // Attempt to call backend logout to invalidate refresh token
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        await apiLogout();
      }
    } catch {
      // Backend logout failure should not block client-side logout
    }
    auth.logout();
    navigate('/login');
  };

  const navItems = isLoggedIn ? (
    <>
      <NotificationBell />
      <Link to="/posts/new" className="nav-link">发布帖子</Link>
      <Link to="/dashboard" className="nav-link">控制台</Link>
      <button
        onClick={handleLogout}
        className="nav-link button-link"
        aria-label="退出登录"
      >
        退出登录
      </button>
    </>
  ) : (
    <>
      <Link to="/login" className="nav-link">登录</Link>
      <Link to="/register" className="nav-link">注册</Link>
    </>
  );

  return (
    <header className="header" role="banner">
      <div className="container header-content">
        <Link to="/" className="logo" aria-label="CC91 论坛首页">
          CC91 论坛
        </Link>

        {/* Desktop Search Bar */}
        <div className="header-search" role="search">
          {showSearchInput ? (
            <form onSubmit={handleSearchSubmit} className="header-search-form">
              <label htmlFor="header-search-input" className="visually-hidden">
                搜索帖子
              </label>
              <input
                id="header-search-input"
                type="text"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                placeholder="搜索帖子..."
                autoFocus
                className="header-search-input"
                onBlur={() => {
                  if (!searchKeyword) setShowSearchInput(false);
                }}
              />
              <button
                type="submit"
                className="header-search-btn"
                aria-label="搜索"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="11" cy="11" r="8" />
                  <path d="M21 21l-4.35-4.35" />
                </svg>
              </button>
            </form>
          ) : (
            <button
              onClick={() => setShowSearchInput(true)}
              className="header-search-trigger"
              aria-label="展开搜索框"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <span>搜索帖子...</span>
            </button>
          )}
        </div>

        {/* Desktop Navigation */}
        <nav className="nav" aria-label="主导航">
          <Link to="/" className="nav-link">首页</Link>
          <Link to="/posts" className="nav-link">帖子</Link>
          {navItems}
        </nav>

        {/* Mobile Menu Button */}
        <button
          className="mobile-menu-btn"
          onClick={() => setShowMobileMenu(!showMobileMenu)}
          aria-label={showMobileMenu ? '关闭菜单' : '打开菜单'}
          aria-expanded={showMobileMenu}
        >
          <span className={`hamburger ${showMobileMenu ? 'hamburger-open' : ''}`}>
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
      </div>

      {/* Mobile Menu Overlay */}
      {showMobileMenu && (
        <div className="mobile-menu-overlay" onClick={() => setShowMobileMenu(false)} />
      )}

      {/* Mobile Navigation */}
      <nav
        className={`mobile-nav ${showMobileMenu ? 'mobile-nav-open' : ''}`}
        aria-label="Mobile navigation"
      >
        <Link to="/" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>首页</Link>
        <Link to="/posts" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>帖子</Link>
        {isLoggedIn ? (
          <>
            <Link to="/posts/new" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>发布帖子</Link>
            <Link to="/dashboard" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>控制台</Link>
            <Link to="/notifications" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>通知</Link>
            <button
              onClick={() => { handleLogout(); setShowMobileMenu(false); }}
              className="mobile-nav-link mobile-nav-link-logout"
            >
              退出登录
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>登录</Link>
            <Link to="/register" className="mobile-nav-link" onClick={() => setShowMobileMenu(false)}>注册</Link>
          </>
        )}
      </nav>
    </header>
  );
}
