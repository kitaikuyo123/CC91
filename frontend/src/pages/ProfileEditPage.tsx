import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyProfile, updateProfile } from '../api/user';
import { queryKeys } from '../lib/queryKeys';

/**
 * 编辑个人资料页面组件
 */
export default function ProfileEditPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [avatarUrl, setAvatarUrl] = useState('');
  const [bio, setBio] = useState('');
  const [location, setLocation] = useState('');
  const [website, setWebsite] = useState('');
  const [error, setError] = useState('');

  // 使用 React Query 获取当前资料
  const { data: profile, isLoading } = useQuery({
    queryKey: queryKeys.users.me(),
    queryFn: getMyProfile,
  });

  // React Query v5 移除了 onSuccess，改用 useEffect
  useEffect(() => {
    if (profile) {
      setAvatarUrl(profile.avatarUrl || '');
      setBio(profile.bio || '');
      setLocation(profile.location || '');
      setWebsite(profile.website || '');
    }
  }, [profile]);

  // 更新资料的 mutation
  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users.me() });
      if (user) {
        navigate(`/profile/${user.username}`);
      }
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '更新失败，请重试');
    },
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    updateMutation.mutate({
      avatarUrl: avatarUrl.trim() || undefined,
      bio: bio.trim() || undefined,
      location: location.trim() || undefined,
      website: website.trim() || undefined,
    });
  };

  const handleCancel = () => {
    if (user) {
      navigate(`/profile/${user.username}`);
    }
  };

  if (isLoading) {
    return (
      <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1rem' }}>加载中...</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '600px', padding: '2rem' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>编辑个人资料</h1>

        <form onSubmit={handleSubmit}>
          {/* 头像 URL */}
          <div className="form-group">
            <label htmlFor="avatarUrl">头像 URL</label>
            <input
              id="avatarUrl"
              type="url"
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="https://example.com/avatar.jpg"
            />
            <small style={{ color: '#888' }}>请输入图片的完整 URL 地址</small>
          </div>

          {/* 头像预览 */}
          {avatarUrl && (
            <div style={{ marginBottom: '1rem', textAlign: 'center' }}>
              <img
                src={avatarUrl}
                alt="头像预览"
                style={{
                  width: '100px',
                  height: '100px',
                  borderRadius: '50%',
                  objectFit: 'cover',
                  border: '2px solid #e0e0e0',
                }}
                onError={() => setError('图片加载失败，请检查 URL 是否正确')}
              />
            </div>
          )}

          {/* 个人签名 */}
          <div className="form-group">
            <label htmlFor="bio">个人签名</label>
            <textarea
              id="bio"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="介绍一下你自己..."
              rows={3}
              maxLength={500}
              style={{ resize: 'vertical', minHeight: '80px' }}
            />
            <div style={{ textAlign: 'right', fontSize: '0.85rem', color: '#888' }}>
              {bio.length}/500
            </div>
          </div>

          {/* 所在地 */}
          <div className="form-group">
            <label htmlFor="location">所在地</label>
            <input
              id="location"
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="例如：北京, 中国"
              maxLength={100}
            />
          </div>

          {/* 个人网站 */}
          <div className="form-group">
            <label htmlFor="website">个人网站</label>
            <input
              id="website"
              type="url"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
              disabled={updateMutation.isPending}
              placeholder="https://yourwebsite.com"
            />
          </div>

          {error && (
            <div className="error-message" style={{ marginBottom: '1rem' }}>
              {error}
            </div>
          )}

          {/* 按钮组 */}
          <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={updateMutation.isPending}
              style={{ flex: 1 }}
            >
              {updateMutation.isPending ? <span className="spinner"></span> : '保存'}
            </button>
            <button
              type="button"
              onClick={handleCancel}
              className="btn"
              disabled={updateMutation.isPending}
              style={{ flex: 1 }}
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
