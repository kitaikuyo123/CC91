# CC91 论坛系统 — 微服务架构设计文档

> **版本**：v3.0
> **日期**：2026-06-10
> **状态**：全部服务拆分完成（7 个微服务 + 网关 + 注册中心）

---

## 1. 现状分析

### 1.1 当前架构

CC91 论坛系统当前采用 **单体架构（Monolithic）**：

```
┌──────────────────────────────────────────────────┐
│              Spring Boot 应用 (port 8080)         │
│                                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │  Auth     │ │  Post    │ │  Comment  │  ...     │
│  │  模块     │ │  模块    │ │  模块     │          │
│  └──────────┘ └──────────┘ └──────────┘          │
│                                                    │
│  ┌──────────────────────────────────────────┐     │
│  │         共享 MySQL 数据库 (cc91_db)        │     │
│  └──────────────────────────────────────────┘     │
└──────────────────────────────────────────────────┘
```

**规模**：12 个 Controller、13 个 Service、13 个 Repository、12 个 Entity，60 个 API 端点。

### 1.2 单体架构的局限

| 问题 | 影响 |
|------|------|
| 扩展性差 | 无法按模块独立扩展，用户登录高峰与帖子浏览高峰争抢同一份资源 |
| 部署耦合 | 修改通知模块也需要重新部署整个应用 |
| 故障隔离弱 | 一个模块的内存泄漏会导致整个应用崩溃 |
| 团队协作受限 | 多人同时修改同一个代码库，合并冲突频繁 |
| 技术栈锁定 | 无法为不同模块选择最适合的技术 |

---

## 2. 微服务拆分策略

### 2.1 拆分原则

采用 **领域驱动设计（DDD）** 的限界上下文（Bounded Context）方法，按业务域拆分：

1. **高内聚**：同一业务域的实体、服务聚合在一起
2. **低耦合**：服务间通过 API 通信，不共享数据库表
3. **独立部署**：每个服务可独立构建、测试、部署
4. **渐进式**：从单体逐步拆分，不一次性重写

### 2.2 服务划分

```
                    ┌─────────────────────┐
                    │   API Gateway       │
                    │   (port 9000)       │
                    └──────────┬──────────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
   ┌────────▼───────┐ ┌───────▼───────┐ ┌───────▼────────┐
   │  User Service   │ │ Forum Service │ │ Notification   │
   │  (port 8081)    │ │ (port 8082)   │ │ Service        │
   │                 │ │               │ │ (port 8083)    │
   │ • 认证/注册     │ │ • 帖子 CRUD   │ │ • 通知推送     │
   │ • 用户管理      │ │ • 评论        │ │ • 未读计数     │
   │ • 密码重置      │ │ • 分类        │ │                │
   │ • 个人资料      │ │ • 点赞/收藏   │ └────────────────┘
   │                 │ │ • 搜索        │
   └─────────────────┘ └───────────────┘
                                                ┌────────────────┐
   ┌─────────────────┐ ┌───────────────────────┐ │ Content Service│
   │  Admin Service   │ │  File Service         │ │ (port 8085)    │
   │  (port 8084)     │ │  (port 8086)          │ │                │
   │                  │ │                       │ │ • 公告管理     │
   │ • 用户管理       │ │ • 头像上传            │ │ • 举报审核     │
   │ • 内容审核       │ │ • 图片上传            │ │ • 内容审核     │
   │ • 数据统计       │ │ • 文件存储            │ │                │
   └──────────────────┘ └───────────────────────┘ └────────────────┘
```

### 2.3 各服务职责与数据归属

| 服务 | 数据表 | API 前缀 | 说明 |
|------|--------|----------|------|
| **User Service** | users, user_profiles, refresh_tokens, verification_codes | /api/auth/\*\*, /api/users/\*\* | 认证、用户 CRUD、密码管理 |
| **Forum Service** | posts, comments, categories, post_likes, bookmarks | /api/posts/\*\*, /api/comments/\*\*, /api/categories/\*\* | 论坛核心业务 |
| **Notification Service** | notifications | /api/notifications/\*\* | 通知管理与推送 |
| **Admin Service** | （跨服务聚合） | /api/admin/\*\* | 管理后台操作 |
| **Content Service** | announcements, reports | /api/announcements/\*\*, /api/reports/\*\* | 公告与举报 |
| **File Service** | （文件系统） | /api/upload/\*\* | 文件上传与存储 |

---

## 3. 技术选型

### 3.1 Spring Cloud 组件栈

| 组件 | 选型 | 版本 | 用途 |
|------|------|------|------|
| 服务注册与发现 | Spring Cloud Netflix Eureka | 2023.0.0 | 服务注册、心跳检测、负载均衡 |
| API 网关 | Spring Cloud Gateway | 2023.0.0 | 路由转发、负载均衡、限流 |
| 服务间调用 | OpenFeign | 2023.0.0 | 声明式 REST 客户端 |
| 断路器 | Resilience4j | 2023.0.0 | 熔断、降级、重试 |
| 配置中心 | Spring Cloud Config | 2023.0.0 | 集中配置管理（可选，可用 Nacos 替代） |

