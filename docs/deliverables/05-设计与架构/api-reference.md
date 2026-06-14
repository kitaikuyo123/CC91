# CC91 校园论坛系统 API 接口文档

> Base URL: `http://localhost:8080/api`
> 认证方式: Bearer Token (JWT)
> 生成日期: 2026/06/02

---

## 通用约定

### 认证方式

除标注 `Public` 的端点外，所有请求需在 Header 中携带：

```
Authorization: Bearer <accessToken>
```

Access Token 有效期 1 小时（3600秒），过期后使用 Refresh Token 刷新。

### 统一响应格式 ApiResponse\<T\>

```json
// 成功
{ "message": "操作成功", "data": { /* 业务数据 */ } }

// 失败
{ "message": "错误描述", "data": null }
```

### 分页结构 Page\<T\>

```json
{
  "content": [],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 100,
  "totalPages": 10,
  "last": false,
  "first": true,
  "empty": false
}
```

### HTTP 状态码

| 状态码 | 含义 | 场景 |
|--------|------|------|
| 200 | 成功 | GET/PUT/DELETE 成功 |
| 201 | 创建成功 | POST 注册成功 |
| 400 | 请求错误 | 参数校验失败、业务异常 |
| 401 | 未认证 | Token 缺失/过期、密码错误 |
| 403 | 无权限 | 非 Admin 访问管理接口 |
| 404 | 不存在 | 资源未找到 |
| 409 | 冲突 | 用户名/邮箱重复 |
| 423 | 锁定 | 账户被锁定（登录失败过多） |

### 帖子状态枚举

| 值 | 含义 |
|----|------|
| `DRAFT` | 草稿，仅作者可见 |
| `PENDING` | 待审核 |
| `APPROVED` | 已发布，公开可见 |
| `REJECTED` | 审核未通过 |

### 通知类型枚举

| 值 | 含义 |
|----|------|
| `COMMENT` | 评论/回复通知 |
| `SYSTEM` | 系统通知 |

---

## 1. 认证模块 `/api/auth`

> 全部 Public，无需 Token

### POST `/api/auth/register` — 用户注册

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名，唯一 |
| `email` | String | 是 | 邮箱格式，唯一 |
| `password` | String | 是 | 密码，最少 6 位 |

**Response 201:**

```json
{ "username": "newuser", "email": "new@example.com" }
```

**Errors:** `409` 用户名或邮箱已存在 | `400` 参数校验失败

---

### POST `/api/auth/verify-email` — 验证邮箱

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱 |
| `code` | String | 是 | 6 位验证码 |

**Response 200:**

```json
{ "message": "Email verified", "data": null }
```

---

### POST `/api/auth/resend-verification` — 重发验证码

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 注册邮箱，重发验证码 |

---

### POST `/api/auth/login` — 登录

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | |
| `password` | String | 是 | |

**Response 200:**

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "dGhpcyBp...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Errors:** `401` 用户名或密码错误 | `423` 账户被锁定

---

### POST `/api/auth/refresh` — 刷新 Token

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | String | 是 | |

**Response 200:**

