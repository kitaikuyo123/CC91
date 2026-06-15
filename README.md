# CC91 校园论坛系统

> **版本**：v2.0（微服务架构）
> **状态**：已交付（含 Docker 一键部署、Prometheus 监控、压测验证、完整文档）

基于 **Spring Boot 微服务 + React 19 前端** 的校园论坛系统，支持用户认证、帖子管理、评论互动、通知推送、管理后台等完整功能闭环。

经过多轮迭代，项目从单体架构演进为 **7 个微服务 + API 网关 + 服务注册中心** 的分布式系统，通过 Docker Compose 实现一键部署。

📄 **完整文档**：[`docs/project-summary.md`](docs/project-summary.md) 或 PDF 版 [`docs/report.pdf`](docs/report.pdf)

---

## 技术栈

| 层级 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.2.12 + Java 17 |
| 微服务治理 | Spring Cloud 2023.0.0（Eureka + Gateway + OpenFeign） |
| 弹性设计 | Resilience4j 2.1.0（断路器、降级、重试） |
| 安全 | Spring Security + JWT（jjwt 0.12.3） + BCrypt + RBAC |
| 数据库 | MySQL 8.0.46 + Flyway 9.x |
| 前端 | React 19 + Vite + TypeScript + @tanstack/react-query |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus + Grafana |
| 容器化 | Docker + Docker Compose |
| 测试 | JUnit 5 + MockMvc（后端）、Vitest + RTL（前端） |

---

## 功能特性

### 用户系统
- JWT 双 Token 机制（Access + Refresh）
- 注册邮箱验证码（10 分钟过期，6 位数字）
- 账户锁定（连续失败触发）
- 头像上传 + 个人资料编辑

### 论坛核心
- 帖子草稿/发布双状态
- 评论多级嵌套回复（深度限制 5 层）
- 点赞 / 收藏（toggle 机制）
- 全文搜索 + 多维排序（最新 / 最多回复 / 热门）

### 通知系统
- 评论 / 回复实时通知
- 未读数角标 + 一键已读

### 管理后台
- 用户管理（封禁 / 解封 / 角色调整）
- 分类管理（增删改排序）
- 公告管理（发布 / 置顶）
- 内容审核（举报处理）

### 安全防护
- XSS 双层防护：DOMPurify（前端）+ HtmlSanitizer（后端）
- 文件上传 5 层校验（类型 / 扩展名 / 大小 / 路径遍历 / 重命名）
- CORS 白名单 + 服务间 INTERNAL_TOKEN 认证
- OWASP Top 10 全覆盖审查

---

## 项目结构

```
LargeScale/
├── microservices/             # 7 个微服务
│   ├── eureka-server/         # 服务注册中心 :8761
│   ├── gateway/               # API 网关 :9000
│   ├── user-service/          # 认证、用户管理 :8081
│   ├── forum-service/         # 帖子、评论、点赞 :8082
│   ├── notification-service/  # 通知 :8083
│   ├── content-service/       # 公告、举报 :8085
│   └── file-service/          # 文件上传 :8086
├── frontend/                  # React 19 + Vite
├── monitoring/                # Prometheus + Grafana 配置
├── nginx/                     # 前端 Nginx 反向代理
├── scripts/                   # 构建启动脚本
├── docs/                      # 全部项目文档（含 21 页 PDF 报告）
├── docker-compose.yml         # 9 容器一键编排
├── .env.example               # 环境变量模板
├── CLAUDE.md                  # Lead 工作协议（5 阶段工作流）
└── README.md                  # 本文件
```

---

## 快速开始（Docker 推荐）

### 前置要求
- Docker Desktop（含 Compose v2）
- JDK 17+（仅本地构建/调试需要）
- Node.js 18+（仅本地开发需要）

### 1. 克隆并配置
```bash
git clone https://github.com/kitaikuyo123/CC91.git
cd CC91
cp .env.example .env
# 编辑 .env 设置：DB_PASSWORD / JWT_SECRET / INTERNAL_TOKEN
```

### 2. 启动全栈（9 个容器）
```bash
docker compose up -d --build
docker compose ps          # 等待全部 healthy（约 1-2 分钟）
```

容器清单：

