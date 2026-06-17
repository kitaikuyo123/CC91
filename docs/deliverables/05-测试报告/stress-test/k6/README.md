# CC91 论坛 500 并发压测脚本（k6 版）

本目录是用 Grafana k6 重写的压测脚本，替代旧的 `../stress_test.py`（Python + 线程池）。
旧脚本保留不动以向后兼容，仅作为 50 并发基线归档参考。

## 为什么换 k6

- 旧 Python 脚本基于 `ThreadPoolExecutor + urllib`，500 并发在 Windows 上会撞 OS 线程栈（500 × 8MB）+ 短连接 `TIME_WAIT` 累积的客户端墙
- k6 是 Go 实现的事件循环模型，单机稳定支撑 5K+ VU，500 并发毫无压力
- 内置 `http_req_failed`、`http_req_duration` 指标与 `thresholds`，无需手写统计

## 安装

Windows 11 推荐：

```powershell
winget install grafana.k6
# 或
choco install k6
```

验证：

```bash
k6 version
```

## 前置条件

1. 微服务全部启动且 healthy：

   ```bash
   docker compose up -d --build
   docker compose ps   # 7 服务 + MySQL 全部 healthy
   ```

2. 种子数据已灌入（`../seed_data.sql`，10K 帖子 + 90K 评论 + 16 用户）。新装环境用：

   ```bash
   docker exec -i cc91-mysql mysql -uroot -p<password> forum < ../seed_data.sql
   ```

3. Task 1 的服务端配置已生效（Tomcat `threads.max=500`、HikariCP `maximum-pool-size=50`、MySQL `max_connections=500`）：

   ```bash
   curl -s http://localhost:8082/actuator/configprops | jq '."server.tomcat.threads"'
   mysql -h 127.0.0.1 -uroot -p<pwd> -e "SHOW VARIABLES LIKE 'max_connections'"
   ```

4. `admin / admin123` 账号可用（场景 4/5 需要 JWT）。

## 单场景运行

```bash
cd docs/deliverables/05-测试报告/stress-test/k6

# 小规模 smoke test（10 VU × 5s）
k6 run --vus 10 --duration 5s scenario-2c-announcements.js

# 正式 500 并发
k6 run scenario-2c-announcements.js

# 输出原始 JSON 流
k6 run --out json=results/scenario-2c-announcements.json scenario-2c-announcements.js
```

环境变量覆盖：

| 变量 | 默认 | 说明 |
|------|------|------|
| `BASE_URL` | `http://localhost:9000` | Gateway 地址 |
| `USERNAME` | `admin` | 登录用户名（场景 4/5 用） |
| `PASSWORD` | `admin123` | 登录密码 |
| `VUS` | `500` | 默认 VU 数（除场景 5） |
| `DURATION` | `15s` | 默认持续时长（除场景 5） |
| `POST_ID` | （自动取 /api/posts 首条） | 场景 6 的帖子 ID |

## 全量运行（7 个场景串行）

```bash
./run-all.sh
```

> 注：scenario-3-login 不在列表中。**用户决策：登录场景沿用 50 并发基线**（旧 `stress_test_result.json`），因为 BCrypt cost=10 在 500 并发下会烧 CPU 而不是真实业务瓶颈。

`run-all.sh` 行为：
- 顺序跑 7 个场景（不并行，避免互相影响）
- 每个场景的 k6 原始 JSON 写到 `results/scenario-{N}.json`
- 某场景 threshold 失败（失败率 ≥5% 或 P99 ≥5s）不会中断后续场景，但会在末尾列出失败列表
- 最后调 `parse-results.js` 汇总到 `stress_test_result_500.json`

## 结果解析

```bash
node parse-results.js results/*.json > stress_test_result_500.json
```

输出格式与旧 `../stress_test_result.json` 完全兼容：

```json
[
  {
    "name": "只读基准测试（公开接口）",
    "total_requests": 12345,
    "success_count": 12340,
    "fail_count": 5,
    "rps": 823.0,
    "avg_latency_ms": 605.5,
    "p50_latency_ms": 580.1,
    "p90_latency_ms": 850.3,
    "p95_latency_ms": 1050.7,
    "p99_latency_ms": 2100.4,
    "min_latency_ms": 25.1,
    "max_latency_ms": 4500.2,
    "duration_s": 15.0,
    "status_codes": { "200": 12340, "0": 5 },
    "errors": ["connection refused", "..."]
  }
]
```

P50/P90/P95/P99 由 `parse-results.js` 在 `http_req_duration` 样本上自算，与旧 Python 脚本的 `int(len * p / 100)` 口径完全一致。

## 场景清单

| 文件 | 场景 | 配置 |
|------|------|------|
| `scenario-1-read-baseline.js` | 公开接口轮询（categories / posts / announcements） | 500 VU × 15s |
| `scenario-2a-categories.js` | 极限 GET /api/categories | 500 VU × 15s |
| `scenario-2b-posts.js` | 极限 GET /api/posts（50 并发旧基线已知会超时） | 500 VU × 15s |
| `scenario-2c-announcements.js` | 极限 GET /api/announcements | 500 VU × 15s |
| `scenario-4-mixed-read-write.js` | 80% 读 + 20% 写（需 JWT） | 500 VU × 15s |
| `scenario-5-write-posts.js` | POST /api/posts 固定 500 次（需 JWT） | 500 iterations × 500 VU |
| `scenario-6-post-detail-comments.js` | 帖子详情 + 评论列表轮询 | 500 VU × 15s |

## 与 50 并发基线对比

旧 50 并发基线见 `../stress_test_result.json`，500 并发结果见 `stress_test_result_500.json`。
报告中每个场景应列两组数据（50 / 500），重点观察：

- 失败率变化（阈值 5%）
- P99 变化（阈值 5s）
- `/api/posts` 端点：50 并发旧基线已出 3 次 30s 超时，500 并发预计会显著恶化，报告里如实记录

## 数据清理

场景 4/5 会向数据库写入约 ~3000 篇"压测帖子"（500 + 80%×500×15s 估算）。
**脚本不在 k6 进程内自动清理**（teardown 阶段再打 500 DELETE 会再次撞队列）。

清理方法（任选其一）：

```bash
# 用旧 Python 脚本的清理功能（推荐，自带分页搜索）
python ../stress_test.py --cleanup-only

# 或手动调用 API
TOKEN=$(curl -s -X POST http://localhost:9000/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
# 然后 DELETE /api/posts/{id}，可写个简单循环
```

## 已知约束

1. **登录场景跳过 500 并发**（用户决策，BCrypt cost=10 不变）
2. **失败率 >5% 不掩盖**：threshold 设 `rate<0.05` 仅作 k6 退出码标记，不修改实际跑出的数据
3. **token 进程级缓存**：同一 k6 进程内只登录一次（`common.js` 的 `tokenCache`）
4. **Windows dynamicport**：默认 49152-65535 ≈ 16K 端口，500 长连接足够，无需调整
