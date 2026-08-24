@echo off
title VS2026 Portable (x86)
call "%~dp0setup_x86.bat"
if not "%~1"=="" (
    %*
    exit /b %errorlevel%
)
cd /d %USERPROFILE%
echo.
echo === VS2026 Portable - Developer Command Prompt (x86) ===
cl 2>&1 | findstr /C:"Version"
echo.
cmd /k
