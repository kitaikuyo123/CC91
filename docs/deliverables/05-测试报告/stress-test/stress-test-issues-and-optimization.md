# CC91 压力测试问题识别与优化建议（500 RPS 限速版，第三轮）

> 来源报告：`docs/deliverables/05-测试报告/stress-test/stress-test-report.md`
> 原始数据：`docs/deliverables/05-测试报告/stress-test/k6/results/scenario-*.json`
> 汇总 JSON：`docs/deliverables/05-测试报告/stress-test/k6/stress_test_result_500.json`
> 测试时间：2026-06-17
> 测试负载：**k6 `constant-arrival-rate` 限速 500 RPS × 15s/场景**（场景 3 登录沿用 50 并发基线，场景 5 为瞬时 500 iterations）

## 1. 背景

本文件基于 **第三轮 500 RPS 限速实跑**结果重写，与第一/二轮（`vus=500` 无限制）对比，重新评估每项建议在限速模型下的紧迫性。

**三轮调参核心结论**：

| 轮次 | 模型 | 主失败原因 | 7 场景失败率范围 |
|------|------|-----------|--------------|
| 第一轮 | vus=500 无限制 | HikariCP=50 池打满 | 62%-100% |
| 第二轮 | vus=500 无限制（HikariCP→200） | **客户端 TCP 端口耗尽**（17 万 `connectex`） | 94%-100% |
| **第三轮** | **constant-arrival-rate 500 RPS** | 服务端真实瓶颈 + 1 个业务缺陷 | **0%-60%** |

前两轮大量"高失败率"在限速后消失，证明它们主要是 **客户端 TCP 端口耗尽导致的假象**，而非服务端容量不足。第三轮显形的是真实瓶颈。

---

## 2. 已解决的问题（第一/二轮发现，第三轮验证已无）

### 2.1 ~~P0：客户端 TCP 端口耗尽（第二轮最严重）~~ — **已解决**

**第二轮现象**：17 万+ `connectex: No connection could be made` 错误，所有场景失败率 94%-100%。

**根因**：`vus=500` 全速反馈循环，单 k6 进程在 15s 内尝试数十万次连接，Windows 客户端 ephemeral 端口（58K）在 TIME_WAIT 累积下耗尽。

**第三轮验证**：
- 5 个持续型场景（1/2a/2b/2c/6）+ scenario-4 读写混合**全部 0 个 `connectex` 错误**
- scenario-5 仅出现 294 个 `connection refused`（服务端 HikariCP/Tomcat 拒绝），性质完全不同
- 失败率从 94-100% 降到 0-16.5%

**解决手段**：k6 默认 options 改为 `constant-arrival-rate`（rate=500/s, timeUnit=1s, preAllocatedVUs=500, maxVUs=1000）。**根本不需要修改 Windows TCP 注册表**（之前 `TcpTimedWaitDelay` 修改被权限拒绝，反而成了非阻塞项）。

### 2.2 ~~P0：HikariCP 连接池打满（第一轮）~~ — **部分解决**

**第一轮现象**：forum-service `HikariPool-1 Connection is not available, request timed out after 10007ms`，失败率 62-83%。

**第二轮调整**：HikariCP `maximum-pool-size` 50 → 200，`minimum-idle` 20 → 50。

**第三轮验证**：
- 持续型读场景（1/2a/2b/2c/6）在限速下无 HikariPool timeout 错误
- scenario-5 瞬时 500 INSERT 仍有 294 个 `connection refused`，说明 HikariCP=200 在瞬时峰值下仍不够（参见 3.2）

**结论**：扩容到 200 解决了**持续型负载**的容量问题，但**瞬时峰值**仍需进一步处理。

### 2.3 ~~P0：账号锁定策略反噬（第二/三轮）~~ — **绕过，未根本解决**

**第二轮现象**：scenario-4/5 setup 阶段 admin 登录 401，整个 scenario fail-fast。

**第三轮现状**：通过改用 `testuser1/admin123`（USER 角色）+ 限速 500 RPS，前置请求不再冲击 user-service admin 路径，**未触发锁定**。

**第三轮发现的另一触发路径**：`run-all.sh` 第 35 行 `USERNAME="${USERNAME:-testuser1}"` 在 Windows bash 下被 Windows 系统 `USERNAME` 环境变量（如 `18421`）覆盖，导致 scenario-4/5 setup 用空用户名+admin123 登录返回 401。**显式 `export USERNAME=testuser1` 重跑后通过**。这是脚本 bug 不是产品 bug，但建议修 run-all.sh 用强制 export。

**根因未消除**：user-service `account.lock.max-attempts=5, duration=30s` 仍会在用户侧异常流量下触发误锁，参见 3.4。

---

## 3. 仍存在的问题（按优先级）

### 3.1 P0：PostService.create 未注入 author_id（新增，本轮发现）

