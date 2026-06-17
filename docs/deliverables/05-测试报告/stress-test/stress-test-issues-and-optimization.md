# CC91 压力测试问题识别与优化建议（500 RPS 限速版，第四轮 · MVP 优化后）

> 来源报告：`docs/deliverables/05-测试报告/stress-test/stress-test-report.md`
> 原始数据：`docs/deliverables/05-测试报告/stress-test/k6/results/scenario-*.json`
> 汇总 JSON：`docs/deliverables/05-测试报告/stress-test/k6/stress_test_result_500.json`
> 测试时间：2026-06-17
> 测试负载：**k6 `constant-arrival-rate` 限速 500 RPS × 15s/场景**（scenario-3 登录沿用 50 并发基线，scenario-5 本轮也改为 arrival-rate 500 RPS × 5s）

## 1. 背景

本文件基于 **第四轮 500 RPS 限速实跑**结果重写，与第一/二/三轮（HikariCP=200、scenario-5 瞬时 500、GlobalExceptionHandler 假 400）对比，评估 MVP 优化（Task 1 A+B+C+D）的实际效果与剩余瓶颈。

**四轮调参核心结论**：

| 轮次 | 模型 | 主调整 | 主失败原因 | 7 场景失败率范围 |
|------|------|------|-----------|--------------|
| 第一轮 | vus=500 无限制 | 初始配置 | HikariCP=50 池打满 | 62%-100% |
| 第二轮 | vus=500 无限制（HikariCP→200） | 容量扩容 | **客户端 TCP 端口耗尽**（17 万 `connectex`） | 94%-100% |
| 第三轮 | constant-arrival-rate 500 RPS | 限速 | 服务端真实瓶颈 + 1 个业务缺陷 | 0%-60% |
| **第四轮（本轮）** | **同第三轮 + MVP 优化** | **HikariCP→500, MySQL→3000, Flyway V3 索引, GlobalExceptionHandler→500** | **仅 scenario-5 持续写入恶化**（Feign 链路超时） | **0%-93%** |

**本轮核心进展**：plan Task 1 MVP 优化目标"5/7 场景失败率 < 5%"**超额达成 6/7**。scenario-1/2a/2b/2c/4/6 失败率全部 ≤ 0.03%。但延迟仍高（P99 16-23s），scenario-5 恶化（Feign 链路未根治）。

---

## 2. 已解决的问题（前几轮发现，本轮验证已无）

### 2.1 ~~P0：客户端 TCP 端口耗尽（第二轮最严重）~~ — **已解决**（第三轮已验证）

第二轮 17 万+ `connectex: No connection could be made` 错误，k6 改用 `constant-arrival-rate` 后从源头控制累积，**本轮 0 个 connectex 错误**。

### 2.2 ~~P0：HikariCP=200 在持续负载下不够（第三轮）~~ — **本轮解决**

**第三轮现象**：scenario-5 瞬时 500 INSERT 出现 294 个 `connection refused`。

**本轮调整**：5 服务 HikariCP 200→500，MySQL max_connections 1000→3000，innodb_buffer_pool_size 1G→2G。

**本轮验证**：
- scenario-5 瞬时并发模式改为持续 arrival-rate，无连接级失败
- HikariCP pending=0，active 接近 0（容量充足）
- 7 场景无 `connection refused` 错误

### 2.3 ~~P0：GlobalExceptionHandler RuntimeException → 400 假象（第三轮发现）~~ — **本轮解决**

**第三轮现象**：scenario-2a/1 的 180 个 400 实际是 SQL 超时被错误映射成"客户端错误"。

**本轮调整（Task 1 D 项）**：5 服务 `@ExceptionHandler(RuntimeException.class)` 状态码 400→500。

**本轮验证**：
- scenario-1/2a/2b/2c/6 失败率从 10.5%-13% → **全部 0%**
- scenario-2a 180 个 400 全部消失（解析为 0 失败）
- 注意：失败率改善的真实原因是 SQL 索引让超时不再发生（不只是错误码改了）

