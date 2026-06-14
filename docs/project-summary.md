# CC91 校园论坛系统 — 项目总结报告

> **项目周期**：2026 年 3 月 – 2026 年 6 月  
> **团队规模**：15 人  
> **代码提交**：170+ commits  
> **版本**：v2.0（微服务架构）

---

## 一、项目概述

CC91 是一个面向校园场景的现代化论坛系统，采用 Spring Boot 微服务架构 + React 19 前端，支持用户认证、帖子管理、评论互动、通知推送、管理后台等完整功能闭环。

经过多轮迭代，项目从单体架构演进为 **7 个微服务 + API 网关 + 服务注册中心** 的分布式系统，并通过 Docker Compose 实现一键部署。

---

## 二、已实现的功能

### 2.1 核心功能矩阵

| 模块 | 功能 | 状态 |
|------|------|------|
| **用户系统** | 注册 / 登录 / 退出 / 邮箱验证 / 密码重置 | ✅ |
| | JWT 双 Token 机制（Access + Refresh） | ✅ |
| | 账户锁定（5 次失败锁定 30 分钟） | ✅ |
| | 个人资料编辑、头像上传 | ✅ |
| **论坛核心** | 版块分类浏览 | ✅ |
| | 发帖（支持草稿/发布双状态） | ✅ |
| | 帖子编辑、删除（权限控制） | ✅ |
| | 评论 + 多级嵌套回复 | ✅ |
| | 点赞 / 收藏（toggle 机制） | ✅ |
| | 全文搜索 | ✅ |
| **通知系统** | 评论/回复实时通知 | ✅ |
| | 通知已读/全部已读 | ✅ |
| | 未读数角标 | ✅ |
| **管理后台** | 用户管理（封禁/解封） | ✅ |
| | 分类管理（增删改排序） | ✅ |
| | 公告管理（发布/置顶） | ✅ |
| | 内容审核（举报处理） | ✅ |
| **安全性** | bcrypt 密码哈希 | ✅ |
| | XSS 双层防护（DOMPurify + HtmlSanitizer） | ✅ |
| | RBAC 权限控制 | ✅ |
| | JWT 无状态认证 | ✅ |
| | 文件上传多层校验 | ✅ |

### 2.2 页面与路由

| 路径 | 页面 | 认证 |
|------|------|------|
| `/` | 首页（公告 + 版块 + 最新帖子 + 热门） | 否 |
| `/login` | 登录 | 否 |
| `/register` | 注册 | 否 |
| `/category/:id` | 版块帖子列表 | 否 |
| `/posts/:id` | 帖子详情 + 评论 | 否 |
| `/search` | 搜索结果 | 否 |
| `/profile/:username` | 个人主页 | 否 |
| `/profile/edit` | 编辑资料 | 是 |
| `/notifications` | 通知列表 | 是 |
| `/admin/*` | 管理后台 | 管理员 |
| `/announcements/:id` | 公告详情 | 否 |

---

## 三、掌握的关键技术

### 3.1 技术栈总览

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 3.2.12 | Java 17, Maven |
| 微服务治理 | Spring Cloud 2023.0.0 | Eureka + Gateway + OpenFeign |
| 安全框架 | Spring Security + JWT (jjwt 0.12.3) | 无状态认证 + RBAC |
| 数据库 | MySQL 8.0.46 | Flyway 9.x 版本化管理 |
| 前端框架 | React 19 + Vite + TypeScript | 函数组件 + Hooks |
| 状态管理 | @tanstack/react-query | 服务端状态缓存 |
| 断路器 | Resilience4j 2.1.0 | 熔断、降级、重试 |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus + Grafana | 全链路指标采集 |
| 容器化 | Docker + Docker Compose | 9 个容器一键部署 |
| 测试 | JUnit 5 + MockMvc + Vitest + RTL | 分层测试 |
| 前端安全 | DOMPurify | XSS 前端层防护 |
| 后端安全 | HtmlSanitizer + BCrypt | XSS 后端层防护 + 密码哈希 |

### 3.2 智能技术应用（Claude Code + MCP Server）

本项目在开发过程中深度应用了 AI 辅助开发技术：

#### 3.2.1 Claude Code 作为开发平台

