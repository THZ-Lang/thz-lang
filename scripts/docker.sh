#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG — Automação Docker & Podman (Linux / macOS / WSL)
# Suporta: Docker, Podman, Compose e Devcontainer
# Uso: ./scripts/docker.sh [comando] [opções]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

RUNTIME=""
COMPOSE_CMD=""

# Identificação de flag explícita (--podman ou --docker)
POSITIONAL_ARGS=()
for arg in "$@"; do
    case "$arg" in
        --podman)
            RUNTIME="podman"
            ;;
        --docker)
            RUNTIME="docker"
            ;;
        *)
            POSITIONAL_ARGS+=("$arg")
            ;;
    esac
done
set -- "${POSITIONAL_ARGS[@]}"

# Auto-detecção de runtime se não informado
if [ -z "$RUNTIME" ]; then
    if command -v podman >/dev/null 2>&1; then
        RUNTIME="podman"
    elif command -v docker >/dev/null 2>&1; then
        RUNTIME="docker"
    else
        echo -e "\033[0;31m[ERRO] Nenhum runtime de contêiner encontrado (nem podman, nem docker).\033[0m"
        exit 1
    fi
fi

# Detecção do comando compose adequado
if [ "$RUNTIME" = "podman" ]; then
    if podman compose version >/dev/null 2>&1; then
        COMPOSE_CMD="podman compose"
    elif command -v podman-compose >/dev/null 2>&1; then
        COMPOSE_CMD="podman-compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="podman compose"
    fi
else
    if docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
fi

CMD="${1:-ajuda}"
shift || true

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG — Contêineres ($RUNTIME / $COMPOSE_CMD)\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

case "$CMD" in
    build)
        TARGET="${1:-all}"
        if [ "$TARGET" = "all" ]; then
            echo -e "\033[0;33m[Build] Construindo todas as imagens...\033[0m"
            $COMPOSE_CMD build
        else
            echo -e "\033[0;33m[Build] Construindo target '$TARGET'...\033[0m"
            $RUNTIME build --target "$TARGET" -t "thz-lang/$TARGET:latest" -t "thz-lang:latest" .
        fi
        ;;

    up|start)
        echo -e "\033[0;33m[Up] Subindo serviços...\033[0m"
        $COMPOSE_CMD up -d thz-api "$@"
        echo -e "\033[0;32m[OK] THZ-LANG API disponível em http://localhost:8080\033[0m"
        ;;

    down|stop)
        echo -e "\033[0;33m[Down] Encerrando serviços...\033[0m"
        $COMPOSE_CMD down "$@"
        ;;

    api)
        echo -e "\033[0;33m[API] Iniciando microserviço Spring Boot na porta 8080...\033[0m"
        $COMPOSE_CMD up thz-api
        ;;

    cli)
        echo -e "\033[0;33m[CLI] Executando comando THZ via contêiner...\033[0m"
        $RUNTIME run --rm -it -v "$RAIZ:/workspace:z" -w /workspace "thz-lang/cli:latest" "$@"
        ;;

    repl)
        echo -e "\033[0;33m[REPL] Abrindo REPL Interativo no contêiner...\033[0m"
        $RUNTIME run --rm -it -v "$RAIZ:/workspace:z" -w /workspace "thz-lang/cli:latest" repl
        ;;

    test)
        echo -e "\033[0;33m[Test] Executando suíte completa de testes no contêiner...\033[0m"
        $RUNTIME run --rm -v "$RAIZ:/workspace:z" -w /workspace "thz-lang/dev:latest" ./gradlew test
        ;;

    dev)
        echo -e "\033[0;33m[Dev] Abrindo shell interativo no contêiner de desenvolvimento...\033[0m"
        $RUNTIME run --rm -it -p 8080:8080 -p 5005:5005 -v "$RAIZ:/workspace:z" -w /workspace "thz-lang/dev:latest" /bin/bash
        ;;

    clean)
        echo -e "\033[0;33m[Clean] Removendo contêineres e volumes do THZ-LANG...\033[0m"
        $COMPOSE_CMD down -v --rmi local || true
        ;;

    ajuda|help|--help|-h|*)
        echo -e "Uso: ./scripts/docker.sh [--podman|--docker] <comando> [argumentos]"
        echo -e ""
        echo -e "Comandos disponíveis:"
        echo -e "  build [target]   Compila imagens (target: api, cli, dev, all)"
        echo -e "  up               Inicia a API em background (http://localhost:8080)"
        echo -e "  down             Para os serviços em execução"
        echo -e "  api              Inicia a API Spring Boot em foreground com logs"
        echo -e "  cli <args...>    Executa comandos da CLI (ex: cli run exemplos/faturamento.thz)"
        echo -e "  repl             Abre o REPL interativo no contêiner"
        echo -e "  test             Executa todos os testes unitários dentro do contêiner"
        echo -e "  dev              Abre um shell bash interativo dentro do contêiner Dev"
        echo -e "  clean            Remove contêineres, volumes e imagens locais"
        ;;
esac
