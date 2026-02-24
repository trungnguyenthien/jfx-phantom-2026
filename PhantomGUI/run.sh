#!/bin/bash
# Script để chạy executable JAR

JAR_FILE="build/libs/PhantomGUI-1.0-SNAPSHOT-all.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file không tồn tại. Đang build..."
    ./gradlew shadowJar
fi

echo "Đang chạy PhantomGUI..."
java -jar "$JAR_FILE"

