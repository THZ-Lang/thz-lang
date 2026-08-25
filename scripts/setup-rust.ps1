# ==============================================================================
# setup-rust.ps1 — Instalação portátil e autônoma do Rust Toolchain em .tools/rust
# Não requer permissões de Administrador e não polui o PATH global do Windows.
# ==============================================================================

[CmdletBinding()]
param(
    [string]$Toolchain = "stable-x86_64-pc-windows-gnu",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
$ToolsDir = "$Raiz\.tools\rust"
$CargoHome = "$ToolsDir\cargo"
$RustupHome = "$ToolsDir\rustup"
$BinDir = "$CargoHome\bin"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG — PROVISIONAMENTO PORTATIL DE TOOLCHAIN RUST (.tools/rust)" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

if ((Test-Path "$BinDir\cargo.exe") -and -not $Force) {
    Write-Host "[OK] Rust/Cargo portátil já está instalado em: $BinDir" -ForegroundColor Green
    & "$BinDir\rustc.exe" --version
    & "$BinDir\cargo.exe" --version
    exit 0
}

# Cria diretórios isolados
New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null
New-Item -ItemType Directory -Path $CargoHome -Force | Out-Null
New-Item -ItemType Directory -Path $RustupHome -Force | Out-Null

$InstallerUrl = "https://win.rustup.rs/x86_64"
$InstallerPath = "$ToolsDir\rustup-init.exe"

Write-Host "[1/3] Baixando instalador standalone do Rust ($InstallerUrl)..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $InstallerUrl -OutFile $InstallerPath -UseBasicParsing

Write-Host "[2/3] Instalando toolchain Rust portátil ($Toolchain)..." -ForegroundColor Yellow
$env:RUSTUP_HOME = $RustupHome
$env:CARGO_HOME = $CargoHome

$procArgs = @(
    "-y",
    "--no-modify-path",
    "--default-toolchain", $Toolchain,
    "--profile", "minimal"
)

$proc = Start-Process -FilePath $InstallerPath -ArgumentList $procArgs -NoNewWindow -Wait -PassThru

if ($proc.ExitCode -eq 0) {
    Write-Host "[3/3] Configurando shims e permissões..." -ForegroundColor Yellow
    
    # Adiciona ao PATH da sessão atual
    $env:PATH = "$BinDir;$env:PATH"
    
    Write-Host "`n==========================================================================" -ForegroundColor Green
    Write-Host " RUST PORTATIL INSTALADO COM SUCESSO!" -ForegroundColor Green
    Write-Host " Binários: $BinDir" -ForegroundColor White
    if (Test-Path "$BinDir\rustc.exe") {
        & "$BinDir\rustc.exe" --version
        & "$BinDir\cargo.exe" --version
    }
    Write-Host "==========================================================================" -ForegroundColor Green
} else {
    Write-Host "[ERRO] Falha ao instalar Rust portátil. Código de saída: $($proc.ExitCode)" -ForegroundColor Red
    exit $proc.ExitCode
}
