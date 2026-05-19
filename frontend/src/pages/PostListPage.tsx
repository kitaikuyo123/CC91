import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPostList, getPostsByCategory } from '../api/post';
import { getCategories } from '../api/category';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';
import TopicTable from '../components/TopicTable';
import Pagination from '../components/Pagination';

/**
 * CC98 风格全站帖子列表页面 - 支持版块筛选
 */
export default function PostListPage() {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState(0);
  const [categoryFilter, setCategoryFilter] = useState<number | undefined>(undefined);
  const pageSize = 10;

  // 获取版块列表
  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  // 根据是否选中版块调用不同接口获取帖子
  const { data: postsData, isLoading, error } = useQuery({
    queryKey:
      categoryFilter != null
        ? queryKeys.posts.byCategory(categoryFilter, currentPage, pageSize)
        : queryKeys.posts.list({ page: currentPage, size: pageSize, status: 'PUBLISHED' }),
    queryFn: () =>
      categoryFilter != null
        ? getPostsByCategory(categoryFilter, currentPage, pageSize)
        : getPostList(currentPage, pageSize, 'PUBLISHED'),
  });

  const posts = postsData?.content || [];
  const totalPages = postsData?.totalPages || 0;
  const totalElements = postsData?.totalElements || 0;

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleCategoryFilterChange = (categoryId: number | undefined) => {
    setCategoryFilter(categoryId);
    setCurrentPage(0);
  };

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>加载中...</p>
      </div>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message || '加载帖子列表失败';
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-editor-card" style={{ padding: '3rem 1.5rem', textAlign: 'center' }}>
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>加载帖子列表出错</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>{errorMessage}</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="cc98-post-list-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '论坛首页', href: '/' },
          { label: '全站主题帖浏览' }
        ]} 
      />

      {/* 2. 筛选版块导航 */}
      <div className="cc98-filter-card">
        <div className="filter-header">
          <i className="fa fa-filter"></i> 按版面筛选主题
        </div>
        <div className="filter-button-bar">
          <button
            className={`cc98-filter-btn ${categoryFilter === undefined ? 'active' : ''}`}
            onClick={() => handleCategoryFilterChange(undefined)}
          >
            全部版块
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`cc98-filter-btn ${categoryFilter === cat.id ? 'active' : ''}`}
              onClick={() => handleCategoryFilterChange(cat.id)}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* 3. 统计与发帖按钮 */}
      <div className="cc98-list-meta-row">
        <div className="summary-info">
          <i className="fa fa-info-circle"></i>共 {totalElements} 篇帖子
        </div>
        <button
          onClick={() => categoryFilter != null ? navigate('/posts/new', { state: { categoryId: categoryFilter } }) : navigate('/posts/new')}
          className="cc98-new-post-btn"
        >
          <i className="fa fa-pencil"></i> 发布新帖
        </button>
      </div>



      {/* 5. 帖子列表 (TopicTable) 或空状态 */}
      {posts.length === 0 ? (
        <div className="cc98-empty-state card" style={{ padding: '4rem 1.5rem', textAlign: 'center', border: '1px solid var(--border-color)', borderRadius: 'var(--cc98-radius)', backgroundColor: 'var(--card-bg)', marginTop: '0.5rem', boxShadow: 'var(--cc98-shadow)' }}>
          <i className="fa fa-folder-open-o" style={{ fontSize: '3rem', color: 'var(--text-muted)', marginBottom: '1rem', display: 'block', opacity: 0.6 }}></i>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.95rem' }}>暂无符合条件的帖子</p>
          <button className="cc98-new-post-btn" onClick={() => categoryFilter != null ? navigate('/posts/new', { state: { categoryId: categoryFilter } }) : navigate('/posts/new')}>
            <i className="fa fa-pencil"></i> 发布第一篇帖子
          </button>
        </div>
      ) : (
        <div style={{ marginTop: '0.5rem' }}>
          <TopicTable posts={posts} />
        </div>
      )}

      {/* 6. 分页器 (底) */}
      {totalPages > 1 && (
        <div style={{ margin: '0.5rem 0' }}>
          <Pagination 
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      <style>{`
        .cc98-filter-card {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-top: 6px solid var(--primary-color);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
          margin-bottom: 1.5rem;
        }

        .filter-header {
          background-color: var(--quote-bg);
          border-bottom: 1px solid var(--border-color);
          padding: 0.75rem 1.25rem;
          font-weight: bold;
          font-size: 0.92rem;
          color: var(--text-main);
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .filter-button-bar {
          padding: 1.25rem;
          display: flex;
          flex-wrap: wrap;
          gap: 0.5rem;
        }

        .cc98-filter-btn {
          background-color: transparent;
          color: var(--text-muted);
          border: 1px solid var(--border-color);
          padding: 0.4rem 1rem;
          border-radius: var(--cc98-radius-pill);
          cursor: pointer;
          font-size: 0.85rem;
          font-weight: bold;
          transition: var(--cc98-transition);
        }

        .cc98-filter-btn:hover {
          color: var(--primary-color);
          border-color: var(--primary-color);
          background-color: var(--quote-bg);
        }

        .cc98-filter-btn.active {
          background-color: var(--primary-color);
          color: white;
          border-color: var(--primary-color);
        }

        .cc98-list-meta-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 1.25rem;
          gap: 1.5rem;
        }

        @media (max-width: 600px) {
          .cc98-list-meta-row {
            flex-direction: column;
            align-items: flex-start;
          }
          .cc98-new-post-btn {
            width: 100%;
          }
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
      `}</style>
    </div>
  );
}
