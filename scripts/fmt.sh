#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Fmt - Formatador Canônico de Código THZ (Linux/macOS)
# Uso: ./scripts/fmt.sh <arquivo.thz|dir> [--check] [--escrever]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

ALVO="${1:-exemplos/faturamento.thz}"
EXTRA=""

for arg in "$@"; do
    case "$arg" in
        --check) EXTRA="$EXTRA --check" ;;
        --escrever) EXTRA="$EXTRA --escrever" ;;
    esac
done

echo -e "\033[0;36m[fmt] Formatando: $ALVO $EXTRA\033[0m"
./gradlew :thz-cli-jvm:run --args="fmt $ALVO $EXTRA"
