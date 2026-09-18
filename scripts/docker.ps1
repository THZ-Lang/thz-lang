# ==============================================================================
# THZ-LANG -- Automacao Docker & Podman (Windows PowerShell / pwsh)
# Suporta: Docker, Podman, Compose e Devcontainer
# Uso: .\scripts\docker.ps1 [comando] [opcoes]
# ==============================================================================
[CmdletBinding()]
param(
    [Parameter(Position=0)]
    [string]$Comando = "ajuda",

    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$Argumentos,

    [switch]$Podman,
    [switch]$Docker
)

$ErrorActionPreference = "Stop"
$Raiz = (Get-Item -Path "$PSScriptRoot\..").FullName
Set-Location $Raiz

# Auto-deteccao de runtime
$Runtime = $null
if ($Podman) {
    $Runtime = "podman"
} elseif ($Docker) {
    $Runtime = "docker"
} else {
    if (Get-Command "podman" -ErrorAction SilentlyContinue) {
        $Runtime = "podman"
    } elseif (Get-Command "docker" -ErrorAction SilentlyContinue) {
        $Runtime = "docker"
    } else {
        Write-Host "[ERRO] Nenhum runtime de conteiner encontrado (nem podman, nem docker no PATH)." -ForegroundColor Red
        exit 1
    }
}

# Auto-deteccao do utilitario compose
$ComposeExe = $Runtime
$ComposeSub = "compose"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " THZ-LANG -- Conteineres ($Runtime / $ComposeExe $ComposeSub)" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

switch ($Comando.ToLower()) {
    "build" {
        $Target = if ($Argumentos -and $Argumentos.Count -gt 0) { $Argumentos[0] } else { "all" }
        if ($Target -eq "all") {
            Write-Host "[Build] Construindo todas as imagens..." -ForegroundColor Yellow
            & $ComposeExe $ComposeSub build
        } else {
            Write-Host "[Build] Construindo target '$Target'..." -ForegroundColor Yellow
            & $Runtime build --target $Target -t "thz-lang/${Target}:latest" -t "thz-lang:latest" .
        }
    }

    { $_ -in @("up", "start") } {
        Write-Host "[Up] Subindo servicos..." -ForegroundColor Yellow
        & $ComposeExe $ComposeSub up -d thz-api @Argumentos
        Write-Host "[OK] THZ-LANG API disponivel em http://localhost:8080" -ForegroundColor Green
    }

    { $_ -in @("down", "stop") } {
        Write-Host "[Down] Encerrando servicos..." -ForegroundColor Yellow
        & $ComposeExe $ComposeSub down @Argumentos
    }

    "api" {
        Write-Host "[API] Iniciando microservico Spring Boot na porta 8080..." -ForegroundColor Yellow
        & $ComposeExe $ComposeSub up thz-api
    }

    "cli" {
        Write-Host "[CLI] Executando comando THZ via conteiner..." -ForegroundColor Yellow
        & $Runtime run --rm -it -v "${Raiz}:/workspace:z" -w /workspace "thz-lang/cli:latest" @Argumentos
    }

    "repl" {
        Write-Host "[REPL] Abrindo REPL Interativo no conteiner..." -ForegroundColor Yellow
        & $Runtime run --rm -it -v "${Raiz}:/workspace:z" -w /workspace "thz-lang/cli:latest" repl
    }

    "test" {
        Write-Host "[Test] Executando suite completa de testes no conteiner..." -ForegroundColor Yellow
        & $Runtime run --rm -v "${Raiz}:/workspace:z" -w /workspace "thz-lang/dev:latest" ./gradlew test
    }

    "dev" {
        Write-Host "[Dev] Abrindo shell interativo no conteiner de desenvolvimento..." -ForegroundColor Yellow
        & $Runtime run --rm -it -p 8080:8080 -p 5005:5005 -v "${Raiz}:/workspace:z" -w /workspace "thz-lang/dev:latest" /bin/bash
    }

    "clean" {
        Write-Host "[Clean] Removendo conteineres e volumes do THZ-LANG..." -ForegroundColor Yellow
        & $ComposeExe $ComposeSub down -v --rmi local
    }

    default {
        Write-Host "Uso: .\scripts\docker.ps1 [-Podman|-Docker] [comando] [argumentos]"
        Write-Host ""
        Write-Host "Comandos disponiveis:"
        Write-Host "  build [target]   Compila imagens (target: api, cli, dev, all)"
        Write-Host "  up               Inicia a API em background (http://localhost:8080)"
        Write-Host "  down             Para os servicos em execucao"
        Write-Host "  api              Inicia a API Spring Boot em foreground com logs"
        Write-Host "  cli [args...]    Executa comandos da CLI (ex: cli run exemplos/faturamento.thz)"
        Write-Host "  repl             Abre o REPL interativo no conteiner"
        Write-Host "  test             Executa todos os testes unitarios dentro do conteiner"
        Write-Host "  dev              Abre um shell bash interativo dentro do conteiner Dev"
        Write-Host "  clean            Remove conteineres, volumes e imagens locais"
    }
}
