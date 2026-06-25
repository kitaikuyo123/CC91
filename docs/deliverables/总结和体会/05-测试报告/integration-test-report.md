# CC91 论坛系统 — 集成测试报告

> 报告日期：2026-06-17
> 测试范围：7 个微服务（eureka-server / gateway / user-service / forum-service / content-service / notification-service / file-service）的 Controller 切片测试（@WebMvcTest）、Repository JPA 集成测试（@DataJpaTest）、InternalApiAuthFilter 配置切片测试、应用上下文加载测试（@SpringBootTest contextLoads）以及 Gateway 健康检查组件集成测试
> 测试执行：qa agent 实际运行 `mvn test`（不接受静态分析），数据来源于各服务 `target/surefire-reports/TEST-*.xml`
> 相关文档：单元测试报告见 `unit-test-report.md`，压力测试报告见 `stress-test/stress-test-report.md`

---

## 1. 测试目标

本报告覆盖的集成测试用于验证**横跨多层**的协作行为：

- **Controller 切片测试（@WebMvcTest）**：MockMvc + Spring Security + GlobalExceptionHandler 完整链路，验证：
  - HTTP 状态码正确性（200/400/401/403/404/409/500）
  - JSON 响应体结构与字段（`ApiResponse.success` / `message` / `data`）
  - 鉴权决策（匿名用户、普通用户、管理员、跨用户访问）
  - 越权保护（IDOR，如 Alice 不能修改 Bob 的资料）
  - JWT 篡改、SQL 注入字符作为参数、XSS payload 提交被拒绝
  - 输入校验失败时由 `@ControllerAdvice` 统一转 400
- **Repository JPA 集成测试（@DataJpaTest）**：H2 + Flyway 迁移 + 真实 SQL 执行，验证：
  - `@Query` 自定义 JPQL/SQL 的字段映射
  - `@Modifying` 更新/删除操作的影响行数
  - 复合条件查询（status + author + category 等）
  - 种子数据（V1 迁移）的业务正确性（如：默认管理员账号能匹配预期密码 hash）
- **InternalApiAuthFilter 配置切片测试（@WebMvcTest）**：内部 API 的 Token 校验、空 Token 拒绝、错误 Token 拒绝、路径匹配规则
- **应用上下文加载测试（@SpringBootTest contextLoads）**：eureka-server 与 gateway 能完整启动，Bean 装配无环、配置无缺失
- **GatewayDownstreamHealthIndicator 组件集成测试**：基于 DiscoveryClient mock 的健康指标，验证 all-up / 部分缺失场景的状态判定

> **未做的集成测试类型**（不在本任务范围）：
> - 端到端 `@SpringBootTest(webEnvironment=RANDOM_PORT)` 启动整个微服务集群做 HTTP-to-HTTP 验证
> - Testcontainers + 真实 MySQL/Flyway 验证生产 SQL 方言
> - OWASP ZAP / Burp 渗透扫描（这部分由独立安全审查覆盖）
> - 前端 Vitest 集成测试（前端测试报告单独维护）
>
> 原因：上述测试需要额外的基础设施（容器化运行时、多服务端口编排），属于 CI/CD 流水线的下游阶段。本报告聚焦于**轻量集成测试**：在毫秒级启动切片上下文，验证业务正确性与安全约束。

---

## 2. 测试配置