### 2.4 ~~P0：PostService.create author_id null（第三轮发现）~~ — **本轮通过 fallback fix 解决**

**第三轮现象**：scenario-4/5 共 313 个 POST /api/posts 返回 400，错误 `Column 'author_id' cannot be null`。

**本轮调整（Task 1 D 项的 UserServiceClientFallback 修复）**：fallback `getUserByUsername` 返回 null 而非 id=null 的占位对象，让 caller 显式抛 ResourceNotFoundException → 404。

**本轮验证**：
- scenario-4 失败率 16.5% → **0.03%**（3314/3315 成功）
- scenario-4 唯一 1 个失败是 fallback 触发返回 null 抛 404
- **修复方向正确**：把"author_id null MySQL 报错"换成清晰的"用户不存在"404

### 2.5 ~~P1：posts/comments 表缺复合索引（第三轮发现）~~ — **本轮解决**

**第三轮现象**：scenario-2b posts 列表 P99=16s，scenario-6 详情+评论 P99=29s。

**本轮调整（Task 1 C 项）**：forum-service Flyway V3 加 3 个复合索引：
- `idx_posts_category_status_created` on `posts(category_id, status, created_at)`
- `idx_posts_status_created` on `posts(status, created_at)`
- `idx_comments_post_status_created` on `comments(post_id, status, created_at)`

**本轮验证**：
- scenario-2b P99 16s → **11.0s**（-31%）
- scenario-6 P99 29s → **22.5s**（-23%）
- scenario-1/2a P99 也下降
- **索引生效但非根治**：P99 仍 11-22s，需配合缓存才能 < 5s

---

## 3. 仍存在的问题（按优先级）

### 3.1 P0（新增）：PostService.create 强依赖 Feign 调用 user-service（本轮发现）

**第四轮现象**：
- scenario-5 持续写入 500 RPS（arrival-rate 模式）共 1601 个请求，**1496 个失败 93.5%**，全部 status=404，错误体 `{"message":"用户不存在"}`
- forum-service 日志海量：`UserServiceClientFallback: User Service unavailable, returning null for username=testuser1`
- user-service InternalUserController 高并发下被全量打满，Feign 调用超时
- fallback 触发返回 null，PostService.createPost 抛 ResourceNotFoundException → 404

**根因（推断）**：PostService.createPost 每次发帖都调 `UserServiceClient.getUserByUsername(testuser1)` 取 author 信息（id/username/role/email），持续 500 RPS 下 user-service Feign 链路全量超时。

**为什么 Task 1 D 项的 fallback fix 没根治**：fallback fix 只改了**错误码**（从 400 改成 404），没修 Feign 调用本身的容量问题。在低并发（如 scenario-4 的 20% 写入）下，Feign 调用大部分成功，fallback 极少触发；在持续高并发（scenario-5 的 100% 写入 500 RPS）下，Feign 全量超时。

**优化方向**：

| 方向 | 说明 | 实施成本 |
|------|------|---------|
| **PostService.create 从 JWT/SecurityContext 解析 author** | 已有 SecurityContext，无需 Feign 调用 | 低（删 Feign 调用 + 加 SecurityContext 取数） |
| **user-service 调用加本地缓存** | author 信息是低变更数据，Caffeine 1h TTL | 中（每个 forum/content/file/notification 服务都加） |
| **user-service 横向扩容** | 多副本分担 InternalApiController 压力 | 高（架构变更） |

**最优先动作**：从 JWT/SecurityContext 解析 author——这是修 scenario-5 93.5% 失败的**根因修复**，工作量低。

### 3.2 P1：scenario-1/2a/6 的 P99 仍 16-23s（用户体感未达标）

**第四轮实测**：
- scenario-1 只读基准：P99=23.5s（第三轮 30s）
- scenario-2a categories：P99=16.6s（第三轮 23.1s）
- scenario-6 详情+评论：P99=22.5s（第三轮 29.3s）

