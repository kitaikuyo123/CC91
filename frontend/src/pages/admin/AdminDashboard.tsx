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
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>管理后台首页</h1>

      {/* 统计卡片 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="stat-card">
          <div className="stat-card-icon" aria-hidden="true">&#x1F465;</div>
          <div className="stat-card-value" style={{ color: 'var(--color-primary)' }}>{stats.totalUsers}</div>
          <div className="stat-card-label">总用户数</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon" aria-hidden="true">&#x1F4DD;</div>
          <div className="stat-card-value" style={{ color: 'var(--color-success)' }}>{stats.totalPosts}</div>
          <div className="stat-card-label">总帖子数</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon" aria-hidden="true">&#x1F4C1;</div>
          <div className="stat-card-value" style={{ color: 'var(--color-warning)' }}>{stats.totalCategories}</div>
          <div className="stat-card-label">版块数量</div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon" aria-hidden="true">&#x1F512;</div>
          <div className="stat-card-value" style={{ color: 'var(--color-danger)' }}>{stats.lockedUsers}</div>
          <div className="stat-card-label">封禁用户</div>
        </div>
      </div>

      {/* 快捷入口 */}
      <div className="admin-card" style={{ marginBottom: '2rem' }}>
        <h2 style={{ marginBottom: '1rem' }}>快捷操作</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Link to="/admin/categories" className="btn btn-primary">
            管理版块
          </Link>
          <Link to="/admin/content" className="btn btn-success">
            内容审核
          </Link>
          <Link to="/admin/users" className="btn btn-warning">
            用户管理
          </Link>
        </div>
      </div>

      {/* 最新用户 */}
      <div className="table-container">
      <div className="admin-card">
        <h2 style={{ marginBottom: '1rem' }}>最新注册用户</h2>
        {recentUsers.length === 0 ? (
          <div className="empty-state">暂无用户</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>用户名</th>
                <th className="hide-mobile">邮箱</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {recentUsers.map(user => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td className="hide-mobile">{user.email}</td>
                  <td>
                    <span className={`badge ${user.role === 'ADMIN' ? 'badge-danger' : 'badge-muted'}`}>
                      {user.role === 'ADMIN' ? '管理员' : '普通用户'}
                    </span>
                  </td>
                  <td>
                    {user.isLocked ? (
                      <span style={{ color: 'var(--color-danger)' }}>已封禁</span>
                    ) : (
                      <span style={{ color: 'var(--color-success)' }}>正常</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      </div>
    </div>
  );
}
