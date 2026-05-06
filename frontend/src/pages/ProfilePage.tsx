import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getUserProfile, type UserProfile } from '../api/user';
import { queryKeys } from '../lib/queryKeys';

/**
 * 用户资料页面组件
 */
export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user: currentUser, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const isOwnProfile = currentUser?.username === username;

  // 使用 React Query 获取用户资料
  const { data: profile, isLoading, error } = useQuery({
    queryKey: queryKeys.users.detail(username || ''),
    queryFn: () => getUserProfile(username || ''),
    enabled: !!username,
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  if (isLoading) {
    return (
      <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem' }}>加载中...</p>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="container" style={{ padding: '2rem' }}>
        <div className="error-message">{(error as any)?.response?.data?.message || '用户不存在'}</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '800px', padding: '2rem' }}>
      {/* 头部：头像 + 基本信息 */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          {/* 头像 */}
          <div style={{
            width: '120px',
            height: '120px',
            borderRadius: '50%',
            backgroundColor: '#e0e0e0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '3rem',
            color: '#757575',
            overflow: 'hidden'
          }}>
            {profile.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt={`${profile.username} 的头像`}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              <span>{profile.username.charAt(0).toUpperCase()}</span>
            )}
          </div>

          {/* 基本信息 */}
          <div style={{ flex: 1 }}>
            <h1 style={{ marginBottom: '0.5rem' }}>{profile.username}</h1>
            {profile.bio && (
              <p style={{ color: '#666', marginBottom: '0.5rem' }}>{profile.bio}</p>
            )}
            <div style={{ fontSize: '0.9rem', color: '#888' }}>
              加入于 {formatDate(profile.createdAt)}
            </div>

            {/* 编辑按钮（仅自己的资料） */}
            {isOwnProfile && (
              <button
                onClick={() => navigate('/profile/edit')}
                className="btn btn-primary"
                style={{ marginTop: '1rem' }}
              >
                编辑资料
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 详细信息 */}
      <div className="card">
        <h2 style={{ marginBottom: '1rem', fontSize: '1.2rem' }}>详细信息</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          {/* 所在地 */}
          {profile.location && (
            <div>
              <div style={{ fontSize: '0.85rem', color: '#888', marginBottom: '0.25rem' }}>所在地</div>
              <div style={{ fontSize: '1rem' }}>{profile.location}</div>
            </div>
          )}

          {/* 网站 */}
          {profile.website && (
            <div>
              <div style={{ fontSize: '0.85rem', color: '#888', marginBottom: '0.25rem' }}>个人网站</div>
              <div>
                <a
                  href={profile.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#3498db', textDecoration: 'none' }}
                >
                  {profile.website}
                </a>
              </div>
            </div>
          )}

          {/* 邮箱 */}
          <div>
            <div style={{ fontSize: '0.85rem', color: '#888', marginBottom: '0.25rem' }}>邮箱</div>
            <div style={{ fontSize: '1rem' }}>{profile.email}</div>
          </div>
        </div>

        {/* 无详细信息时的占位 */}
        {!profile.location && !profile.website && (
          <div style={{ color: '#888', fontStyle: 'italic' }}>
            暂无详细信息
          </div>
        )}
      </div>
    </div>
  );
}