**版本兼容性**：Spring Boot 3.2.x → Spring Cloud 2023.0.x (Leyton)

### 3.2 为什么选 Eureka + Gateway 而非 Nacos + Kong

| 考量 | Eureka + Gateway | Nacos + Kong |
|------|-----------------|--------------|
| 学习曲线 | 低（Spring 原生生态） | 中（需学习 Nacos 概念） |
| 社区成熟度 | 高（Netflix 生产验证） | 高（阿里生产验证） |
| 功能覆盖 | 满足课程需求 | 功能更丰富（配置中心 + 注册中心一体） |
| 依赖管理 | spring-cloud-dependencies BOM 即可 | 需引入 spring-cloud-alibaba BOM |

选择 Eureka + Gateway 的核心理由：**与现有 Spring Boot 3.2 项目同属一个 BOM，零额外依赖管理成本**。

### 3.3 服务间通信机制

| 通信模式 | 协议 | 适用场景 | 本项目应用 |
|----------|------|----------|------------|
| 同步调用 | HTTP/REST (OpenFeign) | 需要即时响应 | 用户发帖时验证用户是否存在 |
| 异步消息 | RabbitMQ / Kafka | 解耦、削峰填谷 | 帖子创建后触发通知、浏览量异步更新 |
| 事件驱动 | Spring Cloud Stream | 最终一致性 | 用户注册后发送验证邮件 |

---

## 4. 已落地实现

### 4.1 项目结构

```
microservices/
├── eureka-server/          # 注册中心 (port 8761)
│   ├── pom.xml
│   └── src/main/...
├── gateway/                # API 网关 (port 9000)
│   ├── pom.xml
│   └── src/main/...
├── user-service/           # 用户微服务 (port 8081)
│   ├── pom.xml
│   └── src/main/...
├── forum-service/          # 论坛微服务 (port 8082)
│   ├── pom.xml
│   └── src/main/...
├── notification-service/   # 通知微服务 (port 8083)
│   ├── pom.xml
│   └── src/main/...
├── content-service/        # 内容微服务 (port 8085)
│   ├── pom.xml
│   └── src/main/...
└── file-service/           # 文件微服务 (port 8086)
    ├── pom.xml
    └── src/main/...
```

### 4.2 Eureka 注册中心

- **端口**：8761
- **模式**：Standalone（单机，不自注册）
- **功能**：所有微服务启动时向 Eureka 注册，网关通过 Eureka 发现服务实例并做负载均衡

启动命令：
```bash
cd microservices/eureka-server && mvn spring-boot:run
```

访问面板：http://localhost:8761

### 4.3 API Gateway

- **端口**：9000
- **路由规则**：

| 路由 ID | 匹配路径 | 目标服务 |
|---------|----------|----------|
| user-service | /api/auth/\*\*, /api/users/\*\* | lb://user-service |
| admin-user-service | /api/admin/users/\*\* | lb://user-service |
| forum-service | /api/posts/\*\*, /api/comments/\*\*, /api/categories/\*\* | lb://forum-service |
| admin-content-service | /api/admin/posts/\*\*, /api/admin/comments/\*\* | lb://forum-service |
| notification-service | /api/notifications/\*\* | lb://notification-service |
| content-service | /api/announcements/\*\*, /api/reports/\*\*, /api/admin/announcements/\*\*, /api/admin/reports/\*\* | lb://content-service |
| file-service | /api/upload/\*\* | lb://file-service |

- `lb://` 前缀表示通过 Eureka 做客户端负载均衡（Ribbon/LoadBalancer）

启动命令：
```bash
cd microservices/gateway && mvn spring-boot:run
```

### 4.4 User Service（用户微服务）

- **端口**：8081
- **从单体中提取**：
  - 4 个 Entity（User, UserProfile, RefreshToken, VerificationCode）
  - 4 个 Repository
  - 3 个 Controller 端点组（认证、用户管理、密码重置）
  - 5 个 Service（AuthService, UserService, EmailService）
  - 完整 JWT 安全链（JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl）
- **数据库**：共享 cc91_db（与 Forum Service 共用同一数据库实例，独立访问不同表）

启动命令：
```bash
cd microservices/user-service
export DB_USERNAME=root DB_PASSWORD=1qaz2wsx JWT_SECRET=a1b2c3d4e5f6g7h8i9j0
mvn spring-boot:run
```

### 4.5 启动顺序

```bash
# 1. 启动注册中心
cd microservices/eureka-server && mvn spring-boot:run &

# 2. 启动用户微服务
cd microservices/user-service && DB_USERNAME=root DB_PASSWORD=xxx JWT_SECRET=xxx INTERNAL_TOKEN=xxx mvn spring-boot:run &

# 3. 启动论坛微服务
cd microservices/forum-service && DB_USERNAME=root DB_PASSWORD=xxx JWT_SECRET=xxx INTERNAL_TOKEN=xxx mvn spring-boot:run &

# 4. 启动通知微服务
cd microservices/notification-service && DB_USERNAME=root DB_PASSWORD=xxx JWT_SECRET=xxx INTERNAL_TOKEN=xxx mvn spring-boot:run &

# 5. 启动内容微服务
cd microservices/content-service && DB_USERNAME=root DB_PASSWORD=xxx JWT_SECRET=xxx INTERNAL_TOKEN=xxx mvn spring-boot:run &

# 6. 启动文件微服务
cd microservices/file-service && JWT_SECRET=xxx INTERNAL_TOKEN=xxx mvn spring-boot:run &

# 7. 启动网关
cd microservices/gateway && mvn spring-boot:run &

# 8. 通过网关访问
curl http://localhost:9000/api/categories
curl http://localhost:9000/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
```

