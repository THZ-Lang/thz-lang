# ==============================================================================
# THZ-LANG Health Check -- Diagnostico de ambiente (Windows)
# Verifica: Java 25, GraalVM, LLVM/Clang, MinGW/GCC, Node, Gradle
# Uso: .\scripts\health-check.ps1 [-Fix]
# ==============================================================================

[CmdletBinding()]
param([switch]$Fix)

$ErrorActionPreference = "Continue"
$Raiz = Resolve-Path "$PSScriptRoot\.."

function Test-Cmd($name, $desc, $candidates) {
    $found = Get-Command $name -ErrorAction SilentlyContinue
    if ($found) { Write-Host "[OK] $desc : $($found.Source)" -ForegroundColor Green; return $true }
    foreach ($c in $candidates) {
        $p = Get-Item $c -ErrorAction SilentlyContinue
        if ($p) { Write-Host "[OK] $desc : $($p.FullName) (via fallback)" -ForegroundColor Green; return $true }
    }
    Write-Host "[FALTA] $desc ($name) nao encontrado" -ForegroundColor Red
    return $false
}

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG -- Health Check" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

$ok = $true

# Java 25
$javaOk = Test-Cmd "java" "Java" @("C:\Users\lucas\scoop\apps\openjdk25\current\bin\java.exe", "C:\Program Files\Java\jdk-25\bin\java.exe")
if ($javaOk) { & java -version 2>&1 | Select-Object -First 1 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray } }
else { $ok = $false }

# GraalVM
$graal = $env:GRAALVM_HOME
if (-not $graal -or -not (Test-Path "$graal\bin\native-image.cmd")) {
    $graal = @("C:\Users\lucas\scoop\apps\graalvm25-jdk\current", "$env:USERPROFILE\scoop\apps\graalvm25-jdk\current") | Where-Object { Test-Path "$_\bin\native-image.cmd" } | Select-Object -First 1
}
if ($graal) { Write-Host "[OK] GraalVM : $graal" -ForegroundColor Green } else { Write-Host "[AVISO] GraalVM native-image nao encontrado (opcional, so para thz.exe nativo)" -ForegroundColor Yellow }

# LLVM/Clang
if (-not (Test-Cmd "clang" "LLVM Clang" @("C:\Users\lucas\scoop\apps\llvm\current\bin\clang.exe"))) { Write-Host "      Scoop: scoop install llvm" -ForegroundColor DarkYellow }

# GCC/MinGW
if (-not (Test-Cmd "gcc" "MinGW GCC" @("C:\Users\lucas\scoop\apps\mingw\current\bin\gcc.exe"))) { Write-Host "      Scoop: scoop install mingw" -ForegroundColor DarkYellow }

# Node
if (-not (Test-Cmd "node" "Node.js" @())) { $ok = $false } else { & node -v | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray } }

# Gradle wrapper
if (Test-Path "$Raiz\gradlew.bat") { Write-Host "[OK] Gradle Wrapper : $Raiz\gradlew.bat" -ForegroundColor Green } else { Write-Host "[FALTA] gradlew.bat" -ForegroundColor Red; $ok = $false }

# Docker / Podman Containers
$podmanCmd = Get-Command "podman" -ErrorAction SilentlyContinue
$dockerCmd = Get-Command "docker" -ErrorAction SilentlyContinue
if ($podmanCmd) {
    Write-Host "[OK] Podman Container Runtime : $($podmanCmd.Source)" -ForegroundColor Green
} elseif ($dockerCmd) {
    Write-Host "[OK] Docker Container Runtime : $($dockerCmd.Source)" -ForegroundColor Green
} else {
    Write-Host "[INFO] Docker / Podman nao encontrados (opcional, para conteineres/devcontainer)" -ForegroundColor DarkGray
}

# Estrutura
@("JVM/thz-core-jvm","JVM/thz-cli-jvm","JVM/thz-gui-jvm","exemplos") | ForEach-Object {
    if (Test-Path "$Raiz\$_") { Write-Host "[OK] $_" -ForegroundColor Green } else { Write-Host "[FALTA] $_" -ForegroundColor Red; $ok = $false }
}

Write-Host "=================================================" -ForegroundColor Cyan
if ($ok) { Write-Host " Ambiente OK -- pronto para .\scripts\setup.ps1" -ForegroundColor Green }
else { Write-Host " Corrija os itens [FALTA] acima. Use -Fix para tentar instalar via Scoop." -ForegroundColor Yellow }
if ($Fix) {
    Write-Host "`n[FIX] Tentando scoop install openjdk25 llvm mingw nodejs..." -ForegroundColor Yellow
    & scoop install openjdk25 llvm mingw nodejs 2>&1 | Out-Null
    Write-Host " Re-execute .\scripts\health-check.ps1" -ForegroundColor Cyan
}
