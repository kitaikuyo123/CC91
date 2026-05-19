import { useState, useEffect, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
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
              placeholder="请输入您想查找的帖子标题或正文关键字..."
              className="cc98-search-control"
              autoFocus
            />
            <button type="submit" className="cc98-search-submit-btn" disabled={isLoading}>
              {isLoading ? <i className="fa fa-spinner fa-spin"></i> : <><i className="fa fa-search"></i> 开始搜索</>}
            </button>
          </div>
        </form>
      </div>

      {/* 3. 搜索结果展示 */}
      {searched && (
        <div style={{ marginTop: '2rem' }}>
          <div className="cc98-search-summary">
            <i className="fa fa-info-circle"></i> 共检索到 <strong>{searchResults?.totalElements || 0}</strong> 个匹配的主题帖子
          </div>

          {isLoading ? (
            <div style={{ textAlign: 'center', padding: '3rem 0' }}>
              <div className="spinner"></div>
              <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>正在检索匹配帖...</p>
            </div>
          ) : results.length === 0 ? (
            <div className="cc98-search-empty">
              <i className="fa fa-folder-open-o" style={{ fontSize: '2.5rem', opacity: 0.4, display: 'block', marginBottom: '0.8rem' }}></i>
              抱歉，未找到与 “{keyword}” 相关的任何帖子。请尝试更改关键词。
            </div>
          ) : (
            <>
              {/* 分页 */}
              {totalPages > 1 && (
                <div style={{ margin: '0.5rem 0' }}>
                  <Pagination 
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={handlePageChange}
                  />
                </div>
              )}

              {/* 结果表格 */}
              <TopicTable posts={results} />

              {/* 底部分页 */}
              {totalPages > 1 && (
                <div style={{ margin: '0.5rem 0' }}>
                  <Pagination 
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={handlePageChange}
                  />
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
