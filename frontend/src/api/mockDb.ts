import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import type { Category } from './category';
import type { Post, PageResponse } from './post';
import type { Comment } from './comment';
import type { UserProfile, MyComment } from './user';
import type { AdminUser } from './admin';
import type { Notification } from './notification';

const STORAGE_KEY = 'cc91_mock_db';

interface MockDbState {
  categories: Category[];
  posts: Post[];
  comments: Comment[]; // stored flat
  notifications: Notification[];
  users: AdminUser[];
  profiles: { [username: string]: UserProfile };
  lastIds: {
    category: number;
    post: number;
    comment: number;
    notification: number;
    user: number;
  };
}

// Helper to format Date matching backend DTOs (ISO-like string)
const getNowString = () => new Date().toISOString();

const INITIAL_STATE: MockDbState = {
  categories: [
    { id: 1, name: 'CC98广场', description: '这里是CC98论坛的主板块，分享校园见闻、趣事和日常讨论。', sortOrder: 1, createdAt: getNowString() },
    { id: 2, name: '学术大厅', description: '课程推荐、选课讨论、保研考研出国心得，学术交流与分享。', sortOrder: 2, createdAt: getNowString() },
    { id: 3, name: '心灵之约', description: '树洞、情感倾诉、人生感悟，温暖治愈的角落。', sortOrder: 3, createdAt: getNowString() },
    { id: 4, name: '技术交流', description: '程序人生，极客沙龙，React/Next.js/Spring等技术问题探讨。', sortOrder: 4, createdAt: getNowString() },
  ],
  posts: [
    { id: 1, title: '关于CC91 BBS论坛系统正式上线运行的公告', content: '大家好！CC91 BBS论坛系统已于今日正式上线！\n\n新系统采用现代化的 React 19 前端架构与 Spring Boot 后端，支持经典的 CC98 视觉皮肤和丰富的交互功能。欢迎大家体验并反馈 Bug！\n\n我们将致力于打造一个绿色、温馨、高效的校园论坛环境。', authorId: 1, authorUsername: 'admin', categoryId: 1, categoryName: 'CC98广场', status: 'APPROVED', createdAt: '2026-05-18T08:00:00.000Z', updatedAt: '2026-05-18T08:00:00.000Z', viewCount: 1520, commentCount: 3 },
    { id: 2, title: '大家平时都用什么 IDE 或者编辑器写 React 代码呀？', content: '新手入坑前端，目前在尝试用 React 写个个人网站。\n看网上推荐 VS Code 的人特别多，也有人说 WebStorm 比较强大，想问问老哥们都是怎么选的？有什么好用的插件推荐吗？', authorId: 2, authorUsername: 'user', categoryId: 4, categoryName: '技术交流', status: 'APPROVED', createdAt: '2026-05-19T02:30:00.000Z', updatedAt: '2026-05-19T02:30:00.000Z', viewCount: 231, commentCount: 2 },
    { id: 3, title: '求助：浙大紫金港校区附近有什么好吃的宵夜推荐吗？', content: '马上要毕业了，想趁最后几天把紫金港周围好吃的馆子都扫一遍。\n求推荐港湾家园或者堕落街那边好吃的烧烤、烤鱼或者砂锅粥，多谢各位！', authorId: 3, authorUsername: 'editor', categoryId: 1, categoryName: 'CC98广场', status: 'APPROVED', createdAt: '2026-05-19T04:15:00.000Z', updatedAt: '2026-05-19T04:15:00.000Z', viewCount: 95, commentCount: 1 },
    { id: 4, title: '保研/考研心得交流帖 - 欢迎学弟学妹在楼下提问', content: '本人是去年保研的，对计算机学院的保研政策、导师联系以及面试准备有一些心得。\n有这方面疑惑的同学可以在楼下提问，我会抽空一一回复。祝大家前程似锦！', authorId: 1, authorUsername: 'admin', categoryId: 2, categoryName: '学术大厅', status: 'APPROVED', createdAt: '2026-05-19T05:00:00.000Z', updatedAt: '2026-05-19T05:00:00.000Z', viewCount: 512, commentCount: 0 },
    { id: 5, title: '【草稿】待完善的学术研讨会议日程安排', content: '这是一个学术会议草稿。拟定于下个月举办，需要邀请3位专家学者。日程细节待讨论。', authorId: 2, authorUsername: 'user', categoryId: 2, categoryName: '学术大厅', status: 'DRAFT', createdAt: '2026-05-19T06:12:00.000Z', updatedAt: '2026-05-19T06:12:00.000Z', viewCount: 0, commentCount: 0 },
    { id: 6, title: '深夜树洞：聊聊那些让你瞬间释怀的瞬间', content: '有些事一直压在心里，今天走在路上，突然闻到晚风里的青草香味，觉得其实一切也没什么大不了的。\n你们呢？是在哪一瞬间突然觉得一切都释怀了的？', authorId: 2, authorUsername: 'user', categoryId: 3, categoryName: '心灵之约', status: 'APPROVED', createdAt: '2026-05-19T06:20:00.000Z', updatedAt: '2026-05-19T06:20:00.000Z', viewCount: 180, commentCount: 1 },
    { id: 7, title: '前端性能优化：从输入 URL 到页面渲染全流程分析', content: '作为前端开发，性能优化是必修课。\n本文主要分析：\n1. DNS 解析优化 (DNS Prefetch)\n2. TCP 连接优化 (Keep-Alive, HTTP/2)\n3. 资源加载优化 (Gzip, WebP, Tree Shaking)\n4. 浏览器渲染优化 (减少重排重绘)\n希望对大家有所帮助！', authorId: 3, authorUsername: 'editor', categoryId: 4, categoryName: '技术交流', status: 'APPROVED', createdAt: '2026-05-19T06:40:00.000Z', updatedAt: '2026-05-19T06:40:00.000Z', viewCount: 320, commentCount: 2 },
    { id: 8, title: '【待审核】关于对某灌水用户的举报和投诉', content: '今天在学术大厅看到有人恶意刷屏发广告，希望能做封禁处理，维护社区秩序。', authorId: 2, authorUsername: 'user', categoryId: 1, categoryName: 'CC98广场', status: 'PENDING', createdAt: '2026-05-19T07:10:00.000Z', updatedAt: '2026-05-19T07:10:00.000Z', viewCount: 12, commentCount: 0 }
  ],
  comments: [
    // Post 1 comments
    { id: 1, postId: 1, authorId: 2, authorUsername: 'user', content: '支持支持！新版界面很好看，发帖速度变快了！', parentId: null, createdAt: '2026-05-18T08:15:00.000Z', status: 'APPROVED', replies: [] },
    { id: 2, postId: 1, authorId: 3, authorUsername: 'editor', content: '经典风格迁移得很到位，满满的青春回忆啊，表扬前端团队！', parentId: null, createdAt: '2026-05-18T08:30:00.000Z', status: 'APPROVED', replies: [] },
    { id: 3, postId: 1, authorId: 1, authorUsername: 'admin', content: '收到反馈，感谢支持！我们会继续进行视觉微调和性能调优。', parentId: 2, createdAt: '2026-05-18T08:45:00.000Z', status: 'APPROVED', replies: [] },
    
    // Post 2 comments
    { id: 4, postId: 2, authorId: 3, authorUsername: 'editor', content: '果断 VS Code，插件全、速度快。推荐配置 Prettier + ESLint，写 React 效率极高！', parentId: null, createdAt: '2026-05-19T03:00:00.000Z', status: 'APPROVED', replies: [] },
    { id: 5, postId: 2, authorId: 1, authorUsername: 'admin', content: '如果是中大型项目，WebStorm 的重构、转跳以及自带的 Git 工具非常好用，就是稍微吃点内存。', parentId: null, createdAt: '2026-05-19T03:15:00.000Z', status: 'APPROVED', replies: [] },
    
    // Post 3 comments
    { id: 6, postId: 3, authorId: 1, authorUsername: 'admin', content: '港湾家园有一家东北小烧烤很不错，另外大排档的砂锅粥也算一绝，就在大门出来右转。', parentId: null, createdAt: '2026-05-19T04:30:00.000Z', status: 'APPROVED', replies: [] },
    
    // Post 6 comments
    { id: 7, postId: 6, authorId: 3, authorUsername: 'editor', content: '大概就是大哭了一场，第二天早上推开窗户，发现太阳照样升起，公交车上大家依然急匆匆的时候吧。', parentId: null, createdAt: '2026-05-19T06:50:00.000Z', status: 'APPROVED', replies: [] },
    
    // Post 7 comments
    { id: 8, postId: 7, authorId: 2, authorUsername: 'user', content: '讲解得通俗易懂！尤其是重排重绘部分，原来使用 transform 可以开启 GPU 加速，学到了。', parentId: null, createdAt: '2026-05-19T07:00:00.000Z', status: 'APPROVED', replies: [] },
    { id: 9, postId: 7, authorId: 1, authorUsername: 'admin', content: '总结得很好。也可以多关注一下 HTTP/3 (QUIC) 带来的连接建立速度提升。', parentId: null, createdAt: '2026-05-19T07:15:00.000Z', status: 'APPROVED', replies: [] }
  ],
  notifications: [
    { id: 1, userId: 2, type: 'COMMENT', title: '您的帖子有新回复', content: 'admin 在帖子《关于CC91 BBS论坛系统正式上线运行的公告》中回复了您。', relatedId: 1, isRead: false, createdAt: '2026-05-18T08:45:00.000Z' },
    { id: 2, userId: 2, type: 'SYSTEM', title: '注册成功通知', content: '欢迎加入 CC91 BBS，您的账户已成功创建！', isRead: true, createdAt: '2026-05-18T07:00:00.000Z' }
  ],
  users: [
    { id: 1, username: 'admin', email: 'admin@cc98.org', role: 'ADMIN', isLocked: false, createdAt: '2026-05-18T06:00:00.000Z' },
    { id: 2, username: 'user', email: 'user@cc98.org', role: 'USER', isLocked: false, createdAt: '2026-05-18T07:00:00.000Z' },
    { id: 3, username: 'editor', email: 'editor@cc98.org', role: 'USER', isLocked: false, createdAt: '2026-05-18T07:30:00.000Z' }
  ],
  profiles: {
    admin: { username: 'admin', email: 'admin@cc98.org', avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&h=150', bio: '论坛超级管理员，有事请留言。', location: '浙大紫金港', website: 'https://cc98.org', createdAt: '2026-05-18T06:00:00.000Z' },
    user: { username: 'user', email: 'user@cc98.org', avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&h=150', bio: '一只普通的前端菜鸟，热爱生活。', location: '浙大玉泉校区', website: null, createdAt: '2026-05-18T07:00:00.000Z' },
    editor: { username: 'editor', email: 'editor@cc98.org', avatarUrl: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150&h=150', bio: '内容编辑，分享好文章。', location: '浙大西溪校区', website: null, createdAt: '2026-05-18T07:30:00.000Z' }
  },
  lastIds: {
    category: 4,
    post: 8,
    comment: 9,
    notification: 2,
    user: 3
  }
};

/**
 * Get or initialize database state from localStorage
 */
function getDbState(): MockDbState {
  const data = localStorage.getItem(STORAGE_KEY);
  if (!data) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_STATE));
    return INITIAL_STATE;
  }
  try {
    return JSON.parse(data);
  } catch (e) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_STATE));
    return INITIAL_STATE;
  }
}

