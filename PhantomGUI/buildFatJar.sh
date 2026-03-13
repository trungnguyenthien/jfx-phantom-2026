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
# Script này sẽ:
#   1. Chạy shadowJar để tạo JAR trong build/libs/
#   2. Copy JAR + toàn bộ src/main/resources/root/ vào dist/
#   3. Tạo script launcher (run.sh / run.bat) để extract JavaFX native libs và chạy JAR
#   4. dist/ là thư mục deploy cuối cùng — chạy JAR từ đây
#
# Cross-platform:
#   • JAR được build với JavaFX native libs cho CẢ 3 platform:
#     macOS (intel + apple silicon), Windows, Linux
#   • Chỉ cần build 1 lần trên macOS → JAR chạy được trên Windows và Ubuntu
#   • Yêu cầu: Java 17+ được cài trên máy chạy
#   • QUAN TRỌNG: Dùng script run.sh / run.bat thay vì "java -jar" trực tiếp
#
# Lưu ý:
#   • build/libs/  → Gradle output directory, KHÔNG chỉnh sửa thủ công
#   • dist/        → Thư mục deploy, copy cho client/server là đủ
#   • src/main/resources/root/ → Source of truth cho runtime files (data.json, ...)
#
# Output:
#   dist/
#     PhantomGUI-1.0-SNAPSHOT-all.jar   ← fat JAR (cross-platform)
#     run.sh                             ← launcher cho macOS/Linux
#     run.bat                            ← launcher cho Windows
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
echo -e "\n${YELLOW}[1/4] Dọn thư mục dist/ cũ...${NC}"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# ── 2. Chạy Gradle shadowJar ──────────────────────────────────────────────────
echo -e "\n${YELLOW}[2/4] Chạy Gradle shadowJar...${NC}"
./gradlew shadowJar --quiet

# Kiểm tra JAR được tạo ra
BUILT_JAR="$SCRIPT_DIR/build/libs/$JAR_NAME"
if [ ! -f "$BUILT_JAR" ]; then
  echo -e "${RED}✗ Không tìm thấy JAR tại: $BUILT_JAR${NC}"
  exit 1
fi

# ── 3. Copy JAR + root resources vào dist/ ───────────────────────────────────
echo -e "\n${YELLOW}[3/4] Đóng gói vào dist/...${NC}"

cp "$BUILT_JAR" "$DIST_DIR/$JAR_NAME"
echo -e "  ${GREEN}✓${NC} $JAR_NAME"

if [ -d "$ROOT_RESOURCES" ] && [ "$(ls -A "$ROOT_RESOURCES")" ]; then
  cp -r "$ROOT_RESOURCES"/. "$DIST_DIR/"
  echo -e "  ${GREEN}✓${NC} resources/root/ → dist/"
else
  echo -e "  ${YELLOW}⚠${NC}  resources/root/ trống, không có file nào được copy."
fi

# ── 4. Tạo script launcher ────────────────────────────────────────────────────
echo -e "\n${YELLOW}[4/4] Tạo script launcher...${NC}"

# --- run.sh (macOS / Linux) ---
cat > "$DIST_DIR/run.sh" << 'LAUNCHER_SH'
#!/usr/bin/env bash
# PhantomGUI Launcher — macOS / Linux
# JavaFX native libs được extract ra thư mục tạm để JVM có thể load

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/PhantomGUI-1.0-SNAPSHOT-all.jar"

# Kiểm tra Java
if ! command -v java &> /dev/null; then
  echo "ERROR: Không tìm thấy Java. Vui lòng cài Java 17+."
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
  echo "ERROR: Yêu cầu Java 17+. Phiên bản hiện tại: $JAVA_VER"
  exit 1
fi

# Thư mục tạm cho native libs (tên cố định để tái sử dụng giữa các lần chạy)
NATIVE_DIR="$HOME/.phantomgui/natives"
mkdir -p "$NATIVE_DIR"

