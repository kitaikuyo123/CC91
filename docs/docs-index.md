# CC91 文档导航

> 本页用于验收和答辩时快速定位项目文档。所有链接均为相对路径，可在仓库内直接点击查看。

---

## 0. 交付包入口

| 入口 | 作用 | 链接 |
|------|------|------|
| 提交包总览 | 5 大类交付材料的目录结构与复现步骤 | [deliverables/README.md](deliverables/README.md) |
| 提交包压缩包 | 单文件 zip，便于上传 | [CC91-deliverables.zip](CC91-deliverables.zip) |

---

## 1. 验收优先阅读

| 文档 | 作用 | 链接 |
|------|------|------|
| 项目总结 | 汇总项目目标、功能、架构、完成情况 | [project-summary.md](project-summary.md) |
| 项目报告 PDF | 21 页最终报告（评审主交付物） | [report.pdf](report.pdf) |
| 验收指南 | 面向验收人员的功能和流程检查说明 | [acceptance-guide.md](acceptance-guide.md) |
| Docker 部署与验收指南 | 一键部署、容器服务、端口、健康检查、常见问题 | [docker-deployment-guide.md](docker-deployment-guide.md) |
| API 参考 | 后端接口、请求参数、响应结构 | [api-reference.md](api-reference.md) |
| 安全审查报告 | OWASP Top 10、安全措施、残留风险 | [security-audit.md](security-audit.md) |
| 单元测试报告 | 后端 Service/Security 与前端组件/Context 测试结果 | [unit-test-report.md](unit-test-report.md) |
| 集成测试报告 | 后端 Controller/API 与前端页面级测试结果 | [integration-test-report.md](integration-test-report.md) |
| 压力测试报告 | 性能压测结果、场景数据、总体结论 | [stress-test/stress-test-report.md](stress-test/stress-test-report.md) |

---

## 2. 架构与设计

| 文档 | 作用 | 链接 |
|------|------|------|
| 微服务设计 | Gateway、Eureka、各业务微服务职责与交互 | [microservices-design.md](microservices-design.md) |
| 监控设计 | Prometheus、Grafana、监控指标与告警思路 | [monitoring-design.md](monitoring-design.md) |

---

## 3. 部署与环境

| 文档 | 作用 | 链接 |
|------|------|------|
| 环境配置说明 | 环境变量、数据库、邮件、JWT 等配置说明 | [env-setup.md](env-setup.md) |
| Docker 部署与验收指南 | Docker Compose 启动、检查、清理、故障排查 | [docker-deployment-guide.md](docker-deployment-guide.md) |
| Docker Compose 配置 | 实际容器编排文件 | [../docker-compose.yml](../docker-compose.yml) |
| 环境变量模板 | Docker 和本地运行所需变量模板 | [../.env.example](../.env.example) |

---

## 4. 测试与质量

| 文档 | 作用 | 链接 |
|------|------|------|
| 压力测试报告 | 8 场景正式压测数据和结论 | [stress-test/stress-test-report.md](stress-test/stress-test-report.md) |
| 压力测试问题与优化建议 | 登录瓶颈、尾延迟、缓存和异步化建议 | [stress-test/stress-test-issues-and-optimization.md](stress-test/stress-test-issues-and-optimization.md) |
| 单元测试报告 | 后端单元级用例结果、覆盖范围和修复记录 | [unit-test-report.md](unit-test-report.md) |
| 集成测试报告 | 集成级用例结果、接口/页面覆盖范围 | [integration-test-report.md](integration-test-report.md) |
| 安全审查报告 | 安全检查项、已修复问题、部署前安全建议 | [security-audit.md](security-audit.md) |
| 问题记录 | 项目问题和处理记录 | [issues.md](issues.md) |

---

## 5. 构建产物

| 文件 | 作用 |
|------|------|
| `project-summary.md` | PDF 报告源 |
| `report.html` | PDF 中间产物（带样式） |
| `report.pdf` | 最终 PDF（21 页） |
| `build_report.py` | 重建 PDF 的脚本（markdown → HTML → Chrome print-to-pdf） |

---

## 6. 推荐验收阅读顺序

1. [deliverables/README.md](deliverables/README.md) — 提交包总览
2. [project-summary.md](project-summary.md) 或 [report.pdf](report.pdf)
3. [docker-deployment-guide.md](docker-deployment-guide.md)
4. [acceptance-guide.md](acceptance-guide.md)
5. [api-reference.md](api-reference.md)
6. [microservices-design.md](microservices-design.md)
7. [unit-test-report.md](unit-test-report.md)
8. [integration-test-report.md](integration-test-report.md)
9. [stress-test/stress-test-report.md](stress-test/stress-test-report.md)
10. [security-audit.md](security-audit.md)

---

## 7. 常用入口

| 入口 | 地址 |
|------|------|
| 前端 | [http://localhost:3001](http://localhost:3001) |
| Gateway | [http://localhost:9000](http://localhost:9000) |
| Eureka | [http://localhost:8761](http://localhost:8761) |
| Prometheus | [http://localhost:19090](http://localhost:19090) |
| Grafana | [http://localhost:3000](http://localhost:3000) |
