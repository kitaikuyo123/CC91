#!/usr/bin/env bash
#
# CC91 论坛 500 并发压测 - 一键串行运行 7 个场景。
#
# 用法：
#   ./run-all.sh                              # 默认 testuser1/admin123 @ http://localhost:9000
#   BASE_URL=http://10.0.0.5:9000 ./run-all.sh
#   USERNAME=alice PASSWORD=secret ./run-all.sh
#
# 输出：
#   results/scenario-{N}.json  - 每个场景的 k6 原始 JSON 流
#   stress_test_result_500.json - parse-results.js 汇总后的旧报告格式
#
# 注意：scenario-3-login 不在列（用户决策：登录沿用 50 并发基线）。

set -euo pipefail
cd "$(dirname "$0")"
mkdir -p results

# 找 k6：优先 PATH，找不到则尝试 ~/bin（手动解压安装的位置）
if ! command -v k6 >/dev/null 2>&1; then
  if [ -x "$HOME/bin/k6.exe" ]; then
    export PATH="$HOME/bin:$PATH"
  else
    echo "[ERROR] k6 not found in PATH nor in ~/bin. Install: winget install GrafanaLabs.K6"
    exit 127
  fi
fi

# 默认使用 testuser1 而非 admin：
#   1. admin 应专做管理操作，不应参与压测发帖
#   2. admin 在前置失败请求下容易被 AuthService 的 5 次失败锁定 30 秒机制误锁
#   3. 种子数据 seed_data.sql 中 testuser1-testuser10 密码均为 admin123（与 admin 同源 hash）
#   4. testuser1 为 USER 角色，发帖权限符合压测需求
export USERNAME="${USERNAME:-testuser1}"
export PASSWORD="${PASSWORD:-admin123}"
export BASE_URL="${BASE_URL:-http://localhost:9000}"

# 7 个场景（顺序：先只读，再极限读，再写）
SCENARIOS=(
  scenario-1-read-baseline
  scenario-2a-categories
  scenario-2b-posts
  scenario-2c-announcements
  scenario-4-mixed-read-write
  scenario-5-write-posts
  scenario-6-post-detail-comments
)

FAILED=()
for scenario in "${SCENARIOS[@]}"; do
  echo ""
  echo "================================================================"
  echo " Running ${scenario}  (BASE_URL=${BASE_URL}, USER=${USERNAME})"
  echo "================================================================"
  # threshold 失败时 k6 返回非 0，但我们希望继续跑后续场景并最终汇总
  if ! k6 run --out json="results/${scenario}.json" "${scenario}.js"; then
    echo "[WARN] ${scenario} exited non-zero (threshold breach likely). Keeping result JSON for parsing."
    FAILED+=("${scenario}")
  fi
done

echo ""
echo "================================================================"
echo " Parsing results -> stress_test_result_500.json"
echo "================================================================"
node parse-results.js results/*.json > stress_test_result_500.json
echo "Done. Output: stress_test_result_500.json"

if [ "${#FAILED[@]}" -gt 0 ]; then
  echo ""
  echo "[WARN] The following scenarios breached thresholds (see k6 logs above):"
  for s in "${FAILED[@]}"; do
    echo "  - ${s}"
  done
fi
