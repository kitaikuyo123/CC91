import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategories } from '../api/category';
import { getPostList } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import { formatDate } from '../utils/formatDate';

/**
 * 首页组件 - 展示版块列表、最新帖子、热门帖子
 */
export default function HomePage() {
  const navigate = useNavigate();
  const [searchKeyword, setSearchKeyword] = useState('');

  // 获取版块列表
  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  // 获取最新帖子
  const { data: recentPostsData } = useQuery({
    queryKey: queryKeys.posts.list({ page: 0, size: 10 }),
    queryFn: () => getPostList(0, 10),
  });

  // 获取更多帖子用于热门排序
  const { data: morePostsData } = useQuery({
    queryKey: queryKeys.posts.list({ page: 0, size: 50 }),
    queryFn: () => getPostList(0, 50),
  });

  // 计算热门帖子（按浏览量排序）
  const hotPosts = morePostsData?.content
    ? [...morePostsData.content].sort((a, b) => b.viewCount - a.viewCount).slice(0, 5)
    : [];

  const recentPosts = recentPostsData?.content || [];

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchKeyword.trim())}`);
    }
  };

  return (
    <div className="container" style={{ marginTop: '2rem' }}>
      <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
        <h1 style={{ marginBottom: '0.5rem' }}>CC91 论坛</h1>
        <p style={{ color: '#666' }}>一个现代化的技术交流社区</p>
      </div>

      {/* 版块列表 */}
      <section style={{ marginBottom: '3rem' }} aria-labelledby="sections-heading">
        <h2 id="sections-heading" className="section-title">讨论版块</h2>
        <div className="category-grid">
          {categories.map((category) => (
            <div
              key={category.id}
              className="card post-item"
              role="button"
              tabIndex={0}
              onClick={() => navigate(`/category/${category.id}`)}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/category/${category.id}`); } }}
              aria-label={`进入版块: ${category.name}`}
            >
              <h3 style={{ marginBottom: '0.5rem', color: 'var(--color-primary)' }}>{category.name}</h3>
              {category.description && (
                <p style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>{category.description}</p>
              )}
            </div>
          ))}
        </div>
      </section>

      <div className="home-content-grid">
        {/* 最新帖子 */}
        <section aria-labelledby="recent-heading">
          <h2 id="recent-heading" className="section-title">最新帖子</h2>
          <div className="post-list">
            {recentPosts.length === 0 ? (
              <div className="empty-state">暂无帖子</div>
            ) : (
              recentPosts.map((post) => (
                <div
                  key={post.id}
                  className="card post-item"
                  role="link"
                  tabIndex={0}
                  onClick={() => navigate(`/posts/${post.id}`)}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/posts/${post.id}`); } }}
                  aria-label={`帖子: ${post.title}, 作者: ${post.authorUsername}`}
                >
                  <h3 style={{ marginBottom: '0.5rem', fontSize: '1.1rem' }}>{post.title}</h3>
                  <p style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
                    {post.content ? `${post.content.substring(0, 100)}...` : ''}
                  </p>
                  <div className="post-meta" style={{ marginTop: '0.5rem' }}>
                    <span>{post.authorUsername}</span>
                    {post.categoryName && (
                      <span>{post.categoryName}</span>
                    )}
                    <span>
                      <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                    </span>
                    <span>{post.viewCount} 次浏览</span>
                    <span>{post.commentCount} 条评论</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        {/* 热门帖子 */}
        <section aria-labelledby="hot-heading">
          <h2 id="hot-heading" className="section-title" style={{ borderBottomColor: 'var(--color-danger)', color: 'var(--color-danger)' }}>
            热门帖子
          </h2>
          <div className="card">
            {hotPosts.length === 0 ? (
              <div className="empty-state" style={{ padding: '1rem' }}>暂无热门帖子</div>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0 }} role="list">
                {hotPosts.map((post, index) => (
                  <li
                    key={post.id}
                    onClick={() => navigate(`/posts/${post.id}`)}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/posts/${post.id}`); } }}
                    role="link"
                    tabIndex={0}
                    aria-label={`第${index + 1}热门: ${post.title}, ${post.viewCount}次浏览`}
                    style={{
                      padding: '0.75rem 0',
                      borderBottom: index < hotPosts.length - 1 ? '1px solid var(--color-border-light)' : 'none',
                      cursor: 'pointer'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <span className={`badge ${index < 3 ? 'badge-danger' : 'badge-muted'}`} style={{ borderRadius: 'var(--radius-full)', width: '24px', height: '24px', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', marginRight: '0.75rem', flexShrink: 0 }}>
                        {index + 1}
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: '500', marginBottom: '0.25rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {post.title}
                        </div>
                        <div className="post-meta">
                          <span>{post.viewCount} 次浏览</span>
                          <span>{post.commentCount} 条评论</span>
                          <span>{post.authorUsername}</span>
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
