@echo off
title VS2026 Portable (x64)
call "%~dp0setup_x64.bat"
if not "%~1"=="" (
    %*
    exit /b %errorlevel%
)
cd /d %USERPROFILE%
echo.
echo === VS2026 Portable - Developer Command Prompt (x64) ===
cl 2>&1 | findstr /C:"Version"
echo.
cmd /k
