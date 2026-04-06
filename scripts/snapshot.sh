#!/bin/bash
# 快照管理脚本

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 快照目录
SNAPSHOT_DIR="logs/snapshots"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 创建快照
create_snapshot() {
    local name="$1"
    local snapshot_name="snapshot_${TIMESTAMP}_${name}"
    local snapshot_path="${SNAPSHOT_DIR}/${snapshot_name}"

    echo -e "${GREEN}创建快照: ${snapshot_name}${NC}"

    # 创建快照目录
    mkdir -p "${snapshot_path}"

    # 复制项目文件（排除 node_modules, .git, .claude）
    rsync -av --exclude='node_modules' --exclude='.git' --exclude='.claude' --exclude='*.log' . "${snapshot_path}/" 2>/dev/null || \
    cp -r . "${snapshot_path}/" --exclude node_modules --exclude .git --exclude .claude

    # 创建元数据
    cat > "${snapshot_path}/metadata.json" << EOF
{
  "name": "${snapshot_name}",
  "timestamp": "${TIMESTAMP}",
  "description": "${name}",
  "created_at": "$(date -Iseconds)"
}
EOF

    echo -e "${GREEN}快照已创建: ${snapshot_path}${NC}"
    echo "${snapshot_name}"
}

# 列出快照
list_snapshots() {
    echo -e "${YELLOW}可用快照:${NC}"
    ls -la "${SNAPSHOT_DIR}" 2>/dev/null || echo "暂无快照"
}

# 恢复快照
restore_snapshot() {
    local snapshot_name="$1"

    if [ -z "$snapshot_name" ]; then
        echo -e "${RED}请指定快照名称${NC}"
        list_snapshots
        return 1
    fi

    local snapshot_path="${SNAPSHOT_DIR}/${snapshot_name}"

    if [ ! -d "$snapshot_path" ]; then
        echo -e "${RED}快照不存在: ${snapshot_name}${NC}"
        return 1
    fi

    echo -e "${YELLOW}警告: 将覆盖当前文件！${NC}"
    read -p "确认恢复? (y/n): " confirm

    if [ "$confirm" = "y" ]; then
        rsync -av --exclude='node_modules' --exclude='.git' --exclude='.claude' "${snapshot_path}/" . 2>/dev/null || \
        cp -r "${snapshot_path}/." . --exclude node_modules --exclude .git --exclude .claude
        echo -e "${GREEN}已恢复快照: ${snapshot_name}${NC}"
    fi
}

# 删除快照
delete_snapshot() {
    local snapshot_name="$1"

    if [ -z "$snapshot_name" ]; then
        echo -e "${RED}请指定快照名称${NC}"
        return 1
    fi

    local snapshot_path="${SNAPSHOT_DIR}/${snapshot_name}"

    if [ ! -d "$snapshot_path" ]; then
        echo -e "${RED}快照不存在: ${snapshot_name}${NC}"
        return 1
    fi

    rm -rf "${snapshot_path}"
    echo -e "${GREEN}已删除快照: ${snapshot_name}${NC}"
}

# 主命令处理
case "$1" in
    create)
        create_snapshot "${2:-untitled}"
        ;;
    list)
        list_snapshots
        ;;
    restore)
        restore_snapshot "$2"
        ;;
    delete)
        delete_snapshot "$2"
        ;;
    *)
        echo "用法: $0 {create|list|restore|delete} [参数]"
        echo ""
        echo "命令:"
        echo "  create [名称]    创建快照"
        echo "  list             列出所有快照"
        echo "  restore <名称>   恢复快照"
        echo "  delete <名称>    删除快照"
        ;;
esac
