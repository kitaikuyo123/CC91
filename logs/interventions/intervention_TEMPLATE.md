# 人工干预记录: intervention_${NUM}

## 基本信息

| 字段 | 值 |
|------|-----|
| 时间 | ${TIMESTAMP} |
| 类型 | ${TYPE} |
| 触发者 | ${TRIGGER} |
| 轮次 | ${TURN_NUM} |

## 干预类型

- [ ] **BEFORE_TASK** - 任务执行前审批
- [ ] **AFTER_FAILURE** - 失败后干预
- [ ] **MANUAL_APPROVAL** - 关键决策暂停
- [ ] **DIRECTIVE_CHANGE** - 方向调整
- [ ] **EMERGENCY_STOP** - 紧急停止

## 干预内容

### 原始状态
${ORIGINAL_STATE}

### 用户修改
${USER_MODIFICATION}

## 决策

- [ ] 接受修改
- [ ] 拒绝修改
- [ ] 方案调整

## 影响评估

### 新增任务
-

### 移除任务
-

### 修改任务
-

### 影响范围
${IMPACT_SCOPE}

## 执行结果

${RESULT}
