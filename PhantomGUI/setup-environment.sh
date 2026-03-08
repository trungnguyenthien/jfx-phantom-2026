#!/usr/bin/env bash
# =============================================================================
# setup-environment.sh — Cài đặt môi trường cho PhantomGUI
# Hỗ trợ: macOS, Ubuntu/Debian, Fedora/RHEL
# =============================================================================
#
# Những gì script này làm:
#   1. Phát hiện hệ điều hành
#   2. Kiểm tra / cài đặt Java 17+
#   3. Kiểm tra / cài đặt Python 3.8+
#   4. Tạo virtual environment (.venv) trong thư mục project
#   5. Cài numpy + shapely vào .venv
#   6. In tóm tắt kết quả
#
# Usage:
#   chmod +x setup-environment.sh
#   ./setup-environment.sh
# =============================================================================

set -euo pipefail

# ── Màu sắc ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────────────
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
header()  { echo -e "\n${BOLD}${CYAN}━━━ $* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── 0. Detect OS ──────────────────────────────────────────────────────────────
header "Phát hiện hệ điều hành"

OS="unknown"
DISTRO="unknown"

if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
    info "Detected: macOS $(sw_vers -productVersion)"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    if command -v apt-get &>/dev/null; then
        DISTRO="debian"
        info "Detected: Ubuntu / Debian"
    elif command -v dnf &>/dev/null; then
        DISTRO="fedora"
        info "Detected: Fedora / RHEL"
    elif command -v yum &>/dev/null; then
        DISTRO="rhel"
        info "Detected: RHEL / CentOS (yum)"
    else
        error "Linux distribution không được nhận diện (cần apt, dnf hoặc yum)."
        exit 1
    fi
else
    error "Hệ điều hành không được hỗ trợ: $OSTYPE"
    error "Script này chỉ hỗ trợ macOS và Linux."
    error "Trên Windows, hãy chạy: setup-environment.bat"
    exit 1
fi

# ── 1. Java 17+ ───────────────────────────────────────────────────────────────
header "Kiểm tra Java"

install_java_macos() {
    if command -v brew &>/dev/null; then
        info "Cài Java 17 qua Homebrew..."
        brew install openjdk@17
        # Symlink để java command hoạt động
        JAVA_HOME_BREW="$(brew --prefix openjdk@17)"
        if [[ ! -f /usr/local/bin/java ]]; then
            sudo ln -sfn "${JAVA_HOME_BREW}/bin/java" /usr/local/bin/java 2>/dev/null || true
        fi
        export PATH="${JAVA_HOME_BREW}/bin:$PATH"
        success "Java 17 đã được cài qua Homebrew."
        warn "Thêm vào ~/.zshrc hoặc ~/.bash_profile:"
        warn "  export PATH=\"${JAVA_HOME_BREW}/bin:\$PATH\""
    else
        warn "Homebrew chưa được cài. Tải Java 17 thủ công tại:"
        warn "  https://adoptium.net"
        warn "Sau khi cài xong, chạy lại script này."
        exit 1
    fi
}

install_java_linux() {
    if [[ "$DISTRO" == "debian" ]]; then
        info "Cài Java 17 qua apt..."
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-17-jdk
    elif [[ "$DISTRO" == "fedora" ]]; then
        info "Cài Java 17 qua dnf..."
        sudo dnf install -y java-17-openjdk-devel
    elif [[ "$DISTRO" == "rhel" ]]; then
        info "Cài Java 17 qua yum..."
        sudo yum install -y java-17-openjdk-devel
    fi
    success "Java 17 đã được cài."
}

JAVA_OK=false
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    # Xử lý version dạng "1.8" → 8
    if [[ "$JAVA_VER" == "1" ]]; then
        JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2)
    fi
    if [[ "$JAVA_VER" -ge 17 ]] 2>/dev/null; then
        success "Java $JAVA_VER đã có sẵn: $(java -version 2>&1 | head -1)"
        JAVA_OK=true
    else
        warn "Java hiện tại là version $JAVA_VER — cần Java 17+."
    fi
fi

if [[ "$JAVA_OK" == false ]]; then
    info "Đang cài đặt Java 17..."
    if [[ "$OS" == "macos" ]]; then
        install_java_macos
    else
        install_java_linux
    fi
    # Kiểm tra lại
    if command -v java &>/dev/null; then
        success "Java đã được cài: $(java -version 2>&1 | head -1)"
    else
        error "Không thể tìm thấy 'java' sau khi cài. Kiểm tra lại PATH."
        exit 1
    fi
fi

# ── 2. Python 3.8+ ────────────────────────────────────────────────────────────
header "Kiểm tra Python"

install_python_macos() {
    if command -v brew &>/dev/null; then
        info "Cài Python 3 qua Homebrew..."
        brew install python@3
        export PATH="$(brew --prefix python@3)/bin:$PATH"
        success "Python 3 đã được cài qua Homebrew."
    else
        error "Homebrew chưa được cài. Tải Python 3 tại: https://www.python.org/downloads/"
        exit 1
    fi
}

