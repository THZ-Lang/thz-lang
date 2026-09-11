#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG — Script de Compilação em Lote de Todos os Exemplos (Linux/macOS)
# ==============================================================================

set -euo pipefail
DIR_RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR_RAIZ"

ORIGEM="${1:-exemplos}"
DESTINO="${2:-dist/exemplos_compilados}"

GRADLEW="./gradlew"
if [ ! -f "$GRADLEW" ]; then
    GRADLEW="gradle"
fi

echo "Iniciando compilação de todos os programas THZ-LANG..."
"$GRADLEW" :thz-cli-jvm:run --args="compile-all --origem $ORIGEM --saida $DESTINO"

echo "[SUCESSO] Todos os exemplos compilados em: $DESTINO"
