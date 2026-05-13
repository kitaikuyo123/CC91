/**
 * React Query 查询键
 * 用于统一管理所有查询的 key，便于缓存失效
 */

export const queryKeys = {
  // 帖子相关
  posts: {
    all: ['posts'] as const,
    lists: () => [...queryKeys.posts.all, 'list'] as const,
    list: (filters: { page?: number; size?: number; status?: string }) =>
      [...queryKeys.posts.lists(), filters] as const,
    details: () => [...queryKeys.posts.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.posts.details(), id] as const,
    search: (keyword: string, page: number, size: number) =>
      [...queryKeys.posts.all, 'search', keyword, page, size] as const,
    byCategory: (categoryId: number, page: number, size: number) =>
      [...queryKeys.posts.all, 'category', categoryId, page, size] as const,
  },

  // 版块相关
  categories: {
    all: ['categories'] as const,
    lists: () => [...queryKeys.categories.all, 'list'] as const,
    list: () => [...queryKeys.categories.lists()] as const,
    details: () => [...queryKeys.categories.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.categories.details(), id] as const,
  },

  // 用户相关
  users: {
    all: ['users'] as const,
    lists: () => [...queryKeys.users.all, 'list'] as const,
    list: () => [...queryKeys.users.lists()] as const,
    details: () => [...queryKeys.users.all, 'detail'] as const,
    detail: (username: string) => [...queryKeys.users.details(), username] as const,
    me: () => [...queryKeys.users.all, 'me'] as const,
    mePosts: () => [...queryKeys.users.me(), 'posts'] as const,
    meComments: () => [...queryKeys.users.me(), 'comments'] as const,
    meDrafts: () => [...queryKeys.users.me(), 'drafts'] as const,
  },

  // 通知相关
  notifications: {
    all: ['notifications'] as const,
    lists: () => [...queryKeys.notifications.all, 'list'] as const,
    list: (page: number, size: number) =>
      [...queryKeys.notifications.lists(), page, size] as const,
    unreadCount: () => [...queryKeys.notifications.all, 'unreadCount'] as const,
  },

  // 评论相关
  comments: {
    all: ['comments'] as const,
    lists: () => [...queryKeys.comments.all, 'list'] as const,
    byPost: (postId: number) => [...queryKeys.comments.all, 'post', postId] as const,
  },

  // 管理员相关
  admin: {
    posts: (status?: string) => ['admin', 'posts', status] as const,
    users: () => ['admin', 'users'] as const,
    stats: () => ['admin', 'stats'] as const,
  },
};
