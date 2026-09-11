# ==============================================================================
# THZ-LANG — Script de Compilação em Lote de Todos os Exemplos
# ==============================================================================

[CmdletBinding()]
param (
    [string]$Origem = "exemplos",
    [string]$Destino = "dist/exemplos_compilados"
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

$Gradlew = if (Test-Path "$Raiz\gradlew.bat") { "$Raiz\gradlew.bat" } else { "gradle" }

Write-Host "`nIniciando compilação de todos os programas THZ-LANG..." -ForegroundColor Cyan
& $Gradlew :thz-cli-jvm:run --args="compile-all --origem `"$Origem`" --saida `"$Destino`""

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCESSO] Todos os exemplos compilados em: $Destino" -ForegroundColor Green
} else {
    Write-Error "Falha durante a compilação dos exemplos."
}
