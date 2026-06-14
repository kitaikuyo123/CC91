# CC91 论坛系统 — 课程作业提交包

> **提交日期**：2026-06-14
> **项目版本**：v2.0（微服务架构）
> **团队规模**：11 人
> **代码提交**：156 commits

本目录是 CC91 校园论坛系统课程作业的全部交付材料，按 6 个主题分类组织。

---

## 目录结构

```
docs/deliverables/
├── 01-最终报告/                    # 主交付物：PDF + Markdown 源
│   ├── CC91-项目总结报告.pdf       # ⭐ 主报告（21 页）
│   └── project-summary.md          # Markdown 源
├── 02-智能体配置/                  # AI 协作体系
│   ├── CLAUDE.md                   # Lead 智能体工作协议
│   ├── agents-developer.md         # Developer 智能体定义
│   ├── agents-qa.md                # QA 智能体定义
│   ├── settings.json               # 项目级 Claude Code 配置
│   ├── settings.local.json         # 本地权限配置
│   └── mcp-配置说明.md             # CodeGraph / Playwright MCP 接入说明
├── 03-规则与技能/                  # 规则系统 + 技能模块
│   ├── rules-overview.md           # 三类规则文件要点总览
│   └── skills/                     # 5 个 SKILL.md
├── 04-测试报告/                    # 测试体系全部产出
│   ├── unit-test-report.md
│   ├── integration-test-report.md
│   └── stress-test/                # 压测脚本 + 报告 + 原始数据 + 种子
├── 05-设计与架构/                  # 设计文档
│   ├── microservices-design.md     # 微服务架构（取代旧 architecture.md）
│   ├── monitoring-design.md
│   ├── security-audit.md
│   ├── api-reference.md
│   ├── docker-deployment-guide.md
│   └── acceptance-guide.md
└── 06-部署与运行/                  # 一键部署所需
    ├── docker-compose.yml
    ├── .env.example
    ├── env-setup.md
    ├── build.ps1
    └── monitoring/                 # Prometheus + Grafana 配置
```

---

## 推荐阅读顺序（评审）

1. **`01-最终报告/CC91-项目总结报告.pdf`** — 项目全貌，覆盖功能、技术、流程、5 维度分析
2. **`05-设计与架构/microservices-design.md`** — 微服务拆分策略与收益
3. **`05-设计与架构/security-audit.md`** — OWASP Top 10 全覆盖
4. **`04-测试报告/stress-test/stress-test-report.md`** — 万级数据下 8 场景压测
5. **`02-智能体配置/CLAUDE.md`** — AI 协作协议（5 阶段工作流）

---

## 复现步骤

### 1. 启动系统
```powershell
# 在仓库根目录
cp .env.example .env       # 配置密码、密钥
docker compose up -d --build
docker compose ps          # 等待所有容器 healthy
```

### 2. 验证功能
- 前端：http://localhost:3001
- Gateway：http://localhost:9000
- Eureka：http://localhost:8761
- Prometheus：http://localhost:19090
- Grafana：http://localhost:3000

### 3. 复现压测
```powershell
# 灌入万级种子数据（10k 帖 + 30k 评论）
docker exec -i cc91-mysql mysql -uroot -p<DB_PASSWORD> cc91_db < docs/stress-test/seed_data.sql

# 执行压测
python docs/stress-test/stress_test.py --base-url http://localhost:9000 --concurrency 50 --duration 15
```

---

## 关于"个人心得体会"

`01-最终报告/CC91-项目总结报告.pdf` 第五章已为 11 位成员预留占位结构（成员姓名 + 角色 + 4 个空段标题）。具体心得内容由团队成员后续单独收集后回填 `docs/project-summary.md`，再重新执行 `python docs/build_report.py` 重新生成 PDF。

---

## 关于"对话交互过程记录"

按用户决策，Claude Code 对话历史（`.jsonl` 文件）保留在 `~/.claude/projects/D--aToys-LargeScale/` 原位，**不纳入本次提交**。如需追溯开发过程，可在该目录查阅 27 个会话日志。
