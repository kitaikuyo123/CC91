# CC91 文档导航

> 本页用于验收和答辩时快速定位项目文档。所有链接均为相对路径，可在仓库内直接点击查看。

---

## 1. 验收优先阅读

| 文档 | 作用 | 链接 |
|------|------|------|
| 项目总结 | 汇总项目目标、功能、架构、完成情况 | [project-summary.md](project-summary.md) |
| 验收指南 | 面向验收人员的功能和流程检查说明 | [acceptance-guide.md](acceptance-guide.md) |
| Docker 部署与验收指南 | 一键部署、容器服务、端口、健康检查、常见问题 | [docker-deployment-guide.md](docker-deployment-guide.md) |
| API 参考 | 后端接口、请求参数、响应结构 | [api-reference.md](api-reference.md) |
| 安全审查报告 | OWASP Top 10、安全措施、残留风险 | [SECURITY_AUDIT.md](SECURITY_AUDIT.md) |
| 单元测试报告 | 后端 Service/Security 与前端组件/Context 测试结果 | [unit-test-report.md](unit-test-report.md) |
| 集成测试报告 | 后端 Controller/API 与前端页面级测试结果 | [integration-test-report.md](integration-test-report.md) |
| 压力测试报告 | 性能压测结果、场景数据、总体结论 | [stress-test/stress-test-report.md](stress-test/stress-test-report.md) |

---

## 2. 架构与设计

| 文档 | 作用 | 链接 |
|------|------|------|
| 系统架构 | 项目整体架构和模块划分 | [architecture.md](architecture.md) |
| 微服务设计 | Gateway、Eureka、各业务微服务职责与交互 | [microservices-design.md](microservices-design.md) |
| 监控设计 | Prometheus、Grafana、监控指标与告警思路 | [monitoring-design.md](monitoring-design.md) |
| 新增端点说明 | 新增 API 和端点变更记录 | [new-endpoints.md](new-endpoints.md) |

---

## 3. 部署与环境

| 文档 | 作用 | 链接 |
|------|------|------|
| 环境配置说明 | 环境变量、数据库、邮件、JWT 等配置说明 | [ENV_SETUP.md](ENV_SETUP.md) |
| Docker 部署与验收指南 | Docker Compose 启动、检查、清理、故障排查 | [docker-deployment-guide.md](docker-deployment-guide.md) |
| Docker Compose 配置 | 实际容器编排文件 | [../docker-compose.yml](../docker-compose.yml) |
| 环境变量模板 | Docker 和本地运行所需变量模板 | [../.env.example](../.env.example) |

---

## 4. 测试与质量

| 文档 | 作用 | 链接 |
|------|------|------|
| 压力测试报告 | 正式压测数据和结论 | [stress-test/stress-test-report.md](stress-test/stress-test-report.md) |
| 压力测试问题与优化建议 | 登录瓶颈、尾延迟、缓存和异步化建议 | [stress-test/stress-test-issues-and-optimization.md](stress-test/stress-test-issues-and-optimization.md) |
| 单元测试报告 | 214 个单元级用例结果、覆盖范围和修复记录 | [unit-test-report.md](unit-test-report.md) |
| 集成测试报告 | 314 个集成级用例结果、接口/页面覆盖范围 | [integration-test-report.md](integration-test-report.md) |
| 安全审查报告 | 安全检查项、已修复问题、部署前安全建议 | [SECURITY_AUDIT.md](SECURITY_AUDIT.md) |
| 问题记录 | 项目问题和处理记录 | [issues.md](issues.md) |
| 差距分析 | 项目目标和当前实现之间的差距 | [gap-analysis.md](gap-analysis.md) |

---

## 5. 项目过程资料

| 文档 | 作用 | 链接 |
|------|------|------|
| 最终迭代计划 | 开发末期任务规划和收敛安排 | [final-iteration-plan.md](final-iteration-plan.md) |
| 项目总结 | 项目成果和交付说明 | [project-summary.md](project-summary.md) |
| 验收 HTML 版 | 验收指南的 HTML 版本 | [acceptance-guide.html](acceptance-guide.html) |
| PDF 报告 | 项目报告 PDF | [report.pdf](report.pdf) |

---

## 6. 推荐验收阅读顺序

1. [project-summary.md](project-summary.md)
2. [docker-deployment-guide.md](docker-deployment-guide.md)
3. [acceptance-guide.md](acceptance-guide.md)
4. [api-reference.md](api-reference.md)
5. [microservices-design.md](microservices-design.md)
6. [unit-test-report.md](unit-test-report.md)
7. [integration-test-report.md](integration-test-report.md)
8. [stress-test/stress-test-report.md](stress-test/stress-test-report.md)
9. [SECURITY_AUDIT.md](SECURITY_AUDIT.md)

---

## 7. 常用入口

| 入口 | 地址 |
|------|------|
| 前端 | [http://localhost:3001](http://localhost:3001) |
| Gateway | [http://localhost:9000](http://localhost:9000) |
| Eureka | [http://localhost:8761](http://localhost:8761) |
| Prometheus | [http://localhost:19090](http://localhost:19090) |
| Grafana | [http://localhost:3000](http://localhost:3000) |
