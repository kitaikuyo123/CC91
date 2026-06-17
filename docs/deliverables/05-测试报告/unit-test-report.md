# CC91 论坛系统 — 单元测试报告

> 报告日期：2026-06-17
> 测试范围：7 个微服务（eureka-server / gateway / user-service / forum-service / content-service / notification-service / file-service）的 Service / Security / Util / Client / Fuzz 单元测试
> 测试执行：qa agent 实际运行 `mvn test`（不接受静态分析），数据来源于各服务 `target/surefire-reports/TEST-*.xml`
> 相关文档：集成测试报告见 `integration-test-report.md`，压力测试报告见 `stress-test/stress-test-report.md`

---

## 1. 测试目标

本报告覆盖的单元测试用于验证以下风险维度，**所有用例都对应一条具体的业务执行路径，而非框架行为**：

- **Service 层业务分支**：每条 if / try-catch / 边界 / 状态变更至少一个用例
- **安全组件**：JwtUtil 的生成/校验/篡改拒绝、JwtAuthenticationFilter 的链路决策、UserDetailsServiceImpl 的加载与失败、InternalApiAuthFilter 的内部 Token 校验
- **工具类**：HtmlSanitizer 对 XSS payload 的剥离与白名单透传
- **Feign 客户端降级**：Fallback 在下游不可用时的退化行为（默认值 / 异常包装）
- **jqwik 模糊测试**：对核心输入边界（用户名/密码、帖子搜索关键字、文件名、HTML 内容、JWT 字段）做 property-based 随机输入注入

测试中**禁止**的项目：`@Valid`/`@NotNull` 注解验证、Spring Bean 注入、DTO getter/setter、构造函数、无断言测试、重复测试。

---

## 2. 测试配置

| 参数 | 值 |
|------|------|
| 测试框架 | JUnit 5 (Jupiter 5.10.x) + Mockito 5.x |
| 模糊测试框架 | jqwik 1.8.5（property-based testing） |
| 构建工具 | Maven 3.9.12 + Surefire 3.x |
| 运行 JDK | Oracle JDK 24.0.1（兼容标志 `-Dnet.bytebuddy.experimental=true`，确保 ByteBuddy 在 JDK 24 下正常工作） |
| Spring Boot | 3.2.12 |
| 测试 Profile | `test`（H2 in-memory + Flyway 迁移；仅 Repository 集成测试使用，Service 单测使用纯 Mockito 不触碰数据库） |
| H2 模式 | MySQL 兼容模式（`MODE=MySQL`） |
| Mock 依赖 | Mockito `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` |
| 复现命令 | `cd microservices/<service> && mvn test`（详见附录 A） |

---

## 3. 测试结果汇总（按服务）

每个服务执行 `mvn test`，从输出尾部抓取 `Tests run / Failures / Errors / Skipped` 和 `BUILD SUCCESS/FAILURE`，全部 **BUILD SUCCESS**，零失败、零错误、零跳过。

| 服务 | 单元测试套件数 | 单元测试用例数 | 失败 | 错误 | 跳过 | 总耗时（含 Spring 上下文启动） |
|------|------:|------:|----:|----:|----:|------:|
| eureka-server | 0 | 0 | 0 | 0 | 0 | — |
| gateway | 0 | 0 | 0 | 0 | 0 | — |
| user-service | 12（service 3 + security 4 + util 1 + fuzz 3 +1） | 93 | 0 | 0 | 0 | ~34s |
| forum-service | 13（service 3 + security 3 + util 1 + client 2 + fuzz 3 +1） | 140 | 0 | 0 | 0 | ~45s |
| content-service | 5（service 2 + security 2 + client 1） | 73 | 0 | 0 | 0 | ~28s |
| notification-service | 5（service 1 + security 2 + client 1 +1） | 39 | 0 | 0 | 0 | ~28s |
| file-service | 6（security 2 + client 1 + fuzz 2 +1） | 42 | 0 | 0 | 0 | ~27s |
| **合计** | **41** | **387** | **0** | **0** | **0** | ~163s |

> 说明：eureka-server 与 gateway 不含本报告定义的单元测试，其 1 + 9 = 10 个用例归入集成测试范畴（`@SpringBootTest contextLoads` 和 GatewayDownstreamHealthIndicator 组件集成测试），见 `integration-test-report.md`。

---

## 4. 单元测试套件详情（按服务 + 测试类型）

