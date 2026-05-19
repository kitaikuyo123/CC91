import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getPostById, deletePost } from '../api/post';
import CommentSection from '../components/CommentSection';
import { queryKeys } from '../lib/queryKeys';
import Breadcrumbs from '../components/Breadcrumbs';
import PostCard from '../components/PostCard';

/**
 * CC98 风格帖子详情/阅读楼层页面
 */
export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [isFavorited, setIsFavorited] = useState(false);

  const postId = id ? parseInt(id, 10) : 0;

  // 获取帖子详情
  const { data: post, isLoading, error } = useQuery({
    queryKey: queryKeys.posts.detail(postId),
    queryFn: () => getPostById(postId),
    enabled: postId > 0,
  });

  // 删除帖子的 mutation
  const deleteMutation = useMutation({
    mutationFn: deletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      if (post?.categoryId) {
        navigate(`/category/${post.categoryId}`);
      } else {
        navigate('/');
      }
    },
    onError: (err: any) => {
      setIsDeleting(false);
      setDeleteError(err.response?.data?.message || '删除主题帖失败');
    },
  });

  const handleDelete = async () => {
    if (!post) return;
    if (!confirm('确定要删除这篇主题帖吗？此操作不可恢复。')) return;

    setIsDeleting(true);
    deleteMutation.mutate(post.id);
  };

  const handleEdit = () => {
    navigate(`/posts/${postId}/edit`);
  };

  const handleFavoriteToggle = () => {
    setIsFavorited(!isFavorited);
    alert(isFavorited ? '已取消收藏此贴' : '已收藏此贴到您的个人中心');
  };

  const isAuthor = currentUser?.username === post?.authorUsername;

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '5rem 0' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '1.25rem', color: 'var(--text-muted)' }}>正在载入主题帖与楼层回复...</p>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="container" style={{ marginTop: '2rem' }}>
        <div className="cc98-detail-error">
          <i className="fa fa-exclamation-triangle" style={{ fontSize: '2.5rem', color: '#fb6165', marginBottom: '1rem' }}></i>
          <h2>主题不存在</h2>
          <p style={{ color: 'var(--text-muted)', margin: '1rem 0' }}>
            {(error as any)?.response?.data?.message || '该主题帖不存在或已被管理员删除。'}
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首页
          </button>
        </div>
        <style>{`
          .cc98-detail-error {
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

  return (
    <div className="cc98-topic-page container" style={{ marginTop: '1.5rem' }}>
      {/* 1. 面包屑导航 */}
      <Breadcrumbs 
        items={[
          { label: '版面列表', href: '/' },
          { label: post.categoryName || '讨论板块', href: post.categoryId ? `/category/${post.categoryId}` : undefined },
          { label: post.title }
        ]} 
      />

      {/* 2. 主题标题卡片 */}
      <div className="cc98-topic-header-title-box">
        <div className="title-area">
          <h1>{post.title}</h1>
        </div>

        <div className="action-area">
          <button 
            className={`cc98-fav-toggle-btn ${isFavorited ? 'favorited' : ''}`}
            onClick={handleFavoriteToggle}
            title={isFavorited ? '从收藏夹中移除' : '收藏此贴'}
          >
            <i className={`fa ${isFavorited ? 'fa-star' : 'fa-star-o'}`}></i>
            {isFavorited ? ' 已收藏' : ' 收藏'}
          </button>
        </div>
      </div>

      {deleteError && (
        <div className="cc98-error-box" style={{ marginBottom: '1rem' }}>
          {deleteError}
        </div>
      )}

      {/* 3. 第一楼：楼主发布的内容 (PostCard) */}
      <div className="cc98-floors-container">
        <PostCard
          id={post.id}
          authorUsername={post.authorUsername}
          floor="楼主"
          content={post.content}
          createdAt={post.createdAt}
          isTopicAuthor={true}
          currentUserCanModify={isAuthor}
          onEdit={handleEdit}
          onDelete={handleDelete}
          isDeleting={isDeleting}
        />
      </div>

      {/* 4. 评论楼层区域 */}
      <CommentSection 
        postId={postId} 
        commentCount={post.commentCount} 
        topicAuthorUsername={post.authorUsername}
      />

      <style>{`
        .cc98-topic-header-title-box {
          background-color: var(--card-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--cc98-radius);
          padding: 1.25rem 1.75rem;
          display: flex;
          align-items: center;
          justify-content: space-between;
          box-shadow: var(--cc98-shadow);
          margin-bottom: 1.5rem;
          gap: 1.5rem;
        }

        @media (max-width: 768px) {
          .cc98-topic-header-title-box {
            flex-direction: column;
            text-align: center;
            align-items: center;
          }
          .cc98-fav-toggle-btn {
            width: 100%;
          }
        }

        .cc98-topic-header-title-box .title-area {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.5rem;
          flex: 1;
        }

        .cc98-topic-header-title-box h1 {
          font-size: 1.35rem;
          font-weight: bold;
          color: var(--text-main);
          margin: 0;
        }

        .cc98-fav-toggle-btn {
          background: transparent;
          color: var(--text-muted);
          border: 1px solid var(--border-color);
          padding: 0.45rem 1rem;
          border-radius: var(--cc98-radius-pill);
          font-weight: bold;
          font-size: 0.85rem;
          cursor: pointer;
          transition: var(--cc98-transition);
          display: inline-flex;
          align-items: center;
          gap: 0.35rem;
          white-space: nowrap;
        }

        .cc98-fav-toggle-btn:hover {
          color: var(--accent-color);
          border-color: var(--accent-color);
          background-color: var(--quote-bg);
        }

        .cc98-fav-toggle-btn.favorited {
          background-color: var(--accent-color);
          color: #333 !important;
          border-color: var(--accent-color);
        }

        .cc98-error-box {
          background-color: rgba(251, 97, 101, 0.1);
          color: #fb6165;
          padding: 0.75rem 1.25rem;
          border-radius: var(--cc98-radius);
          border: 1px solid rgba(251, 97, 101, 0.2);
          font-size: 0.9rem;
        }
      `}</style>
    </div>
  );
}
