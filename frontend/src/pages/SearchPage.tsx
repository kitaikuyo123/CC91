import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchPosts, type Post } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import { useDebounce } from '../hooks';
import { formatDate } from '../utils/formatDate';

/**
 * 搜索页面组件
 */
export default function SearchPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialKeyword = searchParams.get('q') || '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [page, setPage] = useState(0);
  const [searched, setSearched] = useState(false);

  // 防抖处理搜索关键词
  const debouncedKeyword = useDebounce(keyword, 300);

  // 使用 React Query 进行搜索
  const { data: searchResults, isLoading } = useQuery({
    queryKey: queryKeys.posts.search(debouncedKeyword, page, 10),
    queryFn: () => searchPosts(debouncedKeyword, page, 10),
    enabled: searched && debouncedKeyword.trim().length > 0,
    placeholderData: (prev) => prev,
  });

  const results = searchResults?.content || [];
  const totalPages = searchResults?.totalPages || 0;

  const isFirstRender = useRef(true);

  useEffect(() => {
    if (initialKeyword) {
      setKeyword(initialKeyword);
      setSearched(true);
    }
  }, [initialKeyword]);

  // 自动搜索：用户输入防抖后自动触发搜索，无需手动点击按钮
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    if (debouncedKeyword.trim() && debouncedKeyword !== initialKeyword) {
      setSearchParams({ q: debouncedKeyword }, { replace: true });
      setPage(0);
      setSearched(true);
    }
  }, [debouncedKeyword, initialKeyword, setSearchParams]);

  const handleSubmit = (e: React.FormEvent) => {
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
    }
  };

  return (
    <div className="container" style={{ marginTop: '2rem', maxWidth: '800px' }}>
      <div className="card">
        <h1 style={{ marginBottom: '1.5rem' }}>搜索帖子</h1>

        <form onSubmit={handleSubmit} role="search" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
          <label htmlFor="search-input" className="visually-hidden">搜索关键词</label>
          <input
            id="search-input"
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="输入关键词搜索标题或内容..."
            style={{ flex: 1, padding: '0.75rem', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)' }}
          />
          <button type="submit" className="btn btn-primary" disabled={isLoading} aria-busy={isLoading}>
            {isLoading ? <span className="spinner" aria-hidden="true"></span> : '搜索'}
          </button>
        </form>

        {searched && (
          <>
            <div style={{ marginBottom: '1rem', color: 'var(--color-text-muted)' }}>
              找到 {searchResults?.totalElements || 0} 条结果
            </div>

            {results.length === 0 ? (
              <div className="empty-state">
                {isLoading ? '搜索中...' : '没有找到匹配的帖子'}
              </div>
            ) : (
              <>
                <div className="post-list">
                  {results.map((post) => (
                    <div
                      key={post.id}
                      className="card post-item"
                      role="link"
                      tabIndex={0}
                      onClick={() => navigate(`/posts/${post.id}`)}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/posts/${post.id}`); } }}
                      aria-label={`帖子: ${post.title}`}
                      style={{ marginBottom: '1rem', padding: '1rem' }}
                    >
                      <h3 style={{ marginBottom: '0.5rem', color: 'var(--color-primary)' }}>{post.title}</h3>
                      <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem' }}>
                        {post.content ? `${post.content.substring(0, 150)}...` : ''}
                      </p>
                      <div className="post-meta" style={{ marginTop: '0.5rem' }}>
                        <span>作者: {post.authorUsername}</span>
                        {post.categoryName && (
                          <span>版块: {post.categoryName}</span>
                        )}
                        <span>
                          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                        </span>
                        <span>浏览: {post.viewCount}</span>
                        <span>评论: {post.commentCount}</span>
                      </div>
                    </div>
                  ))}
                </div>

                {totalPages > 1 && (
                  <nav className="pagination" aria-label="搜索结果分页">
                    <button
                      className="btn"
                      onClick={() => handlePageChange(page - 1)}
                      disabled={page === 0}
                      aria-label="上一页"
                    >
                      上一页
                    </button>
                    <span className="pagination-info">
                      第 {page + 1} / {totalPages} 页
                    </span>
                    <button
                      className="btn"
                      onClick={() => handlePageChange(page + 1)}
                      disabled={page >= totalPages - 1}
                      aria-label="下一页"
                    >
                      下一页
                    </button>
                  </nav>
                )}
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
