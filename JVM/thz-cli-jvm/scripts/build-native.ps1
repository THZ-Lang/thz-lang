# Script de Compilacao Nativa THZ-LANG Engine (GraalVM Native Image)

[CmdletBinding()]
param (
    [switch]$PularTestes
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
            "$env:USERPROFILE\scoop\apps\graalvm-jdk\current",
            "C:\Program Files\GraalVM\graalvm-jdk-25*",
            "C:\Program Files\Java\graalvm-jdk-25*"
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
    Write-Host "    scoop install java/graalvm25-jdk" -ForegroundColor White
    Write-Host "    Ou defina a variavel `$env:GRAALVM_HOME apontando para o seu diretorio GraalVM.`n" -ForegroundColor Yellow
    exit 1
}

$env:JAVA_HOME = $GraalHome
$env:GRAALVM_HOME = $GraalHome
$env:PATH = "$GraalHome\bin;" + $env:PATH
Write-Host "[OK] GraalVM detectado: $GraalHome" -ForegroundColor Green

# 2. Verificar MSVC (cl.exe / Visual C++ Tools)
$TemCl = Get-Command cl.exe -ErrorAction SilentlyContinue
if (-not $TemCl) {
    Write-Host "[*] Procurando ambiente do Visual Studio C++ (vcvars64.bat)..." -ForegroundColor DarkGray
    $VcVarsCandidatos = @(
        "C:\Program Files\Microsoft Visual Studio\*\Community\VC\Auxiliary\Build\vcvars64.bat",
        "C:\Program Files\Microsoft Visual Studio\*\Professional\VC\Auxiliary\Build\vcvars64.bat",
        "C:\Program Files\Microsoft Visual Studio\*\Enterprise\VC\Auxiliary\Build\vcvars64.bat",
        "C:\Program Files\Microsoft Visual Studio\*\BuildTools\VC\Auxiliary\Build\vcvars64.bat",
        "C:\Program Files (x86)\Microsoft Visual Studio\*\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
    )
    $VcVarsPath = $null
    foreach ($cand in $VcVarsCandidatos) {
        $found = Get-Item $cand -ErrorAction SilentlyContinue
        if ($found) {
            $VcVarsPath = $found[0].FullName
            break
        }
    }

    if ($VcVarsPath) {
        Write-Host "[OK] vcvars64 encontrado em: $VcVarsPath" -ForegroundColor Green
    } else {
        Write-Host "[!] Aviso: cl.exe nao esta no PATH e vcvars64.bat nao foi localizado automaticamente." -ForegroundColor Yellow
        Write-Host "    Se o build falhar, certifique-se de executar no 'x64 Native Tools Command Prompt for VS'." -ForegroundColor DarkYellow
    }
} else {
    Write-Host "[OK] Compilador C++ (MSVC cl.exe) disponivel no PATH." -ForegroundColor Green
}

# 3. Compilar Shaded JAR via Gradle
Write-Host "`n[1/2] Compilando Shaded JAR com Gradle..." -ForegroundColor Yellow
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

$JarPath = "$Raiz\target\thz-jvm-2.3.0.jar"
if (-not (Test-Path $JarPath)) {
    $JarPath = "$Raiz\thz-cli\build\libs\thz-jvm-2.3.0.jar"
}

# 4. Compilar AOT Nativo com GraalVM native-image
Write-Host "`n[2/2] Gerando executavel nativo com native-image..." -ForegroundColor Yellow
$DistBin = "$Raiz\dist\bin"
if (-not (Test-Path $DistBin)) { New-Item -ItemType Directory -Path $DistBin | Out-Null }
$TargetExe = "$DistBin\thz.exe"

& native-image.cmd --no-fallback -jar $JarPath -o "$DistBin\thz"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha na compilacao com native-image."
}
Write-Host "[OK] Executavel nativo publicado em: $DistBin\thz.exe" -ForegroundColor Green

# 5. Teste de fumaca
Write-Host "`n[2/2] Executando teste de fumaca nativo..." -ForegroundColor Yellow
$ExePath = "$DistBin\thz.exe"
Write-Host "Executando: $ExePath check exemplos\faturamento.thz" -ForegroundColor DarkCyan
& $ExePath check "$Raiz\exemplos\faturamento.thz"

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host " BINARIO NATIVO GERADO COM SUCESSO!" -ForegroundColor Green
Write-Host " Executavel: $ExePath" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Green
