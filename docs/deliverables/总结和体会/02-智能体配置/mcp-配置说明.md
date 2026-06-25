# MCP Server 接入说明

本文档说明 CC91 项目所接入的 MCP（Model Context Protocol）Server 及其应用场景。

---

## 1. MCP 协议简介

MCP（Model Context Protocol）是 Anthropic 提出的开放协议，为 AI 智能体提供连接外部工具和数据源的标准接口。通过 MCP，AI 智能体可以在运行时动态调用外部能力（如代码索引、浏览器自动化、IDE 诊断），而非仅依赖静态提示。

---

## 2. 本项目接入的 MCP Server

### 2.1 CodeGraph

**用途**：基于 Tree-sitter 的代码知识图谱，提供亚毫秒级符号搜索。

**接入方式**（项目级 `.claude/settings.json` 或全局配置）：
```json
{
  "mcpServers": {
    "codegraph": {
      "command": "codegraph",
      "args": ["mcp"]
    }
  }
}
```

**实际应用场景**：
- **代码导航**：`codegraph_context "用户登录流程"` 一键获取相关函数、调用方、被调用方
- **调用追踪**：`codegraph_trace from="AuthService.login" to="JwtUtil.generateToken"` 返回完整调用链
- **影响分析**：`codegraph_impact "UserService"` 评估变更影响范围
- **符号搜索**：`codegraph_search "AuthService"` 替代 grep

**项目索引规模**：
- 文件数：308
- 符号节点：4284
- 边（调用/继承关系）：6998

### 2.2 Playwright

**用途**：浏览器自动化，支持动态决策。

**接入方式**：
```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest"]
    }
  }
}
```

**实际应用场景**：
- QA 智能体的 E2E 测试：动态决定"点击哪个按钮、检查哪个元素"
- 视觉验证：截图对比
- 与预编写脚本的区别：可运行时决策，而非只能执行固定步骤

### 2.3 IDE（VS Code 扩展提供）

**用途**：Jupyter Kernel 执行 + VS Code 语言服务器诊断。

**实际应用场景**：
- 在 Jupyter Kernel 中执行 Python 压测脚本（`mcp__ide__executeCode`）
- 获取 VS Code TypeScript/Java 语言诊断信息（`mcp__ide__getDiagnostics`）

---

## 3. 为什么只接入这几个 MCP

按 Anthropic《Building Effective AI Agents》建议，避免过度工程化：
- 文件操作（Read/Write/Edit）：Claude Code 内置工具完全胜任
- 代码搜索（Glob/Grep）：内置工具足够，但 CodeGraph 提供结构化索引更高效
- 终端执行（Bash）：内置工具完全胜任
- **浏览器自动化**：Bash 可跑预编写脚本，但运行时动态决策需要 MCP
- **代码图谱查询**：传统 grep 无法回答"调用链/影响范围"，需要 MCP

---

## 4. 与内置工具的协作

```
用户需求
  ↓
Lead (CLAUDE.md)
  ↓ SendMessage
Developer ─── 内置工具 (Read/Write/Edit/Bash/Grep) ──┐
  │                                                   │
  └── MCP (CodeGraph 用于代码理解) ────────────────────┤
                                                       │
  ↓ SendMessage                                        │
QA ─── 内置工具 (Bash: mvn test/npm test) ────────────┤
  │                                                    │
  └── MCP (Playwright 用于动态 E2E) ──────────────────┘
  ↓
测试报告 → Lead 审查 → 提交
```
