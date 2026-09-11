# ==============================================================================
# Sincronizador Automático de Versão — THZ-LANG Engine (PowerShell)
# Fonte Única da Verdade (Single Source of Truth): version.txt
# ==============================================================================
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."

$ArqVersao = Join-Path $Raiz "version.txt"
if (-not (Test-Path $ArqVersao)) {
    Write-Error "Arquivo version.txt não encontrado na raiz: $Raiz"
}

$Versao = (Get-Content $ArqVersao -Raw).Trim()
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
    $conteudo = Get-Content $ConfigJson -Raw
    $conteudo = [regex]::Replace($conteudo, '("versao":\s*")[^"]+(")', "`${1}$Versao`${2}")
    Set-Content -Path $ConfigJson -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] thz.config.json -> $Versao" -ForegroundColor Green
}

# 2. Rust Runtime (Cargo.toml)
$CargoToml = Join-Path $Raiz "src\runtime_rs\Cargo.toml"
if (Test-Path $CargoToml) {
    $conteudo = Get-Content $CargoToml -Raw
    $conteudo = [regex]::Replace($conteudo, '(?m)^(version\s*=\s*")[^"]+(")', "`${1}$Versao`${2}")
    Set-Content -Path $CargoToml -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] src/runtime_rs/Cargo.toml -> $Versao" -ForegroundColor Green
}

# 3. Rust Runtime (Cargo.lock)
$CargoLock = Join-Path $Raiz "src\runtime_rs\Cargo.lock"
if (Test-Path $CargoLock) {
    $conteudo = Get-Content $CargoLock -Raw
    $conteudo = [regex]::Replace($conteudo, '(name = "thz_runtime_rs"\r?\nversion = ")[^"]+(")', "`${1}$Versao`${2}")
    Set-Content -Path $CargoLock -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] src/runtime_rs/Cargo.lock -> $Versao" -ForegroundColor Green
}

# 4. WASM Bridge (wasm.rs)
$WasmRs = Join-Path $Raiz "src\runtime_rs\src\wasm.rs"
if (Test-Path $WasmRs) {
    $conteudo = Get-Content $WasmRs -Raw
    $conteudo = [regex]::Replace($conteudo, 'CString::new\("[^"]+-WASM"\)', "CString::new(`"$Versao-WASM`")")
    Set-Content -Path $WasmRs -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] src/runtime_rs/src/wasm.rs -> $Versao-WASM" -ForegroundColor Green
}

# 5. VS Code Extension (package.json)
$PkgJson = Join-Path $Raiz "Extensions\thz-lsp-vscode\package.json"
if (Test-Path $PkgJson) {
    $conteudo = Get-Content $PkgJson -Raw
    $conteudo = [regex]::Replace($conteudo, '("version":\s*")[^"]+(")', "`${1}$Versao`${2}")
    Set-Content -Path $PkgJson -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] Extensions/thz-lsp-vscode/package.json -> $Versao" -ForegroundColor Green
}

# 6. VS Code Extension (extension.ts)
$ExtTs = Join-Path $Raiz "Extensions\thz-lsp-vscode\src\extension.ts"
if (Test-Path $ExtTs) {
    $conteudo = Get-Content $ExtTs -Raw
    $conteudo = [regex]::Replace($conteudo, "'THZ-LANG \d+\.\d+\.\d+'", "'THZ-LANG $Versao'")
    Set-Content -Path $ExtTs -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] Extensions/thz-lsp-vscode/src/extension.ts -> THZ-LANG $Versao" -ForegroundColor Green
}

# 7. README.md (Badge)
$ReadmeMd = Join-Path $Raiz "README.md"
if (Test-Path $ReadmeMd) {
    $conteudo = Get-Content $ReadmeMd -Raw
    $conteudo = [regex]::Replace($conteudo, "badge/version-\d+\.\d+\.\d+-blue\.svg", "badge/version-$Versao-blue.svg")
    Set-Content -Path $ReadmeMd -Value $conteudo -Encoding UTF8
    Write-Host "  [OK] README.md badge -> $Versao" -ForegroundColor Green
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " Sincronização concluída com sucesso!" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
