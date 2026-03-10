@echo off
:: =============================================================================
:: setup-environment.bat — Cài đặt môi trường cho PhantomGUI (Windows)
:: =============================================================================
::
:: Những gì script này làm:
::   1. Kiểm tra / hướng dẫn cài đặt Java 17+
::   2. Kiểm tra / hướng dẫn cài đặt Python 3.8+
::   3. Tạo virtual environment (.venv) trong thư mục project
::   4. Cài numpy + shapely vào .venv
::   5. In tóm tắt kết quả
::
:: Usage: Chạy đúp chuột hoặc mở cmd:
::   setup-environment.bat
:: =============================================================================

setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "SCRIPT_DIR=%~dp0"
:: Bỏ dấu \ cuối
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "VENV_DIR=%SCRIPT_DIR%\.venv"
set "VENV_PYTHON=%VENV_DIR%\Scripts\python.exe"

:: ── Màu (dùng ANSI nếu terminal hỗ trợ) ─────────────────────────────────────
:: Windows 10+ hỗ trợ ANSI trong cmd khi được enable
for /f "tokens=4-5 delims=. " %%i in ('ver') do set "WIN_VER=%%i.%%j"

echo.
echo =================================================
echo   PhantomGUI — Setup Environment (Windows)
echo =================================================
echo.

:: ── 1. Kiểm tra Java ─────────────────────────────────────────────────────────
echo [1/4] Kiem tra Java...
echo ─────────────────────────────────────────────────

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN]  Java chua duoc cai hoac chua co trong PATH.
    echo.
    echo         Tai va cai Java 17+ tai:
    echo           https://adoptium.net
    echo.
    echo         Chon: Temurin 17 LTS ^> Windows x64 Installer ^(.msi^)
    echo         Dam bao tick "Add to PATH" khi cai dat.
    echo.
    echo         Sau khi cai xong, mo lai terminal moi va chay lai script nay.
    echo.
    pause
    exit /b 1
) else (
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "JAVA_VER_STR=%%v"
    )
    :: Bỏ dấu nháy kép
    set "JAVA_VER_STR=!JAVA_VER_STR:"=!"
    echo [OK]    Java da co san: !JAVA_VER_STR!
)

echo.

:: ── 2. Kiểm tra Python ────────────────────────────────────────────────────────
echo [2/4] Kiem tra Python...
echo ─────────────────────────────────────────────────

set "PYTHON_CMD="

:: Thử python, python3
for %%p in (python python3) do (
    if not defined PYTHON_CMD (
        %%p --version >nul 2>&1
        if !errorlevel! equ 0 (
            for /f "tokens=2" %%v in ('%%p --version 2^>^&1') do (
                set "PY_VER=%%v"
            )
            :: Lấy major.minor
            for /f "tokens=1,2 delims=." %%a in ("!PY_VER!") do (
                set "PY_MAJOR=%%a"
                set "PY_MINOR=%%b"
            )
            if !PY_MAJOR! GEQ 3 (
                if !PY_MINOR! GEQ 8 (
                    set "PYTHON_CMD=%%p"
                    echo [OK]    Python !PY_VER! da co san.
                ) else (
                    echo [WARN]  Python !PY_VER! qua cu ^(can 3.8+^), bo qua.
                )
            ) else (
                echo [WARN]  Python !PY_VER! qua cu ^(can 3.8+^), bo qua.
            )
        )
    )
)

if not defined PYTHON_CMD (
    echo [WARN]  Python 3.8+ chua duoc cai hoac chua co trong PATH.
    echo.
    echo         Tai va cai Python tai:
    echo           https://www.python.org/downloads/
    echo.
    echo         Luu y: Dam bao tick "Add Python to PATH" khi cai dat.
    echo.
    echo         Sau khi cai xong, mo lai terminal moi va chay lai script nay.
    echo.
    pause
    exit /b 1
)

echo.

:: ── 3. Tạo Virtual Environment ────────────────────────────────────────────────
echo [3/4] Tao Virtual Environment (.venv)...
echo ─────────────────────────────────────────────────

if exist "%VENV_DIR%" (
    echo [WARN]  .venv da ton tai tai: %VENV_DIR%
    set /p "RECREATE=        Tao lai tu dau? [y/N]: "
    if /i "!RECREATE!"=="y" (
        echo [INFO]  Xoa .venv cu...
        rmdir /s /q "%VENV_DIR%"
    ) else (
        echo [INFO]  Giu nguyen .venv hien tai.
        goto :install_packages
    )
)

echo [INFO]  Tao virtual environment tai: %VENV_DIR%
%PYTHON_CMD% -m venv "%VENV_DIR%"
if %errorlevel% neq 0 (
    echo [ERROR] Khong the tao virtual environment.
    echo         Thu chay: %PYTHON_CMD% -m pip install virtualenv
    pause
    exit /b 1
)
echo [OK]    Virtual environment da duoc tao.

echo.

:: ── 4. Cài Python packages ────────────────────────────────────────────────────
:install_packages
echo [4/4] Cai Python packages (numpy, shapely)...
echo ─────────────────────────────────────────────────

if not exist "%VENV_PYTHON%" (
    echo [ERROR] Khong tim thay python trong .venv: %VENV_PYTHON%
    pause
    exit /b 1
)

echo [INFO]  Upgrade pip...
"%VENV_PYTHON%" -m pip install --upgrade pip --quiet
if %errorlevel% neq 0 (
    echo [WARN]  Khong the upgrade pip, tiep tuc voi pip hien tai...
)

echo [INFO]  Cai numpy ^>= 1.21 va shapely ^>= 1.8...
"%VENV_PYTHON%" -m pip install "numpy>=1.21" "shapely>=1.8"
if %errorlevel% neq 0 (
    echo [ERROR] Cai dat that bai. Kiem tra ket noi mang va thu lai.
    pause
    exit /b 1
)

:: Kiểm tra sau khi cài
"%VENV_PYTHON%" -c "import numpy, shapely; print('[OK]    numpy:', numpy.__version__, '| shapely:', shapely.__version__)" 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Kiem tra import that bai sau khi cai.
    pause
    exit /b 1
)

:: ── 5. Tóm tắt ────────────────────────────────────────────────────────────────
echo.
echo =================================================
echo   Ket qua
echo =================================================
echo.

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo   [OK] Java:    %%v
)
for /f "tokens=2" %%v in ('"%VENV_PYTHON%" --version 2^>^&1') do (
    echo   [OK] Python:  %%v  ^(venv^)
)
for /f %%v in ('"%VENV_PYTHON%" -c "import numpy; print(numpy.__version__)" 2^>^&1') do (
    echo   [OK] numpy:   %%v
)
for /f %%v in ('"%VENV_PYTHON%" -c "import shapely; print(shapely.__version__)" 2^>^&1') do (
    echo   [OK] shapely: %%v
)
echo   [OK] venv:    %VENV_DIR%

echo.
echo   Moi truong da san sang!
echo.
echo   Luu y: Kich hoat venv truoc khi chay JAR:
echo     %VENV_DIR%\Scripts\activate
echo     java -jar dist\PhantomGUI-1.0-SNAPSHOT-all.jar
echo.
echo   Hoac build luon:
echo     gradlew.bat shadowJar
echo.
echo =================================================
pause
endlocal