- **角色**：Claude Code 担任项目 Lead，通过 `.claude/CLAUDE.md` 定义完整的工作协议
- **Subagent 体系**：
  - **Developer Agent**（`.claude/agents/developer.md`）：全栈开发，配备 5 个专业技能（React 最佳实践、Spring Boot 模式、TypeScript 审查、Web 设计规范）
  - **QA Agent**（`.claude/agents/qa.md`）：测试驱动，强制运行测试并输出报告
- **工作流**：Phase 1 理解规划 → Phase 2 开发 → Phase 3 测试 → Phase 4 审查 → Phase 5 收尾

#### 3.2.2 CodeGraph MCP Server

- **用途**：基于 Tree-sitter 的代码知识图谱，提供亚毫秒级符号搜索
- **实际应用**：
  - 代码导航：`codegraph_context` 一键获取任务上下文
  - 调用追踪：`codegraph_trace` 跟踪完整调用链
  - 影响分析：`codegraph_impact` 评估变更影响范围
  - 替代传统 grep/read 循环，大幅提升代码理解效率

#### 3.2.3 技能（Skills）体系

项目配置了 7 个专业 Skills：

| Skill | 用途 |
|-------|------|
| `java-springboot` | Spring Boot 开发最佳实践 |
| `springboot-patterns` | 分层架构、REST API 设计模式 |
| `typescript-react-reviewer` | React 代码审查、反模式识别 |
| `vercel-react-best-practices` | React 性能优化（60+ 条规则） |
| `web-design-guidelines` | Web 界面设计规范 |
| `code-review` | 代码审查（安全 + 代码质量） |
| `verify` | 手动验证代码变更 |

#### 3.2.4 IDE MCP 集成

- **executeCode**：在 Jupyter Kernel 中执行 Python 压力测试脚本
- **getDiagnostics**：获取 VS Code 语言诊断信息

---

## 四、开发流程与团队协作

### 4.1 开发流程

```
需求分析 → 任务拆分 → Developer 实现 → QA 测试 → Lead 审查 → Git 提交
```

### 4.2 团队角色分工

| 角色 | 职责 |
|------|------|
| **Lead**（Claude Code） | 需求分析、任务分配、代码审查、质量把控、工作流调度 |
| **Developer** | 全栈实现（Spring Boot + React）、Bug 修复、构建维护 |
| **QA** | 测试编写与执行、回归验证、测试报告输出 |

### 4.3 协作机制

- **任务驱动**：每个任务包含唯一 ID、背景目标、实现范围、验收标准、依赖关系
- **阻塞升级**：任务阻塞超 30 分钟自动上报，超 2 次未解决则由 Lead 调整策略
- **问题跟踪**：`docs/issues.md` 实时维护未修复 Bug，P0/P1/P2 优先级管理
- **代码审查**：每次提交前通过安全检查、API 设计、错误处理、代码质量、测试质量五大维度审查

### 4.4 Git 工作流

- **分支策略**：`main` → `develop` → `feat/*` / `fix/*`
- **提交规范**：Conventional Commits（`feat:` / `fix:` / `refactor:` / `test:` / `docs:` / `chore:`）
- **PR 流程**：功能分支 → `develop`，通过审查后合并

---

## 五、个人心得体会

### 5.1 成员 A — 后端架构

**角色**：后端微服务拆分与架构设计

**主要挑战**：
1. 从单体到微服务的拆分策略选择——需要在"过度拆分"和"拆分不足"之间找到平衡
2. Spring Cloud 组件版本兼容性问题（Spring Boot 3.2.x vs Spring Cloud 2023.0.x）
3. 中文编码问题——Alpine 容器缺少 locale 导致 UTF-8 乱码，排查了两天才定位到 `-Dfile.encoding=UTF-8`

**解决方法**：
- 采用 DDD 限界上下文方法，按业务域逐步拆分，保留共享数据库作为过渡方案
- 通过 `spring-cloud-dependencies` BOM 统一管理版本，避免手动排依赖冲突
- 为所有 Dockerfile 添加 locale 和 JVM 编码参数，并在 JDBC URL 中使用 `sessionVariables` 强制 utf8mb4

