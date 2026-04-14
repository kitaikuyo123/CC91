# CC91 论坛系统架构

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Spring Boot 3.2 + Maven + MySQL 8 + Flyway + Spring Security (JWT) |
| 前端 | React 19 + Vite + TypeScript + react-router-dom + axios + @tanstack/react-query |
| 测试 | JUnit 5 + MockMvc (后端), Vitest + RTL (前端) |

## 核心模块与优先级

| 模块 | 说明 | 优先级 |
|------|------|--------|
| 用户与个人资料 | 查看/编辑个人资料、头像、签名 | P0 |
| 版块分类 | 论坛分区（如「技术讨论」「灌水区」） | P0 |
| 主题帖 | 发帖、浏览帖子列表、查看帖子详情 | P0 |
| 回复/跟帖 | 在主题下回复 | P0 |
| 搜索 | 按关键词搜索帖子 | P1 |
| 通知 | 被回复/被 @ 时通知 | P1 |
| 管理后台 | 版块管理、内容审核、用户管理 | P2 |

## 数据模型

### 已有表

- **users** — id, username, email, password_hash, created_at, updated_at, is_locked, failed_login_attempts, lock_until
- **verification_codes** — id, email, code, type, expires_at, used, created_at
- **refresh_tokens** — id, user_id, token, expires_at, revoked, created_at

### 新增表

```sql
-- 版块
categories
  id            BIGINT PK AUTO_INCREMENT
  name          VARCHAR(50) NOT NULL UNIQUE
  description   VARCHAR(200)
  sort_order    INT DEFAULT 0
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP

-- 主题帖
topics
  id            BIGINT PK AUTO_INCREMENT
  category_id   BIGINT NOT NULL -> categories.id
  user_id       BIGINT NOT NULL -> users.id
  title         VARCHAR(200) NOT NULL
  content       TEXT NOT NULL
  view_count    INT DEFAULT 0
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

-- 回复
replies
  id            BIGINT PK AUTO_INCREMENT
  topic_id      BIGINT NOT NULL -> topics.id
  user_id       BIGINT NOT NULL -> users.id
  content       TEXT NOT NULL
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

-- 个人资料扩展
user_profiles
  user_id       BIGINT PK -> users.id
  avatar_url    VARCHAR(500)
  bio           VARCHAR(500)
  location      VARCHAR(100)
  website       VARCHAR(200)
```

## 页面路由

| 路径 | 页面 | 需认证 |
|------|------|--------|
| `/` | 首页（版块列表 + 最新帖子） | 否 |
| `/login` | 登录 | 否 |
| `/register` | 注册 | 否 |
| `/profile/:username` | 查看个人资料 | 否 |
| `/profile/edit` | 编辑个人资料 | 是 |
| `/category/:id` | 版块内帖子列表 | 否 |
| `/topic/:id` | 帖子详情 + 回复 | 否 |
| `/topic/new` | 发新帖 | 是 |
| `/search` | 搜索结果 | 否 |

## 前后端分层

```
后端: Controller -> Service -> Repository -> Entity/DTO
前端: Pages -> API(hooks) -> Components(展示/交互)
```

## 开发顺序

1. 用户个人资料（含 DB 迁移、后端 API、前端页面）
2. 版块分类
3. 主题帖
4. 回复/跟帖
5. 搜索
6. 通知
7. 管理后台
