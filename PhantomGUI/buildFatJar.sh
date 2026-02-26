#!/usr/bin/env bash
# =============================================================================
# buildFatJar.sh — Build PhantomGUI fat JAR + đóng gói ra thư mục dist/
# =============================================================================
#
# Luồng build:
#
#   ./gradlew shadowJar
#       └─▶ build/libs/PhantomGUI-1.0-SNAPSHOT-all.jar   (Gradle output, tạm thời)
#
#   Script này sẽ:
#       1. Chạy shadowJar để tạo JAR trong build/libs/
#       2. Copy JAR + toàn bộ src/main/resources/root/ vào dist/
#       3. dist/ là thư mục deploy cuối cùng — chạy JAR từ đây
#
# Lưu ý:
#   • build/libs/  → Gradle output directory, KHÔNG chỉnh sửa thủ công
#   • dist/        → Thư mục deploy, copy cho client/server là đủ
#   • src/main/resources/root/ → Source of truth cho runtime files (data.json, ...)
#
# Output:
#   dist/
#     PhantomGUI-1.0-SNAPSHOT-all.jar   ← fat JAR (tất cả dependencies)
#     data.json                          ← và toàn bộ file trong resources/root/
# =============================================================================

set -euo pipefail

# ── Màu sắc ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ── Đường dẫn gốc (thư mục chứa script này) ──────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DIST_DIR="$SCRIPT_DIR/dist"
JAR_NAME="PhantomGUI-1.0-SNAPSHOT-all.jar"
ROOT_RESOURCES="$SCRIPT_DIR/src/main/resources/root"

echo -e "${CYAN}=================================================${NC}"
echo -e "${CYAN}  PhantomGUI — Build Fat JAR${NC}"
echo -e "${CYAN}=================================================${NC}"

# ── 1. Dọn thư mục dist/ cũ ───────────────────────────────────────────────────
echo -e "\n${YELLOW}[1/3] Dọn thư mục dist/ cũ...${NC}"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# ── 2. Chạy Gradle shadowJar ──────────────────────────────────────────────────
echo -e "\n${YELLOW}[2/3] Chạy Gradle shadowJar...${NC}"
./gradlew shadowJar --quiet

# Kiểm tra JAR được tạo ra
BUILT_JAR="$SCRIPT_DIR/build/libs/$JAR_NAME"
if [ ! -f "$BUILT_JAR" ]; then
  echo -e "${RED}✗ Không tìm thấy JAR tại: $BUILT_JAR${NC}"
  exit 1
fi

# ── 3. Copy JAR + root resources vào dist/ ───────────────────────────────────
echo -e "\n${YELLOW}[3/3] Đóng gói vào dist/...${NC}"

cp "$BUILT_JAR" "$DIST_DIR/$JAR_NAME"
echo -e "  ${GREEN}✓${NC} $JAR_NAME"

if [ -d "$ROOT_RESOURCES" ] && [ "$(ls -A "$ROOT_RESOURCES")" ]; then
  cp -r "$ROOT_RESOURCES"/. "$DIST_DIR/"
  echo -e "  ${GREEN}✓${NC} resources/root/ → dist/"
else
  echo -e "  ${YELLOW}⚠${NC}  resources/root/ trống, không có file nào được copy."
fi

# ── Tóm tắt ──────────────────────────────────────────────────────────────────
echo -e "\n${GREEN}=================================================${NC}"
echo -e "${GREEN}  Build thành công!${NC}"
echo -e "${GREEN}=================================================${NC}"
echo -e "  Output: ${CYAN}$DIST_DIR${NC}"
echo ""
echo -e "  Nội dung dist/:"
ls -1 "$DIST_DIR" | while read -r f; do
  echo -e "    • $f"
done

echo ""
echo -e "  Chạy ứng dụng:"
echo -e "  ${CYAN}cd dist && java -jar $JAR_NAME${NC}"
echo ""