**收获与反思**：微服务不是银弹。对于 15 人以下团队的中小型项目，共享数据库的"适度微服务"比严格的 Database-per-Service 更务实。关键是把服务边界定义清楚，通信机制设计好，剩下的可以渐进演进。

### 5.2 成员 B — 前端开发

**角色**：React 前端架构与组件开发

**主要挑战**：
1. React 19 的并发特性（useTransition、useDeferredValue）在论坛场景下的适用性判断
2. @tanstack/react-query 的缓存策略设计——哪些数据该缓存、缓存多久
3. 前端 mock 模式与真实后端的切换时机——过早 fallback 导致用户体验差

**解决方法**：
- 对高频只读数据（分类列表、公告列表）使用 React Query 默认缓存策略
- 对用户相关数据（通知、收藏）设置较短的 staleTime
- 优化 mock fallback 逻辑：从 1 次 500ms 重试改为 5 次指数退避（总计约 15s），避免后端启动期间误切 mock

**收获与反思**：前端状态管理的关键在于区分"服务端状态"和"客户端状态"。React Query 管理前者，Context/useState 管理后者，两者职责清晰。Mock 模式应该作为最后的兜底，而不是第一个 fallback。

### 5.3 成员 C — 测试与质量保障

**角色**：全栈测试编写与质量保障

**主要挑战**：
1. 如何编写"高价值"测试——区分业务逻辑测试和框架行为测试
2. 集成测试中 Flyway 种子数据与业务逻辑的一致性验证
3. 前端的 navigate 跳转路径断言在 mock 环境下的实现

**解决方法**：
- 制定了"测试质量红线"：禁止测试框架行为（@Valid、Bean 注入、DTO getter/setter），必须测试业务执行路径
- 集成测试使用 H2 内存数据库 + 真实 Flyway 迁移，验证种子数据中的 admin/admin123 能成功登录
- 前端跳转测试使用 `vi.mock('react-router-dom')` mock navigate 函数，并断言完整目标路径

**收获与反思**：测试的价值不在于覆盖率数字，而在于是否覆盖了关键业务分支。38 个通过的前端测试中，真正有防御价值的是登录失败路径（用户不存在、密码错误、账户锁定）和权限拦截测试，而非框架注解验证。

### 5.4 成员 D — DevOps 与部署

**角色**：Docker 容器化、CI/CD、监控部署

**主要挑战**：
1. Alpine 基础镜像的 locale 缺失导致 Java 编码问题
2. MySQL Connector/J 9.3.0 的 charset 协商行为与旧版不兼容
3. 服务启动顺序导致 Gateway 503——Gateway 先就绪但后端服务未注册到 Eureka

**解决方法**：
- 所有 Dockerfile 添加 `apk add tzdata` + `ENV LANG=en_US.UTF-8` + `-Dfile.encoding=UTF-8`
- Connector/J 从 9.3.0 降级到 8.4.0 LTS，JDBC URL 用 `sessionVariables` 直接控制 MySQL 会话字符集
- Gateway 添加 `spring-retry` 依赖 + `loadbalancer.retry.enabled=true` + 5 秒 Eureka 拉取间隔

**收获与反思**：容器化部署的坑往往不在应用代码本身，而在基础设施层。Alpine 的 locale、JDBC 驱动的版本兼容性、Eureka 的注册延迟——这些问题在本地开发时不会暴露，到 Docker 环境才集中爆发。提前做好基础镜像的标准化和版本锁定非常重要。

---

## 六、技术维度深度分析

### 6.1 压力测试

#### 6.1.1 测试方法

使用 Python 3.11 标准库（`concurrent.futures` + `urllib`）编写压测脚本，零第三方依赖。

#### 6.1.2 测试配置

| 参数 | 值 |
|------|------|
| 并发线程 | 50 |
| 持续时间 | 15 秒 / 固定请求数（500-1000） |
| 目标服务 | Gateway (port 9000) |
| 测试脚本 | `docs/stress-test/stress_test.py` |

#### 6.1.3 测试结果

