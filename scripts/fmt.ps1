# ==============================================================================
# THZ-LANG Fmt - formata arquivos .thz (canonico)
# Uso: .\scripts\fmt.ps1 <arquivo.thz|dir> [--check] [--escrever]
# ==============================================================================

param(
    [string]$Alvo = "exemplos/faturamento.thz",
    [switch]$Check,
    [switch]$Escrever
)

$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

$extra = @()
if ($Check) { $extra += "--check" }
if ($Escrever) { $extra += "--escrever" }

$argsStr = "fmt $Alvo $($extra -join ' ')"
Write-Host "[fmt] $argsStr" -ForegroundColor Cyan
& "$Raiz\gradlew.bat" :thz-cli-jvm:run --args="$argsStr"