### 4.6 服务间通信

| 调用方 | 被调用方 | 通信方式 | 用途 |
|--------|----------|----------|------|
| Forum Service | User Service | OpenFeign | 获取帖子/评论作者信息（用户名、头像） |
| Forum Service | Notification Service | OpenFeign | 评论/回复时创建通知 |
| Notification Service | User Service | OpenFeign | 解析 JWT 中的 username → userId |

**断路器配置（Resilience4j）：**
- `userService` 实例：滑动窗口 10 次，失败率 50% 触发熔断，等待 10s 后半开
- `notificationService` 实例：同上
- 通知创建失败时降级处理（记录日志，不影响帖子/评论创建）

### 4.7 内部 API 端点

| 服务 | 端点 | 说明 |
|------|------|------|
| User Service | `GET /api/users/internal/{id}` | 根据 ID 获取用户基本信息（无需 JWT） |
| User Service | `GET /api/users/internal/username/{username}` | 根据用户名获取用户基本信息（无需 JWT） |
| Notification Service | `POST /api/notifications/internal` | 内部创建通知（无需 JWT） |

---

## 5. 数据库拆分方案

### 5.1 当前状态

所有服务共享 `cc91_db`，通过表名隔离。这是 **DB-per-service 过渡模式**的最简形态。

### 5.2 目标状态：Database per Service

| 服务 | 独立数据库 | 表 |
|------|------------|------|
| User Service | cc91_user_db | users, user_profiles, refresh_tokens, verification_codes |
| Forum Service | cc91_forum_db | posts, comments, categories, post_likes, bookmarks |
| Notification Service | cc91_notify_db | notifications |
| Content Service | cc91_content_db | announcements, reports |

### 5.3 拆分步骤

1. **数据迁移**：为每个服务创建独立数据库，编写数据迁移脚本
2. **跨服务查询处理**：
   - 帖子列表中的作者信息 → Forum Service 调用 User Service API 获取
   - 管理后台聚合数据 → Admin Service 聚合多个服务 API
3. **分布式事务**：
   - 使用 **Saga 模式** 替代本地事务
   - 例：用户注册 = User Service 创建用户 + Notification Service 发送欢迎通知
   - 通过事件驱动实现最终一致性

---

## 6. 收益分析

### 6.1 可维护性

| 维度 | 单体 | 微服务 |
|------|------|--------|
| 代码定位 | 在 98 个文件中搜索 | 在对应服务的 10-20 个文件中定位 |
| 修改范围 | 改一个功能可能影响全部 | 改动限定在单个服务内 |
| 测试范围 | 全量回归 | 只测变更的服务 |

### 6.2 可扩展性

| 场景 | 单体 | 微服务 |
|------|------|--------|
| 登录高峰 | 扩展整个应用（浪费） | 只扩展 User Service |
| 帖子浏览高峰 | 扩展整个应用 | 只扩展 Forum Service |
| 文件上传瓶颈 | 扩展整个应用 | 只扩展 File Service |

### 6.3 容错性

| 故障类型 | 单体 | 微服务 |
|----------|------|--------|
| 邮件服务崩溃 | 影响注册流程 | User Service 降级，论坛仍可用 |
| 文件上传故障 | 影响发帖 | File Service 隔离，帖子纯文本仍可发 |
| 数据库压力 | 全局不可用 | 各服务独立连接池，互不影响 |

### 6.4 代价

- **运维复杂度增加**：需要管理多个服务的部署、监控、日志
- **分布式系统问题**：网络延迟、分布式事务、服务间调试
- **基础设施要求**：需要注册中心、网关、消息队列等中间件

---

## 7. 未来演进路线

### Phase 1（已实现）：核心基础设施

- Eureka 注册中心
- API Gateway 路由
- User Service 独立部署

### Phase 2（已实现）：完善服务拆分

- Forum Service 从单体中独立（posts, comments, categories, likes, bookmarks）
- Notification Service 独立
- OpenFeign 声明式服务间调用
- Resilience4j 断路器
- User Service 内部 API（供其他服务查询用户信息）

### Phase 3：生产化

- Spring Cloud Config 配置中心
- RabbitMQ/Kafka 消息总线
- 分布式链路追踪（Micrometer Tracing + Zipkin）
- Database per Service 完整拆分
- Docker Compose / Kubernetes 编排

---

## 附录

- 代码目录：`microservices/`
- Eureka 面板：http://localhost:8761
- Gateway 入口：http://localhost:9000
- User Service 直连：http://localhost:8081
