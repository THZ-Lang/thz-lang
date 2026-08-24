@echo off
REM thz.cmd — shim raiz (Windows CMD) — aceita --gui -> gui
setlocal
set "ARGS=%*"
if "%ARGS%"=="" set "ARGS=gui"
REM normaliza --gui para gui
if "%ARGS:~0,3%"=="gui" (
    call "%~dp0gradlew.bat" :thz-gui-jvm:gui
) else (
    call "%~dp0gradlew.bat" :thz-cli-jvm:run --args="%ARGS%"
)
exit /b %ERRORLEVEL%