**根因**：
- scenario-1/2a 的 `CategoryService.findAll` 调 `postRepository.countStatsByCategory` 做 GROUP BY 聚合统计每分类帖子数 + 今日新帖数，500 RPS 下从基线 755ms 恶化到 P99 16-23s
- scenario-6 每次请求 `incrementViewCount` 是 `@Modifying UPDATE`，500 RPS 下 InnoDB 行锁排队

**优化方向**：

| 方向 | 说明 | 实施成本 |
|------|------|---------|
| **Caffeine 缓存 CategoryService.findAll/findById**（plan Task 2 E 项） | 低变更数据缓存 5m TTL，消除 90% DB 压力 | 中（pom + CacheConfig + @Cacheable + @CacheEvict） |
| **浏览量异步化（plan Task 2 F 项）** | `incrementViewCount` 改 `@Async`，放到 PostViewService 独立 Bean | 中（注意 Spring AOP 跨 Bean 限制） |

**最优先动作**：Caffeine 缓存——这是修 scenario-1/2a P99 23s 的**根因修复**，plan Task 2 已设计好。

### 3.3 P1：user-service 账号锁定策略在生产环境有误锁风险（沿用）

**第四轮未触发，但风险未消除**：
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

### 3.4 P2：run-all.sh USERNAME 环境变量被 Windows 系统覆盖（沿用，未修）

**第三轮发现，本轮再次踩坑**：第 35 行 `export USERNAME="${USERNAME:-testuser1}"` 在 Windows bash 下被 Windows 系统 `USERNAME`（用户名如 `18421`）覆盖。

**本轮影响**：scenario-4/5 第一次跑全部 setup 401 失败，重跑显式传 `USERNAME=testuser1` 后通过。

**修复**：
```bash
# 强制覆盖，不用 fallback
unset USERNAME
export USERNAME=testuser1
export PASSWORD=admin123
```

或脚本顶部加 `unset USERNAME` 后再用 `${1:-testuser1}` 接受命令行参数。

---

## 4. 新增建议（基于第四轮数据）

### 4.1 P0：PostService.create 不依赖 Feign 调用取 author

**问题**：scenario-5 失败 93.5% 全部来自 forum-service → user-service Feign 超时触发 fallback。

**实施**：
- 在 PostService.createPost 中删掉 `userServiceClient.getUserByUsername(...)` 调用
- 改用 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 拿当前用户名
- 如果 author 完整信息（email/role）必须取，加 Caffeine 本地缓存（10min TTL）

**预期**：scenario-5 失败率 93.5% → < 5%。

### 4.2 P1：Caffeine 缓存 CategoryService.findAll/findById（plan Task 2 E 项）

**问题**：scenario-1/2a P99 16-23s，根因是聚合 SQL。

**实施**：
- forum-service pom.xml 加 `spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine`
- 新建 `forum-service/config/CacheConfig.java` 配置 categories 缓存（maximumSize=1000, expireAfterWrite=5m）
- `CategoryService.findAll()` 加 `@Cacheable("categories")`
- `CategoryService.create/update/delete` 加 `@CacheEvict(value="categories", allEntries=true)`
- application.yml 加 `spring.cache.type=caffeine` + `spring.cache.caffeine.spec`

**预期**：scenario-1/2a P99 23s → < 2s。

### 4.3 P1：浏览量异步化（plan Task 2 F 项）

**问题**：scenario-6 P99 22s，根因是 incrementViewCount 行锁排队。

**实施**：
- forum-service config 新建 `AsyncConfig.java`（@EnableAsync + TaskExecutor）
- `PostService.incrementViewCount` 提取成 `@Async` 方法
- 注意：`@Async` 方法不能被同类调用（Spring AOP 限制），需要把 incrementViewCount 放到独立的 `PostViewService` Bean

**预期**：scenario-6 P99 22s → < 5s。

### 4.4 P2：客户端 TCP 调优文档化（沿用，非阻塞）

