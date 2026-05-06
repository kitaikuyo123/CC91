import { Link, Outlet, useLocation } from 'react-router-dom';

/**
 * 管理员后台布局组件
 */
export default function AdminLayout() {
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* 侧边栏 */}
      <aside style={{
        width: '250px',
        background: '#2c3e50',
        color: '#fff',
        padding: '1rem 0',
        flexShrink: 0
      }}>
        <div style={{
          padding: '0 1rem 1rem',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          marginBottom: '1rem'
        }}>
          <h2 style={{ margin: 0, fontSize: '1.2rem' }}>管理后台</h2>
        </div>

        <nav>
          <Link
            to="/admin"
            style={{
              display: 'block',
              padding: '0.75rem 1rem',
              color: '#fff',
              textDecoration: 'none',
              background: isActive('/admin') ? 'rgba(52, 152, 219, 0.3)' : 'transparent'
            }}
          >
            📊 首页
          </Link>
          <Link
            to="/admin/categories"
            style={{
              display: 'block',
              padding: '0.75rem 1rem',
              color: '#fff',
              textDecoration: 'none',
              background: isActive('/admin/categories') ? 'rgba(52, 152, 219, 0.3)' : 'transparent'
            }}
          >
            📁 版块管理
          </Link>
          <Link
            to="/admin/content"
            style={{
              display: 'block',
              padding: '0.75rem 1rem',
              color: '#fff',
              textDecoration: 'none',
              background: isActive('/admin/content') ? 'rgba(52, 152, 219, 0.3)' : 'transparent'
            }}
          >
            📝 内容审核
          </Link>
          <Link
            to="/admin/users"
            style={{
              display: 'block',
              padding: '0.75rem 1rem',
              color: '#fff',
              textDecoration: 'none',
              background: isActive('/admin/users') ? 'rgba(52, 152, 219, 0.3)' : 'transparent'
            }}
          >
            👥 用户管理
          </Link>
          <Link
            to="/"
            style={{
              display: 'block',
              padding: '0.75rem 1rem',
              color: 'rgba(255,255,255,0.7)',
              textDecoration: 'none',
              marginTop: '1rem'
            }}
          >
            ← 返回论坛
          </Link>
        </nav>
      </aside>

      {/* 主内容区 */}
      <main style={{ flex: 1, background: '#f5f5f5', padding: '2rem' }}>
        <Outlet />
      </main>
    </div>
  );
}
