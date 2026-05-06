import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

/**
 * 管理员后台布局组件
 */
export default function AdminLayout() {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const navItems = [
    { to: '/admin', label: '首页', icon: '\u{1F4CA}' },
    { to: '/admin/categories', label: '版块管理', icon: '\u{1F4C1}' },
    { to: '/admin/content', label: '内容审核', icon: '\u{1F4DD}' },
    { to: '/admin/users', label: '用户管理', icon: '\u{1F465}' },
  ];

  return (
    <div className="admin-layout">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="admin-sidebar-overlay"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Mobile header */}
      <div className="admin-mobile-header">
        <button
          className="admin-menu-toggle"
          onClick={() => setSidebarOpen(!sidebarOpen)}
          aria-label={sidebarOpen ? '关闭菜单' : '打开菜单'}
          aria-expanded={sidebarOpen}
        >
          <span className={`hamburger ${sidebarOpen ? 'hamburger-open' : ''}`}>
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
        <span style={{ fontWeight: 600, color: '#fff' }}>管理后台</span>
      </div>

      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? 'admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-header">
          <h2 style={{ margin: 0, fontSize: '1.2rem' }}>管理后台</h2>
        </div>

        <nav aria-label="管理导航">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`admin-nav-link ${isActive(item.to) ? 'admin-nav-active' : ''}`}
              onClick={() => setSidebarOpen(false)}
              aria-current={isActive(item.to) ? 'page' : undefined}
            >
              <span aria-hidden="true">{item.icon}</span> {item.label}
            </Link>
          ))}
          <Link
            to="/"
            className="admin-nav-link admin-nav-back"
            onClick={() => setSidebarOpen(false)}
          >
            &larr; 返回论坛
          </Link>
        </nav>
      </aside>

      {/* Main content */}
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
