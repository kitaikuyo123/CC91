# Agent 配置说明

本目录包含 Claude Code 的 **Subagent 定义**。

## 当前 Agent 角色

| 角色 | 定义位置 | 职责 |
| --- | --- | --- |
| **Lead** | `CLAUDE.md`（项目根目录） | 需求理解、任务拆分、派发工作、质量兜底 |
| **developer** | `.claude/agents/developer.md` | 全栈开发，实现代码、补充测试、修复构建问题 |
| **qa** | `.claude/agents/qa.md` | 测试执行、回归验证、输出测试报告 |

## 工作流程

1. **Lead** 接收需求，拆分为结构化任务（含背景、范围、验收标准）
2. Lead **Spawn developer**，按任务 ID 分派实现
3. 开发完成后 Lead **Spawn qa**，执行测试与回归验证
4. qa 输出测试报告，Lead 审查后关闭任务

## Skills（专业技能）

除 Agent 角色外，还配置了以下专业技能（Skills），供开发时按需调用：

| Skill | 用途 |
|-------|------|
| `java-springboot` | Spring Boot 后端开发最佳实践 |
| `springboot-patterns` | 架构模式、REST API 设计 |
| `typescript-react-reviewer` | TypeScript + React 代码审查 |
| `vercel-react-best-practices` | React 性能优化指南 |
| `web-design-guidelines` | UI 可用性与无障碍审查 |
| `code-review` | 代码差异审查（正确性 + 重构建议） |
| `security-review` | 安全审查（OWASP 等） |
| `verify` | 运行应用验证功能正确性 |
| `deep-research` | 多源深度技术调研 |

## 目录结构

```
.claude/
├── agents/
│   ├── developer.md    # 全栈开发 Agent 定义
│   └── qa.md           # 测试 Agent 定义
├── README.md           # 本文件
└── settings.json       # 全局设置
```

## 参考文档

- [Claude Code 官方文档](https://code.claude.com/docs)
- [Subagents 文档](https://code.claude.com/docs/sub-agents)
