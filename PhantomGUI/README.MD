# PhantomGUI

GUI application để build voxel phantom từ file VRML cho Geant4 (`.g4dcm`), xây dựng bằng **Kotlin + JavaFX**.

---

## Yêu cầu hệ thống

### Java

| Platform | Yêu cầu |
|----------|---------|
| Tất cả   | **Java 17** hoặc cao hơn |

> JavaFX và tất cả Java dependencies đã được đóng gói sẵn trong fat JAR — không cần cài thêm.

### Python

Script `buildPhantom.py` cần **Python 3.8+** với các thư viện sau:

| Thư viện | Phiên bản tối thiểu | Mục đích |
|----------|---------------------|---------|
| `numpy`  | ≥ 1.21              | Xử lý ma trận voxel 3D |
| `shapely`| ≥ 1.8               | Kiểm tra điểm trong polygon (ROI) |

---

## Cài đặt tự động (khuyến nghị)

Chạy script setup để tự động cài đặt toàn bộ môi trường (Java 17, Python 3.8+, venv, numpy, shapely):

### macOS / Linux

```bash
chmod +x setup-environment.sh
./setup-environment.sh
```

### Windows

```cmd
setup-environment.bat
```

> Script sẽ phát hiện OS, kiểm tra phần mềm đã cài, cài phần còn thiếu, và tạo `.venv` với đầy đủ dependencies.

---

## Cài đặt thủ công

### Java

#### macOS

```bash
# Dùng Homebrew (khuyến nghị)
brew install openjdk@17

# Hoặc tải từ https://adoptium.net
```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

#### Linux (Fedora/RHEL)

```bash
sudo dnf install java-17-openjdk-devel
```

#### Windows

Tải và cài đặt từ: https://adoptium.net  
Chọn **Temurin 17 (LTS)**, bản **Windows x64 Installer (.msi)**.

Kiểm tra sau khi cài:
```cmd
java -version
```

### Python dependencies

#### macOS / Linux — Virtual Environment (khuyến nghị)

```bash
# Tạo venv trong thư mục project
python3 -m venv .venv

# Kích hoạt venv
source .venv/bin/activate

# Cài thư viện
pip install numpy shapely

# Kiểm tra
python -c "import numpy, shapely; print('OK')"
```

#### Windows — Virtual Environment (khuyến nghị)

```cmd
:: Tạo venv
python -m venv .venv

:: Kích hoạt venv
.venv\Scripts\activate

:: Cài thư viện
pip install numpy shapely

:: Kiểm tra
python -c "import numpy, shapely; print('OK')"
```

#### macOS — Cài trực tiếp

```bash
brew install python@3
pip3 install numpy shapely

# Nếu gặp lỗi "externally-managed-environment"
pip3 install numpy shapely --break-system-packages
```

#### Linux (Ubuntu/Debian) — Cài trực tiếp

```bash
sudo apt install python3 python3-pip
pip3 install numpy shapely
# hoặc
pip3 install numpy shapely --break-system-packages
```

#### Linux (Fedora/RHEL) — Cài trực tiếp

```bash
sudo dnf install python3 python3-pip
pip3 install numpy shapely
```

#### Windows — Cài trực tiếp

```cmd
:: Tải Python từ https://www.python.org/downloads/
:: Đảm bảo tick "Add Python to PATH" khi cài

pip install numpy shapely
```

---

## Build ứng dụng

### Cách nhanh — Dùng script `buildFatJar.sh`

Script này sẽ build fat JAR và đóng gói tất cả ra thư mục `dist/`:

```bash
# macOS / Linux
chmod +x buildFatJar.sh
./buildFatJar.sh
```

Output:
```
dist/
  PhantomGUI-1.0-SNAPSHOT-all.jar   ← fat JAR
  buildPhantom.py                    ← Python script
  data.json                          ← Runtime data
```

### Cách thủ công — Gradle

```bash
# macOS / Linux
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

JAR sẽ được tạo tại: `build/libs/PhantomGUI-1.0-SNAPSHOT-all.jar`

---

## Chạy ứng dụng

### Từ thư mục `dist/` (sau khi build bằng buildFatJar.sh)

```bash
# macOS / Linux
cd dist
java -jar PhantomGUI-1.0-SNAPSHOT-all.jar

# Windows
cd dist
java -jar PhantomGUI-1.0-SNAPSHOT-all.jar
```

### Dùng script tiện lợi (từ source)

```bash
# macOS / Linux
./run.sh

# Windows
run.bat
```

### Chạy qua Gradle (debug / development)

```bash
./gradlew run
```

---

## Cấu trúc thư mục

```
PhantomGUI/
├── src/
│   └── main/
│       ├── kotlin/          ← Kotlin source code
│       ├── java/            ← module-info.java
│       └── resources/
│           └── root/        ← Runtime files (copy ra cạnh JAR khi deploy)
│               ├── buildPhantom.py
│               └── data.json
├── build/
│   └── libs/                ← Gradle output (fat JAR)
├── dist/                    ← Thư mục deploy (output của buildFatJar.sh)
├── buildFatJar.sh           ← Build script
├── run.sh                   ← Run script (macOS/Linux)
└── run.bat                  ← Run script (Windows)
```

---

## Lưu ý khi deploy

- Thư mục `dist/` phải chứa **cả JAR lẫn các file trong `resources/root/`** cùng cấp.
- `buildPhantom.py` được gọi tự động khi nhấn **OPERATE** — Python phải có sẵn trong `PATH` của hệ thống.
- Nếu dùng virtual environment, hãy kích hoạt venv trước khi chạy JAR, hoặc trỏ `PythonExecutor` về đường dẫn python trong venv.

---

## Troubleshooting

### `ModuleNotFoundError: No module named 'numpy'`

Python đang dùng không có `numpy`. Chạy lệnh sau để xác định Python nào đang được gọi:

```bash
which python3      # macOS/Linux
where python       # Windows
```

Sau đó cài vào đúng bản đó:

```bash
/đường/dẫn/python3 -m pip install numpy shapely
```

### `java: command not found`

Java chưa được cài hoặc chưa có trong `PATH`. Cài Java 17+ theo hướng dẫn bên trên.

### Lỗi permission khi chạy `buildFatJar.sh`

```bash
chmod +x buildFatJar.sh
./buildFatJar.sh
```

