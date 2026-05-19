import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyPosts } from '../api/user';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';
import TopicTable from '../components/TopicTable';

/**
 * CC98 风格我的帖子页面 - 展示当前用户全部帖子
 */
export default function MyPostsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: myPosts = [], isLoading, error } = useQuery({
    queryKey: queryKeys.users.mePosts(),
    queryFn: () => getMyPosts(),
    enabled: !!user,
  });

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>正在载入您发表的主题帖...</p>
      </div>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message || '加载失败';
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-editor-card" style={{ padding: '3rem 1.5rem', textAlign: 'center' }}>
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>获取数据失败</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>{errorMessage}</p>
          <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
            返回个人中心
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="cc98-my-posts-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '首页', href: '/' },
          { label: '个人中心', href: '/dashboard' },
          { label: '我的主题帖' }
        ]} 
      />

      {/* 2. 统计与发帖按钮 */}
      <div className="cc98-list-meta-row">
        <div className="summary-info">
          <i className="fa fa-file-text-o"></i> 您一共发表了 <strong>{myPosts.length}</strong> 篇主题帖子
        </div>
        <button
          onClick={() => navigate('/posts/new')}
          className="cc98-new-post-btn"
        >
          <i className="fa fa-pencil"></i> 发表新主题贴
        </button>
      </div>

      {myPosts.length === 0 ? (
        <div className="cc98-my-posts-empty">
          <i className="fa fa-pencil-square-o" style={{ fontSize: '2.5rem', opacity: 0.4, display: 'block', marginBottom: '0.8rem' }}></i>
          您还没有在论坛发表过任何主题帖。现在去发个帖吧！
        </div>
      ) : (
        <TopicTable posts={myPosts} />
      )}

      <style>{`
        .cc98-list-meta-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 1.25rem;
          gap: 1.5rem;
        }

        .summary-info {
          font-size: 0.9rem;
          color: var(--text-muted);
        }

        .summary-info strong {
          color: var(--primary-text);
        }

        .cc98-new-post-btn {
          background-color: var(--primary-color);
          color: white;
          border: none;
          padding: 0.55rem 1.25rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.88rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          display: inline-flex;
          align-items: center;
          gap: 0.4rem;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .cc98-new-post-btn:hover {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-1px);
        }

        .cc98-my-posts-empty {
          background-color: var(--card-bg);
          border: 1px dashed var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 3.5rem 1.5rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.92rem;
        }
      `}</style>
    </div>
  );
}
