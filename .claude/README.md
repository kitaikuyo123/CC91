# Agent Team 配置说明

本目录包含 Claude Code Agent Team 的 **Subagent 定义**。

## 重要说明

根据官方文档：
- **团队配置 (team config) 不应预先编写**，由运行时自动生成
- **Subagent 定义** 用于定义可重用的队友角色
- 生成队友时引用 subagent 类型：`Spawn a teammate using the frontend agent type`

## 目录结构

```
.claude/
└── agents/           # Subagent 角色定义
    ├── team-lead.md  # 团队编排者
    ├── frontend.md   # 前端开发专家
    ├── backend.md    # 后端开发专家
    ├── tester.md     # 测试专家
    └── critic.md     # 代码审查与架构优化专家
```

## 团队成员

| 角色 | 职责 | 工具权限 |
|------|------|----------|
| **team-lead** | 理解需求、编排计划、派发任务 | 只读 + 任务管理 + 消息 |
| **frontend** | UI 组件、样式、用户交互 | 完整工具集 |
| **backend** | API 设计、数据库、业务逻辑 | 完整工具集 |
| **tester** | 测试策略、用例编写、质量保障 | 完整工具集 |
| **critic** | 代码审查、架构优化、最佳实践 | 只读 + 消息 |

## 使用方式

### 启动团队

告诉 Claude 创建团队并引用 subagent 类型：

```
Create an agent team for this full-stack feature.
Spawn teammates using team-lead, frontend, backend, tester, and critic agent types.
```

### 单独生成队友

```
Spawn a teammate using the critic agent type to review the auth module.
```

### 工作流程

1. Claude 使用 `TeamCreate` 创建团队（自动生成 team config）
2. Claude 使用 `Agent` 工具生成队友，引用 subagent 类型
3. **team-lead** 接收需求，分解任务并派发
4. 任务分配给 **frontend** / **backend** / **tester**
5. **critic** 审查代码质量，提出改进建议
6. 完成后使用 `TeamDelete` 清理团队

## 启用 Agent Teams

Agent teams 默认禁用，需要在 `settings.json` 中启用：

```json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

## 参考文档

- [Agent Teams 官方文档](https://code.claude.com/docs/agent-teams)
- [Subagents 文档](https://code.claude.com/docs/sub-agents)
