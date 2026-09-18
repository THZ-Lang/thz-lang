# ==============================================================================
# THZ-LANG Test All - roda todos os testes JVM + relatorio
# Uso: .\scripts\test-all.ps1 [-Watch] [-Module core|cli|gui|api|lsp]
# ==============================================================================

[CmdletBinding()]
param(
    [ValidateSet("all","core","cli","gui","api","lsp","bench")]
    [string]$Module = "all",
    [switch]$Watch
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

$map = @{
    "core"  = ":thz-core-jvm:test"
    "cli"   = ":thz-cli-jvm:test"
    "gui"   = ":thz-gui-jvm:test"
    "api"   = ":thz-api-jvm:test"
    "lsp"   = ":thz-lsp-jvm:test"
    "bench" = ":thz-bench-jvm:test"
    "all"   = "test"
}

$task = $map[$Module]
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG - Test $Module ($task)" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

if ($Watch) {
    & "$Raiz\gradlew.bat" $task --continuous --parallel
} else {
    & "$Raiz\gradlew.bat" $task --parallel
    if ($LASTEXITCODE -ne 0) { Write-Host "[FALHA] Testes $Module falharam" -ForegroundColor Red; exit 1 }
    Write-Host "[OK] Testes $Module passaram" -ForegroundColor Green
    Write-Host " Relatorios: JVM/*/build/reports/tests/test/index.html" -ForegroundColor DarkGray
}
