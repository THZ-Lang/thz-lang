param(
    [ValidateSet('x64', 'x86')]
    [string]$Arch = 'x64'
)

$batFile = Join-Path $PSScriptRoot "setup_$Arch.bat"
if (-not (Test-Path -LiteralPath $batFile)) {
    throw "Nao encontrado: $batFile"
}

$output = cmd.exe /d /c "call `"$batFile`" >nul 2>&1 && set"
foreach ($line in $output) {
    $idx = $line.IndexOf('=')
    if ($idx -gt 0) {
        Set-Item -Path "env:$($line.Substring(0, $idx))" -Value $line.Substring($idx + 1)
    }
}

Write-Host "=== VS2026 Portable - ambiente $Arch carregado ===" -ForegroundColor Green
$versionLine = (cmd.exe /d /c "cl 2>&1") -match 'Version'
Write-Host $versionLine