install_python_linux() {
    if [[ "$DISTRO" == "debian" ]]; then
        info "Cài Python 3 + pip qua apt..."
        sudo apt-get update -qq
        sudo apt-get install -y python3 python3-pip python3-venv
    elif [[ "$DISTRO" == "fedora" ]]; then
        info "Cài Python 3 + pip qua dnf..."
        sudo dnf install -y python3 python3-pip
    elif [[ "$DISTRO" == "rhel" ]]; then
        info "Cài Python 3 + pip qua yum..."
        sudo yum install -y python3 python3-pip
    fi
    success "Python 3 đã được cài."
}

# Tìm python3 khả dụng
PYTHON_CMD=""
for cmd in python3 python; do
    if command -v "$cmd" &>/dev/null; then
        PY_VER=$("$cmd" --version 2>&1 | awk '{print $2}')
        PY_MAJOR=$(echo "$PY_VER" | cut -d'.' -f1)
        PY_MINOR=$(echo "$PY_VER" | cut -d'.' -f2)
        if [[ "$PY_MAJOR" -ge 3 && "$PY_MINOR" -ge 8 ]]; then
            PYTHON_CMD="$cmd"
            success "Python $PY_VER đã có sẵn: $(command -v $cmd)"
            break
        else
            warn "Python $PY_VER quá cũ (cần 3.8+), bỏ qua."
        fi
    fi
done

if [[ -z "$PYTHON_CMD" ]]; then
    info "Đang cài đặt Python 3..."
    if [[ "$OS" == "macos" ]]; then
        install_python_macos
    else
        install_python_linux
    fi
    # Tìm lại sau khi cài
    for cmd in python3 python; do
        if command -v "$cmd" &>/dev/null; then
            PYTHON_CMD="$cmd"
            break
        fi
    done
    if [[ -z "$PYTHON_CMD" ]]; then
        error "Không tìm thấy python3 sau khi cài. Kiểm tra lại PATH."
        exit 1
    fi
    success "Python đã được cài: $($PYTHON_CMD --version)"
fi

# ── 3. Tạo Virtual Environment ────────────────────────────────────────────────
header "Tạo Virtual Environment (.venv)"

VENV_DIR="$SCRIPT_DIR/.venv"

if [[ -d "$VENV_DIR" ]]; then
    warn ".venv đã tồn tại tại: $VENV_DIR"
    read -r -p "       Tạo lại từ đầu? [y/N] " REPLY
    REPLY="${REPLY:-N}"
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        info "Xóa .venv cũ..."
        rm -rf "$VENV_DIR"
    else
        info "Giữ nguyên .venv hiện tại."
    fi
fi

if [[ ! -d "$VENV_DIR" ]]; then
    info "Tạo virtual environment tại: $VENV_DIR"

    # Ubuntu có thể cần python3-venv riêng
    if ! "$PYTHON_CMD" -m venv "$VENV_DIR" 2>/dev/null; then
        if [[ "$OS" == "linux" && "$DISTRO" == "debian" ]]; then
            warn "Thiếu module venv. Đang cài python3-venv..."
            sudo apt-get install -y python3-venv
            "$PYTHON_CMD" -m venv "$VENV_DIR"
        else
            error "Không thể tạo virtual environment."
            exit 1
        fi
    fi
    success "Virtual environment đã được tạo."
fi

# ── 4. Cài Python packages vào venv ──────────────────────────────────────────
header "Cài Python packages (numpy, shapely)"

VENV_PYTHON="$VENV_DIR/bin/python"
VENV_PIP="$VENV_DIR/bin/pip"

info "Upgrade pip..."
"$VENV_PYTHON" -m pip install --upgrade pip --quiet

info "Cài numpy >= 1.21 và shapely >= 1.8..."
"$VENV_PYTHON" -m pip install "numpy>=1.21" "shapely>=1.8"

# Kiểm tra sau khi cài
if "$VENV_PYTHON" -c "import numpy, shapely; print('numpy:', numpy.__version__, '| shapely:', shapely.__version__)" 2>/dev/null; then
    success "numpy và shapely đã được cài thành công."
else
    error "Cài đặt thất bại. Kiểm tra kết nối mạng và thử lại."
    exit 1
fi

# ── 5. Tóm tắt ────────────────────────────────────────────────────────────────
header "Tóm tắt"

echo ""
echo -e "  ${GREEN}✓${NC} Java:    $(java -version 2>&1 | head -1)"
echo -e "  ${GREEN}✓${NC} Python:  $("$VENV_PYTHON" --version)"
echo -e "  ${GREEN}✓${NC} venv:    $VENV_DIR"
echo -e "  ${GREEN}✓${NC} numpy:   $("$VENV_PYTHON" -c 'import numpy; print(numpy.__version__)')"
echo -e "  ${GREEN}✓${NC} shapely: $("$VENV_PYTHON" -c 'import shapely; print(shapely.__version__)')"
echo ""
echo -e "${BOLD}${GREEN}Môi trường đã sẵn sàng!${NC}"
echo ""
echo -e "  ${YELLOW}Lưu ý:${NC} Khi chạy ứng dụng, đảm bảo Python trong .venv được sử dụng."
echo -e "  Kích hoạt venv trước khi chạy JAR:"
echo -e "    ${CYAN}source .venv/bin/activate${NC}"
echo -e "    ${CYAN}java -jar dist/PhantomGUI-1.0-SNAPSHOT-all.jar${NC}"
echo ""
echo -e "  Hoặc build luôn:"
echo -e "    ${CYAN}./buildFatJar.sh${NC}"
echo ""

