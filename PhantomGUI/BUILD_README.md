# PhantomGUI - JavaFX Application

## Build Executable JAR

Để build executable JAR với tất cả dependencies:

```bash
./gradlew shadowJar
```

hoặc build cùng với các task khác:

```bash
./gradlew build
```

File JAR sẽ được tạo tại: `build/libs/PhantomGUI-1.0-SNAPSHOT-all.jar`

## Chạy ứng dụng

### Cách 1: Chạy trực tiếp JAR file

```bash
java -jar build/libs/PhantomGUI-1.0-SNAPSHOT-all.jar
```

### Cách 2: Sử dụng script tiện lợi

```bash
./run.sh       # trên macOS/Linux
run.bat        # trên Windows
```

Script sẽ tự động build nếu JAR chưa tồn tại và sau đó chạy ứng dụng.

### Cách 3: Chạy qua Gradle (từ source)

```bash
./gradlew run
```

### Cách 4: Chạy shadow JAR qua Gradle

```bash
./gradlew runJar
```

Task này sẽ tự động build shadow JAR (nếu cần) và chạy ứng dụng.

## Yêu cầu hệ thống

- Java 17 hoặc cao hơn
- JavaFX 17 runtime (đã được đóng gói trong JAR)

## Cấu trúc dự án

- `src/main/kotlin/` - Kotlin source code
- `src/main/java/` - Java source code và module-info
- `src/main/resources/` - FXML files và resources
- `build/libs/` - Output JAR files sau khi build

## Ghi chú

- Executable JAR được tạo bởi Shadow plugin
- Tất cả dependencies (bao gồm JavaFX, Kotlin stdlib, Coroutines) đã được đóng gói trong JAR
- File JAR có thể chạy độc lập trên bất kỳ máy nào có Java 17+