**第三轮现象**：
- scenario-4 读写混合 308/1863 个 POST /api/posts 返回 400，错误体：
  ```
  {"success":false,"message":"could not execute statement [Column 'author_id' cannot be null]
  [insert into posts (author_id,category_id,content,...) ..."}
  ```
- scenario-5 也有 5 个同样的错误
- **失败全部发生在写路径**，读路径零错误

**根因（推断）**：PostService.createPost 未从 SecurityContext / @AuthenticationPrincipal 显式取出当前用户 ID 并赋值到 Post.authorId；某些请求路径下 SecurityContext 为空（可能 Gateway 转发的 JWT 头未被 forum-service 正确解析），导致 INSERT 时 author_id 列为 null，被 MySQL NOT NULL 约束拒绝。

**复现条件**：高并发下 JWT 解析 / SecurityContext populate 的线程安全问题，或 Gateway 转发 header 丢失。

**优化方向**：

| 方向 | 说明 |
|------|------|
| PostService 显式注入 authorId | 方法签名加 `@AuthenticationPrincipal` 或在 Service 层强制 `SecurityContextHolder.getContext().getAuthentication()` |
| 复现+定位 | 单线程 1 QPS 跑 100 次 POST /api/posts 看是否复现，对比并发下出现频率 |
| 加 AOP 日志 | 在 PostRepository.save 前打印 author_id，发现 null 时栈追溯 |
| 单元测试覆盖 | 给 PostService 加 `createPost_whenSecurityContextEmpty_throwsUnauthorized` 测试 |

**最优先动作**：这是**业务正确性问题**（不只是性能），应在合并前修复。

### 3.2 P1：瞬时 500 INSERT 失败率 60%（scenario-5）

**第三轮实测**：scenario-5（iterations=500, vus=500, 无 duration）瞬时并发 INSERT，失败率 **59.7%**（299/501）：
- 294 个 `dial: connection refused` — Tomcat accept-count 或 HikariCP 接受新连接被打满
- 5 个 `author_id is null`（参见 3.1）

**根因**：HikariCP=200 在 500 并发 INSERT 同时到达时只能服务 200 个，其余被 Tomcat accept-count=200 缓冲后超时拒绝。

**优化方向**：

| 方向 | 当前值 | 建议值 | 预期 |
|------|------|------|------|
| HikariCP `maximum-pool-size` | 200 | **400-500**（仅限 forum-service） | 吸收瞬时 500 INSERT |
| Tomcat `accept-count` | 200 | 500 | 缓冲更多 |
| Gateway 限流 | 无 | 加 `RequestRateLimiter` 200 req/s 突发 500 | 拒绝超过容量的写入，让客户端重试 |
| 写入异步化 | 同步 INSERT | Kafka 队列 + 异步落库 | 解耦容量 |
| 业务侧分批 | 单次 500 | 限 100 并发/批 | 应用层限流 |

**最优先动作**：HikariCP 200 → 400 复测 scenario-5；若仍失败率 > 30%，再加 Gateway 限流。

### 3.3 P1：GET /api/posts 与 GET /api/categories 出现 400（场景 2a/2b/6）

**第三轮实测**：
- scenario-2a：180/1715 = 10.5% 失败，**全部 status=400**，error_code=1400
- scenario-2b：5/2450 = 0.2% 失败，全部 400
- scenario-6：39/1355 = 2.9% 失败，全部 GET /posts/{id} 返回 400

**根因（推断）**：
- GET /api/categories 的 400 大概率是分页参数校验失败（k6 脚本固定传 page=0&size=20，但偶发请求路径解析错误）
- GET /api/posts/{id} 的 400 可能是路径变量解析失败或帖子被删后查询返回业务 400

**优先级 P1**：失败率不算高，但 10.5% 不是噪声水平，应定位。

**优化方向**：
- forum-service GlobalExceptionHandler 打印 400 的具体原因（哪个字段、哪个约束）
- 复现 scenario-2a 单请求 400 的具体 URL + 响应体

### 3.4 P1：user-service 账号锁定策略在生产环境有误锁风险（沿用）

**第三轮未触发，但风险未消除**：
- max-attempts=5, duration=30s 在以下场景仍会误锁：
  - 网关重试策略把同一请求重发 5 次到 user-service
  - 前端登录表单被脚本攻击，5 次错误密码瞬时锁
  - 微服务链路中调用 /api/auth/login 的某个节点 N+1 重试

**优化方向**（建议优先级 P1，但非压测阻塞）：

| 方向 | 说明 |
|------|------|
| 锁定基于 IP+账号 | 而非仅账号 |
| 锁定策略区分环境 | 测试环境禁用或调大 max-attempts |
| admin 白名单兜底 | 内置 admin 永不锁 |
| 锁定后 exponential backoff | 而非固定 30s |

### 3.5 P2：scenario-1/6 P99 30s 接近超时上限

