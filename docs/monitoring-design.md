# CC91 论坛系统 — 系统监控方案

> 更新日期：2026-06-10
> 覆盖范围：Actuator + Micrometer + Prometheus + Grafana

---

## 1. 监控架构

```
┌─────────────────────────────────────────────────────────┐
│                    Grafana Dashboard                     │
│          (可视化仪表盘 + 告警规则 + 数据查询)              │
└──────────────────────────┬──────────────────────────────┘
                           │ PromQL 查询
┌──────────────────────────▼──────────────────────────────┐
│                   Prometheus Server                      │
│         (指标采集 + 存储 + 告警评估 + 服务发现)           │
└──┬─────┬─────┬─────┬─────┬─────┬─────┬─────────────────┘
   │     │     │     │     │     │     │  HTTP pull
   ▼     ▼     ▼     ▼     ▼     ▼     ▼
 :9000  :8081 :8082 :8083 :8085 :8086 :8761
 GW    User  Forum  Notif  Cont  File  Eureka
```

每个微服务通过 Spring Boot Actuator 暴露 `/actuator/prometheus` 端点，Prometheus 定时拉取指标，Grafana 可视化展示。

---

## 2. 各服务监控端点配置

### 2.1 已完成的 Actuator + Micrometer 配置

所有 6 个服务（Gateway、User、Forum、Notification、Content、File）均已配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
```

**Maven 依赖**（已添加到所有服务 pom.xml）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2.2 各服务暴露的端点

| 端点 | 用途 |
|------|------|
| `GET /actuator/health` | 健康检查（含数据库连接状态） |
| `GET /actuator/info` | 应用基本信息 |
| `GET /actuator/prometheus` | Prometheus 格式指标（核心） |
| `GET /actuator/metrics` | 指标列表查看 |

### 2.3 关键指标类别

| 指标类别 | 示例 | 用途 |
|----------|------|------|
| HTTP 请求 | `http_server_requests_seconds_*` | 响应时间、吞吐量、错误率 |
| JVM 内存 | `jvm_memory_used_bytes` | 堆/非堆内存使用 |
| JVM 线程 | `jvm_threads_live_threads` | 线程数监控 |
| 数据库连接池 | `hikaricp_connections_active` | 连接池使用率 |
| Resilience4j | `resilience4j_circuitbreaker_*` | 断路器状态、失败率 |
| 系统资源 | `system_cpu_usage`, `disk_total_bytes` | CPU、磁盘 |

---

## 3. Prometheus 配置

### 3.1 prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:9000']
    labels:
      service: 'api-gateway'

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']

  - job_name: 'forum-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']

  - job_name: 'notification-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8083']

  - job_name: 'content-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8085']

  - job_name: 'file-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8086']

  - job_name: 'eureka-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8761']
```

### 3.2 告警规则 alert_rules.yml

```yaml
groups:
  - name: cc91-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.job }} 不可达"

      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.application }} 5xx 错误率超过 10%"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.application }} P95 延迟超过 2s"

      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "断路器 {{ $labels.name }} 已打开"

      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.application }} 堆内存使用超过 85%"
```

---

## 4. Grafana 仪表盘

### 4.1 推荐面板布局

**仪表盘 1：系统总览**

| 面板 | PromQL | 类型 |
|------|--------|------|
| 各服务健康状态 | `up` | Stat |
| 请求总吞吐量 | `sum(rate(http_server_requests_seconds_count[5m]))` | Graph |
| 各服务 5xx 错误率 | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | Graph |
| 各服务 P95 延迟 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | Graph |

**仪表盘 2：微服务详情（模板变量 `$service`）**

| 面板 | PromQL | 类型 |
|------|--------|------|
| 请求速率 (QPS) | `rate(http_server_requests_seconds_count{application="$service"}[5m])` | Graph |
| 响应时间分布 | `histogram_quantile(0.5/0.9/0.95/0.99, ...)` | Graph |
| 活跃连接池 | `hikaricp_connections_active` | Gauge |
| 断路器状态 | `resilience4j_circuitbreaker_state` | Stat |
| JVM 堆内存 | `jvm_memory_used_bytes{area="heap"}` | Graph |
| 线程数 | `jvm_threads_live_threads` | Graph |

### 4.2 快速启动（Docker Compose）

```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: cc91-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert_rules.yml:/etc/prometheus/alert_rules.yml
    network_mode: host

  grafana:
    image: grafana/grafana:10.2.0
    container_name: cc91-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana

volumes:
  grafana-storage:
```

启动：
```bash
docker compose up -d
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000 (admin/admin)
```

在 Grafana 中添加 Prometheus 数据源 `http://prometheus:9090`，然后导入 JVM (Micrometer) Dashboard（ID: `4701`）和 Spring Boot Dashboard（ID: `12900`）。

---

## 5. 验证

服务启动后，访问以下端点验证指标暴露：

```bash
# 检查各服务 Prometheus 端点
curl -s http://localhost:8081/actuator/prometheus | head -5
curl -s http://localhost:8082/actuator/prometheus | head -5
curl -s http://localhost:9000/actuator/prometheus | head -5

# 检查健康状态
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```