| 容器 | 端口 | 说明 |
|------|------|------|
| cc91-frontend | 3001 | Nginx 反向代理的前端 |
| cc91-gateway | 9000 | API 网关 |
| cc91-eureka | 8761 | 服务注册中心 |
| cc91-user-service | 8081 | 用户服务 |
| cc91-forum-service | 8082 | 论坛服务 |
| cc91-notification-service | 8083 | 通知服务 |
| cc91-content-service | 8085 | 内容服务 |
| cc91-file-service | 8086 | 文件服务 |
| cc91-mysql | 3306 | MySQL 数据库 |
| cc91-prometheus | 19090 | 指标采集 |
| cc91-grafana | 3000 | 监控仪表盘 |

### 3. 验证
- 前端：http://localhost:3001
- Eureka 控制台：http://localhost:8761（应见 7 个注册实例）
- Gateway 健康检查：http://localhost:9000/actuator/health
- Grafana 仪表盘：http://localhost:3000（admin/admin）

---

## 本地开发（无 Docker）

详见 [`docs/env-setup.md`](docs/env-setup.md)。简述：

```bash
# 1. 启动 MySQL（端口 3306），创建 cc91_db
# 2. 启动 Eureka 注册中心
cd microservices/eureka-server && mvn spring-boot:run

# 3. 启动 Gateway 和 6 个业务服务（每个独立终端）
cd microservices/gateway && mvn spring-boot:run
cd microservices/user-service && mvn spring-boot:run
# ... 其他 5 个服务

# 4. 启动前端
cd frontend && npm install && npm run dev
```

---

## API 示例

通过 Gateway（`http://localhost:9000`）访问所有 API：

```bash
# 登录（返回 JWT）
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 浏览帖子（无需认证）
curl http://localhost:9000/api/posts

# 发帖（需 JWT）
curl -X POST http://localhost:9000/api/posts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","content":"内容","categoryId":1}'
```

完整接口文档见 [`docs/api-reference.md`](docs/api-reference.md)。

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| user | user123 | 普通用户 |

> 数据库 init.sql 自动创建（首次启动时由 Flyway 执行）

---

## 测试

```bash
# 后端单元/集成测试（所有微服务）
cd microservices && mvn test

# 前端测试
cd frontend && npx vitest run

# 压力测试（30 并发 / 10 秒，约 85 秒完成）
python docs/stress-test/stress_test.py \
  --base-url http://localhost:9000 \
  --concurrency 30 --duration 10
```

测试结果见：
- [`docs/unit-test-report.md`](docs/unit-test-report.md)
- [`docs/integration-test-report.md`](docs/integration-test-report.md)
- [`docs/stress-test/stress-test-report.md`](docs/stress-test/stress-test-report.md)

---

## 文档导航

| 类别 | 文档 |
|------|------|
| **项目总结** | [`docs/project-summary.md`](docs/project-summary.md) / [`docs/report.pdf`](docs/report.pdf)（21 页 PDF） |
| **架构设计** | [`docs/microservices-design.md`](docs/microservices-design.md) |
| **API 参考** | [`docs/api-reference.md`](docs/api-reference.md) |
| **安全审查** | [`docs/security-audit.md`](docs/security-audit.md)（OWASP Top 10） |
| **监控设计** | [`docs/monitoring-design.md`](docs/monitoring-design.md) |
| **Docker 部署** | [`docs/docker-deployment-guide.md`](docs/docker-deployment-guide.md) |
| **环境搭建** | [`docs/env-setup.md`](docs/env-setup.md)（本地无 Docker） |
| **验收指南** | [`docs/acceptance-guide.md`](docs/acceptance-guide.md) |
| **完整索引** | [`docs/docs-index.md`](docs/docs-index.md) |

---

## 安全提示

⚠️ **这是一个演示项目**。生产部署前务必：

1. 修改 `.env` 中的 JWT_SECRET（至少 256 位随机字符串）
2. 修改 DB_PASSWORD 数据库密码
3. 更新 CORS_ORIGINS 为实际域名
4. 配置 HTTPS/TLS
5. 移除默认 admin 账号或修改密码
6. 邮件服务从控制台输出切换为真实 SMTP
7. 文件存储从本地迁移到 OSS/S3
8. 定期更新依赖：`mvn dependency-check:check` + `npm audit`

详见 [`docs/security-audit.md`](docs/security-audit.md)。

---

## 开源协议

MIT

## 贡献指南

1. Fork 本仓库
2. 从 `develop` 创建特性分支（`feat/xxx` / `fix/xxx`）
3. 遵循 Conventional Commits 规范
4. 提交前通过 `mvn test` + `npm test`
5. 创建 Pull Request 到 `develop` 分支
