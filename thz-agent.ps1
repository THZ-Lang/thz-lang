# ==============================================================================
# thz-agent.ps1 — THZ-Agent direto (sem Gradle)
# Uso: .\thz-agent.ps1 [opções]
#      .\thz-agent.ps1 --modelo caminho/modelo.gguf
#      .\thz-agent.ps1 --api https://api.openai.com/v1 --api-key sk-...
# ==============================================================================

param([Parameter(ValueFromRemainingArguments=$true)][string[]]$ArgsRest)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding  = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
try { chcp 65001 | Out-Null } catch {}

$Raiz = $PSScriptRoot
$Jar = "$Raiz\target\thz-jvm.jar"

# Auto-detectar Rust portátil
$RustLocalBin = "$Raiz\.tools\rust\cargo\bin"
if (Test-Path "$RustLocalBin\cargo.exe") {
    $env:PATH = "$RustLocalBin;$env:PATH"
}

# Verificar se o JAR existe
if (-not (Test-Path $Jar)) {
    Write-Host "[THZ-Agent] Shadow JAR não encontrado. Buildando..." -ForegroundColor Yellow
    & "$Raiz\gradlew.bat" :thz-cli-jvm:shadowJar --no-daemon -q
    if (-not (Test-Path $Jar)) {
        Write-Host "[THZ-Agent] ERRO: Falha ao buildar o JAR." -ForegroundColor Red
        exit 1
    }
}

# Executar direto via java -jar (sem Gradle no meio)
java `
    "-Dfile.encoding=UTF-8" `
    "-Dstdout.encoding=UTF-8" `
    "-Dstderr.encoding=UTF-8" `
    "--enable-native-access=ALL-UNNAMED" `
    -jar $Jar `
    agent @ArgsRest

exit $LASTEXITCODE