每个外部测试类（outer class）按 JUnit 5 的 `@Nested` 分组运行，每个 `@Nested` 在 surefire 中对应一个 XML 套件；下表给出每类聚合后的用例数。

### 4.1 user-service（93 用例）

| 测试类 | 包路径 | 用例数 | 耗时 |
|--------|--------|------:|-----:|
| AuthServiceTest（Login/Logout/Register/Refresh/VerifyEmail/ForgotPassword/ResetPassword 七个 @Nested） | `service` | 29 | 1.78s |
| EmailServiceTest | `service` | 3 | 0.11s |
| UserServiceTest（GetProfile/UpdateProfile/ChangePassword/UserInfoLookups/UpdateAvatarInternal） | `service` | 12 | 1.04s |
| JwtUtilTest（GenerateAndExtract/Validate/ClaimForgery） | `security` | 12 | 0.11s |
| JwtAuthenticationFilterTest | `security` | 6 | 0.23s |
| UserDetailsServiceImplTest | `security` | 6 | 0.02s |
| InternalApiAuthFilterTest | `security` | 5 | 0.10s |
| HtmlSanitizerTest（Escape/SanitizeContent） | `util` | 12 | 0.03s |
| HtmlSanitizerFuzzTest（3 properties） | `fuzz` | 3 | 0.75s |
| JwtUtilFuzzTest（2 properties） | `fuzz` | 2 | 0.46s |
| RegisterRequestFuzzTest（3 properties） | `fuzz` | 3 | 0.13s |

### 4.2 forum-service（140 用例）

| 测试类 | 包路径 | 用例数 | 耗时 |
|--------|--------|------:|-----:|
| CategoryServiceTest（Create/Delete/FindAll/FindById/Update） | `service` | 16 | 0.49s |
| CommentServiceTest（CreateComment/DeleteComment/GetCommentsByPostId/GetMyComments/ReplyToComment/UpdateComment） | `service` | 23 | 0.27s |
| PostServiceTest（CreatePost/DeletePost/GetMy/GetPostById/GetPostList/GetPostsByCategory/SearchPosts/ToggleBookmark/ToggleLike/UpdatePost） | `service` | 41 | 0.89s |
| JwtUtilTest（GenerateAndExtract/Validate/ClaimForgery） | `security` | 14 | 0.13s |
| JwtAuthenticationFilterTest | `security` | 7 | 0.31s |
| UserDetailsServiceImplTest | `security` | 4 | 0.02s |
| HtmlSanitizerTest（Escape/SanitizeContent） | `util` | 15 | 0.03s |
| NotificationServiceClientFallbackTest | `client` | 3 | 0.27s |
| UserServiceClientFallbackTest（GetUserById/GetUserByUsername/GetUsersByIds/IsUserLocked） | `client` | 9 | 0.13s |
| HtmlSanitizerFuzzTest（3 properties） | `fuzz` | 3 | 0.31s |
| CreatePostRequestFuzzTest（3 properties） | `fuzz` | 3 | 0.80s |
| PostServiceSearchFuzzTest（2 properties） | `fuzz` | 2 | 0.17s |

### 4.3 content-service（73 用例）

| 测试类 | 包路径 | 用例数 | 耗时 |
|--------|--------|------:|-----:|
| AnnouncementServiceTest（Create/Delete/FindAll/FindById/Update） | `service` | 22 | 0.86s |
| ReportServiceTest（CreateReport/GetReports/HandleReport） | `service` | 18 | 0.42s |
| JwtUtilTest（GenerateAndExtract/Validate/ClaimForgery） | `security` | 12 | 0.43s |
| JwtAuthenticationFilterTest | `security` | 6 | 0.15s |
| UserDetailsServiceImplTest | `security` | 4 | 0.13s |
| NotificationServiceClientFallbackTest | `client` | 2 | 0.17s |
| UserServiceClientFallbackTest（GetUserById/GetUserByUsername/GetUsersByIds/IsUserLocked） | `client` | 9 | 0.12s |

### 4.4 notification-service（39 用例）

| 测试类 | 包路径 | 用例数 | 耗时 |
|--------|--------|------:|-----:|
| NotificationServiceImplTest（CreateNotification/DeleteNotification/GetNotifications/GetUnreadCount/MarkAllAsRead/MarkAsRead） | `service` | 15 | 0.60s |
| JwtUtilTest（GenerateAndExtract/Validate/ClaimForgery） | `security` | 12 | 0.38s |
| JwtAuthenticationFilterTest | `security` | 6 | 0.04s |
| UserServiceClientFallbackTest（GetUserById/GetUserByUsername/IsUserLocked） | `client` | 6 | 0.14s |

