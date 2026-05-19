import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getUserProfile } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';
import catAvatar from '../assets/cc98_avatar_cat.png';
import studentAvatar from '../assets/cc98_avatar_student.png';

function hashCode(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  return Math.abs(hash);
}

/**
 * CC98 风格用户资料展示页面组件
 */
export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();

  const isOwnProfile = currentUser?.username === username;

  // 使用 React Query 获取用户资料
  const { data: profile, isLoading, error } = useQuery({
    queryKey: queryKeys.users.detail(username || ''),
    queryFn: () => getUserProfile(username || ''),
    enabled: !!username,
  });

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>加载中...</p>
      </div>
    );
  }

  if (error || !profile) {
    const errorMessage = (error as any)?.response?.data?.message || '用户不存在';
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-profile-error">
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>{errorMessage}</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>请检查用户名是否输入正确或稍后再试。</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
        <style>{`
          .cc98-profile-error {
            background-color: var(--card-bg);
            border: 1px solid var(--border-color);
            padding: 3rem 1rem;
            text-align: center;
            border-radius: var(--cc98-radius);
            box-shadow: var(--cc98-shadow);
          }
        `}</style>
      </div>
    );
  }

  const hash = hashCode(profile.username);
  const defaultAvatar = hash % 3 === 0 ? studentAvatar : catAvatar;
  const avatar = profile.avatarUrl || defaultAvatar;

  return (
    <div className="cc98-profile-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs
        items={[
          { label: '讨论区', href: '/' },
          { label: `用户 @${profile.username} 的名片` }
        ]}
      />

      {/* 2. 主体个人卡片 */}
      <div className="cc98-profile-card">
        {/* 顶部彩色装饰条 */}
        <div className="profile-banner-bar"></div>

        <div className="profile-main-area">
          {/* 左侧头像与威望级别 */}
          <div className="profile-left-col">
            <div className="profile-avatar-container">
              {profile.avatarUrl ? (
                <img
                  src={profile.avatarUrl}
                  alt={`${profile.username} 的头像`}
                  className="profile-large-avatar"
                />
              ) : (
                <div className="profile-avatar-placeholder">
                  {profile.username.charAt(0).toUpperCase()}
                </div>
              )}
            </div>

            <div className="profile-meta-stats">
              <div className="stat-line">
                <span>注册时间:</span>
                <strong>{profile.createdAt.split('T')[0]}</strong>
              </div>
            </div>
          </div>

          {/* 右侧详细资料栏 */}
          <div className="profile-right-col">
            <div className="profile-name-header">
              <h2>{profile.username}</h2>
              {isOwnProfile && (
                <button
                  onClick={() => navigate('/profile/edit')}
                  className="cc98-profile-edit-btn"
                >
                  <i className="fa fa-pencil-square-o"></i> 编辑资料
                </button>
              )}
            </div>

            <hr className="profile-divider" />

            <div className="profile-details-grid">
              <div className="profile-detail-item">
                <span className="lbl"><i className="fa fa-envelope-o"></i> 电子邮箱:</span>
                <span className="val">{profile.email || '保密'}</span>
              </div>

              <div className="profile-detail-item">
                <span className="lbl"><i className="fa fa-map-marker"></i> 所在地:</span>
                <span className="val">{profile.location || '求是园 (ZJU)'}</span>
              </div>

              <div className="profile-detail-item">
                <span className="lbl"><i className="fa fa-link"></i> 个人网站:</span>
                <span className="val">
                  {profile.website ? (
                    <a href={profile.website} target="_blank" rel="noopener noreferrer" className="profile-link">
                      {profile.website}
                    </a>
                  ) : (
                    '未填写'
                  )}
                </span>
              </div>
            </div>

            <div className="profile-bio-box">
              <div className="bio-title"><i className="fa fa-quote-left"></i> 个人签名档：</div>
              <div className="bio-content">
                {profile.bio || '暂无详细信息'}
              </div>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .cc98-profile-card {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
          position: relative;
        }

        .profile-banner-bar {
          height: 12px;
          background: linear-gradient(90deg, var(--cc98-alt-color-a), var(--cc98-alt-color-b));
        }

        .profile-main-area {
          padding: 2.5rem 2rem;
          display: flex;
          gap: 2.5rem;
        }

        @media (max-width: 768px) {
          .profile-main-area {
            flex-direction: column;
            align-items: center;
            text-align: center;
          }
          .profile-left-col {
            border-right: none !important;
            padding-right: 0 !important;
          }
          .profile-name-header {
            flex-direction: column;
            gap: 1rem;
            align-items: center;
          }
        }

        .profile-left-col {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 180px;
          flex-shrink: 0;
          border-right: 1px dashed var(--border-color);
          padding-right: 2.5rem;
        }

        .profile-avatar-container {
          width: 120px;
          height: 120px;
          border-radius: 50%;
          padding: 3px;
          border: 2px solid var(--border-color);
          background-color: var(--card-bg);
          box-shadow: 0 4px 10px rgba(0,0,0,0.1);
        }

        .profile-large-avatar {
          width: 100%;
          height: 100%;
          object-fit: cover;
          border-radius: 50%;
        }
        .profile-avatar-placeholder {
          width: 100%;
          height: 100%;
          background-color: var(--primary-color);
          color: white;
          font-size: 2.5rem;
          font-weight: bold;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 50%;
        }

        

        .profile-meta-stats {
          margin-top: 1.5rem;
          width: 100%;
          font-size: 0.8rem;
          color: var(--text-muted);
        }

        .stat-line {
          display: flex;
          justify-content: space-between;
          margin-bottom: 0.4rem;
        }

        .profile-right-col {
          flex: 1;
          min-width: 0;
        }

        .profile-name-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .profile-name-header h2 {
          font-size: 1.65rem;
          font-weight: bold;
          margin: 0;
          color: var(--text-main);
        }

        .cc98-profile-edit-btn {
          background-color: transparent;
          color: var(--primary-color);
          border: 1px solid var(--primary-color);
          padding: 0.45rem 1.25rem;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          font-weight: bold;
          font-size: 0.85rem;
          transition: var(--cc98-transition);
          white-space: nowrap;
        }

        .cc98-profile-edit-btn:hover {
          background-color: var(--primary-color);
          color: white;
        }

        .profile-divider {
          border: none;
          border-top: 1px solid var(--border-color);
          margin: 1.25rem 0;
        }

        .profile-details-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
          gap: 1.25rem;
          margin-bottom: 2rem;
        }

        .profile-detail-item {
          display: flex;
          flex-direction: column;
          gap: 0.25rem;
        }

        .profile-detail-item .lbl {
          font-size: 0.85rem;
          color: var(--text-muted);
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }

        .profile-detail-item .val {
          font-size: 0.95rem;
          font-weight: 500;
          color: var(--text-main);
        }

        .profile-link {
          color: var(--primary-color);
          text-decoration: none;
        }

        .profile-link:hover {
          text-decoration: underline;
        }

        .profile-bio-box {
          background-color: var(--quote-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 1.25rem;
        }

        .bio-title {
          font-size: 0.82rem;
          color: var(--text-muted);
          margin-bottom: 0.5rem;
          font-weight: bold;
        }

        .bio-content {
          font-size: 0.9rem;
          color: var(--text-main);
          line-height: 1.6;
          word-break: break-all;
        }
      `}</style>
    </div>
  );
}