```json
{
  "accessToken": "new...",
  "refreshToken": "rotated...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

> Refresh Token 使用轮转机制（Rotation），每次刷新后旧 Token 自动失效。

---

### POST `/api/auth/logout` — 登出 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | String | 是 | 吊销该 Refresh Token |

---

### POST `/api/auth/forgot-password` — 忘记密码

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 无论邮箱是否存在均返回成功（防枚举） |

---

### POST `/api/auth/reset-password` — 重置密码

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | |
| `code` | String | 是 | 6 位验证码 |
| `newPassword` | String | 是 | 新密码，最少 6 位 |

---

### GET `/api/auth/health` — 健康检查

Response: `{ "message": "OK", "data": "Auth API is running" }`

---

## 2. 帖子模块 `/api/posts`

### GET `/api/posts` — 帖子列表 🌐 Public

**Query Parameters:**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 0 | 页码（从 0 开始） |
| `size` | int | 10 | 每页数量 |
| `status` | String | PUBLISHED | 状态筛选（非 Admin 只能查 PUBLISHED） |
| `sort` | String | latest | **新增** 排序方式: `latest`(默认), `comments`, `hot` |

**Response 200 — Page\<PostResponse\>:**

```json
{
  "content": [
    {
      "id": 1,
      "title": "帖子标题",
      "content": "帖子内容...",
      "authorId": 1,
      "authorUsername": "admin",
      "categoryId": 1,
      "categoryName": "CC98广场",
      "status": "APPROVED",
      "createdAt": "2026-05-18T08:00:00",
      "updatedAt": "2026-05-18T08:00:00",
      "viewCount": 1520,
      "commentCount": 3,
      "likeCount": 42,
      "isLikedByCurrentUser": false,
      "isBookmarkedByCurrentUser": false
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "last": false,
  "first": true
}
```

---

### GET `/api/posts/{id}` — 帖子详情 🌐 Public

每次访问 viewCount +1。返回单个 PostResponse 对象。

---

### GET `/api/posts/by-category/{categoryId}` — 按版块查询 🌐 Public

**Query Parameters:** `page`, `size`, `sort`（同列表接口）

返回 Page\<PostResponse\>。

---

### GET `/api/posts/search` — 搜索帖子 🌐 Public

**Query Parameters:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 是 | 按标题或内容模糊搜索 |
| `page` | int | 否 | 默认 0 |
| `size` | int | 否 | 默认 10 |

---

### POST `/api/posts` — 创建帖子 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | String | 是 | 最长 200 字符 |
| `content` | String | 是 | |
| `categoryId` | Long | 是 | 所属版块 ID |
| `status` | String | 否 | APPROVED(默认) 或 DRAFT |

**Response 200:**

```json
{ "message": "帖子创建成功", "data": { /* PostResponse */ } }
```

---

### PUT `/api/posts/{id}` — 更新帖子 🔒 Auth

**Request Body（所有字段可选，仅传需要修改的）:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 最长 200 |
| `content` | String | |
| `categoryId` | Long | |
| `status` | String | APPROVED / DRAFT |

> 权限：帖子作者或 Admin 可编辑。

---

### DELETE `/api/posts/{id}` — 删除帖子 🔒 Auth

硬删除帖子及其所有评论。权限：作者或 Admin。

---

### POST `/api/posts/{id}/like` — 点赞/取消点赞 🆕 🔒 Auth

Toggle 模式：已赞则取消，未赞则点赞。

**Response 200:**

```json
{ "message": "liked", "data": { "likeCount": 43, "isLiked": true } }
```

---

### POST `/api/posts/{id}/bookmark` — 收藏/取消收藏 🆕 🔒 Auth

Toggle 模式。

**Response 200:**

```json
{ "message": "bookmarked", "data": { "isBookmarked": true } }
```

---

## 3. 评论模块 `/api`

### GET `/api/posts/{postId}/comments` — 评论列表 🌐 Public

获取帖子的评论树（嵌套结构），仅返回 APPROVED 状态。

```json
[
  {
    "id": 1,
    "postId": 1,
    "authorId": 2,
    "authorUsername": "user",
    "content": "评论内容",
    "parentId": null,
    "createdAt": "2026-05-18T08:15:00",
    "status": "APPROVED",
    "replies": [
      { /* 子评论，结构相同 */ }
    ]
  }
]
```

---

### POST `/api/posts/{postId}/comments` — 创建评论 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 评论内容 |

> 创建评论时自动通知帖子作者。

---

### POST `/api/comments/{id}/reply` — 回复评论 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 回复内容 |

> 回复评论时自动通知被回复的评论作者。

---

### DELETE `/api/comments/{id}` — 删除评论 🔒 Auth

删除评论，同时减少帖子 commentCount。权限：评论作者或 Admin。

---

### PUT `/api/comments/{id}` — 编辑评论 🆕 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 修改后的评论内容 |

> 权限：仅评论作者可编辑自己的评论。

---

## 4. 用户模块 `/api/users`

### GET `/api/users/me` — 我的资料 🔒 Auth

```json
{
  "username": "admin",
  "email": "admin@cc98.org",
  "avatarUrl": "https://...",
  "bio": "简介",
  "location": "浙大紫金港",
  "website": "https://...",
  "createdAt": "2026-05-18T06:00:00"
}
```

---

### GET `/api/users/{username}` — 查看用户资料 🌐 Public

---

### PUT `/api/users/me/profile` — 更新资料 🔒 Auth

**Request Body（所有字段可选）:**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| `avatarUrl` | String | URL 格式，最长 500 | 头像 URL |
| `bio` | String | 最长 200 | 个人简介 |
| `location` | String | 最长 100 | 所在地 |
| `website` | String | URL 格式或 null | 个人网站 |

---

### PUT `/api/users/me/password` — 修改密码 🔒 Auth

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `oldPassword` | String | 是 | 当前密码 |
| `newPassword` | String | 是 | 新密码，最少 6 位 |
| `confirmPassword` | String | 是 | 确认新密码 |

---

### GET `/api/users/me/posts` — 我的帖子 🔒 Auth

返回 List\<PostResponse\>，仅 APPROVED 状态。

---

### GET `/api/users/me/drafts` — 我的草稿 🔒 Auth

返回 List\<PostResponse\>，仅 DRAFT 状态。

---

### GET `/api/users/me/comments` — 我的评论 🔒 Auth

返回 List\<UserCommentResponse\>。

```json
{
  "id": 1,
  "postId": 1,
  "postTitle": "帖子标题",
  "content": "评论",
  "parentId": null,
  "createdAt": "...",
  "status": "APPROVED"
}
```

---

### GET `/api/users/me/bookmarks` — 我的收藏 🆕 🔒 Auth

返回 List\<PostResponse\>。

---

## 5. 版块模块 `/api/categories`

### GET `/api/categories` — 版块列表 🌐 Public

按 sortOrder 排序，含帖子统计数量。

```json
[
  {
    "id": 1,
    "name": "CC98广场",
    "description": "...",
    "sortOrder": 1,
    "postCount": 5,
    "createdAt": "..."
  }
]
```

---

### GET `/api/categories/{id}` — 版块详情 🌐 Public

---

### POST `/api/categories` — 创建版块 🔐 Admin

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 版块名称，唯一 |
| `description` | String | 否 | |
| `sortOrder` | Integer | 否 | 默认 1 |

---

### PUT `/api/categories/{id}` — 更新版块 🔐 Admin

**Request Body（所有字段可选）:** `name`, `description`, `sortOrder`

---

### DELETE `/api/categories/{id}` — 删除版块 🔐 Admin

如果版块下有帖子则返回 400 错误。

---

## 6. 通知模块 `/api/notifications`

> 全部需要 Auth

### GET `/api/notifications` — 通知列表 🔒 Auth

**Query Parameters:** `page` (默认 0), `size` (默认 20)

```json
[
  {
    "id": 1,
    "userId": 2,
    "type": "COMMENT",
    "title": "您的帖子有新回复",
    "content": "...",
    "relatedId": 1,
    "isRead": false,
    "createdAt": "2026-05-18T08:45:00"
  }
]
```

---

### GET `/api/notifications/unread-count` — 未读数量 🔒 Auth

返回纯数字：`3`

---

### PUT `/api/notifications/{id}/read` — 标记已读 🔒 Auth

只能标记自己的通知。

---

### PUT `/api/notifications/read-all` — 全部已读 🔒 Auth

---

## 7. 公告模块 `/api/announcements`

### GET `/api/announcements` — 公告列表 🌐 Public

置顶优先，时间倒序。

```json
[
  {
    "id": 1,
    "title": "公告标题",
    "content": "...",
    "authorId": 1,
    "authorUsername": "admin",
    "isPinned": true,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

---

### GET `/api/announcements/{id}` — 公告详情 🌐 Public

---

### POST `/api/admin/announcements` — 创建公告 🔐 Admin

**Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | String | 是 | 最长 200 |
| `content` | String | 是 | |
| `isPinned` | Boolean | 否 | 是否置顶，默认 false |

---

### PUT `/api/admin/announcements/{id}` — 更新公告 🔐 Admin

**Request Body（所有字段可选）:** `title`, `content`, `isPinned`

---

### DELETE `/api/admin/announcements/{id}` — 删除公告 🔐 Admin

---

## 8. 管理后台 `/api/admin`

> 全部需要 Admin 权限

### 用户管理 `/api/admin/users`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users?page=&size=` | 分页获取用户列表 |
| PUT | `/api/admin/users/{id}/ban` | 永久封禁用户 |
| PUT | `/api/admin/users/{id}/unban` | 解封用户 |
| PUT | `/api/admin/users/{id}/role` | 修改角色 (`USER` / `ADMIN`) |

**AdminUserDTO:**

```json
{
  "id": 1,
  "username": "admin",
  "email": "...",
  "role": "ADMIN",
  "isLocked": false,
  "createdAt": "...",
  "lockUntil": null
}
```

### 内容管理 `/api/admin/posts` + `/api/admin/comments`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/posts?status=` | 获取所有帖子（可按状态筛选） |
| PUT | `/api/admin/posts/{id}/status` | 审核帖子（APPROVED/PENDING/REJECTED） |
| DELETE | `/api/admin/posts/{id}` | 强制删除帖子及评论 |
| GET | `/api/admin/comments` | 获取所有评论列表 |
| DELETE | `/api/admin/comments/{id}` | 强制删除评论 |

### 举报管理 🆕

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/reports` | Auth | 用户提交举报 |
| GET | `/api/admin/reports?status=` | Admin | 获取举报列表 |
| PUT | `/api/admin/reports/{id}` | Admin | 处理举报 |

**POST `/api/reports` Request Body:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `targetType` | String | 是 | `POST` 或 `COMMENT` |
| `targetId` | Long | 是 | 帖子或评论 ID |
| `reason` | String | 是 | 举报原因，最长 500 字符 |

**PUT `/api/admin/reports/{id}` Request Body:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | `REVIEWED` 或 `RESOLVED` |
| `adminComment` | String | 管理员处理备注 |

---

## 9. 文件上传 🆕

### POST `/api/upload` — 上传文件 🔒 Auth

**Request:** `multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| `file` | MultipartFile | 支持 jpg/png/gif/webp，最大 2MB |

**Response 200:**

```json
{ "message": "上传成功", "data": { "url": "/uploads/2026/06/abc123.jpg" } }
```

> 上传的文件保存到 `backend/uploads/` 目录，通过 `/uploads/**` 静态资源映射访问。

---

## 权限标记说明

| 标记 | 含义 |
|------|------|
| 🌐 Public | 无需认证，任何人可访问 |
| 🔒 Auth | 需登录，Header 携带 Bearer Token |
| 🔐 Admin | 需 ADMIN 角色 |
| 🆕 | 本次迭代新增端点 |
