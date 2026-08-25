<#
.SYNOPSIS
    Script de build e empacotamento da extensão VS Code / Antigravity (.vsix) do THZ-LANG.
.DESCRIPTION
    Compila o servidor LSP Java (shadowJar), copia para a extensão, compila TypeScript,
    gera o pacote instalável .vsix via @vscode/vsce e salva em dist/.
.PARAMETER Instalar
    Se especificado, instala o .vsix gerado automaticamente no VS Code / Antigravity IDE.
.PARAMETER PularBuildJar
    Pula a compilação do shadowJar Java caso já esteja compilado.
#>
param(
    [switch]$Instalar,
    [switch]$PularBuildJar
)

$ErrorActionPreference = "Stop"
$Raiz = (Get-Item $PSScriptRoot).Parent.FullName
$PastaExtensao = Join-Path $Raiz "Extensions\thz-lsp-vscode"
$PastaDist = Join-Path $Raiz "dist"
$PastaServerExt = Join-Path $PastaExtensao "server"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "         BUILD DO PACOTE VSIX DA EXTENSÃO THZ-LANG (LSP + SINTAXE)        " -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# 1. Garantir diretórios
if (-not (Test-Path $PastaDist)) {
    New-Item -ItemType Directory -Path $PastaDist -Force | Out-Null
}
if (-not (Test-Path $PastaServerExt)) {
    New-Item -ItemType Directory -Path $PastaServerExt -Force | Out-Null
}

# 2. Compilar shadowJar do Servidor LSP Java se necessário
$LspJarItem = Get-ChildItem (Join-Path $Raiz "JVM\thz-lsp-jvm\build\libs\thz-lsp*.jar"), (Join-Path $Raiz "target\thz-lsp*.jar") -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "-sources" } | Select-Object -First 1

if (-not $PularBuildJar -or -not $LspJarItem) {
    Write-Host "[1/5] Compilando Servidor LSP Java 25 (shadowJar)..." -ForegroundColor Yellow
    Push-Location $Raiz
    try {
        if ($IsWindows -or $env:OS -like "*Windows*") {
            .\gradlew.bat :thz-lsp-jvm:shadowJar
        } else {
            ./gradlew :thz-lsp-jvm:shadowJar
        }
    } finally {
        Pop-Location
    }
    $LspJarItem = Get-ChildItem (Join-Path $Raiz "JVM\thz-lsp-jvm\build\libs\thz-lsp*.jar"), (Join-Path $Raiz "target\thz-lsp*.jar") -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "-sources" } | Select-Object -First 1
} else {
    Write-Host "[1/5] Utilizando shadowJar existente em $($LspJarItem.FullName)..." -ForegroundColor DarkGray
}

if (-not $LspJarItem) {
    Write-Error "Erro: JAR do LSP não encontrado."
    exit 1
}

$LspJarOrigem = $LspJarItem.FullName
$LspJarDestino = Join-Path $PastaServerExt "thz-lsp.jar"

# 3. Copiar JAR para a pasta da extensão
Write-Host "[2/5] Copiando servidor LSP para a extensão ($LspJarDestino)..." -ForegroundColor Yellow
Copy-Item -Path $LspJarOrigem -Destination $LspJarDestino -Force
Copy-Item -Path $LspJarOrigem -Destination (Join-Path $PastaServerExt $LspJarItem.Name) -Force
if (Test-Path (Join-Path $Raiz "LICENSE")) {
    Copy-Item -Path (Join-Path $Raiz "LICENSE") -Destination (Join-Path $PastaExtensao "LICENSE") -Force
}

# 4. Compilar TypeScript da extensão
Write-Host "[3/5] Compilando extensão TypeScript..." -ForegroundColor Yellow
Push-Location $PastaExtensao
try {
    npm run compile
} finally {
    Pop-Location
}

# 5. Empacotar .vsix via @vscode/vsce
Write-Host "[4/5] Empacotando arquivo .vsix..." -ForegroundColor Yellow
Push-Location $PastaExtensao
try {
    npx -y @vscode/vsce package --no-dependencies
    $VsixGerado = Get-ChildItem -Path $PastaExtensao -Filter "*.vsix" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($VsixGerado) {
        $DestinoVsix = Join-Path $PastaDist $VsixGerado.Name
        Copy-Item -Path $VsixGerado.FullName -Destination $DestinoVsix -Force
        Write-Host " [SUCESSO] VSIX criado com sucesso: $DestinoVsix" -ForegroundColor Green
    } else {
        Write-Error "Erro: Nenhum arquivo .vsix foi gerado."
        exit 1
    }
} finally {
    Pop-Location
}

