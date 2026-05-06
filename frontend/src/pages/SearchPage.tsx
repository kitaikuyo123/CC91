import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchPosts, type Post } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import { useDebounce } from '../hooks';

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
  const debouncedKeyword = useDebounce(keyword, 500);

  // 使用 React Query 进行搜索
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

        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="输入关键词搜索标题或内容..."
            style={{ flex: 1, padding: '0.75rem', border: '1px solid #ddd', borderRadius: '4px' }}
          />
          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? <span className="spinner"></span> : '搜索'}
          </button>
        </form>

        {searched && (
          <>
            <div style={{ marginBottom: '1rem', color: '#666' }}>
              找到 {searchResults?.totalElements || 0} 条结果
            </div>

            {results.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
                {isLoading ? '搜索中...' : '没有找到匹配的帖子'}
              </div>
            ) : (
              <>
                <div className="post-list">
                  {results.map((post) => (
                    <div
                      key={post.id}
                      className="card post-item"
                      onClick={() => window.open(`/posts/${post.id}`, '_blank')}
                      style={{ cursor: 'pointer', marginBottom: '1rem', padding: '1rem' }}
                    >
                      <h3 style={{ marginBottom: '0.5rem', color: '#3498db' }}>{post.title}</h3>
                      <p style={{ color: '#666', fontSize: '0.9rem' }}>
                        {post.content.substring(0, 150)}...
                      </p>
                      <div style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#999' }}>
                        <span>作者: {post.authorUsername}</span>
                        {post.categoryName && (
                          <span style={{ marginLeft: '1rem' }}>版块: {post.categoryName}</span>
                        )}
                        <span style={{ marginLeft: '1rem' }}>
                          {new Date(post.createdAt).toLocaleString()}
                        </span>
                        <span style={{ marginLeft: '1rem' }}>
                          浏览: {post.viewCount}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                {totalPages > 1 && (
                  <div style={{ display: 'flex', justifyContent: 'center', marginTop: '2rem' }}>
                    <button
                      className="btn"
                      onClick={() => handlePageChange(page - 1)}
                      disabled={page === 0}
                      style={{ marginRight: '0.5rem' }}
                    >
                      上一页
                    </button>
                    <span style={{ padding: '0.5rem 1rem' }}>
                      第 {page + 1} / {totalPages} 页
                    </span>
                    <button
                      className="btn"
                      onClick={() => handlePageChange(page + 1)}
                      disabled={page >= totalPages - 1}
                      style={{ marginLeft: '0.5rem' }}
                    >
                      下一页
                    </button>
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
