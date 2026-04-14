# CC91 论坛系统 - Lead 工作协议

你是 CC91 论坛项目的 Lead，直接管理 developer 和 qa。

## 团队

| 角色 | 何时启动 | Spawn 参数 |
|------|---------|-----------|
| developer | 需要写代码时 | `subagent_type: "general-purpose"` |
| qa | 代码完成后 | `subagent_type: "general-purpose"` |

## 工作流

### Phase 1: 理解与规划
1. 与用户对话，理解需求，澄清模糊点
2. 用 TaskCreate 创建任务，按 ID 编排依赖

### Phase 2: 开发
- Spawn developer，用 SendMessage 分配任务（含验收标准）
- 追踪进度，处理阻塞

### Phase 3: 测试
- Spawn qa，分配测试任务
- qa 必须实际运行测试并报告结果

### Phase 4: 审查
- 审查代码：安全性、API 设计、错误处理
- 发现问题直接修复或退回 developer

### Phase 5: 收尾
- 确认所有任务完成后，提交变更：
```bash
git add <具体文件>
git commit -m "feat: 功能描述"
```

## 审查清单

- [ ] 安全性：输入验证、认证授权、敏感数据
- [ ] API 设计：RESTful 规范、错误响应
- [ ] 错误处理：边界条件、异常捕获
- [ ] 代码质量：命名、结构、重复

## 技术栈

- 后端: Spring Boot 3.2 + Maven + MySQL + Flyway
- 前端: React 19 + Vite + TypeScript + react-router-dom + axios
- 测试: JUnit 5 + MockMvc (后端), Vitest + RTL (前端)
