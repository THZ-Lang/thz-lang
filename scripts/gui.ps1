# ==============================================================================
# THZ-LANG GUI - lanca IDE (WebView padrao, Swing opcional)
# Uso: .\scripts\gui.ps1 [-Swing] [-Port 8080]
# Padrao Fase 3: WebView (thz gui) - sem AWT/GraalVM issues
# ==============================================================================

[CmdletBinding()]
param([switch]$Swing, [int]$Port = 0)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

if ($Swing) {
    Write-Host "[GUI] Iniciando Swing legada (./gradlew :thz-gui:gui)..." -ForegroundColor Yellow
    & "$Raiz\gradlew.bat" :thz-gui-jvm:gui
    exit $LASTEXITCODE
}

# WebView (padrao) - usa thz-cli via Gradle run
Write-Host "[GUI] Iniciando IDE WebView (padrao Fase 3)..." -ForegroundColor Cyan
Write-Host "      thz gui ? http://127.0.0.1:porta/ (Edge/WebView2 --app)" -ForegroundColor DarkGray
Write-Host "      Dica: .\scripts\gui.ps1 -Swing  para Swing legada" -ForegroundColor DarkGray

# Garante jar pronto
$jar = Get-ChildItem "$Raiz\JVM\thz-cli-jvm\build\libs\thz-jvm-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jar) {
    Write-Host "[gui] JAR nao encontrado, compilando..." -ForegroundColor Yellow
    & "$Raiz\gradlew.bat" :thz-cli-jvm:shadowJar -x test 2>&1 | Out-Null
}

& "$Raiz\gradlew.bat" :thz-cli-jvm:run --args="gui"
