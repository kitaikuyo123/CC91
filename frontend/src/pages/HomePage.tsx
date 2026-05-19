import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getCategories } from '../api/category';
import { getPostList } from '../api/post';
import { queryKeys } from '../lib/queryKeys';
import BoardCard from '../components/BoardCard';
import TopicTable from '../components/TopicTable';
import AnnouncementPanel from '../components/AnnouncementPanel';

const mockAnnouncements = [
  { id: 1, title: 'CC91 论坛经典 CC98 视觉风格美化重构上线公告', date: '2026-05-19', isRed: true, isBold: true },
  { id: 2, title: '关于规范社区讨论、禁止灌水与文明发言的通知', date: '2026-05-18', isBold: true },
  { id: 3, title: '推荐使用主流现代浏览器（Chrome/Edge/Safari）以获得最佳体验', date: '2026-05-17' },
];

/**
 * CC98 风格经典社区首页
 */
export default function HomePage() {
  const navigate = useNavigate();

  // 获取版块列表
  const { data: categories = [], isLoading: isCategoriesLoading } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  // 获取最新帖子
  const { data: recentPostsData, isLoading: isPostsLoading } = useQuery({
    queryKey: queryKeys.posts.list({ page: 0, size: 10 }),
    queryFn: () => getPostList(0, 10),
  });

  // 获取更多帖子用于热门排序
  const { data: morePostsData } = useQuery({
    queryKey: queryKeys.posts.list({ page: 0, size: 40 }),
    queryFn: () => getPostList(0, 40),
  });

  // 计算热门帖子（按浏览量排序）
  const hotPosts = morePostsData?.content
    ? [...morePostsData.content].sort((a, b) => b.viewCount - a.viewCount).slice(0, 8)
    : [];

  const recentPosts = recentPostsData?.content || [];

  const isLoading = isCategoriesLoading || isPostsLoading;

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>正在载入 CC91 社区，请稍候...</p>
      </div>
    );
  }

  return (
    <div className="cc98-home-page container">
      {/* 1. 全站公告 */}
      <AnnouncementPanel announcements={mockAnnouncements} />

      {/* 2. 双栏布局 (左栏版块 & 帖子列表，右栏热门) */}
      <div className="cc98-home-grid">
        <div className="cc98-home-main-col">
          {/* 版块区域 */}
          <section style={{ marginBottom: '2.5rem' }}>
            <div className="cc98-section-title-external">
              <i className="fa fa-th-large" style={{ color: 'var(--cc98-alt-color-b)' }}></i> 热门讨论板块
            </div>
            <div className="cc98-board-grid">
              {categories.map((category) => (
                <BoardCard 
                  key={category.id} 
                  category={category} 
                  onClick={(id) => navigate(`/category/${id}`)}
                />
              ))}
            </div>
          </section>

          {/* 最新主题帖区域 */}
          <section>
            <div className="cc98-section-title-external">
              <i className="fa fa-clock-o" style={{ color: 'var(--cc98-alt-color-a)' }}></i> 最新发表主题帖
            </div>
            <TopicTable posts={recentPosts} />
          </section>
        </div>

        {/* 右边栏：今日热门推荐 */}
        <div className="cc98-home-sidebar-col">
          <div className="cc98-sidebar-wrapper">
            <div className="cc98-sidebar-title-external">
              <i className="fa fa-fire" style={{ color: '#fb6165' }}></i> 今日热门推荐
            </div>
            <div className="cc98-sidebar-box-classic">
              <div className="cc98-sidebar-hot-list">
                {hotPosts.length === 0 ? (
                  <div className="cc98-sidebar-empty">暂无热门主题</div>
                ) : (
                  hotPosts.map((post, index) => (
                    <div key={post.id} className="cc98-sidebar-hot-item">
                      <span className={`cc98-hot-rank ${index < 3 ? 'rank-top' : 'rank-normal'}`}>
                        {index + 1}
                      </span>
                      <div className="cc98-hot-detail">
                        <a 
                          href={`/posts/${post.id}`}
                          onClick={(e) => { e.preventDefault(); navigate(`/posts/${post.id}`); }}
                          className="cc98-hot-link"
                          title={post.title}
                        >
                          {post.title}
                        </a>
                        <div className="cc98-hot-meta">
                          <span>阅: {post.viewCount}</span>
                          <span>·</span>
                          <span>评: {post.commentCount}</span>
                          <span>·</span>
                          <span className="cc98-hot-author">{post.authorUsername}</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .cc98-home-page {
          margin-top: 1.5rem;
        }

        .cc98-home-grid {
          display: flex;
          gap: 1.75rem;
          align-items: flex-start;
        }

        .cc98-home-main-col {
          flex: 1;
          min-width: 0;
        }

        .cc98-home-sidebar-col {
          width: 280px;
          flex-shrink: 0;
        }

        @media (max-width: 992px) {
          .cc98-home-grid {
            flex-direction: column;
          }
          .cc98-home-sidebar-col {
            width: 100%;
          }
        }

        .cc98-section-title-external {
          font-size: 1.25rem;
          font-weight: bold;
          color: var(--text-main);
          display: flex;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 1rem;
          padding-left: 0.25rem;
        }

        .cc98-board-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: 1.25rem;
        }

        /* Sidebar Styling */
        .cc98-sidebar-wrapper {
          display: flex;
          flex-direction: column;
          width: 100%;
        }

        .cc98-sidebar-title-external {
          font-size: 1.25rem;
          font-weight: bold;
          color: #fb6165;
          display: flex;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 1rem;
          padding-left: 0.25rem;
        }

        .cc98-sidebar-box-classic {
          border: 1px solid var(--border-color);
          border-top: 8px solid #fb6165;
          border-radius: 4px;
          background-color: var(--card-bg);
          box-shadow: var(--cc98-shadow);
          overflow: hidden;
        }

        .cc98-sidebar-hot-list {
          padding: 0.75rem 0;
          display: flex;
          flex-direction: column;
        }

        .cc98-sidebar-hot-item {
          display: flex;
          padding: 0.65rem 1rem;
          border-bottom: 1px dashed var(--border-color);
          gap: 0.75rem;
          align-items: flex-start;
        }

        .cc98-sidebar-hot-item:last-child {
          border-bottom: none;
        }

        .cc98-sidebar-hot-item:hover {
          background-color: var(--quote-bg);
        }

        .cc98-hot-rank {
          font-size: 0.75rem;
          font-weight: bold;
          width: 1.25rem;
          height: 1.25rem;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          margin-top: 0.15rem;
        }

        .rank-top {
          background-color: #fb6165;
          color: white;
        }

        .rank-normal {
          background-color: var(--quote-bg);
          color: var(--text-muted);
          border: 1px solid var(--border-color);
        }

        .cc98-hot-detail {
          flex: 1;
          min-width: 0;
        }

        .cc98-hot-link {
          color: var(--text-main);
          font-weight: 500;
          text-decoration: none;
          font-size: 0.88rem;
          display: block;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-bottom: 0.25rem;
          transition: var(--cc98-transition);
        }

        .cc98-hot-link:hover {
          color: var(--link-color);
          text-decoration: underline;
        }

        .cc98-hot-meta {
          font-size: 0.72rem;
          color: var(--text-muted);
          display: flex;
          gap: 0.35rem;
          align-items: center;
          white-space: nowrap;
        }

        .cc98-hot-author {
          font-weight: bold;
          color: var(--text-main);
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 80px;
        }

        .cc98-sidebar-empty {
          padding: 3rem 1rem;
          text-align: center;
          color: var(--text-muted);
          font-size: 0.85rem;
        }
      `}</style>
    </div>
  );
}