/**
 * Save database state to localStorage
 */
function saveDbState(state: MockDbState) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

/**
 * Helper to build nested comment tree from flat list
 */
function buildCommentTree(flatComments: Comment[], postId: number): Comment[] {
  const postComments = flatComments.filter(c => c.postId === postId && c.status === 'APPROVED');
  const commentMap = new Map<number, Comment>();

  postComments.forEach(c => {
    commentMap.set(c.id, { ...c, replies: [] });
  });

  const roots: Comment[] = [];

  commentMap.forEach(c => {
    if (c.parentId === null) {
      roots.push(c);
    } else {
      const parent = commentMap.get(c.parentId);
      if (parent) {
        parent.replies.push(c);
      } else {
        roots.push(c); // Fallback
      }
    }
  });

  return roots;
}

/**
 * Identify user from request Authorization header
 */
function getCurrentUser(config: AxiosRequestConfig): { username: string; role: string; id: number } | null {
  const authHeader = config.headers?.Authorization || config.headers?.authorization;
  let token = '';

  if (typeof authHeader === 'string' && authHeader.startsWith('Bearer ')) {
    token = authHeader.replace('Bearer ', '');
  } else {
    // Fallback: check localStorage directly
    token = localStorage.getItem('access_token') || '';
  }

  if (token && token.startsWith('mock-token-')) {
    const username = token.replace('mock-token-', '');
    const state = getDbState();
    const dbUser = state.users.find(u => u.username === username);
    if (dbUser) {
      return {
        username: dbUser.username,
        role: dbUser.role,
        id: dbUser.id
      };
    }
  }
  return null;
}

