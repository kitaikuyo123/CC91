import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategoryById } from '../api/category';
import { getPostsByCategory } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';
import TopicTable from '../components/TopicTable';
import Pagination from '../components/Pagination';

/**
 * CC98 风格版块列表页面组件
 */
export default function CategoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const categoryId = id ? parseInt(id) : 0;

  // 获取版块信息
  const { data: category, isLoading: categoryLoading, error: categoryError } = useQuery({
    queryKey: queryKeys.categories.detail(categoryId),
    queryFn: () => getCategoryById(categoryId),
    enabled: categoryId > 0,
  });

  // 获取版块帖子
  const { data: postsData, isLoading: postsLoading, error: postsError } = useQuery({
    queryKey: queryKeys.posts.byCategory(categoryId, page, 10),
    queryFn: () => getPostsByCategory(categoryId, page, 10),
    enabled: categoryId > 0,
  });

  const posts = postsData?.content || [];
  const totalPages = postsData?.totalPages || 0;
  const loading = categoryLoading || postsLoading;
  const error = categoryError || postsError;

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>正在载入版块主题帖...</p>
      </div>
    );
  }

  if (error || !category) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-category-error">
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>加载版块出错</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>
            {(error as any)?.response?.data?.message || '版块不存在或已被删除。'}
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
        <style>{`
          .cc98-category-error {
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

  // Choose a board icon based on ID
  const icon = categoryId % 4 === 0 
    ? 'fa-laptop' 
    : categoryId % 4 === 1 
      ? 'fa-heartbeat' 
      : categoryId % 4 === 2 
        ? 'fa-gamepad' 
        : 'fa-comments-o';

  return (
    <div className="cc98-category-page container">
      {/* 1. 面包屑导航 */}
      <Breadcrumbs 
        items={[
          { label: '讨论板块' },
          { label: category.name }
        ]} 
      />

      {/* 2. 版块头部 Banner 卡片 */}
      <div className="cc98-board-header-box">
        <div className="cc98-board-info-row">
          <div className="cc98-board-large-icon">
            <i className={`fa ${icon}`}></i>
          </div>
          <div className="cc98-board-meta-details">
            <h1>{category.name}</h1>
            <p>{category.description || '这里是求是学子自由讨论和交流的家园。请文明发言，遵守版规 🍃'}</p>
          </div>
        </div>

        <div className="cc98-board-actions-row">
          <button
            onClick={() => navigate('/posts/new', { state: { categoryId } })}
            className="cc98-new-topic-btn"
          >
            <i className="fa fa-pencil-square-o"></i> 发表新主题
          </button>
        </div>
      </div>

      {/* 3. 顶部翻页器 */}
      {totalPages > 1 && (
        <div className="cc98-page-row-container">
          <Pagination 
            currentPage={page}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      {/* 4. 帖子列表 (TopicTable) */}
      <div style={{ marginTop: '1rem' }}>
        <TopicTable posts={posts} />
      </div>

      {/* 5. 底部翻页器 */}
      {totalPages > 1 && (
        <div className="cc98-page-row-container">
          <Pagination 
            currentPage={page}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      <style>{`
        .cc98-category-page {
          margin-top: 1.5rem;
        }

        .cc98-board-header-box {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-top: 8px solid var(--primary-color);
          border-radius: var(--cc98-radius);
          padding: 1.5rem;
          display: flex;
          align-items: center;
          justify-content: space-between;
          box-shadow: var(--cc98-shadow);
          margin-bottom: 1.5rem;
          gap: 1.5rem;
        }

        @media (max-width: 768px) {
          .cc98-board-header-box {
            flex-direction: column;
            text-align: center;
            align-items: center;
          }
          .cc98-board-info-row {
            flex-direction: column;
            align-items: center;
          }
          .cc98-new-topic-btn {
            width: 100%;
          }
        }

        .cc98-board-info-row {
          display: flex;
          align-items: center;
          gap: 1.5rem;
          flex: 1;
        }

        .cc98-board-large-icon {
          width: 64px;
          height: 64px;
          border-radius: 12px;
          background-color: var(--quote-bg);
          color: var(--primary-color);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 2.2rem;
          border: 1px solid var(--border-color);
          flex-shrink: 0;
        }

        .cc98-board-meta-details h1 {
          font-size: 1.5rem;
          font-weight: bold;
          color: var(--text-main);
          margin: 0 0 0.5rem 0;
        }

        .cc98-board-meta-details p {
          font-size: 0.88rem;
          color: var(--text-muted);
          margin: 0;
        }

        .cc98-new-topic-btn {
          background-color: var(--primary-color);
          color: white;
          border: none;
          padding: 0.6rem 1.5rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.92rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          display: inline-flex;
          align-items: center;
          gap: 0.4rem;
          box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
          white-space: nowrap;
        }

        .cc98-new-topic-btn:hover {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-2px);
          box-shadow: 0 5px 12px rgba(0, 0, 0, 0.15);
        }

        .cc98-page-row-container {
          margin: 0.75rem 0;
        }
      `}</style>
    </div>
  );
}
