@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac --release 17 -Xlint:all -encoding UTF-8 -d build src\attendance\*.java tests\attendance\*.java
if errorlevel 1 exit /b 1
java -cp build attendance.AttendanceUiTest
