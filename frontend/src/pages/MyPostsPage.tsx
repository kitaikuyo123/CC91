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

      <div style={{ marginBottom: '1.25rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <button
          onClick={() => navigate('/dashboard')}
          className="cc98-btn btn-publish"
          style={{ fontSize: '0.85rem', padding: '0.4rem 1rem' }}
        >
          <i className="fa fa-chevron-left"></i> 返回 Dashboard
        </button>
        <span className="summary-info" style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          共 {myPosts.length} 篇帖子
        </span>
      </div>

      {myPosts.length === 0 ? (
        <div className="cc98-my-posts-empty" style={{ padding: '3.5rem 1.5rem', textAlign: 'center', backgroundColor: 'var(--card-bg)', border: '1px dashed var(--border-color)', borderRadius: '4px', color: 'var(--text-muted)' }}>
          <i className="fa fa-pencil-square-o" style={{ fontSize: '2.5rem', opacity: 0.4, display: 'block', marginBottom: '0.8rem' }}></i>
          你还没有发布帖子
        </div>
      ) : (
        <div className="cc98-posts-card-list" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {myPosts.map((post) => (
            <div
              key={post.id}
              className="card cc98-post-card-item"
              onClick={() => navigate(`/posts/${post.id}`)}
              style={{
                cursor: 'pointer',
                backgroundColor: 'var(--card-bg)',
                border: '1px solid var(--border-color)',
                borderRadius: '4px',
                padding: '1.25rem',
                transition: 'var(--cc98-transition)',
                boxShadow: 'var(--cc98-shadow)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--quote-bg)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--card-bg)';
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <h3 className="card-title" style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-main)', fontWeight: 'bold' }}>
                  {post.categoryName && <span style={{ color: 'var(--primary-color)', marginRight: '0.5rem' }}>[{post.categoryName}]</span>}
                  {post.title}
                </h3>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{post.createdAt.split('T')[0]}</span>
              </div>
              <p className="card-content" style={{ fontSize: '0.9rem', color: 'var(--text-muted)', margin: '0.5rem 0 1rem 0', lineBreak: 'anywhere', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {post.content}
              </p>
              <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                <span><i className="fa fa-eye"></i> 阅读量: {post.viewCount}</span>
                <span><i className="fa fa-comments"></i> 回复数: {post.commentCount ?? 0}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
