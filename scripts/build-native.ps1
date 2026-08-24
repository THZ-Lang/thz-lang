# ==============================================================================
# Script de Compilacao Nativa GraalVM Native Image (Zero JVM Runtime)
# Gera os binários:
#   1. dist/bin/thz.exe      (CLI ultrarrápida do THZ-LANG)
#   2. dist/bin/thz-gui.exe  (Desktop IDE Swing + FlatLaf nativa)
#
# Uso: .\scripts\build-native.ps1 [-PularTestes] [-ApenasCli] [-ApenasGui]
# ==============================================================================

[CmdletBinding()]
param (
    [switch]$PularTestes,
    [switch]$ApenasCli,
    [switch]$ApenasGui
)

$ErrorActionPreference = "Stop"

$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG Engine JVM - Compilador Nativo GraalVM" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Localizar GraalVM
$GraalHome = $env:GRAALVM_HOME
if (-not $GraalHome -or -not (Test-Path "$GraalHome\bin\native-image.cmd")) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\native-image.cmd")) {
        $GraalHome = $env:JAVA_HOME
    } else {
        $Candidatos = @(
            "C:\Users\lucas\scoop\apps\graalvm25-jdk\current",
            "$env:USERPROFILE\scoop\apps\graalvm25-jdk\current",
            "C:\Users\lucas\scoop\apps\graalvm-jdk\current",
            "$env:USERPROFILE\scoop\apps\graalvm-jdk\current"
        )
        foreach ($cand in $Candidatos) {
            $resolvidos = Get-Item $cand -ErrorAction SilentlyContinue
            if ($resolvidos) {
                $dir = $resolvidos[0].FullName
                if (Test-Path "$dir\bin\native-image.cmd") {
                    $GraalHome = $dir
                    break
                }
            }
        }
    }
}

if (-not $GraalHome) {
    Write-Host "`n[!] GraalVM JDK nao foi detectado no sistema." -ForegroundColor Red
    Write-Host "    Para instalar via Scoop, execute:" -ForegroundColor Yellow
    Write-Host "    scoop install java/graalvm25-jdk`n" -ForegroundColor White
    exit 1
}

$env:JAVA_HOME = $GraalHome
$env:GRAALVM_HOME = $GraalHome
$env:PATH = "$GraalHome\bin;" + $env:PATH
Write-Host "[OK] GraalVM detectado: $GraalHome" -ForegroundColor Green

# 1.1 Localizar e carregar ambiente MSVC x64 (necessario para native-image no Windows)
if ($IsWindows -or $env:OS -match "Windows") {
    $PortableMsvc = "$Raiz\extra\MSVC\setup_x64.bat"
    if (Test-Path $PortableMsvc) {
        Write-Host "[OK] Carregando ambiente MSVC Portable integrado ($PortableMsvc)..." -ForegroundColor Green
        $envVars = cmd.exe /c "call `"$PortableMsvc`" > nul 2>&1 && set"
        foreach ($line in $envVars) {
            if ($line -match '^([^=]+)=(.*)$') {
                [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
            }
        }
    } else {
        $VcVarsCandidates = @(
            "C:\Program Files (x86)\Microsoft Visual Studio\18\BuildTools\VC\Auxiliary\Build\vcvars64.bat",
            "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat",
            "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat",
            "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat",
            "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat",
            "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
        )
        $VcVars = $VcVarsCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
        if ($VcVars) {
            Write-Host "[OK] Carregando ambiente Visual Studio C++ ($VcVars)..." -ForegroundColor Green
            $envVars = cmd.exe /c "call `"$VcVars`" > nul 2>&1 && set"
            foreach ($line in $envVars) {
                if ($line -match '^([^=]+)=(.*)$') {
                    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
                }
            }
        }
    }
}




$DistBin = "$Raiz\dist\bin"
if (-not (Test-Path $DistBin)) { New-Item -ItemType Directory -Path $DistBin | Out-Null }
$Gradlew = if (Test-Path "$Raiz\gradlew.bat") { "$Raiz\gradlew.bat" } else { "gradle" }

# 2. Compilar CLI (thz.exe)
if (-not $ApenasGui) {
    Write-Host "`n[1/2] Compilando CLI nativa (thz.exe)..." -ForegroundColor Yellow
    $GradleArgs = @(":thz-cli-jvm:shadowJar")
    if ($PularTestes.IsPresent) { $GradleArgs += @("-x", "test") }
    & $Gradlew @GradleArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha na compilacao shadowJar do CLI." }

    $JarCli = "$Raiz\JVM\thz-cli-jvm\build\libs\thz-jvm-2.3.0.jar"
    if (-not (Test-Path $JarCli)) { $JarCli = "$Raiz\target\thz-jvm-2.3.0.jar" }

    & native-image.cmd --no-fallback -jar $JarCli -o "$DistBin\thz"
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha na compilacao nativa de thz.exe." }
    Write-Host "[OK] CLI nativa gerada: $DistBin\thz.exe" -ForegroundColor Green
}

# 3. Compilar GUI (thz-gui.exe) com Swing/AWT
if (-not $ApenasCli) {
    Write-Host "`n[2/2] Compilando Desktop IDE Swing nativa (thz-gui.exe)..." -ForegroundColor Yellow
    $GradleArgsGui = @(":thz-gui-jvm:shadowJar")
    if ($PularTestes.IsPresent) { $GradleArgsGui += @("-x", "test") }
    & $Gradlew @GradleArgsGui
    if ($LASTEXITCODE -ne 0) { Write-Error "Falha na compilacao shadowJar da GUI." }

    $JarGui = Get-ChildItem "$Raiz\JVM\thz-gui-jvm\build\libs\thz-gui-jvm-*-all.jar", "$Raiz\JVM\thz-gui-jvm\build\libs\thz-gui-jvm-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $JarGui) {
        # Fallback de busca
        $JarGui = Get-ChildItem "$Raiz\JVM\thz-gui-jvm\build\libs\*.jar" | Select-Object -First 1
    }

    if ($JarGui) {
        & native-image.cmd --no-fallback -Djava.awt.headless=false -jar $JarGui.FullName -o "$DistBin\thz-gui"
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Falha na compilacao nativa direta da GUI via Substrate. Verifique se o ambiente possui Visual Studio C++ e execute ./gradlew :thz-gui-jvm:guiColetarMetadadosAgente se faltarem metadados."
        } else {
            Write-Host "[OK] GUI nativa gerada: $DistBin\thz-gui.exe" -ForegroundColor Green
        }
    }
}

# 4. Teste de fumaça CLI
if (Test-Path "$DistBin\thz.exe") {
    Write-Host "`nExecutando teste de fumaca da CLI nativa..." -ForegroundColor Yellow
    & "$DistBin\thz.exe" check "$Raiz\exemplos\faturamento.thz"
}

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " BINARIOS NATIVOS GRAALVM PUBLICADOS COM SUCESSO" -ForegroundColor Green
Write-Host " Diretorio: $DistBin" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Green

