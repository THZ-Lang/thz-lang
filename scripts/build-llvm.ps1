# ==============================================================================
# AOT experimental: gera LLVM via o host JVM e linka o runtime Rust.
# NAO USE para GUI. Padrao agora: thz.exe WebView + jpackage.
#   thz gui          -> IDE WebView (thz_webview2.c host, sem thz_runtime Win32)
#   thz run *.thz    -> TELA.* via ThzWebViewLauncher
# Este script foi desativado para alvos GUI. Use .\scripts\package-all.ps1
# ou .\thz.ps1 run <arquivo>. Para forcar legado: -ForceLegado
# ==============================================================================

[CmdletBinding()]
param (
    [Parameter(Mandatory=$true)]
    [string]$ArquivoThz,

    [ValidateSet("ambos", "windows", "linux")]
    [string]$Alvo = "ambos",

    [switch]$ForceLegado
)

$ErrorActionPreference = "Stop"
$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG Engine - Compilador AOT Nativo (LLVM IR)" -ForegroundColor Cyan
Write-Host " Alvo de Compilacao: $Alvo" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

if (-not (Test-Path $ArquivoThz)) {
    Write-Error "Arquivo fonte nao encontrado: $ArquivoThz"
}

$FullArquivoThz = (Resolve-Path $ArquivoThz).Path
$NomeBase = [System.IO.Path]::GetFileNameWithoutExtension($ArquivoThz)

# Bloqueio GUI Win32 feio — exige -ForceLegado
if (($NomeBase -match "_gui") -and -not $ForceLegado) {
    Write-Host "=================================================" -ForegroundColor Yellow
    Write-Host " BLOQUEADO: $NomeBase contem _gui (Win32 thz_runtime.c feio)" -ForegroundColor Yellow
    Write-Host " Use o fluxo recomendado:" -ForegroundColor Cyan
    Write-Host "   .\thz.ps1 run $ArquivoThz      (WebView Edge/WebView2)" -ForegroundColor White
    Write-Host "   .\scripts\gui.ps1              (IDE WebView)" -ForegroundColor White
    Write-Host " Para forcar legado (nao recomendado):" -ForegroundColor DarkGray
    Write-Host "   .\scripts\build-llvm.ps1 -ArquivoThz $ArquivoThz -ForceLegado" -ForegroundColor DarkGray
    Write-Host "=================================================" -ForegroundColor Yellow
    exit 0
}
$DistBin = "$Raiz\dist\bin"
if (-not (Test-Path $DistBin)) { New-Item -ItemType Directory -Path $DistBin | Out-Null }

$LlvmFile = "$DistBin\$NomeBase.ll"
$Clang    = "$env:USERPROFILE\scoop\apps\llvm\current\bin\clang.exe"
if (-not (Test-Path $Clang)) { $Clang = "clang" }
$Gcc      = "$env:USERPROFILE\scoop\apps\mingw\current\bin\gcc.exe"
if (-not (Test-Path $Gcc)) { $Gcc = "gcc" }

# 1. Gerar LLVM IR via THZ IR Generator
Write-Host "`n[1/3] Gerando LLVM IR a partir do fonte THZ..." -ForegroundColor Yellow
$Gradlew = if (Test-Path "$Raiz\gradlew.bat") { "$Raiz\gradlew.bat" } else { "gradle" }
& $Gradlew :thz-cli-jvm:run --args="ir `"$FullArquivoThz`" --llvm --saida `"$LlvmFile`""
if ($LASTEXITCODE -ne 0) { Write-Error "Falha ao gerar LLVM IR." }

# 2 & 3. Compilar Alvo Windows (.exe)
if ($Alvo -eq "ambos" -or $Alvo -eq "windows") {
    Write-Host "`n[2/3] Compilando e Linkando para Windows (.exe)..." -ForegroundColor Yellow
    $ObjWin = "$DistBin\$NomeBase-win.o"
    $ExeWin = "$DistBin\$NomeBase.exe"
    
    & $Clang -target x86_64-w64-windows-gnu -c $LlvmFile -o $ObjWin
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha ao compilar objeto Windows com LLVM Clang." }
    
    # Linker com Runtime Nativo Rust (src/runtime_rs)
    $RuntimeRs = "$Raiz\src\runtime_rs"
    $CargoBin = "$Raiz\.tools\rust\cargo\bin\cargo.exe"
    if (-not (Test-Path $CargoBin)) { $CargoBin = "cargo" }
    
    $RustLibDir = "$RuntimeRs\target\release"
    if (Test-Path "$RuntimeRs\Cargo.toml") {
        Write-Host "  [RUST] Verificando runtime nativo em Rust ($RuntimeRs)..." -ForegroundColor DarkCyan
        try {
            & $CargoBin build --release --manifest-path "$RuntimeRs\Cargo.toml" 2>$null
        } catch {}
    }

    if (-not (Test-Path "$RustLibDir\thz_runtime.lib") -and -not (Test-Path "$RustLibDir\libthz_runtime.a")) {
        Write-Error "Runtime Rust não encontrado em $RustLibDir. Compile o runtime antes do link."
    }
    $GccLinkFlags = @("-O3", $ObjWin, "-o", $ExeWin, "-L", $RustLibDir, "-lthz_runtime", "-lgdi32", "-luser32", "-lkernel32", "-ldwmapi", "-lole32", "-lshlwapi")
    
    & $Gcc @GccLinkFlags
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha ao linkar executavel Windows." }
    Write-Host "[OK] Executavel Windows gerado: $ExeWin" -ForegroundColor Green
}

# 2 & 3. Compilar Alvo Linux (.elf)
if ($Alvo -eq "ambos" -or $Alvo -eq "linux") {
    Write-Host "`n[3/3] Cross-compilando objeto ELF para Linux (.elf)..." -ForegroundColor Yellow
    $ObjLin = "$DistBin\$NomeBase-lin.o"
    $ElfLin = "$DistBin\$NomeBase.elf"
    
    & $Clang -target x86_64-unknown-linux-gnu -c $LlvmFile -o $ObjLin
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha ao compilar objeto Linux com LLVM Clang." }
    
    Copy-Item $ObjLin $ElfLin -Force
    Write-Host "[OK] Objeto ELF Linux gerado: $ElfLin" -ForegroundColor Green
}

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " BINARIO(S) NATIVO(S) AOT GERADO(S) COM SUCESSO!" -ForegroundColor Green
if ($Alvo -eq "ambos" -or $Alvo -eq "windows") { Write-Host " Windows Executavel: $DistBin\$NomeBase.exe" -ForegroundColor White }
if ($Alvo -eq "ambos" -or $Alvo -eq "linux")   { Write-Host " Linux Executavel:   $DistBin\$NomeBase.elf" -ForegroundColor White }
Write-Host "=================================================" -ForegroundColor Green