**理由**：当前依赖 k6 限速避免端口耗尽。未来做 > 500 RPS 压测时仍需调 Windows TCP。

**实施**：在 stress-test-report.md 附录 B 已写入注册表 / netsh 调优命令，运维手册引用之。

---

## 5. 优化动作清单（按合并优先级）

| 优先级 | 动作 | 预期收益 | 实施成本 | 状态 |
|------|------|---------|---------|------|
| **P0** | **PostService.create 从 JWT/SecurityContext 解析 author（不调 Feign）** | scenario-5 失败率 93.5% → < 5% | 低 | **本轮新发现，待修** |
| **P0** | 修 run-all.sh USERNAME 强制 export | 避免下次压测 setup 失败 | 极低 | **沿用未修** |
| **P1** | **Caffeine 缓存 CategoryService**（plan Task 2 E） | scenario-1/2a P99 23s → < 2s | 中 | plan Task 2 已设计 |
| **P1** | **浏览量异步化**（plan Task 2 F） | scenario-6 P99 22s → < 5s | 中 | plan Task 2 已设计 |
| **P1** | user-service 账号锁定策略改 IP+账号 | 防误锁 | 中 | 沿用未修 |
| **P2** | user-service 调用加本地缓存 | 减压 Feign 调用 | 中 | 待评估 |

---

## 6. 四轮调参总结表

| 轮次 | 主调整 | 失败率范围 | 主瓶颈 | 解决/未解决 |
|------|--------|----------|--------|------------|
| 第一轮 | 初始配置 vus=500 | 62%-100% | HikariCP=50 | 已解决（扩到 200） |
| 第二轮 | HikariCP→200, MySQL→1000 | 94%-100% | **客户端 TCP 端口耗尽** | 已解决（限速 500 RPS） |
| 第三轮 | constant-arrival-rate 500 RPS | 0%-60% | 真实瓶颈：author_id 业务缺陷 + 瞬时 INSERT 容量 + GlobalExceptionHandler 假 400 | 已解决（本轮 MVP 修复） |
| **第四轮（本轮）** | **HikariCP→500, MySQL→3000, Flyway V3 索引, GlobalExceptionHandler→500, fallback fix** | **0%-93%** | **新瓶颈：PostService.create 强依赖 Feign 调用** | **本轮新增 4 项建议（plan Task 2 Caffeine + 浏览量异步 + PostService 不调 Feign + run-all.sh 修复）** |

---

## 7. 本轮 MVP 优化效果（前 vs 后）

| 改动项 | 影响场景 | 第三轮基线 | 第四轮实测 | 评价 |
|--------|--------|----------:|----------:|------|
| **A** scenario-5 改 arrival-rate | 5 | 59.7%（瞬时模式） | 93.5%（持续模式） | ❌ 模式变了反而恶化（Feign 容量未修） |
| **B** HikariCP 200→500 + MySQL 1000→3000 | 全部 | 容量假设的瓶颈 | 实测不饱和 | ✅ 容量充足，证伪"容量瓶颈"假设 |
| **C** Flyway V3 复合索引 | 1, 2a, 2b, 6 | P99 16-30s | P99 11-23s | ✅ 部分改善（治标） |
| **D** GlobalExceptionHandler 500 + fallback fix | 1, 2a, 2b, 4, 5, 6 | 假 400 + author_id null | 真状态码 + 清晰 404 | ✅ 错误码清晰（但 scenario-5 暴露 Feign 容量问题） |

**综合评价**：
- plan Task 1 MVP 目标"5/7 场景失败率 < 5%"**超额达成 6/7**
- scenario-5 恶化是因为 arrival-rate 持续模式暴露了第三轮瞬时模式没暴露的 Feign 链路瓶颈
- 延迟（P99）未根治，需 Task 2 的 Caffeine 缓存 + 浏览量异步化
- **本轮最有价值的发现**：PostService.create 强依赖 Feign 调用是 scenario-5 的根因，需新增 P0 修复项
