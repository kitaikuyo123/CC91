import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetUsers, adminBanUser, adminUnbanUser, adminUpdateUserRole, adminDeleteUser } from '../../api/admin';
import { useAuth } from '../../context/AuthContext';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 用户管理页面
 */
export default function UserManage() {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 使用 React Query 获取用户列表
  const { data: users = [], isLoading } = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: adminGetUsers,
  });

  // 封禁用户的 mutation
  const banMutation = useMutation({
    mutationFn: ({ userId }: { userId: number; username: string }) =>
      adminBanUser(userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setError('');
      setSuccess(`用户「${variables.username}」已封禁`);
    },
    onError: (err: any) => {
      setSuccess('');
      setError(err.response?.data?.message || '封禁失败');
    },
  });

  // 解封用户的 mutation
  const unbanMutation = useMutation({
    mutationFn: ({ userId }: { userId: number; username: string }) =>
      adminUnbanUser(userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setError('');
      setSuccess(`用户「${variables.username}」已解封`);
    },
    onError: (err: any) => {
      setSuccess('');
      setError(err.response?.data?.message || '解封失败');
    },
  });

  // 修改角色的 mutation
  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: string; username: string }) =>
      adminUpdateUserRole(userId, role),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setError('');
      setSuccess(`用户「${variables.username}」角色已修改为 ${variables.role === 'ADMIN' ? '管理员' : '普通用户'}`);
    },
    onError: (err: any) => {
      setSuccess('');
      setError(err.response?.data?.message || '修改角色失败');
    },
  });

  const handleBan = (userId: number, username: string) => {
    if (!confirm(`确定要封禁用户「${username}」吗？`)) return;
    banMutation.mutate({ userId, username });
  };

  const handleUnban = (userId: number, username: string) => {
    unbanMutation.mutate({ userId, username });
  };

  const handleRoleChange = (userId: number, username: string, newRole: string, currentRole: string) => {
    if (newRole === currentRole) return;
    if (username === currentUser?.username) {
      if (!confirm('警告：修改自己的角色可能导致您失去管理权限，确定继续吗？')) return;
    } else {
      const label = newRole === 'ADMIN' ? '管理员' : '普通用户';
      if (!confirm(`确定要将用户「${username}」的角色修改为「${label}」吗？`)) return;
    }
    roleMutation.mutate({ userId, role: newRole, username });
  };

  // 删除用户的 mutation
  const deleteMutation = useMutation({
    mutationFn: ({ userId }: { userId: number; username: string }) =>
      adminDeleteUser(userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setError('');
      setSuccess(`用户「${variables.username}」已删除`);
    },
    onError: (err: any) => {
      setSuccess('');
      setError(err.response?.data?.message || '删除失败');
    },
  });

  const handleDelete = (userId: number, username: string) => {
    if (!confirm(`确定要删除用户「${username}」吗？此操作不可撤销！`)) return;
    deleteMutation.mutate({ userId, username });
  };

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>用户管理</h1>

      {error && (
        <div className="error-message" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {success && (
        <div className="success-message" style={{ marginBottom: '1rem' }}>
          {success}
        </div>
      )}

      {isLoading ? (
        <div className="loading-container">
          <div className="spinner spinner-lg"></div>
          <span>加载中...</span>
        </div>
      ) : (
        <div className="table-container">
        <div className="card">
          <p style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>共 {users.length} 位用户</p>

          {users.length === 0 ? (
            <div className="empty-state">暂无用户</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th className="hide-mobile">邮箱</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th className="hide-mobile">注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td style={{ color: 'var(--color-text-muted)' }}>#{user.id}</td>
                    <td style={{ fontWeight: '500' }}>{user.username}</td>
                    <td className="hide-mobile">{user.email}</td>
                    <td>
                      <select
                        value={user.role}
                        onChange={(e) => handleRoleChange(user.id, user.username, e.target.value, user.role)}
                        disabled={roleMutation.isPending}
                        style={{
                          padding: '0.2rem 0.4rem',
                          borderRadius: '4px',
                          border: '1px solid var(--color-border)',
                          background: 'var(--color-bg)',
                          color: 'var(--color-text)',
                          fontSize: '0.85rem',
                          cursor: roleMutation.isPending ? 'not-allowed' : 'pointer',
                        }}
                      >
                        <option value="USER">普通用户</option>
                        <option value="ADMIN">管理员</option>
                      </select>
                    </td>
                    <td>
                      {user.isLocked ? (
                        <span style={{ color: 'var(--color-danger)' }}>
                          <span aria-hidden="true">&#x1F512;</span> 已封禁
                          {user.lockUntil && <span className="hide-mobile"> 至 {new Date(user.lockUntil).toLocaleString()}</span>}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--color-success)' }}>&#x2713; 正常</span>
                      )}
                    </td>
                    <td className="hide-mobile" style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
                      {new Date(user.createdAt).toLocaleDateString()}
                    </td>
                    <td>
                      {user.isLocked ? (
                        <button
                          className="btn btn-success btn-sm"
                          onClick={() => handleUnban(user.id, user.username)}
                          disabled={user.username === currentUser?.username}
                          title={user.username === currentUser?.username ? '不能解封自己' : ''}
                        >
                          解封
                        </button>
                      ) : (
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => handleBan(user.id, user.username)}
                          disabled={user.username === currentUser?.username}
                          title={user.username === currentUser?.username ? '不能封禁自己' : ''}
                        >
                          封禁
                        </button>
                      )}
                      <button
                        className="btn btn-danger btn-sm"
                        style={{ marginLeft: '0.25rem', background: 'var(--color-danger)', borderColor: 'var(--color-danger)' }}
                        onClick={() => handleDelete(user.id, user.username)}
                        disabled={user.username === currentUser?.username}
                        title={user.username === currentUser?.username ? '不能删除自己' : '删除用户'}
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
                </table>
          )}
        </div>
        </div>
      )}
    </div>
  );
}
