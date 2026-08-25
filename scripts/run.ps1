# ==============================================================================
# THZ-LANG Run - wrapper para thz run/check/audit/doc/ir/ui
# Uso: .\scripts\run.ps1 <comando> <arquivo.thz> [args...]
# Ex:  .\scripts\run.ps1 run exemplos/faturamento.thz
#      .\scripts\run.ps1 check exemplos/showcase_widgets_gui.thz --estrito
# ==============================================================================

param(
    [Parameter(Position=0)][string]$Comando = "run",
    [Parameter(Position=1)][string]$Arquivo,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$Rest
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

# Se primeiro arg parece arquivo .thz ou .thzui, assume comando=run
if (($Comando -like "*.thz" -or $Comando -like "*.thzui") -and -not $Arquivo) { $Arquivo = $Comando; $Comando = "run" }

$argsStr = @($Comando)
if ($Arquivo) { $argsStr += $Arquivo }
if ($Rest) { $argsStr += $Rest }

$joined = $argsStr -join " "
Write-Host "[thz] gradlew :thz-cli-jvm:run --args=`"$joined`"" -ForegroundColor Cyan

& "$Raiz\gradlew.bat" :thz-cli-jvm:run --args="$joined"
exit $LASTEXITCODE
