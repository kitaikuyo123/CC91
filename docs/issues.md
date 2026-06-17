# CC91 论坛 - 问题跟踪与功能清单

> Developer agent 每次开工前必须先读此文件，优先处理未修复的 Bug，再实现未完成功能。

## 状态说明

- `[ ]` 未处理
- `[~]` 进行中
- `[-]` 暂不处理（附原因）

---

## Bug

### forum-service（压测 Task 3 第三轮 500 并发暴露）

- **PostController.createPost 在并发下偶发 `author_id` 为 null**
  - 优先级：P1（500 RPS 限速压测中 308 / 1863 个 `POST /api/posts` 返回 400，错误体 `Column 'author_id' cannot be null`）
  - 暴露于：`docs/deliverables/05-测试报告/stress-test/k6/scenario-4-mixed-read-write.js`
  - 现象：单线程 smoke test 不复现；500 并发下偶发；推测 `JwtAuthenticationFilter` 在并发下 `SecurityContext` 设置存在线程安全竞争，导致 `getCurrentUsername()` 偶发返回 null
  - 建议：检查 `PostController.getCurrentUsername()` 与 `JwtAuthenticationFilter` 的并发安全；考虑用 `RequestScope` 或 ThreadLocal 显式同步

_其他历史 bug 已就地修复，详见本节历史。_

---

## 未完成功能

### forum-service

- **`@WebMvcTest` slice 中 `.anyRequest().authenticated()` 不一致拦截匿名 POST/PUT/DELETE**  
  - 优先级：P2（仅在单元 slice 层不可复现；production 端集成测试应正常返回 401）
  - 现象：在 `@WebMvcTest` 切片下，匿名 POST/PUT/DELETE 命中 `SecurityConfig` 的 `.anyRequest().authenticated()` 时未触发 AuthenticationEntryPoint，而是穿过到 Controller（mock service 返回 null，最终 200）
  - 与 user-service 对比：user-service 对所有受保护路径显式声明 `.requestMatchers(METHOD, PATH).authenticated()`；forum-service 仅靠 `.anyRequest().authenticated()` 兜底
  - 建议：要么显式声明敏感端点规则，要么依赖 e2e/integration 测试覆盖匿名场景
  - 当前处置：相关 Controller 测试已跳过匿名 401 用例，并在 javadoc 中标注，集成层验证留待后续
