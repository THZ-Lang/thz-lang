# Script de Empacotamento THZ-LANG Engine JVM (jpackage / Java 25)

[CmdletBinding()]
param (
    [string]$Versao = "2.3.0",
    [switch]$PularTestes
)

$ErrorActionPreference = "Stop"

$Raiz = Resolve-Path "$PSScriptRoot\.."
Set-Location $Raiz

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG Engine JVM - Gerador de Pacote jpackage" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Resolver Java 25
$JavaHome = $null
$Candidatos = @(
    "C:\Users\lucas\scoop\apps\openjdk25\current",
    "$env:USERPROFILE\scoop\apps\openjdk25\current",
    $env:JAVA_HOME,
    "C:\Program Files\Java\jdk-25"
)
foreach ($cand in $Candidatos) {
    if ($cand -and (Test-Path "$cand\bin\jpackage.exe") -and (Test-Path "$cand\release")) {
        $releaseContent = Get-Content "$cand\release" -Raw
        if ($releaseContent -match 'JAVA_VERSION="25') {
            $JavaHome = $cand
            break
        }
    }
}
# Fallback se não validou release string
if (-not $JavaHome) {
    if (Test-Path "C:\Users\lucas\scoop\apps\openjdk25\current\bin\jpackage.exe") {
        $JavaHome = "C:\Users\lucas\scoop\apps\openjdk25\current"
    } elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
        $JavaHome = $env:JAVA_HOME
    }
}

if (-not $JavaHome -or -not (Test-Path "$JavaHome\bin\jpackage.exe")) {
    Write-Error "jpackage.exe do Java 25 nao foi encontrado. Certifique-se de que o JDK 25 esta instalado."
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;" + $env:PATH
Write-Host "[OK] Usando JDK 25: $JavaHome" -ForegroundColor Green


# 2. Build Gradle
Write-Host "`n[1/3] Compilando Shaded JAR com Gradle..." -ForegroundColor Yellow
$Gradlew = if (Test-Path "$Raiz\gradlew.bat") { "$Raiz\gradlew.bat" } else { "gradle" }
$GradleArgs = @("shadowJar")
if ($PularTestes.IsPresent) {
    $GradleArgs += "-x"
    $GradleArgs += "test"
}
& $Gradlew @GradleArgs
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha na compilacao do Gradle."
}

$JarPath = "$Raiz\target\thz-jvm-$Versao.jar"
if (-not (Test-Path $JarPath)) {
    $Jars = Get-ChildItem "$Raiz\target\thz-jvm-*.jar" | Select-Object -First 1
    if ($Jars) {
        $JarPath = $Jars.FullName
    } else {
        Write-Error "JAR shaded nao encontrado em target/."
    }
}
Write-Host "[OK] JAR pronto: $JarPath" -ForegroundColor Green

# 3. Preparar diretorio de entrada para jpackage
$AppInputDir = "$Raiz\target\jpackage-input"
if (Test-Path $AppInputDir) { Remove-Item -Recurse -Force $AppInputDir }
New-Item -ItemType Directory -Path $AppInputDir | Out-Null
Copy-Item $JarPath -Destination "$AppInputDir\thz-engine.jar"

$DistDir = "$Raiz\dist"
$DestDir = "$DistDir\thz"
if (Test-Path $DestDir) {
    Write-Host "[!] Removendo distribuicao anterior em $DestDir..." -ForegroundColor DarkGray
    Get-Process "thz*", "javaw" -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*$Raiz*" } | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 300
    Remove-Item -Recurse -Force $DestDir -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 200
}
if (-not (Test-Path $DistDir)) { New-Item -ItemType Directory -Path $DistDir | Out-Null }


# 4. Executar jpackage para gerar App-Image
Write-Host "`n[2/3] Gerando pacote autonomo com jpackage (Java 25)..." -ForegroundColor Yellow

$JpackageArgs = @(
    "--type", "app-image",
    "--input", $AppInputDir,
    "--dest", $DistDir,
    "--name", "thz",
    "--main-jar", "thz-engine.jar",
    "--main-class", "thz.lang.cli.ThzCli",
    "--app-version", $Versao,
    "--vendor", "THZ-LANG Team",
    "--description", "THZ-LANG Engine e Desktop IDE",
    "--win-console",
    "--add-launcher", "thz-gui=$PSScriptRoot\launcher-gui.properties",
    "--add-launcher", "thz-desktop=$PSScriptRoot\launcher-gui.properties",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "--enable-native-access=ALL-UNNAMED"
)

& "$JavaHome\bin\jpackage.exe" @JpackageArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha na execucao do jpackage."
}

# Sincronizar dist/thz-desktop com o runtime Java 25 atualizado
$LegacyDestDir = "$DistDir\thz-desktop"
if (Test-Path $LegacyDestDir) {
    Remove-Item -Recurse -Force $LegacyDestDir
}
Copy-Item -Recurse -Path $DestDir -Destination $LegacyDestDir

Write-Host "[OK] Pacotes criados com sucesso em:`n - $DestDir`n - $LegacyDestDir" -ForegroundColor Green

# 5. Teste de fumaca
Write-Host "`n[3/3] Executando teste de fumaca no binario gerado..." -ForegroundColor Yellow
$ThzExe = "$DestDir\thz.exe"

Write-Host "Executando: $ThzExe check exemplos\faturamento.thz" -ForegroundColor DarkCyan
& $ThzExe check "$Raiz\exemplos\faturamento.thz"

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " EMPACOTAMENTO CONCLUIDO COM SUCESSO!" -ForegroundColor Green
Write-Host " Pasta Principal:       $DestDir" -ForegroundColor White
Write-Host " Executavel CLI:        $DestDir\thz.exe" -ForegroundColor White
Write-Host " Executavel GUI:        $DestDir\thz-gui.exe" -ForegroundColor White
Write-Host " Executavel Desktop:    $DestDir\thz-desktop.exe" -ForegroundColor White
Write-Host " Pasta Legada/Alias:    $LegacyDestDir" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Green


