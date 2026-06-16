# CC91 论坛系统 — 单元测试报告

> **报告日期**：2026-06-14  
> **测试范围**：后端 Service/Security 单元测试、前端组件与 Context 单元测试  
> **后端结果来源**：`backend/target/surefire-reports/` 现有 Surefire 报告（生成时间：2026-06-09 23:32）  
> **前端复跑命令**：`cd frontend && npm test -- --run src/__tests__/components src/__tests__/context`  
> **相关文档**：集成测试报告见 `docs/integration-test-report.md`，压力测试报告见 `docs/stress-test/stress-test-report.md`

---

## 1. 测试目标

单元测试用于验证 CC91 论坛系统核心业务逻辑和前端基础组件的正确性，重点覆盖：

- 后端认证、用户、帖子、分类、评论、通知等 Service 层业务分支
- Spring Security 用户加载逻辑
- 前端通用组件、受保护路由、评论组件、通知铃铛、分页、布局等 UI 基础能力
- AuthContext 登录态、Token 存储和退出逻辑
- 异常路径、空状态、权限状态和用户交互状态

---

## 2. 测试配置

| 参数 | 值 |
|------|------|
| 后端测试框架 | JUnit 5 + Mockito + Maven Surefire |
| 后端运行环境 | Java 17, Spring Boot 3.2 |
| 前端测试框架 | Vitest 4.1.4 + React Testing Library + jsdom |
| 前端运行环境 | React 19, TypeScript 6, Vite 8 |
| 前端测试命令 | `npm test -- --run src/__tests__/components src/__tests__/context` |
| 后端报告目录 | `backend/target/surefire-reports/` |
| 测试执行策略 | 后端采用现有 Surefire 报告，前端在当前环境复跑 |

> 本次补充报告前，前端测试中存在两处旧断言与当前 UI 不一致：评论回复框 placeholder 已变为 `回复 @用户名...`，管理后台首页已不再显示旧版快捷链接。已优先修复测试断言并完成复跑。

---

## 3. 测试结果汇总

| 范围 | 测试套件/文件 | 用例数 | 失败 | 错误 | 跳过 | 结果 |
|------|--------------:|------:|----:|----:|----:|------|
| 后端 Service/Security | 7 | 89 | 0 | 0 | 0 | 通过 |
| 前端组件/Context | 19 | 125 | 0 | 0 | 0 | 通过 |
| **合计** | **26** | **214** | **0** | **0** | **0** | **通过** |

前端单元/组件测试复跑结果：

```text
Test Files  19 passed (19)
Tests       125 passed (125)
Duration    10.36s
```

后端 Service/Security Surefire 汇总结果：

| 测试套件 | 用例数 | 失败 | 错误 | 跳过 | 耗时(s) |
|----------|------:|----:|----:|----:|--------:|
| `com.cc91.security.UserDetailsServiceImplTest` | 5 | 0 | 0 | 0 | 0.428 |
| `com.cc91.service.AuthServiceTest` | 13 | 0 | 0 | 0 | 1.892 |
| `com.cc91.service.CategoryServiceTest` | 16 | 0 | 0 | 0 | 0.161 |
| `com.cc91.service.CommentServiceTest` | 13 | 0 | 0 | 0 | 5.238 |
| `com.cc91.service.NotificationServiceTest` | 12 | 0 | 0 | 0 | 4.855 |
| `com.cc91.service.PostServiceTest` | 21 | 0 | 0 | 0 | 8.323 |
| `com.cc91.service.UserServiceTest` | 9 | 0 | 0 | 0 | 0.780 |
| **合计** | **89** | **0** | **0** | **0** | **21.677** |

---

## 4. 覆盖范围分析

### 4.1 后端单元测试

| 模块 | 覆盖重点 |
|------|----------|
| AuthService | 注册、登录、验证码、Token 签发、异常认证路径 |
| UserService | 用户资料、用户查询、状态变更、权限相关业务 |
| PostService | 发帖、编辑、删除、查询、状态流转 |
| CommentService | 评论创建、回复、删除、评论树结构 |
| CategoryService | 分类增删改查、排序、边界条件 |
| NotificationService | 通知创建、读取、标记已读 |
| UserDetailsServiceImpl | Spring Security 用户加载和认证主体构建 |

这些测试集中验证业务分支，不依赖完整 HTTP 链路，能够快速定位核心逻辑回归。

### 4.2 前端单元与组件测试

| 模块 | 覆盖重点 |
|------|----------|
| 布局与导航 | Header、Footer、Layout、AdminLayout、Breadcrumbs |
| 权限与路由 | ProtectedRoute、AdminRoute、SafeLink |
| 论坛组件 | PostCard、TopicTable、BoardCard、CommentSection、Pagination |
| 状态组件 | ErrorBoundary、ErrorMessage、MockBanner、NotificationBell |
| AuthContext | 登录态初始化、Token 存储、用户状态、退出逻辑 |

其中 `CommentSection.test.tsx` 覆盖评论加载、空状态、错误状态、登录/未登录差异、发表评论、回复、删除等交互，是前端组件测试中覆盖业务最多的测试文件。

---

## 5. 问题修复记录

| 问题 | 原因 | 处理 |
|------|------|------|
| 评论回复测试找不到 `写下你的回复...` | 当前产品已将回复框 placeholder 调整为 `回复 @用户名...` | 更新测试断言，并固定点击父评论回复按钮 |
| 管理后台首页测试查找旧快捷入口 | 当前页面保留统计卡片和最新用户表，不再渲染旧版快捷链接 | 将断言改为管理后台标题、最新用户、用户状态展示 |

修复后执行完整前端测试：

```text
Test Files  46 passed (46)
Tests       350 passed (350)
Duration    44.52s
```

---

## 6. 结论

CC91 论坛系统当前单元测试通过情况良好。后端 Service/Security 层 89 个用例全部通过，前端组件/Context 125 个用例全部通过，合计 214 个单元级用例无失败、无错误、无跳过。

从覆盖范围看，测试已覆盖认证、用户、帖子、评论、分类、通知等核心业务逻辑，以及前端基础组件、权限路由和登录态管理。单元测试可以作为后续修改业务逻辑和基础组件时的快速回归门禁。

---

## 附录

复现命令：

```powershell
cd frontend
npm test -- --run src/__tests__/components src/__tests__/context
```

后端现有报告位置：

```text
backend/target/surefire-reports/
```