/**
 * Mock Request Interceptor Adapter
 */
export async function mockRequestAdapter(config: AxiosRequestConfig): Promise<AxiosResponse<any>> {
  const state = getDbState();
  const url = config.url || '';
  const method = (config.method || 'GET').toUpperCase();
  const params = config.params || {};
  const data = config.data ? (typeof config.data === 'string' ? JSON.parse(config.data) : config.data) : {};
  const currentUser = getCurrentUser(config);

  let responseData: any = null;
  let status = 200;

  // Helper to paginate an array
  const paginateArray = <T>(arr: T[], page: number, size: number): PageResponse<T> => {
    const start = page * size;
    const end = start + size;
    const content = arr.slice(start, end);
    const totalElements = arr.length;
    const totalPages = Math.ceil(totalElements / size);

    return {
      content,
      pageable: { pageNumber: page, pageSize: size },
      totalElements,
      totalPages,
      last: page >= totalPages - 1,
      first: page === 0,
      empty: content.length === 0
    };
  };

  try {
    // ============ Auth API ============
    if (url.match(/^\/auth\/login/)) {
      const { username, password } = data;
      const userObj = state.users.find(u => u.username === username);
      // Mock validation: password is username + '123'
      if (userObj && password === `${username}123`) {
        if (userObj.isLocked) {
          status = 403;
          throw new Error('该账户已被锁定，请联系管理员！');
        }
        responseData = {
          accessToken: `mock-token-${username}`,
          refreshToken: `mock-refresh-${username}`,
          tokenType: 'Bearer',
          expiresIn: 3600
        };
      } else {
        status = 401;
        throw new Error('用户名或密码错误！');
      }
    } 
    else if (url.match(/^\/auth\/register/)) {
      const { username, email } = data;
      if (state.users.some(u => u.username === username)) {
        status = 400;
        throw new Error('用户名已存在！');
      }
      state.lastIds.user++;
      const newUserId = state.lastIds.user;
      
      const newAdminUser: AdminUser = {
        id: newUserId,
        username,
        email,
        role: 'USER',
        isLocked: false,
        createdAt: getNowString()
      };

      const newProfile: UserProfile = {
        username,
        email,
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: getNowString()
      };

      state.users.push(newAdminUser);
      state.profiles[username] = newProfile;
      saveDbState(state);

      responseData = { username, email };
    }
    else if (url.match(/^\/auth\/forgot-password/)) {
      responseData = { message: '验证码已发送至您的邮箱！' };
    }
    else if (url.match(/^\/auth\/reset-password/)) {
      responseData = { message: '密码重置成功，请重新登录！' };
    }
    else if (url.match(/^\/auth\/refresh/)) {
      const { refreshToken } = data;
      if (refreshToken && refreshToken.startsWith('mock-refresh-')) {
        const username = refreshToken.replace('mock-refresh-', '');
        responseData = {
          accessToken: `mock-token-${username}`,
          refreshToken: `mock-refresh-${username}`,
          tokenType: 'Bearer',
          expiresIn: 3600
        };
      } else {
        status = 401;
        throw new Error('Refresh token invalid');
      }
    }
    else if (url.match(/^\/auth\/logout/)) {
      responseData = null;
    }

    // ============ Category API ============
    else if (url === '/categories' && method === 'GET') {
      responseData = state.categories;
    }
    else if (url.match(/^\/categories\/\d+$/) && method === 'GET') {
      const catId = parseInt(url.split('/').pop() || '0');
      const category = state.categories.find(c => c.id === catId);
      if (!category) {
        status = 404;
        throw new Error('该分类版块不存在！');
      }
      responseData = category;
    }
    // Admin category management routes
    else if ((url === '/categories' || url === '/admin/categories') && method === 'POST') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      state.lastIds.category++;
      const newCat: Category = {
        id: state.lastIds.category,
        name: data.name,
        description: data.description || '',
        sortOrder: data.sortOrder || 1,
        createdAt: getNowString()
      };
      state.categories.push(newCat);
      saveDbState(state);
      responseData = { message: '创建成功', data: newCat };
    }
    else if ((url.match(/^\/categories\/\d+$/) || url.match(/^\/admin\/categories\/\d+$/)) && method === 'PUT') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const catId = parseInt(url.split('/').pop() || '0');
      const idx = state.categories.findIndex(c => c.id === catId);
      if (idx === -1) {
        status = 404;
        throw new Error('分类不存在！');
      }
      state.categories[idx] = {
        ...state.categories[idx],
        name: data.name ?? state.categories[idx].name,
        description: data.description ?? state.categories[idx].description,
        sortOrder: data.sortOrder ?? state.categories[idx].sortOrder,
      };
      saveDbState(state);
      responseData = { message: '更新成功', data: state.categories[idx] };
    }
    else if ((url.match(/^\/categories\/\d+$/) || url.match(/^\/admin\/categories\/\d+$/)) && method === 'DELETE') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const catId = parseInt(url.split('/').pop() || '0');
      state.categories = state.categories.filter(c => c.id !== catId);
      // Optionally cascade delete posts
      state.posts = state.posts.filter(p => p.categoryId !== catId);
      saveDbState(state);
      responseData = null;
    }

    // ============ Post API ============
    else if (url === '/posts' && method === 'GET') {
      const page = parseInt(params.page || '0');
      const size = parseInt(params.size || '10');
      const statusParam = params.status;

      let filtered = state.posts;
      if (statusParam) {
        filtered = filtered.filter(p => p.status === statusParam);
      } else {
        // Normal list shows APPROVED only
        filtered = filtered.filter(p => p.status === 'APPROVED');
      }
      
      // Sort: pinned first (our mock does not have pinned but we sort by id desc)
      filtered = [...filtered].sort((a, b) => b.id - a.id);
      responseData = paginateArray(filtered, page, size);
    }
    else if (url.match(/^\/posts\/by-category\/\d+$/) && method === 'GET') {
      const catId = parseInt(url.split('/').pop() || '0');
      const page = parseInt(params.page || '0');
      const size = parseInt(params.size || '10');
      const filtered = state.posts
        .filter(p => p.categoryId === catId && p.status === 'APPROVED')
        .sort((a, b) => b.id - a.id);
      
      responseData = paginateArray(filtered, page, size);
    }
    else if (url === '/posts/search' && method === 'GET') {
      const keyword = (params.keyword || '').toLowerCase();
      const page = parseInt(params.page || '0');
      const size = parseInt(params.size || '10');
      const filtered = state.posts
        .filter(p => p.status === 'APPROVED' && (p.title.toLowerCase().includes(keyword) || p.content.toLowerCase().includes(keyword)))
        .sort((a, b) => b.id - a.id);
      
      responseData = paginateArray(filtered, page, size);
    }
    else if (url.match(/^\/posts\/\d+$/) && method === 'GET') {
      const postId = parseInt(url.split('/').pop() || '0');
      const postIdx = state.posts.findIndex(p => p.id === postId);
      if (postIdx === -1) {
        status = 404;
        throw new Error('主题帖不存在或已被删除！');
      }
      // Increment views
      state.posts[postIdx].viewCount++;
      saveDbState(state);
      responseData = state.posts[postIdx];
    }
    else if (url === '/posts' && method === 'POST') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      state.lastIds.post++;
      const newPostId = state.lastIds.post;
      const category = state.categories.find(c => c.id === data.categoryId);

      const newPost: Post = {
        id: newPostId,
        title: data.title,
        content: data.content,
        authorId: currentUser.id,
        authorUsername: currentUser.username,
        categoryId: data.categoryId,
        categoryName: category ? category.name : '未知版块',
        status: data.status || 'APPROVED', // defaults to APPROVED, but support DRAFT
        createdAt: getNowString(),
        updatedAt: getNowString(),
        viewCount: 0,
        commentCount: 0
      };

      state.posts.push(newPost);
      saveDbState(state);
      responseData = { message: '发布成功', data: newPost };
    }
    else if (url.match(/^\/posts\/\d+$/) && method === 'PUT') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      const postId = parseInt(url.split('/').pop() || '0');
      const idx = state.posts.findIndex(p => p.id === postId);
      if (idx === -1) {
        status = 404;
        throw new Error('帖子不存在！');
      }
      
      // Permission check: admin or author
      if (state.posts[idx].authorUsername !== currentUser.username && currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('您无权编辑此贴！');
      }

      const category = state.categories.find(c => c.id === data.categoryId);

      state.posts[idx] = {
        ...state.posts[idx],
        title: data.title ?? state.posts[idx].title,
        content: data.content ?? state.posts[idx].content,
        categoryId: data.categoryId ?? state.posts[idx].categoryId,
        categoryName: category ? category.name : state.posts[idx].categoryName,
        status: data.status ?? state.posts[idx].status,
        updatedAt: getNowString()
      };
      
      saveDbState(state);
      responseData = { message: '修改成功', data: state.posts[idx] };
    }
    else if (url.match(/^\/posts\/\d+$/) && method === 'DELETE') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      const postId = parseInt(url.split('/').pop() || '0');
      const post = state.posts.find(p => p.id === postId);
      if (!post) {
        status = 404;
        throw new Error('帖子不存在！');
      }
      if (post.authorUsername !== currentUser.username && currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('无权删除！');
      }
      state.posts = state.posts.filter(p => p.id !== postId);
      state.comments = state.comments.filter(c => c.postId !== postId);
      saveDbState(state);
      responseData = null;
    }

    // ============ Comment API ============
    else if (url.match(/^\/posts\/\d+\/comments$/) && method === 'GET') {
      const postId = parseInt(url.split('/')[2]);
      responseData = buildCommentTree(state.comments, postId);
    }
    else if (url.match(/^\/posts\/\d+\/comments$/) && method === 'POST') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      const postId = parseInt(url.split('/')[2]);
      const postIdx = state.posts.findIndex(p => p.id === postId);
      if (postIdx === -1) {
        status = 404;
        throw new Error('帖子不存在！');
      }

      state.lastIds.comment++;
      const newCommentId = state.lastIds.comment;

      const newComment: Comment = {
        id: newCommentId,
        postId,
        authorId: currentUser.id,
        authorUsername: currentUser.username,
        content: data.content,
        parentId: null,
        createdAt: getNowString(),
        status: 'APPROVED',
        replies: []
      };

      state.comments.push(newComment);
      // Increment comment count
      state.posts[postIdx].commentCount++;
      
      // If replying, maybe create notification for post author
      const postAuthor = state.posts[postIdx].authorId;
      if (postAuthor !== currentUser.id) {
        state.lastIds.notification++;
        state.notifications.push({
          id: state.lastIds.notification,
          userId: postAuthor,
          type: 'COMMENT',
          title: '您的帖子有新回复',
          content: `${currentUser.username} 在帖子《${state.posts[postIdx].title}》中回复了您。`,
          relatedId: postId,
          isRead: false,
          createdAt: getNowString()
        });
      }

      saveDbState(state);
      responseData = { message: '回复发表成功', data: newComment };
    }
    else if (url.match(/^\/comments\/\d+\/reply$/) && method === 'POST') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      const parentCommentId = parseInt(url.split('/')[2]);
      const parentComment = state.comments.find(c => c.id === parentCommentId);
      if (!parentComment) {
        status = 404;
        throw new Error('回复的主评论已被删除！');
      }
      const postIdx = state.posts.findIndex(p => p.id === parentComment.postId);
      if (postIdx === -1) {
        status = 404;
        throw new Error('帖子不存在！');
      }

      state.lastIds.comment++;
      const newCommentId = state.lastIds.comment;

      const newComment: Comment = {
        id: newCommentId,
        postId: parentComment.postId,
        authorId: currentUser.id,
        authorUsername: currentUser.username,
        content: data.content,
        parentId: parentCommentId,
        createdAt: getNowString(),
        status: 'APPROVED',
        replies: []
      };

      state.comments.push(newComment);
      state.posts[postIdx].commentCount++;

      // Create notification for comment author
      if (parentComment.authorId !== currentUser.id) {
        state.lastIds.notification++;
        state.notifications.push({
          id: state.lastIds.notification,
          userId: parentComment.authorId,
          type: 'COMMENT',
          title: '您的评论被回复了',
          content: `${currentUser.username} 回复了您的评论。`,
          relatedId: parentComment.postId,
          isRead: false,
          createdAt: getNowString()
        });
      }

      saveDbState(state);
      responseData = { message: '回复成功', data: newComment };
    }
    else if (url.match(/^\/comments\/\d+$/) && method === 'DELETE') {
      if (!currentUser) {
        status = 401;
        throw new Error('请先登录！');
      }
      const commentId = parseInt(url.split('/').pop() || '0');
      const commentIdx = state.comments.findIndex(c => c.id === commentId);
      if (commentIdx === -1) {
        status = 404;
        throw new Error('评论不存在！');
      }

      const comment = state.comments[commentIdx];
      if (comment.authorUsername !== currentUser.username && currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('无权删除！');
      }

      // Decrement comment count on post
      const postIdx = state.posts.findIndex(p => p.id === comment.postId);
      if (postIdx !== -1) {
        state.posts[postIdx].commentCount = Math.max(0, state.posts[postIdx].commentCount - 1);
      }

      // Mark as deleted/moderated rather than hard delete to prevent UI broke, or hard delete
      state.comments = state.comments.filter(c => c.id !== commentId);
      saveDbState(state);
      responseData = null;
    }

    // ============ User Profile API ============
    else if (url === '/users/me' && method === 'GET') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      const profile = state.profiles[currentUser.username];
      responseData = profile || {
        username: currentUser.username,
        email: `${currentUser.username}@cc98.org`,
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: getNowString()
      };
    }
    else if (url.match(/^\/users\/[a-zA-Z0-9_-]+$/) && method === 'GET') {
      const username = url.split('/').pop() || '';
      const profile = state.profiles[username];
      if (!profile) {
        status = 404;
        throw new Error('该用户不存在！');
      }
      responseData = profile;
    }
    else if (url === '/users/me/profile' && method === 'PUT') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      const profile = state.profiles[currentUser.username] || {
        username: currentUser.username,
        email: `${currentUser.username}@cc98.org`,
        avatarUrl: null,
        bio: null,
        location: null,
        website: null,
        createdAt: getNowString()
      };

      state.profiles[currentUser.username] = {
        ...profile,
        avatarUrl: data.avatarUrl !== undefined ? data.avatarUrl : profile.avatarUrl,
        bio: data.bio !== undefined ? data.bio : profile.bio,
        location: data.location !== undefined ? data.location : profile.location,
        website: data.website !== undefined ? data.website : profile.website
      };

      saveDbState(state);
      responseData = state.profiles[currentUser.username];
    }
    else if (url === '/users/me/posts' && method === 'GET') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      responseData = state.posts.filter(p => p.authorUsername === currentUser.username && p.status === 'APPROVED');
    }
    else if (url === '/users/me/drafts' && method === 'GET') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      responseData = state.posts.filter(p => p.authorUsername === currentUser.username && p.status === 'DRAFT');
    }
    else if (url === '/users/me/comments' && method === 'GET') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      const userComments = state.comments.filter(c => c.authorUsername === currentUser.username);
      const myComments: MyComment[] = userComments.map(c => {
        const post = state.posts.find(p => p.id === c.postId);
        return {
          id: c.id,
          postId: c.postId,
          postTitle: post ? post.title : '未知帖子',
          content: c.content,
          parentId: c.parentId,
          createdAt: c.createdAt,
          status: c.status
        };
      });
      responseData = myComments;
    }

    // ============ Notifications API ============
    else if (url === '/notifications' && method === 'GET') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      const size = parseInt(params.size || '20');
      const page = parseInt(params.page || '0');
      const userNotifs = state.notifications
        .filter(n => n.userId === currentUser.id)
        .sort((a, b) => b.id - a.id);
      
      responseData = userNotifs.slice(page * size, (page + 1) * size);
    }
    else if (url === '/notifications/unread-count' && method === 'GET') {
      if (!currentUser) {
        responseData = 0;
      } else {
        responseData = state.notifications.filter(n => n.userId === currentUser.id && !n.isRead).length;
      }
    }
    else if (url.match(/^\/notifications\/\d+\/read$/) && method === 'PUT') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      const notifId = parseInt(url.split('/')[2]);
      const idx = state.notifications.findIndex(n => n.id === notifId && n.userId === currentUser.id);
      if (idx !== -1) {
        state.notifications[idx].isRead = true;
        saveDbState(state);
      }
      responseData = null;
    }
    else if (url === '/notifications/read-all' && method === 'PUT') {
      if (!currentUser) {
        status = 401;
        throw new Error('请登录！');
      }
      state.notifications.forEach(n => {
        if (n.userId === currentUser.id) {
          n.isRead = true;
        }
      });
      saveDbState(state);
      responseData = null;
    }

    // ============ Admin Moderation API ============
    else if ((url === '/admin/posts' || url === '/admin/posts/') && method === 'GET') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const statusParam = params.status;
      let posts = state.posts;
      if (statusParam) {
        posts = posts.filter(p => p.status === statusParam);
      }
      responseData = posts;
    }
    else if (url.match(/^\/admin\/posts\/\d+\/status$/) && method === 'PUT') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const postId = parseInt(url.split('/')[3]);
      const idx = state.posts.findIndex(p => p.id === postId);
      if (idx !== -1) {
        state.posts[idx].status = data.status;
        saveDbState(state);
      }
      responseData = null;
    }
    else if (url.match(/^\/admin\/posts\/\d+$/) && method === 'DELETE') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const postId = parseInt(url.split('/').pop() || '0');
      state.posts = state.posts.filter(p => p.id !== postId);
      state.comments = state.comments.filter(c => c.postId !== postId);
      saveDbState(state);
      responseData = null;
    }
    else if (url.match(/^\/admin\/comments\/\d+$/) && method === 'DELETE') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const commentId = parseInt(url.split('/').pop() || '0');
      const comment = state.comments.find(c => c.id === commentId);
      if (comment) {
        const postIdx = state.posts.findIndex(p => p.id === comment.postId);
        if (postIdx !== -1) {
          state.posts[postIdx].commentCount = Math.max(0, state.posts[postIdx].commentCount - 1);
        }
        state.comments = state.comments.filter(c => c.id !== commentId);
        saveDbState(state);
      }
      responseData = null;
    }
    else if (url === '/admin/users' && method === 'GET') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      responseData = state.users;
    }
    else if (url.match(/^\/admin\/users\/\d+\/ban$/) && method === 'PUT') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const userId = parseInt(url.split('/')[3]);
      const idx = state.users.findIndex(u => u.id === userId);
      if (idx !== -1) {
        state.users[idx].isLocked = true;
        saveDbState(state);
      }
      responseData = null;
    }
    else if (url.match(/^\/admin\/users\/\d+\/unban$/) && method === 'PUT') {
      if (!currentUser || currentUser.role !== 'ADMIN') {
        status = 403;
        throw new Error('权限不足！');
      }
      const userId = parseInt(url.split('/')[3]);
      const idx = state.users.findIndex(u => u.id === userId);
      if (idx !== -1) {
        state.users[idx].isLocked = false;
        saveDbState(state);
      }
      responseData = null;
    }

    // Default error for unhandled Mock paths
    else {
      status = 404;
      throw new Error(`Mock endpoint not found: ${method} ${url}`);
    }

    // Return successful response
    return {
      data: responseData,
      status,
      statusText: 'OK',
      headers: {} as any,
      config: config as any
    };

  } catch (error: any) {
    const errorResponse = {
      message: error.message || 'Internal Server Error'
    };

    return Promise.reject({
      config,
      response: {
        data: errorResponse,
        status,
        statusText: status === 401 ? 'Unauthorized' : status === 403 ? 'Forbidden' : 'Error',
        headers: {} as any,
        config: config as any
      }
    });
  }
}
