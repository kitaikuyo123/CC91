# CC91 Docker 部署与验收指南

> 适用范围：本地验收、演示部署、Docker Compose 一键启动验证  
> Compose 文件：[`../docker-compose.yml`](../docker-compose.yml)  
> 环境变量模板：[`../.env.example`](../.env.example)

---

## 1. 部署目标

本项目提供 Docker Compose 编排，用于一次性启动 CC91 论坛系统的前端、后端微服务、数据库、服务注册中心和监控组件。验收人员可通过本指南完成以下检查：

- 系统容器能否完整构建并启动
- 前端页面是否可访问
- Gateway API 是否可访问
- 微服务是否成功注册到 Eureka
- MySQL、上传目录、监控组件是否正常工作
- Prometheus 和 Grafana 是否可用于查看运行状态

---

## 2. 服务组成

| 服务 | 容器名 | 说明 | 对外端口 |
|------|--------|------|----------|
| MySQL | `cc91-mysql` | 业务数据库，数据卷持久化 | 不暴露 |
| Eureka Server | `cc91-eureka` | 服务注册中心 | `8761` |
| Gateway | `cc91-gateway` | API 统一入口 | `9000` |
| User Service | `cc91-user-service` | 用户、认证、资料服务 | 内部访问 |
| Forum Service | `cc91-forum-service` | 帖子、评论、版块服务 | 内部访问 |
| Notification Service | `cc91-notification-service` | 通知服务 | 内部访问 |
| Content Service | `cc91-content-service` | 公告、举报等内容服务 | 内部访问 |
| File Service | `cc91-file-service` | 文件上传与访问服务 | 内部访问 |
| Frontend | `cc91-frontend` | React + Nginx 前端 | `3001` |
| Prometheus | `cc91-prometheus` | 指标采集 | `19090` |
| Grafana | `cc91-grafana` | 监控面板 | `3000` |

---

## 3. 前置条件

请先确认本机已安装：

- Docker Desktop
- Docker Compose v2
- 可用的 4GB 以上内存
- 未被占用的端口：`3000`、`3001`、`8761`、`9000`、`19090`

检查 Docker：

```powershell
docker --version
docker compose version
```

---

## 4. 环境变量配置

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

至少需要设置以下变量：

```env
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_jwt_secret_at_least_32_characters_long
INTERNAL_TOKEN=your_internal_token
```

建议：

- `JWT_SECRET` 至少 32 字符，生产环境使用强随机字符串。
- `INTERNAL_TOKEN` 用于微服务内部调用鉴权，不应与 JWT 密钥相同。
- `.env` 不应提交到 Git。

---

## 5. 构建与启动

首次启动建议执行完整构建：

```powershell
docker compose up -d --build
```

查看容器状态：

```powershell
docker compose ps
```

查看启动日志：

```powershell
docker compose logs -f
```

单独查看 Gateway 日志：

```powershell
docker compose logs -f gateway
```

---

## 6. 访问地址

