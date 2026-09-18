# ==============================================================================
# THZ-LANG GUI - Lança a Desktop IDE oficial (Swing + FlatLaf)
# Uso: .\scripts\gui.ps1
# ==============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "[GUI] Iniciando Desktop IDE oficial THZ-LANG (Swing + FlatLaf)..." -ForegroundColor Cyan
& "$Raiz\gradlew.bat" :thz-gui-jvm:gui

