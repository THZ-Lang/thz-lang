# ==============================================================================
# thz.ps1 — shim raiz (Windows PowerShell)
# Uso: .\thz.ps1 check exemplos/faturamento.thz
#      .\thz.ps1 run exemplos/showcase_widgets_gui.thz
#      .\thz.ps1 gui
# Delega para JVM/thz-cli-jvm via Gradle (sem precisar npm)
# ==============================================================================

param([Parameter(ValueFromRemainingArguments=$true)][string[]]$ArgsRest)

$Raiz = $PSScriptRoot
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
    & "$Raiz\gradlew.bat" :thz-gui-jvm:gui
} else {
    & "$Raiz\gradlew.bat" :thz-cli-jvm:run --args="$joined"
}
exit $LASTEXITCODE

