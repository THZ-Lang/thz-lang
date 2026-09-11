#!/usr/bin/env bash
# ==============================================================================
# thz.sh / thz — Shim universal de execução na raiz do workspace (Linux/macOS/Docker)
# Uso: ./thz check exemplos/faturamento.thz
#      ./thz run exemplos/showcase_widgets_gui.thz
#      ./thz livro --saida dist/MANUAL_THZ_LANG.pdf
#      ./thz gui
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$RAIZ"

ARGS=("$@")
if [ ${#ARGS[@]} -eq 0 ]; then
    ARGS=("gui")
fi

# Normalização de flags para comandos canônicos
CMD="${ARGS[0]}"
case "$CMD" in
    --gui|-g) ARGS[0]="gui" ;;
    --help|--ajuda|-h) ARGS[0]="--ajuda" ;;
    --version|--versao|-v) ARGS[0]="--versao" ;;
esac

if [ "${ARGS[0]}" = "gui" ]; then
    if [ ${#ARGS[@]} -ge 2 ]; then
        REST=("${ARGS[@]:1}")
        ./gradlew :thz-gui-jvm:run --args="${REST[*]}"
    else
        ./gradlew :thz-gui-jvm:gui
    fi
elif [ "${ARGS[0]}" = "agent" ] || [ "${ARGS[0]}" = "agente" ]; then
    # Agent roda direto via java -jar (sem Gradle no console)
    AGENT_ARGS=("${ARGS[@]:1}")
    ./thz-agent.sh "${AGENT_ARGS[@]}"
else
    ./gradlew :thz-cli-jvm:run --args="${ARGS[*]}"
fi
