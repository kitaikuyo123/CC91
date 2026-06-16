# CC91 论坛系统 — 课程作业提交包

> **提交日期**：2026-06-14
> **项目版本**：v2.0（微服务架构）
> **团队规模**：11 人
> **代码提交**：156 commits

本目录是 CC91 校园论坛系统课程作业的核心交付材料，按 5 个主题分类组织。设计文档（架构 / 监控 / 安全 / API / 部署）与运行配置（docker-compose / .env / monitoring）统一在 `docs/` 根目录维护单份副本，本目录不再重复收录。

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
├── 04-对话记录/                    # Claude Code 对话历史（37 份 .jsonl）
└── 05-测试报告/                    # 测试体系全部产出
    ├── unit-test-report.md
    ├── integration-test-report.md
    └── stress-test/                # 压测脚本 + 报告 + 种子

# 设计与部署文档（在 docs/ 根目录维护，不在 deliverables 内重复）
docs/
├── microservices-design.md         # 微服务架构（取代旧 architecture.md）
├── monitoring-design.md            # Prometheus / Grafana 监控设计
├── security-audit.md               # OWASP Top 10 安全审查
├── api-reference.md                # 后端 API 参考
├── acceptance-guide.md             # 验收指南
├── docker-deployment-guide.md      # Docker 部署与验收
├── env-setup.md                    # 环境变量说明
├── docker-compose.yml              # → 仓库根目录
└── .env.example                    # → 仓库根目录
```

---

## 推荐阅读顺序（评审）

1. **`01-最终报告/CC91-项目总结报告.pdf`** — 项目全貌，覆盖功能、技术、流程、5 维度分析
2. **`../microservices-design.md`** — 微服务拆分策略与收益
3. **`../security-audit.md`** — OWASP Top 10 全覆盖
4. **`05-测试报告/stress-test/stress-test-report.md`** — 万级数据下 8 场景压测
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

Claude Code 全程对话历史（共 37 份 `.jsonl`，约 40MB）已纳入 `04-对话记录/` 提交，每份对应一次完整会话。文件名为会话 UUID；时间跨度覆盖整个开发周期，可用于追溯任意功能的实现过程与决策依据。

附件类资源（上传文件、截图、临时产物等）保留在本地 `~/.claude/projects/D--aToys-LargeScale/` 同名子目录，未入库。
