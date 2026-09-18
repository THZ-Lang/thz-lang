# ==============================================================================
# Sincronizador Automático de Versão — THZ-LANG Engine (PowerShell)
# Fonte Única da Verdade (Single Source of Truth): version.txt
# ==============================================================================
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Ler-Utf8($caminho) {
    return [System.IO.File]::ReadAllText($caminho, [System.Text.Encoding]::UTF8)
}

function Escrever-Utf8($caminho, $conteudo) {
    [System.IO.File]::WriteAllText($caminho, $conteudo, $Utf8NoBom)
}

$ArqVersao = Join-Path $Raiz "version.txt"
if (-not (Test-Path $ArqVersao)) {
    Write-Error "Arquivo version.txt não encontrado na raiz: $Raiz"
}

$Versao = (Ler-Utf8 $ArqVersao).Trim().Trim([char]0xFEFF)
if ([string]::IsNullOrWhiteSpace($Versao)) {
    Write-Error "version.txt está vazio."
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " Sincronizando versão global do THZ-LANG: v$Versao" -ForegroundColor Green
Write-Host " Fonte da verdade: $ArqVersao" -ForegroundColor Gray
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. thz.config.json
$ConfigJson = Join-Path $Raiz "thz.config.json"
if (Test-Path $ConfigJson) {
    $conteudo = Ler-Utf8 $ConfigJson
    $conteudo = [regex]::Replace($conteudo, '("versao":\s*")[^"]+(")', "`${1}$Versao`${2}")
    Escrever-Utf8 $ConfigJson $conteudo
    Write-Host "  [OK] thz.config.json -> $Versao" -ForegroundColor Green
}

# 2. Rust Runtime (Cargo.toml)
$CargoToml = @(Join-Path $Raiz "..\thz-runtime-rs\Cargo.toml"), (Join-Path $Raiz "src\runtime_rs\Cargo.toml") | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($CargoToml) {
    $conteudo = Ler-Utf8 $CargoToml
    $conteudo = [regex]::Replace($conteudo, '(?m)^(version\s*=\s*")[^"]+(")', "`${1}$Versao`${2}")
    Escrever-Utf8 $CargoToml $conteudo
    Write-Host "  [OK] $CargoToml -> $Versao" -ForegroundColor Green
}

# 3. Rust Runtime (Cargo.lock)
$CargoLock = @(Join-Path $Raiz "..\thz-runtime-rs\Cargo.lock"), (Join-Path $Raiz "src\runtime_rs\Cargo.lock") | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($CargoLock) {
    $conteudo = Ler-Utf8 $CargoLock
    $conteudo = [regex]::Replace($conteudo, '(name = "thz_runtime_rs"\r?\nversion = ")[^"]+(")', "`${1}$Versao`${2}")
    Escrever-Utf8 $CargoLock $conteudo
    Write-Host "  [OK] $CargoLock -> $Versao" -ForegroundColor Green
}

# 4. WASM Bridge (wasm.rs)
$WasmRs = @(Join-Path $Raiz "..\thz-runtime-rs\src\wasm.rs"), (Join-Path $Raiz "src\runtime_rs\src\wasm.rs") | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($WasmRs) {
    $conteudo = Ler-Utf8 $WasmRs
    $conteudo = [regex]::Replace($conteudo, 'CString::new\("[^"]+-WASM"\)', "CString::new(`"$Versao-WASM`")")
    Escrever-Utf8 $WasmRs $conteudo
    Write-Host "  [OK] $WasmRs -> $Versao-WASM" -ForegroundColor Green
}

# 5. VS Code Extension (package.json & package-lock.json)
$PkgJson = @(Join-Path $Raiz "..\thz-vscode\package.json"), (Join-Path $Raiz "Extensions\thz-lsp-vscode\package.json") | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($PkgJson) {
    $conteudo = Ler-Utf8 $PkgJson
    $conteudo = [regex]::Replace($conteudo, '("version":\s*")[^"]+(")', "`${1}$Versao`${2}")
    Escrever-Utf8 $PkgJson $conteudo
    Write-Host "  [OK] $PkgJson -> $Versao" -ForegroundColor Green
}

# 6. VS Code Extension (extension.ts)
$ExtTs = @(Join-Path $Raiz "..\thz-vscode\src\extension.ts"), (Join-Path $Raiz "Extensions\thz-lsp-vscode\src\extension.ts") | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($ExtTs) {
    $conteudo = Ler-Utf8 $ExtTs
    $conteudo = [regex]::Replace($conteudo, "'THZ-LANG \d+\.\d+\.\d+'", "'THZ-LANG $Versao'")
    Escrever-Utf8 $ExtTs $conteudo
    Write-Host "  [OK] $ExtTs -> THZ-LANG $Versao" -ForegroundColor Green
}

# 7. Sincronizar version.txt em todos os submódulos irmãos
Get-ChildItem (Join-Path $Raiz "..\thz-*\version.txt") -ErrorAction SilentlyContinue | ForEach-Object {
    Escrever-Utf8 $_.FullName "$Versao`n"
    Write-Host "  [OK] $($_.Name) ($($_.Directory.Name)) -> $Versao" -ForegroundColor Green
}

# 8. README.md (Badge)
$ReadmeMd = Join-Path $Raiz "README.md"
if (Test-Path $ReadmeMd) {
    $conteudo = Ler-Utf8 $ReadmeMd
    $conteudo = [regex]::Replace($conteudo, "badge/version-\d+\.\d+\.\d+-blue\.svg", "badge/version-$Versao-blue.svg")
    Escrever-Utf8 $ReadmeMd $conteudo
    Write-Host "  [OK] README.md badge -> $Versao" -ForegroundColor Green
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " Sincronização concluída com sucesso!" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
