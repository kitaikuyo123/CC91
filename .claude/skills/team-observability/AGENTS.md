# Team Observability Skill

Agent Team 可观测性系统，记录团队协作全流程。

## 目录结构

```
logs/
├── conversations/    # 对话记录（每轮迭代）
├── interventions/    # 人工干预记录
├── snapshots/       # 代码版本快照
└── changes/         # 变更记录
```

## 记录格式

### 对话记录 (conversations/turn_N.md)

```markdown
# 对话记录: turn_N

## 元信息
- 时间: 2024-04-06 14:30
- 团队: fullstack-dev
- 任务: 用户登录功能
- 轮次: 1

## 事件流
| 时间 | 角色 | 事件 | 详情 | 状态 |
|------|------|------|------|------|
| 14:30 | user | 发送任务 | 实现登录 | ✅ |
| 14:30 | team-lead | 分解任务 | 创建3个子任务 | ✅ |
| 14:31 | backend | 执行 | 编写API | 🔄 |

## 决策记录
- 14:35 - 用户干预：要求添加密码哈希
- 14:40 - 决策：采用 bcrypt 方案

## 变更摘要
- backend/server.js: +50 行
- frontend/app.js: +20 行
```

### 干预记录 (interventions/intervention_N.md)

```markdown
# 人工干预: intervention_N

## 基本信息
- 时间: 2024-04-06 14:35
- 类型: BEFORE_TASK | AFTER_FAILURE | MANUAL_APPROVAL
- 触发者: 用户

## 干预内容
**原始请求**: 实现登录功能
**用户修改**: 添加密码哈希验证

## 决策
- [x] 接受修改
- [ ] 拒绝修改
- [ ] 方案调整

## 影响
- 新增 Task 4: 密码哈希实现
- 影响范围: backend
```

## 快照机制

### 自动快照点
- 任务开始时
- 任务完成时
- 人工干预前
- 迭代结束时

### 快照内容
```
snapshots/snapshot_20240406_143000/
├── backend/
│   ├── server.js
│   └── package.json
├── frontend/
│   ├── index.html
│   └── app.js
└── metadata.json
```

## 协作追踪

### 任务状态
```markdown
## 任务看板
| ID | 任务 | 负责人 | 状态 | 开始时间 | 结束时间 |
|----|------|--------|------|----------|----------|
| 1 | 登录API | backend | ✅ 完成 | 14:30 | 14:45 |
| 2 | 前端表单 | frontend | 🔄 进行中 | 14:31 | - |
```

## 使用方式

### 记录事件
```javascript
// 在 agent 中调用
logEvent({
  type: 'task_start',
  agent: 'backend',
  task: '登录API实现',
  timestamp: new Date().toISOString()
})
```

### 创建快照
```bash
# 自动快照
snapshot.sh create "登录功能完成"

# 恢复快照
snapshot.sh restore snapshot_20240406_143000
```

## 配置

在 settings.json 中启用：
```json
{
  "teamObservability": {
    "enabled": true,
    "logPath": "logs/",
    "autoSnapshot": true,
    "interventionPoints": ["BEFORE_TASK", "AFTER_FAILURE"]
  }
}
```
