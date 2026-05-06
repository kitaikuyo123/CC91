import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetUsers, adminBanUser, adminUnbanUser, type AdminUser } from '../../api/admin';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 用户管理页面
 */
export default function UserManage() {
  const queryClient = useQueryClient();

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 使用 React Query 获取用户列表
  const { data: users = [], isLoading } = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: adminGetUsers,
  });

  // 封禁用户的 mutation
  const banMutation = useMutation({
    mutationFn: ({ userId, username }: { userId: number; username: string }) =>
      adminBanUser(userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setSuccess(`用户「${variables.username}」已封禁`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '封禁失败');
    },
  });

  // 解封用户的 mutation
  const unbanMutation = useMutation({
    mutationFn: ({ userId, username }: { userId: number; username: string }) =>
      adminUnbanUser(userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
      setSuccess(`用户「${variables.username}」已解封`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '解封失败');
    },
  });

  const handleBan = (userId: number, username: string) => {
    if (!confirm(`确定要封禁用户「${username}」吗？`)) return;
    banMutation.mutate({ userId, username });
  };

  const handleUnban = (userId: number, username: string) => {
    unbanMutation.mutate({ userId, username });
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
        <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#d4edda', color: '#155724', borderRadius: '4px' }}>
          {success}
        </div>
      )}

      {loading ? (
        <div>加载中...</div>
      ) : (
        <div className="card">
          <p style={{ color: '#666', marginBottom: '1rem' }}>共 {users.length} 位用户</p>

          {users.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
              暂无用户
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #eee' }}>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>ID</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>用户名</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>邮箱</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>角色</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>状态</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>注册时间</th>
                  <th style={{ padding: '0.75rem', textAlign: 'left' }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '0.75rem', color: '#666' }}>#{user.id}</td>
                    <td style={{ padding: '0.75rem', fontWeight: '500' }}>{user.username}</td>
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
                        <span style={{ color: '#e74c3c' }}>
                          🔒 已封禁
                          {user.lockUntil && ` 至 ${new Date(user.lockUntil).toLocaleString()}`}
                        </span>
                      ) : (
                        <span style={{ color: '#2ecc71' }}>✓ 正常</span>
                      )}
                    </td>
                    <td style={{ padding: '0.75rem', color: '#666', fontSize: '0.9rem' }}>
                      {new Date(user.createdAt).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      {user.isLocked ? (
                        <button
                          className="btn"
                          onClick={() => handleUnban(user.id, user.username)}
                          style={{ background: '#2ecc71', color: '#fff', fontSize: '0.85rem' }}
                        >
                          解封
                        </button>
                      ) : (
                        <button
                          className="btn"
                          onClick={() => handleBan(user.id, user.username)}
                          style={{ background: '#e74c3c', color: '#fff', fontSize: '0.85rem' }}
                        >
                          封禁
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
