import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { logout as apiLogout } from '../api/auth';
import NotificationBell from './NotificationBell';
import bannerImg from '../assets/cc98_banner.png';
import catAvatar from '../assets/cc98_avatar_cat.png';

/**
 * CC98 风格的 Header 头部组件
 */
export default function Header() {
  const { user, isAuthenticated, isAdmin, logout: authLogout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showThemeMenu, setShowThemeMenu] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const isHomePage = location.pathname === '/';

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchKeyword.trim())}`);
    }
  };

  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        await apiLogout();
      }
    } catch {
      // Ignored - ensure client-side logout works
    }
    authLogout();
    navigate('/login');
  };

  const handleThemeSelect = (selectedTheme: string) => {
    window.dispatchEvent(new CustomEvent('cc98-theme-change', { detail: selectedTheme }));
    setShowThemeMenu(false);
  };

  return (
    <div className="cc98-nav-container" role="banner">
      {/* 1. Translucent/Glassmorphism Navbar */}
      <nav className={`cc98-navbar ${isHomePage ? 'homepage-navbar' : ''}`}>
        <div className="cc98-nav-inner">
          <div className="cc98-nav-left-group">
            {/* Logo and text */}
            <div className="cc98-logo-wrap">
              <img src={catAvatar} alt="CC91 Logo" className="cc98-cat-icon" />
              <Link to="/" className="cc98-nav-link site-title">
                CC91 论坛
              </Link>
            </div>

            {/* Navigation Links */}
            <div className="cc98-nav-links hide-mobile">
              <span className="divider-bar">|</span>
              <Link
                to="/"
                className={`cc98-nav-link ${location.pathname === '/' ? 'active' : ''}`}
              >
                首页
              </Link>
              <Link
                to="/posts"
                className={`cc98-nav-link ${location.pathname.startsWith('/posts') && location.pathname !== '/posts/new' ? 'active' : ''}`}
              >
                帖子
              </Link>
            </div>
          </div>

          {/* Search Bar */}
          <form onSubmit={handleSearchSubmit} className="cc98-search-bar hide-mobile" role="search">
            <input
              type="text"
              placeholder="搜索帖子..."
              className="cc98-search-input"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              aria-label="搜索帖子"
            />
            <button type="submit" className="cc98-search-btn" aria-label="执行搜索">
              <i className="fa fa-search"></i>
            </button>
          </form>

          {/* User Account / Controls Area */}
          <div className="cc98-user-area">
            {/* Theme switcher */}
            <div className="cc98-theme-wrapper">
              <button
                className="cc98-theme-trigger"
                onClick={() => setShowThemeMenu(!showThemeMenu)}
                aria-label="切换主题"
                title="切换主题"
              >
                <i className="fa fa-paint-brush"></i>
              </button>
              {showThemeMenu && (
                <>
                  <div className="cc98-dropdown-overlay" onClick={() => setShowThemeMenu(false)} />
                  <div className="cc98-theme-dropdown">
                    <button onClick={() => handleThemeSelect('classic')} className="cc98-dropdown-item">
                      <span className="theme-indicator classic" /> 经典蓝
                    </button>
                    <button onClick={() => handleThemeSelect('warm')} className="cc98-dropdown-item">
                      <span className="theme-indicator warm" /> 温暖茶
                    </button>
                    <button onClick={() => handleThemeSelect('sakura')} className="cc98-dropdown-item">
                      <span className="theme-indicator sakura" /> 樱花粉
                    </button>
                    <button onClick={() => handleThemeSelect('dark')} className="cc98-dropdown-item">
                      <span className="theme-indicator dark" /> 深邃暗
                    </button>
                  </div>
                </>
              )}
            </div>

            {isAuthenticated && user ? (
              <>
                {/* Active Notification Bell */}
                <NotificationBell />

                {/* Username & Dropdown */}
                <div className="cc98-username-wrapper">
                  <span className="cc98-nav-link username-display">
                    {user.username} <i className="fa fa-caret-down"></i>
                  </span>

                  {/* Dropdown Menu */}
                  <div className="cc98-user-dropdown">
                    <Link to="/dashboard" className="cc98-dropdown-item">
                      <i className="fa fa-tachometer"></i> 个人中心
                    </Link>
                    <Link to="/profile/edit" className="cc98-dropdown-item">
                      <i className="fa fa-edit"></i> 修改资料
                    </Link>
                    <Link to={`/profile/${user.username}`} className="cc98-dropdown-item">
                      <i className="fa fa-user"></i> 我的资料
                    </Link>
                    {isAdmin && (
                      <Link to="/admin" className="cc98-dropdown-item cc98-dropdown-admin-item">
                        <i className="fa fa-cogs"></i> 管理后台
                      </Link>
                    )}
                    <div className="cc98-dropdown-divider"></div>
                    <button onClick={handleLogout} className="cc98-dropdown-item btn-logout-action">
                      <i className="fa fa-sign-out"></i> 退出登录
                    </button>
                  </div>
                </div>

                {/* Avatar */}
                <Link to={`/profile/${user.username}`} className="hide-mobile">
                  <img src={catAvatar} alt={`${user.username}的头像`} className="cc98-avatar-round" />
                </Link>
              </>
            ) : (
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <Link to="/login" className="cc98-auth-btn-header login">
                  登录
                </Link>
                <Link to="/register" className="cc98-auth-btn-header register">
                  注册
                </Link>
              </div>
            )}

            {/* Mobile Menu Toggle Button */}
            <button
              className="mobile-menu-toggle-btn show-mobile-only"
              onClick={() => setShowMobileMenu(!showMobileMenu)}
              aria-label="菜单"
            >
              <i className={`fa ${showMobileMenu ? 'fa-times' : 'fa-bars'}`}></i>
            </button>
          </div>
        </div>
      </nav>

      {/* 2. CC98 Banner Background (Home page only) */}
      {isHomePage && (
        <div className="cc98-banner-bg">
          <div className="cc98-banner-overlay"></div>
        </div>
      )}

      {/* 3. Mobile Navigation Overlay & Menu */}
      {showMobileMenu && (
        <>
          <div className="mobile-menu-overlay" onClick={() => setShowMobileMenu(false)} />
          <nav className="mobile-nav-panel">
            <div className="mobile-search-section">
              <form onSubmit={handleSearchSubmit} className="mobile-search-form">
                <input
                  type="text"
                  placeholder="搜索帖子..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                />
                <button type="submit"><i className="fa fa-search"></i></button>
              </form>
            </div>
            <div className="mobile-nav-links-list">
              <Link to="/" onClick={() => setShowMobileMenu(false)}>首页</Link>
              <Link to="/posts" onClick={() => setShowMobileMenu(false)}>所有帖子</Link>
              {isAuthenticated && (
                <>
                  <Link to="/posts/new" onClick={() => setShowMobileMenu(false)}>发布新帖子</Link>
                  <Link to="/dashboard" onClick={() => setShowMobileMenu(false)}>个人中心</Link>
                  <Link to="/notifications" onClick={() => setShowMobileMenu(false)}>我的通知</Link>
                  {isAdmin && <Link to="/admin" onClick={() => setShowMobileMenu(false)}>管理后台</Link>}
                  <button onClick={() => { handleLogout(); setShowMobileMenu(false); }} className="mobile-logout-btn">
                    退出登录
                  </button>
                </>
              )}
            </div>
          </nav>
        </>
      )}

      <style>{`
        .cc98-nav-container {
          position: relative;
          width: 100%;
          box-shadow: var(--cc98-shadow);
          background-color: var(--page-bg);
        }
        .cc98-banner-bg {
          height: 160px;
          background-image: url('${bannerImg}');
          background-size: cover;
          background-position: center;
          position: relative;
          width: 100%;
        }
        .cc98-banner-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(to bottom, rgba(0,0,0,0.35), rgba(0,0,0,0.05));
          z-index: 1;
        }
        
        .cc98-navbar {
          width: 100%;
          background: var(--primary-color);
          position: relative;
          z-index: 10;
          border-bottom: 2px solid var(--accent-color);
          transition: var(--cc98-transition);
        }

        .cc98-navbar.homepage-navbar {
          position: absolute;
          top: 0;
          left: 0;
          background: rgba(57, 70, 118, 0.72) !important;
          backdrop-filter: blur(10px);
          border-bottom: 1px solid rgba(255, 255, 255, 0.15);
        }
        
        .cc98-nav-inner {
          max-width: 1200px;
          width: 100%;
          margin: 0 auto;
          padding: 0.6rem 1.5rem;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        
        .cc98-nav-left-group {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .cc98-logo-wrap {
          display: flex;
          align-items: center;
          gap: 0.6rem;
        }
        .cc98-cat-icon {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: white;
          padding: 2px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.15);
        }
        .cc98-nav-links {
          display: flex;
          align-items: center;
          gap: 1.25rem;
          font-size: 0.95rem;
        }
        .divider-bar {
          color: rgba(255,255,255,0.45);
          user-select: none;
          margin: 0 0.1rem;
        }
        .cc98-nav-link {
          color: rgba(255,255,255,0.9) !important;
          text-decoration: none;
          font-weight: bold;
          transition: var(--cc98-transition);
          display: flex;
          align-items: center;
        }
        .cc98-nav-link:hover, .cc98-nav-link.active {
          color: white !important;
          text-shadow: 0 0 8px rgba(255,255,255,0.6);
          text-decoration: none;
        }
        .cc98-logo-wrap .site-title {
          font-size: 1.1rem;
          font-weight: bold;
        }

        .cc98-search-bar {
          display: flex;
          align-items: center;
          background: rgba(255, 255, 255, 0.18);
          border: 1px solid rgba(255, 255, 255, 0.15);
          border-radius: var(--cc98-radius-pill);
          padding: 0.25rem 0.75rem;
          width: 180px;
          transition: var(--cc98-transition);
        }
        .cc98-search-bar:focus-within {
          background: rgba(255, 255, 255, 0.28);
          width: 220px;
        }
        .cc98-search-input {
          background: transparent;
          border: none;
          color: white;
          font-size: 0.82rem;
          width: 100%;
          outline: none;
        }
        .cc98-search-input::placeholder { color: rgba(255,255,255,0.65); }
        .cc98-search-btn {
          background: none;
          border: none;
          color: white;
          cursor: pointer;
          font-size: 0.85rem;
          padding-left: 0.25rem;
        }

        .cc98-user-area {
          display: flex;
          align-items: center;
          gap: 1.1rem;
        }

        /* Theme trigger */
        .cc98-theme-wrapper {
          position: relative;
          display: flex;
          align-items: center;
        }
        .cc98-theme-trigger {
          background: none;
          border: none;
          color: rgba(255,255,255,0.9);
          font-size: 1.1rem;
          cursor: pointer;
          padding: 0.25rem;
          transition: var(--cc98-transition);
        }
        .cc98-theme-trigger:hover {
          color: white;
          text-shadow: 0 0 8px rgba(255,255,255,0.6);
        }
        .cc98-theme-dropdown {
          position: absolute;
          top: calc(100% + 10px);
          right: -10px;
          width: 120px;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          border-radius: var(--cc98-radius);
          border: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
          z-index: 1000;
          padding: 0.25rem 0;
          animation: slideDownFade 0.15s ease-out;
        }
        .theme-indicator {
          display: inline-block;
          width: 10px;
          height: 10px;
          border-radius: 50%;
          margin-right: 0.4rem;
        }
        .theme-indicator.classic { background-color: #394676; }
        .theme-indicator.warm { background-color: #ebdcb9; border: 1px solid #736952; }
        .theme-indicator.sakura { background-color: #e87a90; }
        .theme-indicator.dark { background-color: #1e1e24; border: 1px solid #3a3a44; }

        .cc98-theme-dropdown .cc98-dropdown-item {
          color: var(--text-main) !important;
          background: none;
          border: none;
          text-align: left;
        }
        .cc98-theme-dropdown .cc98-dropdown-item:hover {
          background-color: var(--quote-bg);
          color: var(--primary-text) !important;
        }

        .cc98-username-wrapper {
          position: relative;
          cursor: pointer;
          padding: 0.4rem 0;
        }
        .username-display {
          font-weight: bold;
          gap: 0.35rem;
        }
        .cc98-user-dropdown {
          position: absolute;
          top: 100%;
          right: 0;
          width: 140px;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          border: 1px solid var(--border-color);
          border-radius: 0 0 var(--cc98-radius) var(--cc98-radius);
          display: none;
          flex-direction: column;
          z-index: 1000;
          padding: 0.25rem 0;
          animation: slideDownFade 0.15s ease-out;
        }
        .cc98-username-wrapper:hover .cc98-user-dropdown {
          display: flex;
        }
        
        .cc98-user-dropdown .cc98-dropdown-item {
          color: var(--text-main) !important;
        }
        .cc98-user-dropdown .cc98-dropdown-item:hover {
          background-color: var(--quote-bg);
          color: var(--primary-text) !important;
        }
        .cc98-dropdown-admin-item {
          color: #e74c3c !important;
          font-weight: bold;
        }
        .cc98-dropdown-admin-item:hover {
          background-color: rgba(231, 76, 60, 0.08) !important;
          color: #c0392b !important;
        }

        .cc98-dropdown-overlay {
          position: fixed;
          inset: 0;
          z-index: 998;
        }
        .cc98-dropdown-item {
          padding: 0.5rem 0.75rem;
          font-size: 0.85rem;
          white-space: nowrap;
          text-decoration: none;
          transition: var(--cc98-transition);
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-weight: 500;
          border: none;
          width: 100%;
          cursor: pointer;
        }
        .cc98-dropdown-divider {
          height: 1px;
          background-color: var(--border-color);
          margin: 0.25rem 0;
        }
        .cc98-avatar-round {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          border: 1.5px solid white;
          object-fit: cover;
          transition: var(--cc98-transition);
          display: block;
        }
        .cc98-avatar-round:hover {
          transform: scale(1.1);
          box-shadow: 0 0 6px rgba(255,255,255,0.8);
        }
        .cc98-auth-btn-header {
          padding: 0.35rem 0.9rem;
          border-radius: var(--cc98-radius-pill);
          font-size: 0.85rem;
          font-weight: bold;
          text-decoration: none;
          transition: var(--cc98-transition);
          display: inline-block;
        }
        .cc98-auth-btn-header.login {
          background-color: transparent;
          border: 1px solid rgba(255,255,255,0.7);
          color: white !important;
        }
        .cc98-auth-btn-header.login:hover {
          background-color: rgba(255,255,255,0.15);
          border-color: white;
          text-decoration: none;
        }
        .cc98-auth-btn-header.register {
          background-color: var(--accent-color);
          color: #333 !important;
          border: 1px solid var(--accent-color);
        }
        .cc98-auth-btn-header.register:hover {
          opacity: 0.9;
          text-decoration: none;
        }

        /* Mobile specific styling */
        .show-mobile-only {
          display: none;
        }
        .mobile-menu-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0,0,0,0.5);
          z-index: 99;
        }
        .mobile-nav-panel {
          position: fixed;
          top: 0;
          right: 0;
          bottom: 0;
          width: 260px;
          background-color: var(--card-bg);
          border-left: 1px solid var(--border-color);
          z-index: 100;
          display: flex;
          flex-direction: column;
          padding: 1.5rem;
          gap: 1.5rem;
          box-shadow: -4px 0 15px rgba(0,0,0,0.1);
        }
        .mobile-search-form {
          display: flex;
          background: var(--quote-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 0.25rem 0.5rem;
        }
        .mobile-search-form input {
          width: 100%;
          border: none;
          background: transparent;
          padding: 0.25rem;
          font-size: 0.88rem;
          color: var(--text-main);
          outline: none;
        }
        .mobile-search-form button {
          border: none;
          background: transparent;
          color: var(--text-muted);
          cursor: pointer;
        }
        .mobile-nav-links-list {
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }
        .mobile-nav-links-list a {
          color: var(--text-main);
          font-weight: bold;
          text-decoration: none;
          font-size: 1rem;
          padding: 0.25rem 0;
        }
        .mobile-logout-btn {
          background: none;
          border: none;
          color: #fb6165;
          text-align: left;
          font-size: 1rem;
          font-weight: bold;
          cursor: pointer;
          padding: 0.25rem 0;
        }

        @media (max-width: 768px) {
          .hide-mobile {
            display: none !important;
          }
          .show-mobile-only {
            display: block !important;
          }
          .mobile-menu-toggle-btn {
            background: none;
            border: none;
            color: white;
            font-size: 1.3rem;
            cursor: pointer;
          }
        }
      `}</style>
    </div>
  );
}
