# ==============================================================================
# THZ-LANG Clean - limpa builds, caches e dist
# Uso: .\scripts\clean.ps1 [-Deep] [-DistOnly]
# ==============================================================================

[CmdletBinding()]
param([switch]$Deep, [switch]$DistOnly)

$ErrorActionPreference = "Continue"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

if ($DistOnly) {
    Write-Host "[clean] Removendo dist/..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "$Raiz\dist" -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force "$Raiz\target" -ErrorAction SilentlyContinue
    Write-Host "[OK] dist/ limpo" -ForegroundColor Green
    exit 0
}

Write-Host "[clean] gradlew clean..." -ForegroundColor Yellow
& "$Raiz\gradlew.bat" clean 2>&1 | Select-Object -Last 5

Get-ChildItem "$Raiz\JVM\*\build", "$Raiz\build", "$Raiz\target", "$Raiz\dist" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  removendo $($_.FullName)" -ForegroundColor DarkGray
    Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue
}

if ($Deep) {
    Write-Host "[clean --deep] Limpando .gradle cache local..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "$Raiz\.gradle" -ErrorAction SilentlyContinue
}

Write-Host "[OK] Clean concluido" -ForegroundColor Green
