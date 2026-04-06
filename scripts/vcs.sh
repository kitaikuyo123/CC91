#!/bin/bash
# Git 版本控制脚本 - Agent Team 用

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
BRANCH_PREFIX="turn/"
TAG_PREFIX="v"
LOG_DIR="logs/changes"

# 初始化日志目录
mkdir -p "$LOG_DIR"

# 获取当前分支名
get_branch() {
    git branch --show-current 2>/dev/null || echo "main"
}

# 获取当前 turn 编号
get_turn_num() {
    git tag --list "${TAG_PREFIX}*" 2>/dev/null | wc -l | tr -d ' '
}

# 创建新 turn 分支
new_turn() {
    local turn_num=$(($(get_turn_num) + 1))
    local branch_name="${BRANCH_PREFIX}${turn_num}"

    echo -e "${BLUE}创建新轮次: turn_${turn_num}${NC}"

    # 创建分支
    git checkout -b "$branch_name"

    # 创建初始提交
    git add -A
    git commit -m "chore: start turn_${turn_num}

[Turn ${turn_num}]
- 开始新的迭代周期" 2>/dev/null || true

    # 创建标签
    git tag -a "${TAG_PREFIX}${turn_num}.0" -m "Turn ${turn_num} 初始状态"

    echo -e "${GREEN}已创建 turn_${turn_num} 分支${NC}"
    echo -e "${CYAN}分支: ${branch_name}${NC}"
    echo -e "${CYAN}标签: ${TAG_PREFIX}${turn_num}.0${NC}"

    # 记录变更
    log_change "turn_start" "创建 turn_${turn_num}" "$branch_name"
}

# 创建快照（提交）
snapshot() {
    local description="${1:-snapshot}"
    local turn_num=$(get_turn_num)
    local current_branch=$(get_branch)
    local version_num=$(git tag --list "${TAG_PREFIX}${turn_num}.*" | wc -l | tr -d ' ')

    echo -e "${BLUE}创建快照: ${description}${NC}"

    # 添加并提交
    git add -A
    git commit -m "snapshot: ${description}

[Turn ${turn_num}]
$(git diff --stat HEAD~1 2>/dev/null | tail -n +2 || echo "Initial snapshot")"

    # 创建标签
    local new_version="${TAG_PREFIX}${turn_num}.${version_num}"
    git tag -a "$new_version" -m "${description}"

    echo -e "${GREEN}快照已创建: ${new_version}${NC}"

    # 记录变更
    log_change "snapshot" "$description" "$new_version"
}

# 创建干预前快照
snapshot_before_intervention() {
    local intervention_type="${1:-manual}"
    local turn_num=$(get_turn_num)
    local version_num=$(git tag --list "${TAG_PREFIX}${turn_num}.*" | wc -l | tr -d ' ')
    local new_version="${TAG_PREFIX}${turn_num}.${version_num}-pre-${intervention_type}"

    echo -e "${YELLOW}创建干预前快照: ${intervention_type}${NC}"

    git add -A
    git commit -m "snapshot: pre-intervention (${intervention_type})" 2>/dev/null || true
    git tag -a "$new_version" -m "Pre-intervention snapshot (${intervention_type})"

    echo -e "${GREEN}已创建: ${new_version}${NC}"

    # 记录干预
    log_change "intervention" "${intervention_type}" "$new_version"
}

# 列出历史
history() {
    echo -e "${CYAN}=== 版本历史 ===${NC}"
    echo ""

    echo -e "${YELLOW}分支:${NC}"
    git branch -a

    echo ""
    echo -e "${YELLOW}标签:${NC}"
    git tag --list

    echo ""
    echo -e "${YELLOW}最近提交:${NC}"
    git log --oneline -10
}

# 查看变更
diff() {
    local ref="${1:-HEAD}"
    echo -e "${CYAN}=== 变更: ${ref} ===${NC}"
    git diff "$ref" --stat
    echo ""
    git diff "$ref" -- . ':(exclude)node_modules'
}

# 切换版本
checkout_version() {
    local ref="${1:-HEAD}"

    if [ -z "$ref" ]; then
        echo -e "${RED}请指定版本 (标签或提交)${NC}"
        echo "可用标签:"
        git tag --list
        return 1
    fi

    echo -e "${YELLOW}切换到: ${ref}${NC}"
    git checkout "$ref"
}

# 合并到 main
merge_to_main() {
    local turn_num=$(get_turn_num)
    local branch_name="${BRANCH_PREFIX}${turn_num}"

    echo -e "${BLUE}合并 ${branch_name} 到 main${NC}"

    git checkout main
    git merge "$branch_name" --no-ff -m "merge: turn_${turn_num} completed

完成 turn_${turn_num} 迭代

标签: ${TAG_PREFIX}${turn_num}.*"

    echo -e "${GREEN}已合并到 main${NC}"
}

# 记录变更到日志
log_change() {
    local type="$1"
    local description="$2"
    local ref="$3"
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    local turn_num=$(get_turn_num)

    local log_file="${LOG_DIR}/turn_${turn_num}.md"

    # 创建或追加日志
    if [ ! -f "$log_file" ]; then
        cat > "$log_file" << EOF
# 变更记录: turn_${turn_num}

## 变更流

| 时间 | 类型 | 描述 | 引用 |
|------|------|------|------|
EOF
    fi

    echo "| ${timestamp} | ${type} | ${description} | \`${ref}\` |" >> "$log_file"
}

# 状态
status() {
    echo -e "${CYAN}=== Agent Team 版本状态 ===${NC}"
    echo ""
    echo -e "${YELLOW}当前分支:${NC} $(get_branch)"
    echo -e "${YELLOW}当前轮次:${NC} $(get_turn_num)"
    echo -e "${YELLOW}最近标签:${NC} $(git describe --tags --abbrev=0 2>/dev/null || echo '无')"
    echo ""
    echo -e "${YELLOW}工作区状态:${NC}"
    git status --short
}

# 帮助
help() {
    cat << EOF
${CYAN}Agent Team Git 版本控制${NC}

${YELLOW}命令:${NC}
  vcs new-turn           创建新轮次分支
  vcs snapshot [描述]    创建快照（提交）
  vcs intervention [类型] 创建干预前快照
  vcs history            查看版本历史
  vcs diff [版本]        查看变更
  vcs checkout [版本]    切换版本
  vcs merge              合并到 main
  vcs status             查看状态
  vcs log                查看变更日志

${YELLOW}示例:${NC}
  vcs new-turn
  vcs snapshot "登录功能完成"
  vcs intervention "添加密码哈希"
  vcs checkout v1.0
EOF
}

# 主命令处理
case "$1" in
    new-turn)
        new_turn
        ;;
    snapshot)
        snapshot "$2"
        ;;
    intervention)
        snapshot_before_intervention "$2"
        ;;
    history)
        history
        ;;
    diff)
        diff "$2"
        ;;
    checkout)
        checkout_version "$2"
        ;;
    merge)
        merge_to_main
        ;;
    status)
        status
        ;;
    log)
        cat logs/changes/turn_*.md 2>/dev/null || echo "暂无变更记录"
        ;;
    help|--help|-h)
        help
        ;;
    *)
        if [ -z "$1" ]; then
            status
        else
            echo -e "${RED}未知命令: $1${NC}"
            help
        fi
        ;;
esac
EOF
