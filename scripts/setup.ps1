# ==============================================================================
# THZ-LANG Setup - One-click bootstrap (Windows)
# Uso: .\scripts\setup.ps1 [-SkipTests]
# Faz: health-check ? core:publish ? cli:jar ? testes ? jar pronto
# ==============================================================================

[CmdletBinding()]
param([switch]$SkipTests)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG - Setup (One Click)" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Health check rapido
Write-Host "`n[0/4] Verificando ambiente..." -ForegroundColor Yellow
& "$PSScriptRoot\health-check.ps1"
if ($LASTEXITCODE -ne 0) { Write-Host "[AVISO] Health check com pendencias, continuando..." -ForegroundColor Yellow }

# 2. Publicar thz-core no mavenLocal (necessario para composite build)
Write-Host "`n[1/4] Publicando thz-core (mavenLocal)..." -ForegroundColor Yellow
& "$Raiz\gradlew.bat" :thz-core-jvm:publishToMavenLocal --parallel 2>&1 | Select-Object -Last 15
if ($LASTEXITCODE -ne 0) { Write-Error "Falha em publishToMavenLocal" }

# 3. Build JVM (shadowJar)
Write-Host "`n[2/4] Compilando JVM (shadowJar)..." -ForegroundColor Yellow
$gradleArgs = @(":thz-cli-jvm:shadowJar", ":thz-gui-jvm:classes", ":thz-api-jvm:classes")
if ($SkipTests) { $gradleArgs += "-x"; $gradleArgs += "test" }
& "$Raiz\gradlew.bat" @gradleArgs 2>&1 | Select-Object -Last 10
if ($LASTEXITCODE -ne 0) { Write-Error "Falha no build JVM" }

# 4. Testes (opcional)
if (-not $SkipTests) {
    Write-Host "`n[3/4] Rodando testes (core/cli/gui)..." -ForegroundColor Yellow
    & "$Raiz\gradlew.bat" :thz-core-jvm:test :thz-cli-jvm:test :thz-gui-jvm:test 2>&1 | Select-Object -Last 20
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERRO] Testes falharam - verifique build/reports" -ForegroundColor Red; exit 1 }
}

# 5. Resumo
$jar = Get-ChildItem "$Raiz\JVM\thz-cli-jvm\build\libs\thz-jvm-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
$jar2 = Get-ChildItem "$Raiz\target\thz-jvm-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " SETUP OK!" -ForegroundColor Green
Write-Host " JAR CLI : $($jar.FullName)" -ForegroundColor White
Write-Host " JAR dist: $($jar2.FullName)" -ForegroundColor White
Write-Host "-------------------------------------------------" -ForegroundColor Green
Write-Host " Proximos passos:" -ForegroundColor Cyan
Write-Host "   thz run exemplos/faturamento.thz   (via .\thz.ps1)" -ForegroundColor White
Write-Host "   .\scripts\gui.ps1                  (IDE WebView, padrao)" -ForegroundColor White
Write-Host "   .\scripts\build-jvm.ps1            (rebuild rapido)" -ForegroundColor White
Write-Host "   npm run thz -- check <arquivo>" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Green
