# ==============================================================================
# THZ-LANG Build JVM - compila todos os modulos JVM (sem jpackage/native)
# Uso: .\scripts\build-jvm.ps1 [-SkipTests] [-Parallel]
# ==============================================================================

[CmdletBinding()]
param([switch]$SkipTests, [switch]$NoParallel)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

$argsGradle = @("build")
if ($SkipTests) { $argsGradle += "-x"; $argsGradle += "test" }
if (-not $NoParallel) { $argsGradle += "--parallel" }

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG - Build JVM" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " Gradle: gradlew $($argsGradle -join ' ')" -ForegroundColor DarkGray

& "$Raiz\gradlew.bat" @argsGradle
if ($LASTEXITCODE -ne 0) { Write-Error "Build JVM falhou" }

Write-Host "`n[OK] Build JVM concluido." -ForegroundColor Green
Get-ChildItem "$Raiz\JVM\*\build\libs\*.jar" -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  $($_.FullName) ($([math]::Round($_.Length/1KB)) KB)" -ForegroundColor DarkGray }