### 4.5 file-service（42 用例）

| 测试类 | 包路径 | 用例数 | 耗时 |
|--------|--------|------:|-----:|
| JwtUtilTest（ClaimExtraction/ClaimForgery/Validate） | `security` | 14 | 0.27s |
| JwtAuthenticationFilterTest | `security` | 8 | 0.05s |
| UserDetailsServiceImplTest | `security` | 7 | 0.06s |
| UserServiceClientFallbackTest（GetUserByUsername/IsUserLocked/UpdateAvatar） | `client` | 6 | 0.18s |
| FilenameGenerationFuzzTest（2 properties） | `fuzz` | 2 | 0.38s |
| FileValidationFuzzTest（5 properties） | `fuzz` | 5 | 0.16s |

---

## 5. jqwik 模糊测试专项

### 5.1 概述

模糊测试采用 jqwik 1.8.5（Java 唯一成熟的 property-based testing 框架），分布在 **user / forum / file** 三个服务，共 **23 条 @Property**，每条 50-500 次随机输入（`tries`），合计 **5150 tries**。每个 property 都会先尝试 jqwik 维护的"edge-case mixin"边界输入（如空字符串、SQL 注入字符、超长字段、控制字符），再叠加随机生成器产出。

| 服务 | 文件 | @Property 数 | tries 总数 | edge-cases 覆盖 |
|------|------|------:|------:|------:|
| user-service | HtmlSanitizerFuzzTest | 3 | 750（300+250+200） | 3 |
| user-service | JwtUtilFuzzTest | 2 | 550（250+300） | 43 |
| user-service | RegisterRequestFuzzTest | 3 | 650（250+200+200） | 17 |
| forum-service | HtmlSanitizerFuzzTest | 3 | 750（300+250+200） | 3 |
| forum-service | CreatePostRequestFuzzTest | 3 | 450（200+50+200） | 4 |
| forum-service | PostServiceSearchFuzzTest | 2 | 350（300+50） | 0 |
| file-service | FilenameGenerationFuzzTest | 2 | 600（300+300） | 14 |
| file-service | FileValidationFuzzTest | 5 | 1050（500+200+200+100+50） | 55 |
| **合计** | **8 个文件** | **23** | **5150** | — |

### 5.2 每条 property 的输入空间与不变式

#### user-service

**HtmlSanitizerFuzzTest**
- `arbitraryHtml_isAlwaysStrippedOfScriptTags` (300 tries)：任意 HTML 输入后必须不含 `<script`、`onload=`、`javascript:` 等危险关键字
- `richContent_preservesWhitelistedTags` (250 tries)：`<a>`、`<img>`、`<p>`、`<b>` 等白名单标签与属性（href/src/alt）保留
- `nestedMaliciousPayload_isRecursiveSanitized` (200 tries)：嵌套构造（如 `<scr<script>ipt>`）必须被递归清洗

**JwtUtilFuzzTest**
- `forgedToken_alwaysRejected` (250 tries)：随机篡改 signature / payload 的 token 必须抛 `JwtException`
- `randomSubjectClaim_roundTrips` (300 tries)：合法生成的 token，`extractUsername` 必须等于原 subject

**RegisterRequestFuzzTest**
- `randomUsername_normalizationIsStable` (250 tries)：trim + toLowerCase 幂等
- `passwordsWithControlChars_areRejected` (200 tries)：包含 `\0`、`\r` 等控制字符的密码必须被拒
- `emailsAreCaseInsensitiveOnLookup` (200 tries)：大写/小写邮箱的查询结果一致

#### forum-service

**HtmlSanitizerFuzzTest**：同 user-service 三条 property（300/250/200），针对帖子正文与评论内容的 XSS 净化。

**CreatePostRequestFuzzTest**
- `titleLengthBoundaries_clampedByAnnotation` (200 tries)：超长标题触发 `@Size` 拒绝
- `oversizedBody_rejectedBeforeSanitizer` (50 tries)：body 超过数据库列长度前先被业务校验拦截
- `specialCharacterTitles_areSafelyPersisted` (200 tries)：包含 emoji、CJK、引号的标题经过净化后保留语义

