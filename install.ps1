# ==============================================================================
# THZ-LANG — Instalador Oficial de 1 Linha para Windows
# Uso: irm https://raw.githubusercontent.com/THZ-Lang/thz-lang/main/install.ps1 | iex
# ==============================================================================

[CmdletBinding()]
param(
    [string]$Versao = "latest"
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "    Instalador Oficial da Linguagem THZ-LANG     " -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

$Repo = "THZ-Lang/thz-lang"
$DestinoRaiz = "$HOME\.thz"
$DestinoBin  = "$DestinoRaiz\bin"
$DestinoApp  = "$DestinoRaiz\app"

# 1. Resolver URL de Download da Release
Write-Host "`n[1/4] Identificando pacote oficial de distribuicao..." -ForegroundColor Yellow

$DownloadUrl = $null
$TagReal = $Versao

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13
    if ($Versao -eq "latest") {
        $ReleaseInfo = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ "User-Agent" = "THZ-Installer" } -ErrorAction SilentlyContinue
        if ($ReleaseInfo) {
            $TagReal = $ReleaseInfo.tag_name
            $Asset = $ReleaseInfo.assets | Where-Object { $_.name -like "*windows-x64.zip" -or $_.name -like "*thz*.zip" } | Select-Object -First 1
            if ($Asset) { $DownloadUrl = $Asset.browser_download_url }
        }
    }
} catch {
    Write-Host "[!] Aviso ao consultar GitHub API: $($_.Exception.Message)" -ForegroundColor DarkGray
}

if (-not $DownloadUrl) {
    # Fallback para padrao de URL de release tag
    if ($TagReal -eq "latest") { $TagReal = "v0.5.0" }
    $DownloadUrl = "https://github.com/$Repo/releases/download/$TagReal/thz-windows-x64.zip"
}

Write-Host "[OK] Versao alvo: $TagReal" -ForegroundColor Green
Write-Host "[OK] URL do Pacote: $DownloadUrl" -ForegroundColor DarkGray

# 2. Baixar Pacote Zip
$TempZip = "$env:TEMP\thz-install-$([Guid]::NewGuid().ToString('N')).zip"
Write-Host "`n[2/4] Baixando pacote executavel autônomo (com runtime embutido)..." -ForegroundColor Yellow

try {
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $TempZip -UseBasicParsing
} catch {
    Write-Error "Falha ao baixar pacote THZ-LANG em $DownloadUrl. Verifique sua conexao ou a tag de release."
}

# 3. Extrair e Instalar
Write-Host "`n[3/4] Instalando em $DestinoRaiz..." -ForegroundColor Yellow

if (-not (Test-Path $DestinoRaiz)) { New-Item -ItemType Directory -Path $DestinoRaiz | Out-Null }
if (-not (Test-Path $DestinoBin))  { New-Item -ItemType Directory -Path $DestinoBin | Out-Null }
if (Test-Path $DestinoApp) { Remove-Item -Recurse -Force $DestinoApp -ErrorAction SilentlyContinue }

Expand-Archive -Path $TempZip -DestinationPath $DestinoApp -Force
Remove-Item -Force $TempZip -ErrorAction SilentlyContinue

# Se o zip extraiu dentro de uma pasta 'thz', ajustar
$OrigemBin = if (Test-Path "$DestinoApp\thz\thz.exe") { "$DestinoApp\thz" } else { $DestinoApp }

# Copiar / linkar os executáveis para ~/.thz/bin
$ThzExe = "$OrigemBin\thz.exe"
$ThzGuiExe = "$OrigemBin\thz-gui.exe"

# Criar scripts wrappers em ~/.thz/bin para que o runtime ao lado seja resolvido
$CmdThz = "@echo off`r`n`"$ThzExe`" %*"
Set-Content -Path "$DestinoBin\thz.cmd" -Value $CmdThz -Encoding ASCII

if (Test-Path $ThzGuiExe) {
    $CmdGui = "@echo off`r`nstart `"`" `"$ThzGuiExe`" %*"
    Set-Content -Path "$DestinoBin\thz-gui.cmd" -Value $CmdGui -Encoding ASCII
}

# 4. Configurar PATH do Usuário
Write-Host "`n[4/4] Configurando variaveis de ambiente (PATH)..." -ForegroundColor Yellow

$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$DestinoBin*") {
    $NovoPath = "$DestinoBin;" + $UserPath
    [Environment]::SetEnvironmentVariable("Path", $NovoPath, "User")
    $env:PATH = "$DestinoBin;" + $env:PATH
    Write-Host "[OK] Pasta '$DestinoBin' adicionada ao PATH do usuario!" -ForegroundColor Green
} else {
    Write-Host "[OK] Pasta '$DestinoBin' ja constava no PATH." -ForegroundColor Green
}

Write-Host "`n=================================================" -ForegroundColor Green
Write-Host "       THZ-LANG INSTALADO COM SUCESSO!           " -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green
Write-Host " Versao: $TagReal" -ForegroundColor White
Write-Host " Local : $DestinoRaiz" -ForegroundColor White
Write-Host "`n Para testar no terminal atual:" -ForegroundColor Cyan
Write-Host "   & `"$DestinoBin\thz.cmd`" --versao" -ForegroundColor White
Write-Host "`n Primeiros passos:" -ForegroundColor Cyan
Write-Host "   thz init meu_projeto    # Cria novo projeto" -ForegroundColor White
Write-Host "   thz run app.thz         # Executa codigo THZ" -ForegroundColor White
Write-Host "   thz gui                 # Abre o Desktop Studio IDE" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Green
