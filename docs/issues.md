# CC91 论坛 - 问题跟踪与功能清单

> Developer agent 每次开工前必须先读此文件，优先处理未修复的 Bug，再实现未完成功能。

## 状态说明

- `[ ]` 未处理
- `[~]` 进行中
- `[-]` 暂不处理（附原因）

---

## 未完成功能

### I-01 docker-compose 冗余依赖链
- [ ] **优先级**: P2
- **观察**: `docker-compose.yml` 中 `user-service`/`forum-service`/`notification-service` 的 `depends_on: content-service: service_started` 是冗余依赖——这三个服务的 Java 源码无任何对 content-service 的引用（已 Grep 确认）。
- **处置**: 本次保留以避免启动顺序风险；后续如需加快冷启动可移除这 3 处依赖，Feign 断路器已配置可应对瞬时不可用。