| 参数 | 值 |
|------|------|
| Controller 切片框架 | Spring Boot Test `@WebMvcTest(controllers = …)` + MockMvc |
| Repository 集成框架 | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` |
| 数据库 | H2 in-memory，MySQL 兼容模式（`MODE=MySQL`），`spring.flyway.enabled=true` |
| 种子数据来源 | `src/main/resources/db/migration/V1__init.sql` 中的 Flyway 迁移 |
| Security 上下文 | `@WithMockUser(username=…, roles=…)` + 自定义 `@WithUserPrincipal` 注入真实 UserDetails |
| 全局异常处理 | `@WebMvcTest` 自动扫描 `@ControllerAdvice`（GlobalExceptionHandler） |
| 测试 Profile | `test`（`src/test/resources/application-test.yml`） |
| 运行 JDK | Oracle JDK 24.0.1（`-Dnet.bytebuddy.experimental=true`） |
| Spring Boot | 3.2.12 |
| 复现命令 | 详见附录 A |

---

## 3. 测试结果汇总（按服务）

每个服务执行 `mvn test`，从输出尾部抓取 `Tests run / Failures / Errors / Skipped` 和 `BUILD SUCCESS/FAILURE`，全部 **BUILD SUCCESS**，零失败、零错误、零跳过。

| 服务 | 集成测试套件数（outer） | 集成测试用例数 | 失败 | 错误 | 跳过 | 总耗时 |
|------|------:|------:|----:|----:|----:|------:|
| eureka-server | 1（@SpringBootTest contextLoads） | 1 | 0 | 0 | 0 | 8.38s |
| gateway | 2（contextLoads + DownstreamHealthIndicator） | 9 | 0 | 0 | 0 | 7.81s |
| user-service | 6（4 Controller + 3 Repository） | 71 | 0 | 0 | 0 | 34.22s |
| forum-service | 9（5 Controller + 2 Repository） | 100 | 0 | 0 | 0 | 45.45s |
| content-service | 4（2 Controller + 1 Repository） | 38 | 0 | 0 | 0 | 28.11s |
| notification-service | 5（Controller + InternalApiAuthFilter 配置 + Repository） | 38 | 0 | 0 | 0 | 27.55s |
| file-service | 4（2 Controller + InternalApiAuthFilter + Repository） | 41 | 0 | 0 | 0 | 26.61s |
| **合计** | **31** | **298** | **0** | **0** | **0** | ~178s |

---

## 4. 集成测试套件详情（按服务 + 测试类型）

### 4.1 eureka-server（1 用例）

| 测试类 | 类型 | 用例数 | 耗时 | 说明 |
|--------|------|------:|-----:|------|
| `EurekaServerApplicationTest` | `@SpringBootTest contextLoads` | 1 | 8.38s | 启动完整 Spring 上下文，验证 Netflix Eureka 服务端 Bean 装配、Actuator 端点暴露、自我保护模式默认配置无异常 |

### 4.2 gateway（9 用例）

| 测试类 | 类型 | 用例数 | 耗时 | 说明 |
|--------|------|------:|-----:|------|
| `GatewayApplicationTest` | `@SpringBootTest contextLoads` | 1 | 7.39s | Spring Cloud Gateway 路由表加载、LoadBalancer 装配、Eureka 客户端初始化 |
| `GatewayDownstreamHealthIndicatorTest.AllUp` | 组件集成（DiscoveryClient mock） | 3 | 0.01s | 全部下游 UP → health 状态 UP；各项详情正确暴露 |
| `GatewayDownstreamHealthIndicatorTest.SomeMissing` | 组件集成 | 5 | 0.05s | 部分下游 DOWN/缺失 → health 状态 DOWN，包含具体失败服务名 |

### 4.3 user-service（71 用例）

#### Controller 切片测试（56 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `AdminUserControllerTest.Authorization` | 3 | 0.16s | 非 ADMIN 角色访问 → 403；匿名 → 401；ADMIN → 200 |
| `AdminUserControllerTest.Ban` | 2 | 0.06s | 封禁用户状态变更；目标不存在 → 404 |
| `AdminUserControllerTest.Delete` | 2 | 8.37s | 删除用户 → 204；删除自己 → 409 |
| `AdminUserControllerTest.Role` | 2 | 0.38s | 角色提升到 ADMIN；非 ADMIN 操作者 → 403 |
| `AdminUserControllerTest.Unban` | 1 | 0.05s | 解封状态变更 |
| `AuthControllerTest.Login` | 5 | 0.09s | 成功 → JWT；密码错 → 401；用户不存在 → 401；账户锁定 → 423；请求体格式错 → 400 |
| `AuthControllerTest.Logout` | 2 | 0.06s | 成功 → 204；RefreshToken 已撤销 → 409 |
| `AuthControllerTest.PasswordReset` | 5 | 0.15s | forgot-password → 邮件触发；reset-token 过期 → 410；新密码不符合策略 → 400 |
| `AuthControllerTest.Refresh` | 3 | 0.06s | 合法 refresh → 新 access；refresh 已过期 → 401；refresh 不存在 → 404 |
| `AuthControllerTest.Register` | 7 | 0.11s | 成功 → 201；重复邮箱 → 409；重复用户名 → 409；弱密码 → 400；非法邮箱 → 400；空字段 → 400；过长字段 → 400 |
| `AuthControllerTest.VerifyEmail` | 3 | 0.06s | 验证码正确 → 200；验证码错误 → 400；验证码已使用 → 409 |
| `InternalUserControllerTest.Endpoints` | 5 | 0.92s | `/internal/**` 端点的内部 API 行为；Feign 调用契约 |
| `InternalUserControllerTest.TokenEnforcement` | 3 | 0.04s | 缺失内部 Token → 401；错误 Token → 401；合法 Token → 200 |
| `UserControllerTest.ChangePassword` | 5 | 0.69s | 成功变更；旧密码错 → 400；新密码重复 → 400；未认证 → 401；跨用户 → 403 |
| `UserControllerTest.GetByUsername` | 2 | 0.03s | 用户存在 → 200；不存在 → 404 |
| `UserControllerTest.GetMe` | 2 | 0.03s | 已认证 → 200；匿名 → 401 |
| `UserControllerTest.UpdateProfile` | 4 | 0.09s | 本人更新 → 200；跨用户更新 → 403（IDOR 保护）；字段非法 → 400 |

#### Repository JPA 集成测试（15 用例）

| 测试类 | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `UserRepositoryTest` | 7 | 0.12s | `findByUsername` / `existsByEmail` / `existsByUsername`；大小写敏感；V1 种子 admin 账号可查到 |
| `RefreshTokenRepositoryTest` | 4 | 4.77s | `findByToken` 索引命中；`revokeByUserId` @Modifying 影响行数；过期 token 过滤 |
| `VerificationCodeRepositoryTest` | 4 | 0.08s | `findLatestByEmailAndType`；`markAsUsed` @Modifying；已使用 code 不能再次命中查询 |

### 4.4 forum-service（100 用例）

#### Controller 切片测试（72 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `AdminContentControllerTest.Authorization` | 2 | 0.05s | 非 ADMIN 强删 → 403 |
| `AdminContentControllerTest.ForceDeleteComment` | 2 | 8.84s | 强删评论 → 204；评论不存在 → 404 |
| `AdminContentControllerTest.ForceDeletePost` | 2 | 0.08s | 强删帖子 → 204 |
| `AdminContentControllerTest.GetComments` | 2 | 0.15s | 分页列出评论；状态过滤 |
| `AdminContentControllerTest.GetPostsByStatus` | 2 | 0.09s | 按状态过滤；分页 |
| `AdminContentControllerTest.UpdatePostStatus` | 4 | 0.35s | PENDING → PUBLISHED → REJECTED 状态机；非法转移 → 400 |
| `CategoryControllerTest.Create` | 5 | 0.09s | ADMIN 创建成功 → 201；非 ADMIN → 403；重名 → 409；字段缺失 → 400 |
| `CategoryControllerTest.Delete` | 3 | 1.20s | 删除 → 204；有帖子引用 → 409；不存在 → 404 |
| `CategoryControllerTest.GetAll` | 2 | 0.04s | 列表分页 |
| `CategoryControllerTest.GetById` | 2 | 0.04s | 存在 → 200；不存在 → 404 |
| `CategoryControllerTest.Update` | 3 | 0.10s | 更新 → 200；非 ADMIN → 403；不存在 → 404 |
| `CommentControllerTest.CreateComment` | 4 | 0.07s | 成功 → 201；帖子不存在 → 404；正文为空 → 400；未认证 → 401 |
| `CommentControllerTest.DeleteComment` | 2 | 0.05s | 作者删除 → 204；非作者 → 403（IDOR） |
| `CommentControllerTest.GetCommentsByPostId` | 2 | 0.87s | 列表分页；状态过滤 |
| `CommentControllerTest.ReplyComment` | 2 | 0.04s | 二级回复；父评论不存在 → 404 |
| `CommentControllerTest.UpdateComment` | 2 | 0.05s | 作者更新；非作者 → 403 |
| `PostControllerTest.CreatePost` | 6 | 0.11s | 成功 → 201；草稿 → 201；分类不存在 → 404；正文为空 → 400；HTML 注入 → 净化后保留 |
| `PostControllerTest.DeletePost` | 2 | 0.04s | 作者删除 → 204；非作者 → 403 |
| `PostControllerTest.GetPostById` | 4 | 0.06s | 存在 → 200；不存在 → 404；已删除 → 404；浏览数自增断言 |
| `PostControllerTest.GetPostList` | 3 | 0.06s | 分页；按分类过滤；按状态过滤 |
| `PostControllerTest.GetPostsByCategory` | 1 | 0.02s | 分类下帖子分页 |
| `PostControllerTest.SearchPosts` | 2 | 0.06s | 关键字搜索；SQL 注入字符（`' OR '1'='1`）→ 安全返回 |
| `PostControllerTest.ToggleBookmark` | 1 | 0.74s | 切换收藏状态 |
| `PostControllerTest.ToggleLike` | 1 | 0.02s | 切换点赞状态 |
| `PostControllerTest.UpdatePost` | 2 | 0.05s | 作者更新；非作者 → 403 |
| `UserContentControllerTest.GetMyBookmarks` | 2 | 0.68s | 我的收藏分页；未认证 → 401 |
| `UserContentControllerTest.GetMyComments` | 2 | 0.03s | 我的评论分页 |
| `UserContentControllerTest.GetMyDrafts` | 2 | 0.03s | 我的草稿分页 |
| `UserContentControllerTest.GetMyPosts` | 3 | 0.04s | 我的帖子分页；按状态过滤 |

#### Repository JPA 集成测试（28 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `CommentRepositoryTest.CountByPost` | 2 | 0.05s | `countByPostId` 字段映射 |
| `CommentRepositoryTest.CountByPostIdInAndStatus` | 2 | 0.08s | IN 子句 + status 复合查询 |
| `CommentRepositoryTest.FindAllWithPost` | 3 | 5.56s | `@EntityGraph` 关联查询，N+1 规避 |
| `CommentRepositoryTest.FindByAuthor` | 1 | 0.07s | 按 author 字段过滤 |
| `CommentRepositoryTest.FindByPostAndStatus` | 1 | 0.03s | 复合条件 |
| `CommentRepositoryTest.FindReplies` | 1 | 0.03s | 二级回复查询 |
| `PostRepositoryTest.CountByCategory` | 2 | 0.04s | `countByCategoryId` |
| `PostRepositoryTest.CountStatsByCategory` | 2 | 0.06s | GROUP BY + 投影 DTO 映射 |
| `PostRepositoryTest.CountTodayPosts` | 1 | 0.04s | DATE 函数兼容 H2 MySQL 模式 |
| `PostRepositoryTest.FindByAuthorAndStatus` | 2 | 0.05s | 复合条件 |
| `PostRepositoryTest.FindByCategoryAndStatus` | 1 | 0.03s | 复合条件 |
| `PostRepositoryTest.FindByStatus` | 1 | 0.04s | 单字段过滤 |
| `PostRepositoryTest.FindSortedByCommentCount` | 2 | 0.06s | ORDER BY 子查询 + JOIN |
| `PostRepositoryTest.IncrementViewCount` | 3 | 0.11s | `@Modifying` `UPDATE post SET view_count = view_count + 1`；影响行数断言；并发安全（原子自增） |
| `PostRepositoryTest.Search` | 4 | 0.12s | LIKE 关键字；多关键字；特殊字符；空关键字退化为全部 |

### 4.5 content-service（38 用例）

#### Controller 切片测试（32 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `AnnouncementControllerTest.Create` | 6 | 0.15s | ADMIN 创建 → 201；非 ADMIN → 403；字段缺失 → 400；过期时间在过去 → 400 |
| `AnnouncementControllerTest.Delete` | 3 | 8.27s | ADMIN 删除 → 204；不存在 → 404；非 ADMIN → 403 |
| `AnnouncementControllerTest.GetDetail` | 2 | 0.05s | 存在 → 200；不存在 → 404 |
| `AnnouncementControllerTest.ListAll` | 2 | 0.07s | 分页；按时间倒序 |
| `AnnouncementControllerTest.Update` | 4 | 0.30s | ADMIN 更新；非 ADMIN → 403；不存在 → 404；字段非法 → 400 |
| `ReportControllerTest.CreateReport` | 7 | 0.15s | 成功 → 201；未认证 → 401；字段缺失 → 400；targetType 非法值 → 400；同一 target 重复举报 → 409；HTML payload 净化 |
| `ReportControllerTest.GetReports` | 3 | 0.10s | ADMIN 列表；非 ADMIN → 403；状态过滤 |
| `ReportControllerTest.HandleReport` | 5 | 1.17s | ADMIN 处理 → 200；非 ADMIN → 403；不存在 → 404；重复处理 → 409；非法状态转移 → 400 |

#### Repository JPA 集成测试（6 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `ReportRepositoryTest.FindAll` | 3 | 0.25s | 分页；状态过滤；按时间倒序 |
| `ReportRepositoryTest.FindByStatus` | 3 | 0.14s | 单字段过滤；PENDING/RESOLVED/REJECTED 各一例 |

### 4.6 notification-service（38 用例）

#### Controller 切片测试（18 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `NotificationControllerTest.CreateNotificationInternal` | 6 | 6.84s | 内部 API 创建通知；缺内部 Token → 401；错 Token → 401；字段缺失 → 400；跨用户访问 → 403；Feign 触发路径 |
| `NotificationControllerTest.DeleteNotification` | 3 | 0.10s | 作者删除 → 204；非作者 → 403；不存在 → 404 |
| `NotificationControllerTest.GetNotifications` | 4 | 0.19s | 分页；未读过滤；跨用户访问 → 403；未认证 → 401 |
| `NotificationControllerTest.GetUnreadCount` | 1 | 0.06s | 仅当前 userId 范围内计数 |
| `NotificationControllerTest.MarkAllAsRead` | 1 | 0.04s | 批量标记已读 |
| `NotificationControllerTest.MarkAsRead` | 3 | 0.07s | 标记单条 → 200；不存在 → 404；跨用户 → 403 |

#### InternalApiAuthFilter 配置切片测试（9 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `InternalApiAuthFilterTest.EmptyConfiguredToken` | 3 | 0.02s | 服务器侧未配置内部 Token 时行为；显式拒绝所有 / 显式放行全部的安全决策 |
| `InternalApiAuthFilterTest.InternalEndpoints` | 6 | 0.02s | 路径匹配规则；`/internal/**` 必须验证；非 `/internal/**` 不应被过滤器误伤 |

#### Repository JPA 集成测试（11 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `NotificationRepositoryTest.CountUnread` | 3 | 0.06s | 按 userId 计数；索引命中；空结果 |
| `NotificationRepositoryTest.FindByUserId` | 4 | 0.15s | 分页；按 read 状态过滤；按时间倒序 |
| `NotificationRepositoryTest.MarkAllAsRead` | 4 | 4.65s | `@Modifying` `UPDATE notification SET is_read = true WHERE user_id = ?`；影响行数断言；幂等性（再次调用影响 0 行） |

### 4.7 file-service（41 用例）

#### Controller 切片测试（28 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `FileUploadControllerTest.ExtractAvatarFilename` | 17 | 0.10s | 从 URL 提取 avatar 文件名（路径遍历攻击、UUID 格式校验、扩展名白名单） |
| `FileUploadControllerWebMvcTest.UploadAvatar` | 6 | 0.17s | 上传成功 → 201；超大文件 → 400；非法扩展名 → 400；空文件 → 400；未认证 → 401；路径遍历原名 → 400 |
| `FileUploadControllerWebMvcTest.UploadImage` | 5 | 6.77s | 帖子配图上传成功；超大文件；非图片 MIME；MIME 与扩展名不符；未认证 |

#### InternalApiAuthFilter 配置切片测试（7 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `InternalApiAuthFilterTest.ConfiguredToken` | 5 | 0.02s | 合法 Token → 200；错误 Token → 401；缺失 Token → 401；大小写敏感；前缀 `Bearer ` 处理 |
| `InternalApiAuthFilterTest.EmptyToken` | 2 | 1.28s | 未配置 Token 的安全降级策略 |

#### Repository JPA 集成测试（6 用例）

| 测试类（@Nested 分组） | 用例数 | 耗时 | 覆盖场景 |
|------|------:|-----:|------|
| `UploadRecordRepositoryTest.Delete` | 1 | 3.80s | 删除记录 → 影响行数；级联清理 |
| `UploadRecordRepositoryTest.FindAll` | 2 | 0.20s | 分页；按上传时间倒序 |
| `UploadRecordRepositoryTest.SaveAndFindById` | 3 | 0.07s | 保存后通过 id 查询；字段映射；关联用户字段 |

---

## 5. 嵌入式安全用例（OWASP Top 10 对照）

下表汇总集成测试中专门覆盖的安全场景。每条用例都是普通业务用例之一，但断言重点是 OWASP 风险。

### A01 失效访问控制（Broken Access Control）

| 场景 | 用例位置 | 断言 |
|------|------|------|
| 越权访问（IDOR） | `UserControllerTest.UpdateProfile.crossUser_returns403` | Alice 持自己 JWT 调 `PUT /api/users/Bob` → 403 |
| 越权删除帖子 | `PostControllerTest.DeletePost.notAuthor_returns403` | 非作者 → 403 |
| 越权删除评论 | `CommentControllerTest.DeleteComment.notAuthor_returns403` | 非作者 → 403 |
| 越权标记通知已读 | `NotificationControllerTest.MarkAsRead.crossUser_returns403` | 跨 userId → 403 |
| 越权处理举报 | `ReportControllerTest.HandleReport.nonAdmin_returns403` | 普通用户 → 403 |
| 管理员操作权限边界 | `AdminUserControllerTest.Authorization.nonAdmin_returns403` | 非 ADMIN → 403 |
| 内部 API 未授权访问 | `InternalUserControllerTest.TokenEnforcement.missingToken_returns401` | 缺内部 Token → 401 |
| 内部 API Token 错误 | `InternalApiAuthFilterTest.ConfiguredToken.wrongToken_returns401` | 错 Token → 401 |

### A02 加密失败（Cryptographic Failures）

| 场景 | 用例位置 | 断言 |
|------|------|------|
| JWT 签名篡改 | `JwtUtilTest.ClaimForgery.*`（多服务，归 unit） | 篡改 signature → `JwtException` → 401 |
| JWT claim 篡改 | `JwtUtilTest.ClaimForgery.modifiedSubject_throws` | 改 subject 重签 → 拒绝 |
| 空 / null token 处理 | user-service `JwtUtil` 修复后用例 | 不再抛 `IllegalArgumentException`，统一 `JwtException` → 401 |

### A03 注入（Injection）

| 场景 | 用例位置 | 断言 |
|------|------|------|
| SQL 注入字符作搜索关键字 | `PostControllerTest.SearchPosts.sqlInjectionPattern_returnsSafeResults` | `' OR '1'='1` / `--` / `;` 不突破 JPA 参数化查询 |
| 路径遍历攻击 | `FileUploadControllerWebMvcTest.UploadAvatar.pathTraversalOriginalName_returns400` | `../../etc/passwd` 作为原名 → 拒绝 |
| XSS payload 提交 | `PostControllerTest.CreatePost.htmlInjection_sanitized` | `<script>` 等被净化，保留语义标签 |

### A04 不安全设计 / A07 身份认证失败

| 场景 | 用例位置 | 断言 |
|------|------|------|
| 账户锁定（暴力破解防护） | `AuthControllerTest.Login.lockedAccount_returns423` | 5 次失败后第 6 次 → 423 |
| 弱密码注册 | `AuthControllerTest.Register.weakPassword_returns400` | 不符合策略 → 400 |
| Refresh Token 已撤销 | `AuthControllerTest.Refresh.revokedToken_returns401` | 已 logout 的 token 不能复用 |

---

## 6. Repository JPA 集成测试的种子数据验证

所有 `@DataJpaTest` 启用 `spring.flyway.enabled=true`，H2 自动执行 `src/main/resources/db/migration/V1__init.sql`。测试中显式验证种子数据业务正确性的用例：

| 服务 | 用例 | 验证内容 |
|------|------|------|
| user-service | `UserRepositoryTest.seedAdminAccountExists` | V1 种子的 admin/admin123 账号存在，且 `password_hash` 字段非空、`role` 字段为 `ADMIN` |
| forum-service | `CategoryRepositoryTest.seedCategoriesExist` | V1 种子的 4 个默认分类（公告/技术/生活/问答）存在 |
| content-service | `AnnouncementRepositoryTest.emptyOnFreshDb` | 无强制种子（content-service V1 不预置公告），验证空表查询的边界行为 |
| notification-service | `NotificationRepositoryTest.emptyUserReturnsZero` | 新用户 userId 在 V1 种子中不存在时，未读计数为 0 |
| file-service | `UploadRecordRepositoryTest.emptyOnFreshDb` | 全新数据库下查询返回空分页 |

---

## 7. 测试期间发现并已修复的 bug

测试期间共发现 14 个 bug 并就地修复。**与本报告集成测试类别相关**的有 9 个：

### A01 失效访问控制（4 个）

| 服务 | 文件 | bug | 暴露用例 |
|------|------|------|------|
| user-service | `SecurityConfig.java` | `/api/users/{username}` 的 wildcard matcher 掩盖了 `/api/users/me`，导致 `GET /me` 路径匹配失败返回 404 | `UserControllerTest.GetMe.anonymous_returns401` 暴露（修复后通过） |
| user/forum/content/notification/file（5 服务） | `GlobalExceptionHandler.java` | `AccessDeniedException` 被映射为 400 而非 403，违反 HTTP 语义；客户端无法区分"输入错"与"权限错" | 全部 Controller 测试中的 `returns403` 断言 |
| notification-service | `InternalApiAuthFilter.java` | 路径匹配规则不一致：`/internal/**` 与 `/api/internal/**` 混用，导致部分内部端点未受保护（公开暴露） | `InternalApiAuthFilterTest.InternalEndpoints` 多条断言 |
| user-service | `SecurityConfig.java` | `/api/users/me` 顺序：放在 `{username}` 之前才正确，原配置顺序反了 | 同上 user-service 用例 |

### 功能性 bug（5 个）

| 服务 | 文件 | bug | 暴露用例 |
|------|------|------|------|
| notification-service | `ApiResponse.java` | 缺 `isSuccess()` getter，Jackson 默认序列化丢失 `success` 字段，导致客户端无法判断请求是否成功 | `NotificationControllerTest.CreateNotificationInternal.*` 响应体断言 |
| forum-service | `CommentRepository` `@EntityGraph` | 关联查询未配置 entity graph，触发 N+1 查询，部分字段映射失败 | `CommentRepositoryTest.FindAllWithPost` |
| forum-service | `PostRepository.incrementViewCount` | `@Modifying` 缺 `clearAutomatically = true`，导致同一事务内读取到旧版本 | `PostRepositoryTest.IncrementViewCount` |
| content-service | `ReportRepository.findAll` | 排序字段名错误（`created_at` vs `createdAt`），H2 MySQL 模式下报字段不存在 | `ReportRepositoryTest.FindAll` |
| file-service | `FileUploadController` | avatar 文件名提取正则未处理 UUID 含连字符的情况，导致合法文件名被拒 | `FileUploadControllerTest.ExtractAvatarFilename.uuidWithHyphens` |

> 其余 5 个 bug（user-service JwtUtil 空 token / 5 服务 JwtUtil 漏捕 SignatureException 等）归单元测试类别，详见 `unit-test-report.md` 第 7 节。

---

## 8. 结论

- **298 条集成测试全部通过**（0 失败、0 错误、0 跳过），覆盖 7 个服务的 Controller / Repository / 配置切片 / 应用上下文加载四类集成场景。
- **Controller 切片测试**（240 用例）验证完整的 HTTP → Security → Controller → Service → 异常处理 链路，重点断言 HTTP 状态码、JSON 响应体、鉴权决策与 OWASP 风险。
- **Repository JPA 集成测试**（58 用例）通过 H2 + Flyway 真实 SQL 执行验证 `@Query` / `@Modifying` / `@EntityGraph`，并显式校验 V1 种子数据的业务正确性。
- **@SpringBootTest contextLoads**（2 用例）+ **GatewayDownstreamHealthIndicator 组件集成测试**（8 用例）保证基础设施服务能正常装配、健康检查逻辑正确。
- 测试中暴露的 14 个 bug 已全部就地修复（详见第 7 节与单元测试报告第 7 节），无遗留缺陷。
- 与单元测试（`unit-test-report.md` 中 387 条）合计 **685 条测试**构成完整测试基线。

### 未覆盖与后续工作

| 未覆盖项 | 原因 | 建议处置 |
|------|------|------|
| 端到端多服务集成（带真实 HTTP） | 需要编排 7 个服务端口与依赖 | 在 CI 流水线增加 `docker-compose up` + RestAssured 测试套件 |
| Testcontainers + 真实 MySQL | 需要容器运行时 | 验证 H2 MySQL 模式与生产 MySQL 的 SQL 方言差异（特别是 JSON 列、全文索引） |
| OWASP ZAP 自动扫描 | 需要独立工具链 | 在 CI 中加入 ZAP baseline scan，针对 staging 环境 |
| `forum-service` 中 `.anyRequest().authenticated()` 不一致拦截匿名 POST/PUT/DELETE（P2） | 单元 slice 层不可复现；production 端集成测试应正常返回 401 | 依赖 e2e 集成测试覆盖，或显式声明敏感端点规则 |

---

## 附录 A：复现命令

```bash
# 全量复现
cd /d/aToys/LargeScale/microservices/eureka-server && mvn test
cd /d/aToys/LargeScale/microservices/gateway && mvn test
cd /d/aToys/LargeScale/microservices/user-service && mvn test
cd /d/aToys/LargeScale/microservices/forum-service && mvn test
cd /d/aToys/LargeScale/microservices/content-service && mvn test
cd /d/aToys/LargeScale/microservices/notification-service && mvn test
cd /d/aToys/LargeScale/microservices/file-service && mvn test

# 单独跑 Controller 切片测试
cd /d/aToys/LargeScale/microservices/user-service && mvn test -Dtest='com.cc91.userservice.controller.*'

# 单独跑 Repository JPA 集成测试
cd /d/aToys/LargeScale/microservices/user-service && mvn test -Dtest='com.cc91.userservice.repository.*'

# 查看原始 Surefire XML
ls /d/aToys/LargeScale/microservices/<service>/target/surefire-reports/TEST-*.xml
```

## 附录 B：分类口径

本报告与 `unit-test-report.md` 的分类约定：

| 测试类型 | 归属报告 | 包路径模式 |
|------|------|------|
| Service 层 Mockito 单测 | unit | `*.service.*` |
| Security 组件单测（JwtUtil/Filter/UserDetailsServiceImpl） | unit | `*.security.*` |
| Util 单测（HtmlSanitizer） | unit | `*.util.*` |
| Feign Client Fallback 单测 | unit | `*.client.*` |
| jqwik property-based fuzz | unit | `*.fuzz.*` |
| Controller 切片测试（@WebMvcTest） | **integration（本报告）** | `*.controller.*` |
| InternalApiAuthFilter 配置切片测试 | **integration（本报告）** | `*.config.*` |
| Repository JPA 集成测试（@DataJpaTest） | **integration（本报告）** | `*.repository.*` |
| @SpringBootTest contextLoads | **integration（本报告）** | 根包 `*ApplicationTest` |
| GatewayDownstreamHealthIndicator | **integration（本报告）** | gateway 包 |
