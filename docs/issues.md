# CC91 论坛 - 问题跟踪与功能清单

> Developer agent 每次开工前必须先读此文件，优先处理未修复的 Bug，再实现未完成功能。

## 状态说明

- `[ ]` 未处理
- `[~]` 进行中
- `[-]` 暂不处理（附原因）

---

## 未完成功能

（无）

---

## 已完成功能

### F-01 Dashboard "我的帖子" / "我的评论"
- [x] **文件**: `DashboardPage.tsx`、`MyPostsPage.tsx`、`MyCommentsPage.tsx`
- **修复**:
  1. 后端增加 `GET /api/users/me/posts`、`GET /api/users/me/comments`
  2. Dashboard "我的帖子"/"我的评论"各展示前 5 条，点击 "查看全部帖子" 跳转到独立页面 `/dashboard/posts`，点击 "查看所有评论" 跳转到 `/dashboard/comments`
  3. 新增 `MyPostsPage.tsx`：展示当前用户全部帖子，点击行跳转到帖子详情
  4. 新增 `MyCommentsPage.tsx`：展示当前用户全部评论，点击行跳转到对应帖子详情

### F-02 管理后台评论审核 UI
- [x] **文件**: `ContentModeration.tsx`
- **修复**: 已实现评论审核 tab，包含评论列表、删除确认弹窗，调用 `adminGetComments` 和 `adminDeleteComment`

### F-03 已登录用户修改密码
- [x] **文件**: `ChangePasswordPage.tsx`、`App.tsx`、`DashboardPage.tsx`
- **修复**:
  1. 新增 `ChangePasswordPage.tsx`：旧密码 + 新密码 + 确认密码表单，调用 `changePassword()` API
  2. `App.tsx` 添加 `dashboard/password` 路由
  3. `DashboardPage.tsx` 添加"修改密码"快捷卡片

### F-04 管理员修改用户角色
- [x] **文件**: `UserManage.tsx`
- **修复**: 角色列从只读 badge 改为 `<select>` 下拉框，调用 `adminUpdateUserRole()`，含 confirm 确认

### F-05 404 页面
- [x] **文件**: `App.tsx`
- **修复**: 增加 catch-all `*` 路由；未匹配 URL 不再显示空白内容区，而是展示 404 页面（包含 `/admin/*` 兜底）

### F-06 Admin 页面测试覆盖
- [x] **文件**: `AdminDashboard.test.tsx`、`CategoryManage.test.tsx`、`ContentModeration.test.tsx`、`UserManage.test.tsx`
- **修复**: 新增 4 个测试文件共 19 个测试用例，覆盖渲染、交互、mutation 调用

### F-07 后端搜索和分类查询测试
- [x] **文件**: `PostControllerTest.java`、`PostServiceTest.java`
- **修复**: PostControllerTest 新增 4 个测试方法，PostServiceTest 新增 6 个测试方法
