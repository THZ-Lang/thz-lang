# ==============================================================================
# THZ-LANG Package All - distribuicao completa (jpackage + opcional native + llvm)
# Uso: .\scripts\package-all.ps1 [-SkipTests] [-WithNative] [-WithLlvm]
# Gera: dist/thz/thz.exe (jpackage app-image) + opcional dist/bin/thz.exe (GraalVM)
# ==============================================================================

[CmdletBinding()]
param([switch]$SkipTests, [switch]$WithNative, [switch]$WithLlvm)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG - Package All" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

# 1. jpackage (sempre - padrao recomendado)
Write-Host "`n[1/3] jpackage (dist/thz)..." -ForegroundColor Yellow
$pkgArgs = @()
if ($SkipTests) { $pkgArgs += "-PularTestes" }
& "$Raiz\JVM\thz-cli-jvm\scripts\build-package.ps1" @pkgArgs
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage falhou" }

# 2. GraalVM native (opcional, experimental - ver docs)
if ($WithNative) {
    Write-Host "`n[2/3] GraalVM native-image (dist/bin/thz.exe)..." -ForegroundColor Yellow
    $nArgs = @()
    if ($SkipTests) { $nArgs += "-PularTestes" }
    & "$Raiz\scripts\build-native.ps1" @nArgs
    if ($LASTEXITCODE -ne 0) { Write-Host "[AVISO] native-image falhou (opcional)" -ForegroundColor Yellow }
}

# 3. LLVM AOT (legado, opcional)
if ($WithLlvm) {
    Write-Host "`n[3/3] LLVM AOT (dist/bin/*.exe)..." -ForegroundColor Yellow
    & "$Raiz\scripts\build-all.ps1"
    if ($LASTEXITCODE -ne 0) { Write-Host "[AVISO] build-llvm falhou (legado)" -ForegroundColor Yellow }
}

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " Package All OK" -ForegroundColor Green
Write-Host "  jpackage : dist/thz/thz.exe (+ thz-gui.exe)" -ForegroundColor White
if ($WithNative) { Write-Host "  native   : dist/bin/thz.exe" -ForegroundColor White }
if ($WithLlvm)  { Write-Host "  llvm     : dist/bin/*.exe + *.elf" -ForegroundColor White }
Write-Host "=================================================" -ForegroundColor Green