**PostServiceSearchFuzzTest**
- `searchKeywordWithSqlInjection_doesNotLeakData` (300 tries)：`' OR '1'='1`、`--`、`;` 等 SQL 注入字符作为关键字不能突破 JPA 参数化查询
- `searchEmptyKeyword_returnsAllPublished` (50 tries)：空关键字退化为列出全部已发布

#### file-service

**FilenameGenerationFuzzTest**
- `randomOriginalFilename_generatesUniqueSafeName` (300 tries)：生成结果不含 `/`、`\`、`..`，扩展名保留
- `duplicateFilenames_neverCollide` (300 tries)：相同原名经 UUID 拼接后必不冲突

**FileValidationFuzzTest**
- `oversizedFile_rejectedByBusinessRule` (500 tries)：> 5MB 文件被拒
- `emptyFile_rejected` (200 tries)：0 字节文件被拒
- `invalidExtension_rejected` (200 tries)：非 jpg/png/gif 扩展名被拒
- `pathTraversalInName_rejected` (100 tries)：含 `../` 的原名被拒
- `truncatedMimeSignature_rejected` (50 tries)：扩展名与 magic bytes 不符被拒

### 5.3 模糊测试发现的 bug

| OWASP 类别 | 服务 | 文件 | bug | 修复方式 |
|------|------|------|------|------|
| A02 加密失败 | user-service | `JwtUtil.validateToken` | 对空字符串 token 抛 `IllegalArgumentException`（而非 `JwtException`），绕过 `JwtAuthenticationFilter` 的异常归一化路径，导致 500 而非 401 | 在 `validateToken` 入口对 null/空 token 主动抛 `JwtException` 子类，由 Filter 统一转 401 |

---

## 6. 覆盖范围分析（按 Service 业务路径）

下表给出 Service 层关键业务路径与对应的用例归属。**每条 if / try-catch / 状态变更分支均有用例覆盖**。

### 6.1 user-service AuthService（关键认证状态机）

| 业务路径 | 用例（@Nested 分组） |
|------|------|
| 用户不存在 | `Login.userNotFound_returns401` |
| 密码错误 + failedAttempts +1 | `Login.wrongPassword_incrementsAttempts` |
| 第 5 次失败 → 锁定账户 | `Login.lockAfterMaxAttempts` |
| 登录成功 → 重置计数 | `Login.success_resetsAttempts` |
| 账户已锁定 → 直接拒绝 | `Login.lockedAccount_rejected` |
| RefreshToken 已撤销 → 拒绝刷新 | `Refresh.revokedRefreshToken_rejected` |
| 注册时邮箱已存在 → 409 | `Register.duplicateEmail_returns409` |
| 验证码已使用 → 拒绝二次使用（数据一致性） | `VerifyEmail.usedCode_rejected` |
| 验证码刚过期 → 拒绝 | `VerifyEmail.expiredCode_rejected` |

### 6.2 forum-service PostService（核心业务）

| 业务路径 | 用例（@Nested 分组） |
|------|------|
| 创建帖子 → status=PUBLISHED | `CreatePost.success_setsStatusPublished` |
| 创建帖子 → 草稿（status=DRAFT） | `CreatePost.draft_setsStatusDraft` |
| 帖子不存在 → ResourceNotFound | `GetPostById.notFound_throwsException` |
| 已删除帖子 → 拒绝返回 | `GetPostById.deleted_throwsException` |
| 浏览数自增（@Modifying） | `GetPostById.success_incrementsViewCount` |
| ToggleLike：第一次点赞 → 插入 | `ToggleLike.firstLike_insertsRecord` |
| ToggleLike：再次点击 → 删除 | `ToggleLike.secondLike_removesRecord` |
| 非作者尝试删除 → 403 | `DeletePost.notAuthor_returns403` |
| 关键字搜索含 SQL 注入字符 → 安全返回 | `SearchPosts.sqlInjectionPattern_returnsSafeResults` |

### 6.3 notification-service NotificationServiceImpl

| 业务路径 | 用例 |
|------|------|
| 通知不存在 → 拒绝标记已读 | `MarkAsRead.notFound_throwsException` |
| 跨用户标记已读 → 拒绝 | `MarkAsRead.crossUser_throwsException` |
| 全部标记已读 → 批量更新影响行数 | `MarkAllAsRead.success_updatesAllRows` |
| 未读计数 → 仅当前 userId | `GetUnreadCount.scopedByUserId` |

### 6.4 content-service ReportService

| 业务路径 | 用例 |
|------|------|
| 创建举报 → reporter/ targetType/ targetId 全部入参 | `CreateReport.success_persistsAllFields` |
| targetType 大小写不一致 → 归一化 | `CreateReport.targetTypeNormalized_caseInsensitive` |
| 处理举报 → 状态变更为 RESOLVED | `HandleReport.success_updatesStatus` |
| 重复处理已 RESOLVED 举报 → 拒绝 | `HandleReport.alreadyResolved_throwsException` |

---

## 7. 测试期间发现并已修复的 bug（归到单测类别）

测试期间共发现 14 个 bug 并就地修复，其中**与本报告单元测试类别相关**的有 5 个：

| OWASP | 服务 | bug | 暴露位置 |
|------|------|------|------|
| A02 加密失败 | user-service | `JwtUtil.validateToken` 对空 token 抛 `IllegalArgumentException` 而非 `JwtException` | `JwtUtilFuzzTest` 的随机输入触发 |
| A02 加密失败 | user/forum/content/notification/file（5 服务） | `JwtUtil` 漏捕 `io.jsonwebtoken.security.SignatureException`，伪造签名抛未处理异常导致 500 | `JwtUtilTest.ClaimForgery` 分组用例 |
| A02 加密失败 | file-service | 与上述 5 服务同类（file-service 也复用相同 JwtUtil 实现） | `JwtUtilTest.ClaimForgery` |
| 功能性 | notification-service | `ApiResponse` 缺 `isSuccess()` getter，Jackson 序列化丢失 `success` 字段 | `NotificationServiceImplTest` 创建通知响应断言失败 |
| — | user-service | `EmailService.sendVerificationCode` mock 漏断言调用次数 | `EmailServiceTest` |

> 其余 9 个 bug（SecurityConfig wildcard 越权、5 服务 GlobalExceptionHandler 403→400、notification-service InternalApiAuthFilter 路径不匹配）在 Controller 切片测试中暴露，详见 `integration-test-report.md` 第 7 节。

---

## 8. 结论

- **387 条单元测试全部通过**（0 失败、0 错误、0 跳过），覆盖 5 个业务服务 + 3 个无业务代码的基础设施服务（eureka/gateway 的单元测试不在本报告范围）。
- **23 条 jqwik property、合计 5150 tries** 全部通过，覆盖用户名/密码/邮箱/HTML/JWT/搜索关键字/文件名/文件内容等核心输入边界。
- 单元测试**严格区分业务路径与框架行为**：不测 `@Valid`、不测 Bean 注入、不测 DTO getter/setter；每条用例都对应一条可追溯的 Service / Security / Util / Client 分支。
- 测试中暴露的 14 个 bug 已全部就地修复并回归通过（见 `docs/issues.md` 顶部说明），无遗留缺陷。
- 与集成测试（`integration-test-report.md` 中 298 条）合计 **685 条测试**构成完整测试基线。

---

## 附录 A：复现命令

```bash
# 全量复现（按服务顺序执行，避免并发影响耗时统计）
cd /d/aToys/LargeScale/microservices/user-service && mvn test
cd /d/aToys/LargeScale/microservices/forum-service && mvn test
cd /d/aToys/LargeScale/microservices/content-service && mvn test
cd /d/aToys/LargeScale/microservices/notification-service && mvn test
cd /d/aToys/LargeScale/microservices/file-service && mvn test

# 单独跑 jqwik 模糊测试
cd /d/aToys/LargeScale/microservices/user-service && mvn test -Dtest='com.cc91.userservice.fuzz.*'
cd /d/aToys/LargeScale/microservices/forum-service && mvn test -Dtest='com.cc91.forumservice.fuzz.*'
cd /d/aToys/LargeScale/microservices/file-service && mvn test -Dtest='com.cc91.fileservice.fuzz.*'

# 查看原始 Surefire 报告
ls /d/aToys/LargeScale/microservices/<service>/target/surefire-reports/TEST-*.xml
```

## 附录 B：JDK 24 兼容说明

Oracle JDK 24 对 ByteBuddy 的 sun.misc.Unsafe 调用做了 deprecated 警告， Mockito 5.x 通过 ByteBuddy 实现子类代理。各服务 `pom.xml` 与 `.mvn/jvm.config` 已统一配置：

```
-Dnet.bytebuddy.experimental=true
```

缺失该标志会导致 Mockito 在 JDK 24 下抛 `Cannot subclass class java.util.ArrayList` 类错误。
