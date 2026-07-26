@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
title CleanHub - TubesTest

REM JDK 17 dari Spring Tool Suite (folder D:\apps\sts-...)
set "JDK_BIN=D:\apps\sts-4.13.0.RELEASE\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.1.v20211116-1657\jre\bin"
if not exist "%JDK_BIN%\javac.exe" (
    if defined JAVA_HOME set "JDK_BIN=%JAVA_HOME%\bin"
)

if not exist "%JDK_BIN%\javac.exe" (
    echo [ERROR] javac tidak ditemukan. Install JDK 17+ atau edit path JDK_BIN di run.bat
    pause
    exit /b 1
)

set "MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar"
if not exist "%MYSQL_JAR%" set "MYSQL_JAR=%~dp0lib\mysql-connector-j-9.1.0.jar"

if not exist "%MYSQL_JAR%" (
    echo [ERROR] mysql-connector-j tidak ada di:
    echo   %USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\9.1.0\
    pause
    exit /b 1
)

set "SRC=%~dp0src\main\java"
set "OUT=%~dp0out"
set "ROOT=%~dp0.."
set "CP=%MYSQL_JAR%;%OUT%"

echo.
echo === Matikan server lama di port 8080 ===
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo === Compile ===
if exist "%OUT%\com" rmdir /s /q "%OUT%\com"
mkdir "%OUT%" 2>nul

set "SOURCES="
for /r "%SRC%" %%f in (*.java) do (
    set "FN=%%~nxf"
    if /i not "!FN!"=="LaundryApiServlet.java" if /i not "!FN!"=="StatusServlet.java" (
        set "SOURCES=!SOURCES! "%%f""
    )
)

"%JDK_BIN%\javac" -encoding UTF-8 -d "%OUT%" -cp "%MYSQL_JAR%" !SOURCES!
if errorlevel 1 (
    echo [ERROR] Compile gagal.
    pause
    exit /b 1
)

if not exist "%~dp0src\main\resources\database.properties" (
    copy /Y "%~dp0src\main\resources\database.properties.example" "%~dp0src\main\resources\database.properties" >nul
)
if exist "%~dp0src\main\resources\database.properties" (
    copy /Y "%~dp0src\main\resources\database.properties" "%OUT%\" >nul
) else (
    copy /Y "%~dp0src\main\resources\database.properties.example" "%OUT%\database.properties" >nul
)

echo.
echo === Server jalan ===
echo Browser: http://localhost:8080/  (refresh pakai Ctrl+F5)
echo MySQL XAMPP harus ON + database cleanhub sudah di-import
echo Jika fitur baru tidak jalan: tutup jendela ini lalu jalankan run.bat lagi
echo Tekan Ctrl+C untuk stop
echo.

cd /d "%ROOT%"
"%JDK_BIN%\java" -cp "%CP%" com.mycompany.tubestest.TubesTest

pause