| 场景 | 成功率 | RPS | 平均延迟 | P99 |
|------|--------|-----|---------|-----|
| 只读基准（categories + posts + announcements） | 100% | 33 | 29ms | 70ms |
| 单接口 GET /api/posts | 100% | 33 | 49ms | 109ms |
| 认证登录 POST /api/auth/login | 100% | 33 | 662ms | 745ms |
| 读写混合 80/20 | 100% | 453 | 107ms | 454ms |
| 并发写入 POST /api/posts | 100% | 724 | 58ms | 114ms |
| 帖子详情 + 评论 | 100% | 33 | 202ms | 288ms |

#### 6.1.4 瓶颈与优化方向

- **P0 瓶颈**：BCrypt 密码验证（平均 662ms），可通过登录限流 + Redis Session 缓存优化
- **P1 瓶颈**：帖子详情查询（202ms），因浏览量同步更新 + 评论关联查询，可异步化浏览量更新
- **优化方向**：引入 Redis 缓存、异步消息队列、数据库连接池调优

详见：`docs/stress-test/stress-test-report.md`

---

### 6.2 微服务架构

#### 6.2.1 拆分策略

采用 DDD（领域驱动设计）的限界上下文方法，按业务域拆分：

| 服务 | 端口 | 职责 | 数据表 |
|------|------|------|--------|
| **Eureka Server** | 8761 | 服务注册与发现 | — |
| **API Gateway** | 9000 | 路由转发、负载均衡、重试 | — |
| **User Service** | 8081 | 认证、用户管理、密码重置 | users, user_profiles, refresh_tokens, verification_codes |
| **Forum Service** | 8082 | 帖子、评论、分类、点赞、收藏 | posts, comments, categories, post_likes, bookmarks |
| **Notification Service** | 8083 | 通知管理与推送 | notifications |
| **Content Service** | 8085 | 公告、举报、内容审核 | announcements, reports |
| **File Service** | 8086 | 文件上传与存储 | 文件系统 |

#### 6.2.2 服务间通信

| 调用方 | 被调方 | 方式 | 用途 |
|--------|--------|------|------|
| Forum | User | OpenFeign | 获取帖子/评论作者信息 |
| Forum | Notification | OpenFeign | 评论时创建通知 |
| Content | User | OpenFeign | 公告作者信息 |
| Content | Notification | OpenFeign | 公告发布通知 |

#### 6.2.3 收益分析

| 维度 | 单体 | 微服务 |
|------|------|--------|
| 代码定位 | 98 个文件中搜索 | 10-20 个文件中定位 |
| 修改影响 | 全局回归 | 单服务测试 |
| 独立扩容 | 不可 | 按服务扩缩 |
| 故障隔离 | 单点全局不可用 | 降级后核心功能正常 |

#### 6.2.4 过渡方案

当前采用 **共享数据库模式**（所有服务共用 `cc91_db`，通过表名隔离），作为 DB-per-Service 的过渡形态。未来可逐步拆分为独立数据库，引入 Saga 模式处理分布式事务。

详见：`docs/microservices-design.md`

---

### 6.3 系统监控

#### 6.3.1 监控架构

```
Grafana (可视化)
  ↑
Prometheus (采集 + 存储 + 告警)
  ↑ HTTP pull /actuator/prometheus
Gateway + 6 个微服务 + Eureka
```

#### 6.3.2 工具选型

| 组件 | 选型 | 用途 |
|------|------|------|
| 指标暴露 | Spring Boot Actuator + Micrometer | 每个服务暴露 `/actuator/prometheus` |
| 指标采集 | Prometheus v2.48.0 | 每 15 秒拉取指标 |
| 可视化 | Grafana 10.2.0 | 仪表盘展示 |
| 告警 | Prometheus AlertManager | 5 条告警规则 |

#### 6.3.3 监控指标

| 类别 | 指标 | 用途 |
|------|------|------|
| HTTP | `http_server_requests_seconds_*` | 响应时间、吞吐、错误率 |
| JVM | `jvm_memory_used_bytes` | 堆内存使用 |
| JVM | `jvm_threads_live_threads` | 线程数 |
| 连接池 | `hikaricp_connections_active` | 数据库连接池 |
| 断路器 | `resilience4j_circuitbreaker_*` | 熔断状态 |

#### 6.3.4 告警规则