# 6. Instalar se solicitado
if ($Instalar -and $DestinoVsix) {
    Write-Host "[5/5] Instalando extensão no ambiente..." -ForegroundColor Yellow
    
    # Tenta code
    if (Get-Command "code" -ErrorAction SilentlyContinue) {
        Write-Host "  -> Instalando no VS Code (code --install-extension $DestinoVsix)..." -ForegroundColor Cyan
        code --install-extension $DestinoVsix --force
    }
    
    # Sincroniza diretamente com pastas de extensões do Antigravity IDE e VS Code
    $PastasAlvo = @(
        (Join-Path $env:USERPROFILE ".antigravity-ide\extensions\thz-lang.thz-lang-0.3.0"),
        (Join-Path $env:USERPROFILE ".antigravity-ide\extensions\thz-lang-0.3.0"),
        (Join-Path $env:USERPROFILE ".vscode\extensions\thz-lang.thz-lang-0.3.0"),
        (Join-Path $env:USERPROFILE ".vscode\extensions\thz-lang-0.3.0")
    )

    foreach ($Alvo in $PastasAlvo) {
        $Pai = Split-Path $Alvo -Parent
        if (Test-Path $Pai) {
            Write-Host "  -> Sincronizando com $Alvo..." -ForegroundColor Cyan
            if (-not (Test-Path $Alvo)) {
                New-Item -ItemType Directory -Path $Alvo -Force | Out-Null
            }
            if (-not (Test-Path (Join-Path $Alvo "dist"))) { New-Item -ItemType Directory -Path (Join-Path $Alvo "dist") -Force | Out-Null }
            if (-not (Test-Path (Join-Path $Alvo "syntaxes"))) { New-Item -ItemType Directory -Path (Join-Path $Alvo "syntaxes") -Force | Out-Null }
            if (-not (Test-Path (Join-Path $Alvo "server"))) { New-Item -ItemType Directory -Path (Join-Path $Alvo "server") -Force | Out-Null }
            if (-not (Test-Path (Join-Path $Alvo "assets"))) { New-Item -ItemType Directory -Path (Join-Path $Alvo "assets") -Force | Out-Null }
            if (-not (Test-Path (Join-Path $Alvo "snippets"))) { New-Item -ItemType Directory -Path (Join-Path $Alvo "snippets") -Force | Out-Null }

            Copy-Item -Path (Join-Path $PastaExtensao "dist\*") -Destination (Join-Path $Alvo "dist") -Recurse -Force
            Copy-Item -Path (Join-Path $PastaExtensao "syntaxes\*") -Destination (Join-Path $Alvo "syntaxes") -Recurse -Force
            if (Test-Path (Join-Path $PastaExtensao "server\*")) {
                Copy-Item -Path (Join-Path $PastaExtensao "server\*") -Destination (Join-Path $Alvo "server") -Recurse -Force
            }
            if (Test-Path (Join-Path $PastaExtensao "assets\*")) {
                Copy-Item -Path (Join-Path $PastaExtensao "assets\*") -Destination (Join-Path $Alvo "assets") -Recurse -Force
            }
            if (Test-Path (Join-Path $PastaExtensao "snippets\*")) {
                Copy-Item -Path (Join-Path $PastaExtensao "snippets\*") -Destination (Join-Path $Alvo "snippets") -Recurse -Force
            }
            Copy-Item -Path (Join-Path $PastaExtensao "package.json") -Destination $Alvo -Force
            Copy-Item -Path (Join-Path $PastaExtensao "language-configuration.json") -Destination $Alvo -Force
            if (Test-Path (Join-Path $PastaExtensao "icon.png")) {
                Copy-Item -Path (Join-Path $PastaExtensao "icon.png") -Destination $Alvo -Force
            }
            Write-Host "     Extensão sincronizada em $Alvo com sucesso!" -ForegroundColor Green
        }
    }
}

Write-Host "==========================================================================" -ForegroundColor Green
Write-Host " Pacote VSIX pronto para distribuição: $DestinoVsix" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Green
