import { useState, useEffect, type FormEvent } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchPosts } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import { useDebounce } from '../hooks';
import Breadcrumbs from '../components/Breadcrumbs';
import TopicTable from '../components/TopicTable';
import Pagination from '../components/Pagination';

/**
 * CC98 风格搜索页面组件
 */
export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialKeyword = searchParams.get('q') || '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [page, setPage] = useState(0);
  const [searched, setSearched] = useState(false);

  // 防抖处理搜索关键词
  const debouncedKeyword = useDebounce(keyword, 500);

  // 进行搜索的 Query
  const { data: searchResults, isLoading } = useQuery({
    queryKey: queryKeys.posts.search(debouncedKeyword, page, 10),
    queryFn: () => searchPosts(debouncedKeyword, page, 10),
    enabled: searched && debouncedKeyword.trim().length > 0,
  });

  const results = searchResults?.content || [];
  const totalPages = searchResults?.totalPages || 0;

  useEffect(() => {
    if (initialKeyword) {
      setKeyword(initialKeyword);
      setSearched(true);
    }
  }, [initialKeyword]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) {
      setSearchParams({ q: keyword });
      setPage(0);
      setSearched(true);
    }
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const navigate = useNavigate();

  return (
    <div className="cc98-search-page container" style={{ marginTop: '1.5rem', marginBottom: '3rem' }}>
      {/* 1. 面包屑 */}
      <Breadcrumbs 
        items={[
          { label: '版面列表', href: '/' },
          { label: '论坛搜索' }
        ]} 
      />

      {/* 2. 搜索输入卡片 */}
      <div className="cc98-search-card">
        <div className="cc98-search-header">
          <i className="fa fa-search"></i> 检索论坛主题帖
        </div>

        <form onSubmit={handleSubmit} className="cc98-search-form">
          <div className="cc98-search-input-wrap">
            <input
              id="search-input"
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="输入关键词搜索标题或内容..."
              className="cc98-search-control"
              autoFocus
            />
            <button type="submit" className="cc98-search-submit-btn" disabled={isLoading}>
              搜索
            </button>
          </div>
        </form>
      </div>

      {/* 3. 搜索结果展示 */}
      {searched && (
        <div style={{ marginTop: '2rem' }}>
          <div className="cc98-search-summary">找到 {searchResults?.totalElements || 0} 条结果</div>

          {isLoading ? (
            <div style={{ textAlign: 'center', padding: '3rem 0' }}>
              <div className="spinner"></div>
              <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>搜索中...</p>
            </div>
          ) : results.length === 0 ? (
            <div className="cc98-search-empty" style={{ padding: '3.5rem 1.5rem', textAlign: 'center', backgroundColor: 'var(--card-bg)', border: '1px dashed var(--border-color)', borderRadius: '4px', color: 'var(--text-muted)' }}>
              <i className="fa fa-folder-open-o" style={{ fontSize: '2.5rem', opacity: 0.4, display: 'block', marginBottom: '0.8rem' }}></i>没有找到匹配的帖子
            </div>
          ) : (
            <>
              {/* 结果列表 */}
              <div className="cc98-posts-card-list" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {results.map((post) => (
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

              {/* 底部分页 */}
              {totalPages > 1 && (
                <div style={{ margin: '1rem 0', display: 'flex', gap: '0.5rem', alignItems: 'center', justifyContent: 'center' }}>
                  <button
                    onClick={() => handlePageChange(page - 1)}
                    disabled={page === 0}
                    className="cc98-btn btn-publish"
                    style={{ fontSize: '0.85rem', padding: '0.4rem 1rem' }}
                  >上一页</button>
                  <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>{`第 ${page + 1} / ${totalPages} 页`}</span>
                  <button
                    onClick={() => handlePageChange(page + 1)}
                    disabled={page === totalPages - 1}
                    className="cc98-btn btn-publish"
                    style={{ fontSize: '0.85rem', padding: '0.4rem 1rem' }}
                  >下一页</button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      <style>{`
        .cc98-search-card {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-top: 8px solid var(--cc98-alt-color-a);
          border-radius: var(--cc98-radius);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-search-header {
          background-color: var(--quote-bg);
          border-bottom: 1px solid var(--border-color);
          padding: 0.85rem 1.5rem;
          font-weight: bold;
          font-size: 1.05rem;
          color: var(--text-main);
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .cc98-search-form {
          padding: 1.75rem 1.5rem;
        }

        .cc98-search-input-wrap {
          display: flex;
          gap: 0.75rem;
          width: 100%;
        }

        @media (max-width: 600px) {
          .cc98-search-input-wrap {
            flex-direction: column;
          }
          .cc98-search-submit-btn {
            width: 100%;
          }
        }

        .cc98-search-control {
          flex: 1;
          padding: 0.75rem 1rem;
          border: 1px solid var(--border-color);
          background-color: var(--card-bg);
          color: var(--text-main);
          border-radius: var(--cc98-radius-pill);
          font-size: 0.95rem;
          transition: var(--cc98-transition);
        }

        .cc98-search-control:focus {
          border-color: var(--primary-color);
          outline: none;
          box-shadow: 0 0 0 3px rgba(84, 110, 122, 0.15);
        }

        .cc98-search-submit-btn {
          background-color: var(--primary-color);
          color: white;
          border: none;
          padding: 0.75rem 1.75rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.95rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          display: inline-flex;
          align-items: center;
          gap: 0.4rem;
          justify-content: center;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .cc98-search-submit-btn:hover:not(:disabled) {
          background-color: var(--accent-color);
          color: #333;
          transform: translateY(-1px);
        }

        .cc98-search-submit-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .cc98-search-summary {
          font-size: 0.9rem;
          color: var(--text-muted);
          margin-bottom: 1rem;
          padding-left: 0.25rem;
        }

        .cc98-search-summary strong {
          color: var(--primary-text);
        }

        .cc98-search-empty {
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