| 告警 | 条件 | 级别 |
|------|------|------|
| ServiceDown | `up == 0` 持续 1 分钟 | Critical |
| HighErrorRate | 5xx 错误率 > 10% | Warning |
| HighLatency | P95 > 2s | Warning |
| CircuitBreakerOpen | 断路器打开 | Warning |
| HighMemoryUsage | 堆内存 > 85% | Warning |

详见：`docs/monitoring-design.md`

---

### 6.4 弹性设计

#### 6.4.1 已落地的弹性措施

| 措施 | 实现方式 | 效果 |
|------|----------|------|
| **断路器** | Resilience4j CircuitBreaker | 服务不可用时熔断，10s 后半开探测 |
| **重试机制** | Gateway spring-retry + LoadBalancer retry | 服务启动期间自动重试，避免 503 |
| **降级处理** | Feign Fallback | 通知发送失败时记录日志，不影响主流程 |
| **负载均衡** | Spring Cloud LoadBalancer + Eureka | 多实例时自动 Round Robin |
| **连接池隔离** | HikariCP 每服务独立连接池 | 单服务数据库压力不传染其他服务 |
| **容器重启** | Docker `restart: unless-stopped` | 服务异常退出自动恢复 |
| **健康检查** | Spring Boot Actuator + Docker healthcheck | MySQL、Eureka 就绪后才启动下游 |

#### 6.4.2 可引入的进阶弹性策略

| 策略 | 适用场景 | 推荐方案 |
|------|----------|----------|
| **舱壁隔离** | 防止某接口耗尽线程池 | Resilience4j Bulkhead，为不同 API 分配独立线程池 |
| **限流** | 防止暴力破解登录 | Bucket4j 令牌桶，登录接口 10 次/分钟 |
| **服务降级** | 论坛浏览不受通知服务影响 | 通知不可用时返回空列表，不阻塞帖子加载 |
| **异步解耦** | 通知、浏览量更新 | RabbitMQ 消息队列，削峰填谷 |
| **分布式追踪** | 排查跨服务调用延迟 | Micrometer Tracing + Zipkin |

---

### 6.5 安全性保障

#### 6.5.1 安全措施总览（按 OWASP Top 10）

| OWASP | 类别 | 措施 | 状态 |
|-------|------|------|------|
| A01 | 权限控制失效 | RBAC 角色控制 + `@PreAuthorize` + `InternalApiAuthFilter` | ✅ |
| A02 | 加密机制失败 | BCrypt 密码哈希，JWT HMAC-SHA 签名 | ✅ |
| A03 | 注入攻击 | JPA 参数化查询，DOMPurify + HtmlSanitizer 双重 XSS 防护 | ✅ |
| A04 | 不安全设计 | 无状态 JWT，DTO 与 Entity 分离 | ✅ |
| A05 | 安全配置错误 | 环境变量管理密钥，无硬编码，CORS 白名单 | ✅ |
| A06 | 过时组件 | `mvn dependency-check:check` + `npm audit` | ✅ |
| A07 | 认证失败 | 双 Token 机制，账户锁定（5 次/30min），Refresh Token 撤销 | ✅ |
| A08 | 数据完整性 | Flyway 版本化迁移，数据库 schema 可追溯 | ✅ |
| A09 | 日志失败 | 异常全量记录，登录失败 WARN 日志 | ✅ |
| A10 | SSRF | 服务间调用使用内部 API + INTERNAL_TOKEN 认证 | ✅ |

#### 6.5.2 XSS 防护实战

前后端双重净化：
- **前端**：DOMPurify 对 `dangerouslySetInnerHTML` 内容白名单过滤
- **后端**：`HtmlSanitizer.java` 移除 `<script>`、`onXXX` 事件、`javascript:` 协议

#### 6.5.3 文件上传安全

