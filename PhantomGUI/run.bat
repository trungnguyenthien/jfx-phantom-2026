@echo off
REM Script để chạy executable JAR trên Windows

set JAR_FILE=build\libs\PhantomGUI-1.0-SNAPSHOT-all.jar

if not exist "%JAR_FILE%" (
    echo JAR file khong ton tai. Dang build...
    call gradlew.bat shadowJar
)

echo Dang chay PhantomGUI...
java -jar "%JAR_FILE%"
pause

