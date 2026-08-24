# ==============================================================================
# THZ-LANG Dev - modo desenvolvimento (API + GUI watch)
# Uso: .\scripts\dev.ps1 [-ApiOnly] [-GuiOnly]
# ==============================================================================

[CmdletBinding()]
param([switch]$ApiOnly, [switch]$GuiOnly)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

if ($GuiOnly) {
    Write-Host "[dev] GUI WebView..." -ForegroundColor Cyan
    & "$PSScriptRoot\gui.ps1"
    exit $LASTEXITCODE
}
if ($ApiOnly) {
    Write-Host "[dev] API Spring Boot..." -ForegroundColor Cyan
    & "$Raiz\gradlew.bat" :thz-api-jvm:bootRun
    exit $LASTEXITCODE
}

Write-Host "[dev] Iniciando API (bootRun) em paralelo..." -ForegroundColor Yellow
Write-Host "      API: http://localhost:8080 | GUI: thz gui (outro terminal: .\scripts\gui.ps1)" -ForegroundColor DarkGray
& "$Raiz\gradlew.bat" :thz-api-jvm:bootRun