| 防护层 | 措施 |
|--------|------|
| Content-Type | 仅允许 image/jpeg、image/png、image/webp |
| 扩展名 | 白名单 .jpg/.jpeg/.png/.webp |
| 大小限制 | Spring 2MB + Service 层二次校验 |
| 路径遍历 | 过滤 `..`、`/`、`\` 字符 |
| 重命名 | `{userId}_{timestamp}.{ext}` 格式 |

#### 6.5.4 已知风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| CORS 开发配置 | 中 | 生产部署前更新为实际域名 |
| 文件本地存储 | 低 | 单实例 OK，生产建议迁移 OSS/S3 |
| 邮件服务依赖 | 低 | 不影响核心功能，控制台可输出验证码 |

详见：`docs/SECURITY_AUDIT.md`

---

## 七、配套文档索引

### 7.1 核心设计文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 架构设计 | `docs/architecture.md` | 系统架构、数据模型、路由设计 |
| 微服务设计 | `docs/microservices-design.md` | 拆分策略、通信机制、收益分析 |
| 监控设计 | `docs/monitoring-design.md` | Prometheus + Grafana 方案 |
| 安全审查 | `docs/SECURITY_AUDIT.md` | OWASP Top 10 全覆盖审查 |
| 压力测试报告 | `docs/stress-test/stress-test-report.md` | 6 场景性能数据 |
| API 参考 | `docs/api-reference.md` | 接口文档 |
| 环境搭建 | `docs/ENV_SETUP.md` | 本地开发环境指南 |
| 验收指南 | `docs/acceptance-guide.md` | 答辩/验收操作指南 |

### 7.2 智能体配置

| 文件 | 说明 |
|------|------|
| `.claude/CLAUDE.md` | Lead 工作协议，完整 5 阶段工作流 |
| `.claude/agents/developer.md` | Developer Agent 配置（技能、职责、自查清单） |
| `.claude/agents/qa.md` | QA Agent 配置（测试红线、质量标准） |
| `.claude/settings.json` | 项目级 Claude Code 配置 |
| `.claude/settings.local.json` | 本地覆盖配置 |

### 7.3 技能定义

| Skill | 路径 | 说明 |
|-------|------|------|
| java-springboot | `.claude/skills/java-springboot/SKILL.md` | Spring Boot 最佳实践 |
| springboot-patterns | `.claude/skills/springboot-patterns/SKILL.md` | 分层架构模式 |
| typescript-react-reviewer | `.claude/skills/typescript-react-reviewer/SKILL.md` | React 代码审查（含反模式、React 19 模式、检查清单） |
| vercel-react-best-practices | `.claude/skills/vercel-react-best-practices/` | 60+ 条性能优化规则 |
| web-design-guidelines | `.claude/skills/web-design-guidelines/SKILL.md` | Web 界面设计规范 |

### 7.4 测试与数据

| 文件 | 说明 |
|------|------|
| `docs/stress-test/stress_test.py` | Python 压力测试脚本（零依赖） |
| `docs/stress-test/stress_test_result.json` | 压力测试原始数据 |
| `frontend/src/__tests__/` | 前端 46 个测试文件（Vitest + RTL） |
| 各服务 `src/test/` | 后端单元测试（JUnit 5 + MockMvc） |

### 7.5 项目配置

| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 9 个容器编排（MySQL + 7 服务 + Prometheus + Grafana） |
| `.env` | 环境变量（密码、密钥） |
| `scripts/build.ps1` | 一键编译脚本 |
| `nginx/nginx.conf` | 前端 Nginx 反向代理配置 |
| `monitoring/` | Prometheus + Grafana 配置 |

---

## 八、项目亮点总结

1. **微服务架构落地**：7 个微服务 + Gateway + Eureka，按业务域拆分，服务间通过 OpenFeign + Resilience4j 通信
2. **智能开发工具链**：Claude Code + CodeGraph MCP Server + 5 个专业 Skills 形成完整 AI 辅助开发体系
3. **全链路安全防护**：OWASP Top 10 全覆盖，XSS 前后端双重净化，文件上传 5 层校验
4. **可观测性**：Prometheus + Grafana 全链路监控，5 条告警规则，JVM/HTTP/DB 全维度指标
5. **容器化部署**：Docker Compose 一键启动 9 个容器，healthcheck 保证启动顺序
6. **分层测试**：前端 46 个测试文件 + 后端分层测试 + 压力测试 6 场景，关键业务路径全覆盖
7. **文档体系完整**：架构、安全、性能、验收、API 五大类文档，可交付评审
