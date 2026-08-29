# ==============================================================================
# thz.ps1 — shim raiz (Windows PowerShell)
# Uso: .\thz.ps1 check exemplos/faturamento.thz
#      .\thz.ps1 run exemplos/showcase_widgets_gui.thz
#      .\thz.ps1 gui
# Delega para JVM/thz-cli-jvm via Gradle (sem precisar npm)
# ==============================================================================

param([Parameter(ValueFromRemainingArguments=$true)][string[]]$ArgsRest)

# Garante UTF-8 no console Windows (corrige Verificação/Código/Governança)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding  = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
try { chcp 65001 | Out-Null } catch {}
# Encoding JVM já configurado em gradle.properties + build.gradle.kts (stdout.encoding=UTF-8)

$Raiz = $PSScriptRoot

# Auto-detecção de Rust Portátil (.tools\rust)
$RustLocalBin = "$Raiz\.tools\rust\cargo\bin"
if (Test-Path "$RustLocalBin\cargo.exe") {
    $env:PATH = "$RustLocalBin;$env:PATH"
}

if (-not $ArgsRest -or $ArgsRest.Count -eq 0) { $ArgsRest = @("gui") }
# Normaliza --gui -> gui, --help -> --ajuda etc. (ThzCli espera sem -- para comandos)
if ($ArgsRest.Count -ge 1) {
    $cmd = $ArgsRest[0]
    if ($cmd -eq "--gui" -or $cmd -eq "-g") { $ArgsRest[0] = "gui" }
    elseif ($cmd -eq "--help" -or $cmd -eq "--ajuda" -or $cmd -eq "-h") { $ArgsRest[0] = "--ajuda" }
    elseif ($cmd -eq "--version" -or $cmd -eq "--versao" -or $cmd -eq "-v") { $ArgsRest[0] = "--versao" }
    elseif ($cmd.StartsWith("--")) { $ArgsRest[0] = $cmd.TrimStart("-") }
}
$joined = $ArgsRest -join " "
if ($ArgsRest.Count -ge 1 -and $ArgsRest[0] -eq "gui") {
    if ($ArgsRest.Count -ge 2) {
        $fileArgs = ($ArgsRest[1..($ArgsRest.Count - 1)] -join " ")
        & "$Raiz\gradlew.bat" :thz-gui-jvm:run --args="$fileArgs"
    } else {
        & "$Raiz\gradlew.bat" :thz-gui-jvm:gui
    }
} elseif ($ArgsRest.Count -ge 1 -and ($ArgsRest[0] -eq "agent" -or $ArgsRest[0] -eq "agente")) {
    # Agent roda direto via java -jar (sem Gradle no console)
    $agentArgs = if ($ArgsRest.Count -ge 2) { $ArgsRest[1..($ArgsRest.Count - 1)] } else { @() }
    & "$Raiz\thz-agent.ps1" @agentArgs
} else {
    & "$Raiz\gradlew.bat" :thz-cli-jvm:run --args="$joined"
}
exit $LASTEXITCODE

