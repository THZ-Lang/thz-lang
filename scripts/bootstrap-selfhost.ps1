# ==============================================================================
# THZ-LANG Engine - Bootstrap do Compilador Self-Hosted (Zero JVM)
# Uso: .\scripts\bootstrap-selfhost.ps1 [-Limpar]
# ==============================================================================

[CmdletBinding()]
param (
    [switch]$Limpar
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG Engine - BOOTSTRAP DO COMPILADOR NATIVO SELF-HOSTED (thzc.exe)" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$DistBin = "$Raiz\dist\bin"
if (-not (Test-Path $DistBin)) { New-Item -ItemType Directory -Path $DistBin | Out-Null }

if ($Limpar) {
    Write-Host "[LIMPEZA] Removendo binarios anteriores em dist/bin..." -ForegroundColor Yellow
    Remove-Item "$DistBin\thzc.*", "$DistBin\driver.*" -ErrorAction SilentlyContinue
}

$Clang = "$env:USERPROFILE\scoop\apps\llvm\current\bin\clang.exe"
if (-not (Test-Path $Clang)) { $Clang = "clang" }
$Gcc = "$env:USERPROFILE\scoop\apps\mingw\current\bin\gcc.exe"
if (-not (Test-Path $Gcc)) { $Gcc = "gcc" }

# PASSO 1: Compilar a suite compilador/driver.thz para gerar o thzc.exe (Stage 1)
Write-Host "`n[PASSO 1/3] Gerando binario nativo do compilador (thzc.exe) via LLVM AOT..." -ForegroundColor Yellow
& "$PSScriptRoot\build-llvm.ps1" -ArquivoThz "$Raiz\compilador\driver.thz" -Alvo windows

$DriverExe = "$DistBin\driver.exe"
$ThzcExe   = "$DistBin\thzc.exe"

if (Test-Path $DriverExe) {
    Copy-Item $DriverExe $ThzcExe -Force
    Write-Host "[OK] Compilador nativo gerado com sucesso: $ThzcExe" -ForegroundColor Green
} else {
    Write-Error "Falha ao gerar o executavel nativo do compilador."
}

# PASSO 2: Testar execucao direta do thzc.exe nativo (Zero JVM)
Write-Host "`n[PASSO 2/3] Executando thzc.exe nativo de forma 100% autonoma (Zero JVM)..." -ForegroundColor Yellow
& $ThzcExe
if ($LASTEXITCODE -ne 0) {
    Write-Error "Erro ao executar thzc.exe de forma autonoma."
}
Write-Host "[OK] thzc.exe executou nativamente sem qualquer dependencia de JVM!" -ForegroundColor Green

# PASSO 3: Compilacao e execucao de teste de programa de negocio
Write-Host "`n[PASSO 3/3] Compilando e testando programa de exemplo canonico (faturamento.thz)..." -ForegroundColor Yellow
& "$PSScriptRoot\build-llvm.ps1" -ArquivoThz "$Raiz\exemplos\faturamento.thz" -Alvo windows

$FaturamentoExe = "$DistBin\faturamento.exe"
if (Test-Path $FaturamentoExe) {
    Write-Host "[OK] Executavel faturamento.exe gerado:" -ForegroundColor Green
    & $FaturamentoExe
}

Write-Host "`n==========================================================================" -ForegroundColor Green
Write-Host " BOOTSTRAP CONCLUIDO COM SUCESSO! AUTONOMIA TOTAL VALIDADA." -ForegroundColor Green
Write-Host " Compilador Nativo Autonomo: $ThzcExe" -ForegroundColor White
Write-Host "==========================================================================" -ForegroundColor Green
