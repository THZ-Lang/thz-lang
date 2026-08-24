# ==============================================================================
# DEPRECIADO: Win32 thz_runtime.c feio. Use package-all (jpackage) ou thz.ps1.
# Este script agora pula *_gui.thz por padrao. Use -ForceLegado para incluir GUI.
# ==============================================================================

[CmdletBinding()]
param([switch]$ForceLegado)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "==========================================================================" -ForegroundColor Cyan
if ($ForceLegado) { Write-Host " THZ-LANG Engine - COMPILACAO AOT NATIVA (LEGADO FORCADO)" -ForegroundColor Yellow }
else { Write-Host " THZ-LANG Engine - COMPILACAO AOT NATIVA (sem GUI Win32 feio)" -ForegroundColor Cyan }
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " Dica: use .\scripts\package-all.ps1 (jpackage WebView) ou .\thz.ps1 run" -ForegroundColor DarkGray

$FontesCompilador = Get-ChildItem -Path "$Raiz\compilador\*.thz" -ErrorAction SilentlyContinue
$FontesExemplos   = Get-ChildItem -Path "$Raiz\exemplos\*.thz" -ErrorAction SilentlyContinue
$TodosFontes      = @($FontesCompilador) + @($FontesExemplos)
if (-not $ForceLegado) { $TodosFontes = $TodosFontes | Where-Object { $_.Name -notmatch "_gui" } }

$Total = $TodosFontes.Count
Write-Host "[INFO] $Total arquivos de fonte .thz encontrados para compilacao nativa Dual-OS.`n" -ForegroundColor Yellow

$Sucessos = 0

foreach ($fonte in $TodosFontes) {
    $Nome = $fonte.Name
    Write-Host "--------------------------------------------------------------------------" -ForegroundColor Gray
    Write-Host "Compilando ($($Sucessos + 1)/$Total): $Nome..." -ForegroundColor Yellow
    
    try {
        & powershell.exe -ExecutionPolicy Bypass -File "$Raiz\scripts\build-llvm.ps1" -ArquivoThz $fonte.FullName -Alvo ambos
        if ($LASTEXITCODE -eq 0) {
            $Sucessos++
        } else {
            Write-Host "[ERRO] Falha ao compilar $Nome" -ForegroundColor Red
        }
    } catch {
        Write-Host "[ERRO] Excecao ao compilar $Nome" -ForegroundColor Red
    }
}

Write-Host "`n==========================================================================" -ForegroundColor Green
Write-Host " COMPILACAO EM LOTE CONCLUIDA!" -ForegroundColor Green
Write-Host " Total Processado: $Sucessos / $Total arquivos fonte compilados com sucesso." -ForegroundColor White
Write-Host " Binarios publicados em: $Raiz\dist\bin\" -ForegroundColor White
Write-Host "==========================================================================" -ForegroundColor Green
