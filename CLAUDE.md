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

## 问题跟踪

- 审查代码、与用户对话、或任何环节中发现 Bug / 未完成功能时，**必须立即写入 `docs/issues.md`**
- 格式：按现有条目样式，标注优先级（P0/P1/P2）和状态 `[ ]`
- **修复后立即从文件中删除该条目**，不保留 `[x] 已修复` 的条目
- `docs/issues.md` 只保留未完成的工作，保持简洁

## 提交流程

当用户要求提交，或一个完整功能/修复批次完成后：

1. **创建分支** — 从 develop 创建功能分支
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feat/功能描述
   ```

2. **分类暂存并提交** — 按逻辑分组，使用 conventional commits：
   - `feat: 新功能描述`
   - `fix: 修复描述`
   - `refactor: 重构描述`
   - `test: 测试描述`
   - `docs: 文档描述`

3. **推送并创建 PR** — 目标分支为 develop
   ```bash
   git push -u origin feat/功能描述
   gh pr create --base develop --title "标题" --body "描述"
   ```

4. **验证** — 确认 PR 创建成功，等待合并

## 技术栈

- 后端: Spring Boot 3.2 + Maven + MySQL + Flyway
- 前端: React 18 + Vite + TypeScript + react-router-dom + axios
- 测试: JUnit 5 + MockMvc (后端), Vitest + RTL (前端)
