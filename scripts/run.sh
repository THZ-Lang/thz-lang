#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Run - Wrapper para CLI thz run/check/audit/doc/ir (Linux/macOS)
# Uso: ./scripts/run.sh <comando> <arquivo.thz> [args...]
# Ex:  ./scripts/run.sh run exemplos/faturamento.thz
#      ./scripts/run.sh check exemplos/pedidos.thz --estrito
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

if [ $# -eq 0 ]; then
    echo "Uso: ./scripts/run.sh <comando> <arquivo.thz> [args...]"
    echo "     ./scripts/run.sh run exemplos/faturamento.thz"
    exit 1
fi

COMANDO="$1"
shift

# Se o primeiro argumento for um arquivo .thz, assume comando=run
if [[ "$COMANDO" == *.thz ]]; then
    ARQUIVO="$COMANDO"
    COMANDO="run"
    ARGS_CLI=("$COMANDO" "$ARQUIVO" "$@")
else
    ARGS_CLI=("$COMANDO" "$@")
fi

echo -e "\033[0;36m[thz] ./gradlew :thz-cli-jvm:run --args=\"${ARGS_CLI[*]}\"\033[0m"
./gradlew :thz-cli-jvm:run --args="${ARGS_CLI[*]}"
