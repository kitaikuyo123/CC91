# CC91 论坛 - 问题跟踪与功能清单

> Developer agent 每次开工前必须先读此文件，优先处理未修复的 Bug，再实现未完成功能。

## 状态说明

- `[ ]` 未处理
- `[~]` 进行中
- `[-]` 暂不处理（附原因）

---

## 未完成功能

### F-02 管理后台评论审核 UI
- **文件**: `ContentModeration.tsx`
- **状态**: `adminDeleteComment` API 已存在但 UI 未使用，评论审核界面未实现

### F-03 已登录用户修改密码
- **状态**: 目前只能通过忘记密码流程，缺少 `PUT /api/users/me/password` 接口

### F-04 管理员修改用户角色
- **文件**: `AdminUserController`
- **状态**: 只能封禁/解封，无修改角色功能

### F-06 Admin 页面测试覆盖
- **状态**: `AdminDashboard`、`CategoryManage`、`ContentModeration`、`UserManage` 无测试

### F-07 后端搜索和分类查询测试
- **状态**: `PostControllerTest` / `PostServiceTest` 缺少 search 和 by-category 测试用例

---

## 已完成功能

### F-01 Dashboard "我的帖子" / "我的评论"
- [x] **文件**: `DashboardPage.tsx`、`MyPostsPage.tsx`、`MyCommentsPage.tsx`
- **修复**:
  1. 后端增加 `GET /api/users/me/posts`、`GET /api/users/me/comments`
  2. Dashboard "我的帖子"/"我的评论"各展示前 5 条，点击 "查看全部帖子" 跳转到独立页面 `/dashboard/posts`，点击 "查看所有评论" 跳转到 `/dashboard/comments`
  3. 新增 `MyPostsPage.tsx`：展示当前用户全部帖子，点击行跳转到帖子详情
  4. 新增 `MyCommentsPage.tsx`：展示当前用户全部评论，点击行跳转到对应帖子详情

### F-05 404 页面
- [x] **文件**: `App.tsx`
- **修复**: 增加 catch-all `*` 路由；未匹配 URL 不再显示空白内容区，而是展示 404 页面（包含 `/admin/*` 兜底）
