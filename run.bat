@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac --release 17 -encoding UTF-8 -d build src\attendance\*.java
if errorlevel 1 (
    echo Install JDK 17 or newer and ensure java and javac are on PATH.
    pause
    exit /b 1
)
java -cp build attendance.AttendanceApp