# Extract native libs từ JAR nếu chưa có
OS=$(uname -s)
if [ "$OS" = "Darwin" ]; then
  LIB_EXT="dylib"
else
  LIB_EXT="so"
fi

# Kiểm tra xem có cần extract không (dựa vào timestamp JAR)
STAMP_FILE="$NATIVE_DIR/.jar_mtime"
JAR_MTIME=$(stat -c %Y "$JAR" 2>/dev/null || stat -f %m "$JAR" 2>/dev/null)
PREV_MTIME=$(cat "$STAMP_FILE" 2>/dev/null || echo "0")

if [ "$JAR_MTIME" != "$PREV_MTIME" ]; then
  echo "[INFO] Extracting JavaFX native libraries..."
  # Extract chỉ các file native lib
  cd "$NATIVE_DIR"
  jar xf "$JAR" $(jar tf "$JAR" | grep -E "\.$LIB_EXT$" | tr '\n' ' ') 2>/dev/null || true
  cd "$SCRIPT_DIR"
  echo "$JAR_MTIME" > "$STAMP_FILE"
fi

# Chạy ứng dụng với native library path
exec java \
  -Djava.library.path="$NATIVE_DIR" \
  -Dprism.verbose=false \
  --add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
  --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
  -jar "$JAR" "$@"
LAUNCHER_SH
chmod +x "$DIST_DIR/run.sh"
echo -e "  ${GREEN}✓${NC} run.sh"

# --- run.bat (Windows) ---
cat > "$DIST_DIR/run.bat" << 'LAUNCHER_BAT'
@echo off
REM PhantomGUI Launcher — Windows
REM JavaFX native libs được extract ra thư mục tạm để JVM có thể load

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%PhantomGUI-1.0-SNAPSHOT-all.jar"
set "NATIVE_DIR=%USERPROFILE%\.phantomgui\natives"

REM Kiểm tra Java
where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Khong tim thay Java. Vui long cai Java 17+.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

REM Tạo thư mục native
if not exist "%NATIVE_DIR%" mkdir "%NATIVE_DIR%"

REM Extract DLL từ JAR (dùng jar tool đi kèm JDK)
REM Kiểm tra timestamp để tránh extract mỗi lần
set "STAMP_FILE=%NATIVE_DIR%\.jar_mtime"
set "NEED_EXTRACT=1"

if exist "%STAMP_FILE%" (
    REM Nếu đã có stamp thì bỏ qua extract (đơn giản hoá)
    set "NEED_EXTRACT=0"
)

if "!NEED_EXTRACT!"=="1" (
    echo [INFO] Extracting JavaFX native libraries...
    pushd "%NATIVE_DIR%"
    for /f "tokens=*" %%i in ('jar tf "%JAR%" ^| findstr /E ".dll"') do (
        jar xf "%JAR%" "%%i" >nul 2>&1
    )
    REM Di chuyển DLL từ subdirectory lên root nếu cần
    for /r . %%f in (*.dll) do (
        if not "%%~dpf"=="%NATIVE_DIR%\" (
            move /y "%%f" "%NATIVE_DIR%\" >nul 2>&1
        )
    )
    popd
    echo done > "%STAMP_FILE%"
    echo [INFO] Done.
)

REM Chạy ứng dụng
java ^
  -Djava.library.path="%NATIVE_DIR%" ^
  --add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED ^
  --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED ^
  -jar "%JAR%" %*

if errorlevel 1 (
    echo.
    echo [ERROR] Ung dung thoat voi loi. Ma loi: %errorlevel%
    pause
)
LAUNCHER_BAT
echo -e "  ${GREEN}✓${NC} run.bat"

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
echo -e "  ${CYAN}macOS/Linux : cd dist && ./run.sh${NC}"
echo -e "  ${CYAN}Windows     : cd dist && run.bat${NC}"
echo ""
echo -e "  ${YELLOW}⚠  KHÔNG dùng 'java -jar' trực tiếp — native libs sẽ không được load đúng.${NC}"
echo ""