**第三轮实测**：scenario-1 P99=30.0s，scenario-6 P99=29.3s，已逼近 k6 默认 30s timeout。这说明**部分用户请求在 30s 内拿不到响应**，对真实用户体验是灾难性的。

**根因**：单请求处理慢（HikariCP 拿连接 + SQL 执行 + JVM GC），在持续 500 RPS 下排队，P99 落到队列尾部 30s。

**优化方向**：
- 加缓存（Categories 缓存到 Redis）
- 加复合索引（posts 表 `(category_id, status, created_at)`）
- HikariCP 200 → 300 进一步释放

**优先级 P2**：当前失败率仅 13.0%/2.9%，业务可接受；但 P99 30s 不可接受，建议长期跟踪。

---

## 4. 新增建议（基于第三轮数据）

### 4.1 P0：修 run-all.sh 的 USERNAME 环境变量覆盖 bug

**问题**：第 35 行 `export USERNAME="${USERNAME:-testuser1}"` 在 Windows bash 下被 Windows 系统 `USERNAME`（用户名如 `18421`）覆盖。

**影响**：第三轮 scenario-4/5 第一次跑全部 setup 401 失败，浪费一轮。

**修复**：
```bash
# 强制覆盖，不用 fallback
export USERNAME=testuser1
export PASSWORD=admin123
```
或
```bash
unset USERNAME
export USERNAME="${1:-testuser1}"
```

### 4.2 P1：Gateway 引入 RequestRateLimiter

**理由**：scenario-5 瞬时 500 INSERT 失败 60%，根因是服务端容量 < 客户端注入速率。Gateway 加 `RequestRateLimiter`（Redis 令牌桶）限 200 req/s 写入，超出立即返回 429，客户端重试，比让请求堆在 Tomcat 队列里超时更友好。

**实施**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: forum-write
          uri: lb://forum-service
          predicates:
            - Path=/api/posts
            - Method=POST
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 200
                redis-rate-limiter.burstCapacity: 500
```

### 4.3 P1：posts 表加复合索引

**理由**：scenario-2b posts 列表查询在 10,008 行 + 分页下，P99=16s 表明 SQL 走了全表扫描。

**实施**：
```sql
-- V3 迁移
CREATE INDEX idx_posts_category_status_created ON posts (category_id, status, created_at DESC);
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC);
```

### 4.4 P2：Categories 加 Redis 缓存

**理由**：scenario-2a categories 失败 10.5%，根因可能是分页查询慢。Categories 是低变更数据，缓存可消除 90% 的 DB 压力。

**实施**：`@Cacheable("categories")` + Caffeine 1h TTL。

### 4.5 P2：客户端 TCP 调优文档化

**理由**：当前依赖 k6 限速避免端口耗尽。未来做 > 500 RPS 压测时仍需调 Windows TCP。

**实施**：在 stress-test-report.md 附录 B 已写入注册表 / netsh 调优命令，运维手册引用之。

---

## 5. 优化动作清单（按合并优先级）

| 优先级 | 动作 | 预期收益 | 实施成本 |
|------|------|---------|---------|
| **P0** | 修 PostService.create 的 author_id 注入 | 消除 scenario-4/5 的 308+5 个 400 | 低（加 1 行代码 + 单测） |
| **P0** | 修 run-all.sh USERNAME 强制 export | 避免下次压测 setup 失败 | 极低（改 2 行） |
| **P1** | HikariCP forum-service 200 → 400 | scenario-5 失败率 60% → < 20% | 低 |
| **P1** | Gateway 加 RequestRateLimiter（写路径） | 写入超量返回 429 而非超时 | 中（需 Redis） |
| **P1** | 定位 scenario-2a categories 10.5% 400 的具体错误 | 失败率 → < 2% | 中（加日志+复现） |
| **P1** | user-service 账号锁定策略改 IP+账号 | 防误锁 | 中 |
| **P2** | posts 表加复合索引 | scenario-2b/6 P99 减半 | 低 |
| **P2** | Categories 加 Redis 缓存 | scenario-2a P99 减半 | 中 |
| **P2** | HikariCP 200 → 300 | scenario-1/6 P99 30s → 15s | 低 |

---

## 6. 三轮调参总结表

| 轮次 | 主调整 | 失败率范围 | 主瓶颈 | 解决/未解决 |
|------|--------|----------|--------|------------|
| 第一轮 | 初始配置 vus=500 | 62%-100% | HikariCP=50 | 已解决（扩到 200） |
| 第二轮 | HikariCP→200, MySQL→1000 | 94%-100% | **客户端 TCP 端口耗尽** | 已解决（限速 500 RPS） |
| **第三轮** | **constant-arrival-rate 500 RPS** | **0%-60%** | 真实瓶颈：author_id 业务缺陷 + 瞬时 INSERT 容量 | **本轮新增 9 项建议** |
