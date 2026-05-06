import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminGetUsers, adminGetPosts, type AdminUser } from '../../api/admin';
import { getCategories } from '../../api/category';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 管理后台首页 - 统计概览
 */
export default function AdminDashboard() {
  // 使用 React Query 获取用户数据
  const { data: users = [], isLoading: usersLoading } = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: adminGetUsers,
  });

  // 使用 React Query 获取帖子数据
  const { data: posts = [], isLoading: postsLoading } = useQuery({
    queryKey: queryKeys.admin.posts(),
    queryFn: () => adminGetPosts(),
  });

  // 使用 React Query 获取版块数据
  const { data: categories = [], isLoading: categoriesLoading } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  const loading = usersLoading || postsLoading || categoriesLoading;

  // 计算统计数据
  const stats = {
    totalUsers: users.length,
    totalPosts: posts.length,
    totalCategories: categories.length,
    lockedUsers: users.filter(u => u.isLocked).length
  };

  const recentUsers = users.slice(0, 5);

  if (loading) {
    return <div>加载中...</div>;
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>管理后台首页</h1>

      {/* 统计卡片 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>👥</div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#3498db' }}>{stats.totalUsers}</div>
          <div style={{ color: '#666' }}>总用户数</div>
        </div>

        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📝</div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#2ecc71' }}>{stats.totalPosts}</div>
          <div style={{ color: '#666' }}>总帖子数</div>
        </div>

        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📁</div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#f39c12' }}>{stats.totalCategories}</div>
          <div style={{ color: '#666' }}>版块数量</div>
        </div>

        <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🔒</div>
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#e74c3c' }}>{stats.lockedUsers}</div>
          <div style={{ color: '#666' }}>封禁用户</div>
        </div>
      </div>

      {/* 快捷入口 */}
      <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', marginBottom: '2rem' }}>
        <h2 style={{ marginBottom: '1rem' }}>快捷操作</h2>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/admin/categories" style={{
            padding: '0.75rem 1.5rem',
            background: '#3498db',
            color: '#fff',
            textDecoration: 'none',
            borderRadius: '4px'
          }}>
            管理版块
          </Link>
          <Link to="/admin/content" style={{
            padding: '0.75rem 1.5rem',
            background: '#2ecc71',
            color: '#fff',
            textDecoration: 'none',
            borderRadius: '4px'
          }}>
            内容审核
          </Link>
          <Link to="/admin/users" style={{
            padding: '0.75rem 1.5rem',
            background: '#f39c12',
            color: '#fff',
            textDecoration: 'none',
            borderRadius: '4px'
          }}>
            用户管理
          </Link>
        </div>
      </div>

      {/* 最新用户 */}
      <div style={{ background: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
        <h2 style={{ marginBottom: '1rem' }}>最新注册用户</h2>
        {recentUsers.length === 0 ? (
          <div style={{ color: '#999', textAlign: 'center', padding: '1rem' }}>暂无用户</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #eee' }}>
                <th style={{ padding: '0.75rem', textAlign: 'left' }}>用户名</th>
                <th style={{ padding: '0.75rem', textAlign: 'left' }}>邮箱</th>
                <th style={{ padding: '0.75rem', textAlign: 'left' }}>角色</th>
                <th style={{ padding: '0.75rem', textAlign: 'left' }}>状态</th>
              </tr>
            </thead>
            <tbody>
              {recentUsers.map(user => (
                <tr key={user.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '0.75rem' }}>{user.username}</td>
                  <td style={{ padding: '0.75rem' }}>{user.email}</td>
                  <td style={{ padding: '0.75rem' }}>
                    <span style={{
                      padding: '0.25rem 0.5rem',
                      borderRadius: '4px',
                      fontSize: '0.85rem',
                      background: user.role === 'ADMIN' ? '#e74c3c' : '#95a5a6',
                      color: '#fff'
                    }}>
                      {user.role === 'ADMIN' ? '管理员' : '普通用户'}
                    </span>
                  </td>
                  <td style={{ padding: '0.75rem' }}>
                    {user.isLocked ? (
                      <span style={{ color: '#e74c3c' }}>已封禁</span>
                    ) : (
                      <span style={{ color: '#2ecc71' }}>正常</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
