import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategories } from '../api/category';
import { getPostList } from '../api/post';
import { queryKeys } from '../lib/queryKeys';

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

  const openPost = (postId: number) => {
    window.open(`/posts/${postId}`, '_blank');
  };

  return (
    <div className="container" style={{ marginTop: '2rem' }}>
      <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
        <h1 style={{ marginBottom: '0.5rem' }}>CC91 论坛</h1>
        <p style={{ color: '#666' }}>一个现代化的技术交流社区</p>

        {/* 搜索栏 */}
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem', maxWidth: '500px', margin: '1rem auto 0' }}>
          <input
            type="text"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            placeholder="搜索标题或内容..."
            style={{ flex: 1, padding: '0.75rem', border: '1px solid #ddd', borderRadius: '4px', fontSize: '1rem' }}
          />
          <button type="submit" className="btn btn-primary" style={{ padding: '0.75rem 1.5rem' }}>
            搜索
          </button>
        </form>
      </div>

      {/* 版块列表 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ marginBottom: '1rem', borderBottom: '2px solid #3498db', paddingBottom: '0.5rem' }}>
          讨论版块
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '1rem' }}>
          {categories.map((category) => (
            <div
              key={category.id}
              className="card"
              onClick={() => navigate(`/category/${category.id}`)}
              style={{ cursor: 'pointer', transition: 'transform 0.2s' }}
              onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-5px)'}
              onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}
            >
              <h3 style={{ marginBottom: '0.5rem', color: '#3498db' }}>{category.name}</h3>
              {category.description && (
                <p style={{ color: '#666', fontSize: '0.9rem' }}>{category.description}</p>
              )}
            </div>
          ))}
        </div>
      </section>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        {/* 最新帖子 */}
        <section>
          <h2 style={{ marginBottom: '1rem', borderBottom: '2px solid #3498db', paddingBottom: '0.5rem' }}>
            最新帖子
          </h2>
          <div className="post-list">
            {recentPosts.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
                暂无帖子
              </div>
            ) : (
              recentPosts.map((post) => (
                <div
                  key={post.id}
                  className="card post-item"
                  onClick={() => openPost(post.id)}
                  style={{ cursor: 'pointer', marginBottom: '1rem' }}
                >
                  <h3 style={{ marginBottom: '0.5rem', fontSize: '1.1rem' }}>{post.title}</h3>
                  <p style={{ color: '#666', fontSize: '0.9rem' }}>
                    {post.content ? post.content.substring(0, 100) : ''}...
                  </p>
                  <div style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#999' }}>
                    <span>👤 {post.authorUsername}</span>
                    {post.categoryName && (
                      <span style={{ marginLeft: '1rem' }}>📁 {post.categoryName}</span>
                    )}
                    <span style={{ marginLeft: '1rem' }}>
                      🕒 {new Date(post.createdAt).toLocaleString()}
                    </span>
                    <span style={{ marginLeft: '1rem' }}>
                      👁️ {post.viewCount}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        {/* 热门帖子 */}
        <section>
          <h2 style={{ marginBottom: '1rem', borderBottom: '2px solid #e74c3c', paddingBottom: '0.5rem', color: '#e74c3c' }}>
            🔥 热门帖子
          </h2>
          <div className="card">
            {hotPosts.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: '#999' }}>
                暂无热门帖子
              </div>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0 }}>
                {hotPosts.map((post, index) => (
                  <li
                    key={post.id}
                    onClick={() => openPost(post.id)}
                    style={{
                      padding: '0.75rem 0',
                      borderBottom: index < hotPosts.length - 1 ? '1px solid #eee' : 'none',
                      cursor: 'pointer'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                      <span style={{
                        background: index < 3 ? '#e74c3c' : '#95a5a6',
                        color: '#fff',
                        width: '24px',
                        height: '24px',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.8rem',
                        marginRight: '0.75rem',
                        flexShrink: 0
                      }}>
                        {index + 1}
                      </span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: '500', marginBottom: '0.25rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {post.title}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: '#999' }}>
                          👁️ {post.viewCount} · 👤 {post.authorUsername}
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