| 功能 | 地址 | 验收点 |
|------|------|--------|
| 前端系统 | [http://localhost:3001](http://localhost:3001) | 页面能打开，路由可访问 |
| Gateway API | [http://localhost:9000/api/categories](http://localhost:9000/api/categories) | 返回分类 JSON |
| Eureka 控制台 | [http://localhost:8761](http://localhost:8761) | 微服务均显示为 UP |
| Prometheus | [http://localhost:19090](http://localhost:19090) | Targets 为 UP |
| Grafana | [http://localhost:3000](http://localhost:3000) | 可登录并查看仪表盘 |

Grafana 默认账号：

| 用户名 | 密码 |
|--------|------|
| `admin` | `admin` |

---

## 7. 验收检查清单

### 7.1 容器状态

执行：

```powershell
docker compose ps
```

验收标准：

- `cc91-mysql` 为 healthy 或 running
- `cc91-eureka` 为 healthy 或 running
- `cc91-gateway` 为 running
- 所有业务服务为 running
- `cc91-frontend` 为 running
- Prometheus、Grafana 为 running

### 7.2 Eureka 注册检查

打开：

[http://localhost:8761](http://localhost:8761)

验收标准：

- `gateway`
- `user-service`
- `forum-service`
- `notification-service`
- `content-service`
- `file-service`

均已注册，并显示为 UP。

### 7.3 API 检查

公开接口：

```powershell
Invoke-WebRequest http://localhost:9000/api/categories
Invoke-WebRequest http://localhost:9000/api/posts
Invoke-WebRequest http://localhost:9000/api/announcements
```

验收标准：

- HTTP 状态码为 `200`
- 返回 JSON 内容
- Gateway 能正确转发到业务服务

### 7.4 前端检查

打开：

[http://localhost:3001](http://localhost:3001)

验收标准：

- 首页可正常展示
- 刷新页面不出现 404
- 前端通过 `/api/` 代理访问 Gateway

### 7.5 监控检查

打开 Prometheus：

[http://localhost:19090/targets](http://localhost:19090/targets)

验收标准：

- Gateway、User Service、Forum Service、Content Service、Notification Service、File Service 的 targets 尽量为 UP。

打开 Grafana：

[http://localhost:3000](http://localhost:3000)

验收标准：

- 可使用 `admin/admin` 登录
- 可查看预置 Dashboard

---

## 8. 数据与文件持久化

Compose 使用两个命名数据卷：

| Volume | 用途 |
|--------|------|
| `mysql-data` | MySQL 数据持久化 |
| `upload-data` | 用户上传文件持久化 |

普通重启不会清空数据：

```powershell
docker compose down
docker compose up -d
```

如需彻底清理数据：

```powershell
docker compose down -v
```

注意：`-v` 会删除数据库和上传文件数据，验收前请确认可以清空。

---

## 9. 常用运维命令

重新构建并启动：

```powershell
docker compose up -d --build
```

停止服务：

```powershell
docker compose down
```

查看所有日志：

```powershell
docker compose logs -f
```

查看指定服务日志：

```powershell
docker compose logs -f forum-service
```

重启单个服务：

```powershell
docker compose restart gateway
```

进入 MySQL：

```powershell
docker exec -it cc91-mysql mysql -uroot -p
```

查看镜像和容器：

```powershell
docker images
docker ps
```

---

## 10. 常见问题

### 10.1 端口被占用

现象：启动时报 `port is already allocated`。

处理：

1. 检查占用端口的进程。
2. 停止占用进程，或修改 `docker-compose.yml` 中的端口映射。

常见端口：

- `3000`：Grafana
- `3001`：前端
- `8761`：Eureka
- `9000`：Gateway
- `19090`：Prometheus

### 10.2 JWT_SECRET 未配置

现象：业务服务启动失败。

处理：

1. 检查 `.env` 是否存在。
2. 确认 `JWT_SECRET` 已填写且长度足够。
3. 重新启动：

```powershell
docker compose up -d --build
```

### 10.3 MySQL 健康检查不通过

现象：业务服务长时间等待 MySQL。

处理：

```powershell
docker compose logs mysql
```

常见原因：

- `DB_PASSWORD` 为空或与旧 volume 中的 root 密码不一致。
- 旧数据卷保留了之前的密码。

如允许清空数据，可执行：

```powershell
docker compose down -v
docker compose up -d --build
```

### 10.4 前端能打开但接口失败

处理步骤：

1. 检查 Gateway 是否启动：

```powershell
docker compose ps gateway
```

2. 检查 Eureka 中业务服务是否注册。
3. 检查 Gateway 日志：

```powershell
docker compose logs -f gateway
```

---

## 11. 验收结论模板

可在验收记录中使用以下表述：

> CC91 论坛系统已提供 Docker Compose 一键部署方案，覆盖 MySQL、Eureka、Gateway、用户服务、论坛服务、通知服务、内容服务、文件服务、前端、Prometheus 和 Grafana。通过 `docker compose up -d --build` 可完成本地验收环境启动。前端、Gateway API、Eureka 注册中心和监控服务均提供明确访问入口，具备可复现的容器化部署和验收能力。

---

## 12. 相关文档

- [文档导航](docs-index.md)
- [微服务设计文档](microservices-design.md)
- [监控设计文档](monitoring-design.md)
- [环境配置说明](ENV_SETUP.md)
- [验收指南](acceptance-guide.md)
- [压力测试报告](stress-test/stress-test-report.md)
