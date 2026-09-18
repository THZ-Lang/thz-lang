#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Dev - Modo desenvolvimento (API Spring Boot ou GUI) (Linux/macOS)
# Uso: ./scripts/dev.sh [--api-only] [--gui-only]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

API_ONLY=false
GUI_ONLY=false

for arg in "$@"; do
    case "$arg" in
        --api-only) API_ONLY=true ;;
        --gui-only) GUI_ONLY=true ;;
    esac
done

if [ "$GUI_ONLY" = true ]; then
    echo -e "\033[0;36m[dev] Iniciando GUI Desktop...\033[0m"
    bash "$RAIZ/scripts/gui.sh"
    exit 0
fi

if [ "$API_ONLY" = true ]; then
    echo -e "\033[0;36m[dev] Iniciando API Spring Boot...\033[0m"
    ./gradlew :thz-api-jvm:bootRun
    exit 0
fi

echo -e "\033[0;33m[dev] Iniciando API Spring Boot...\033[0m"
echo -e "\033[0;90m      API: http://localhost:8080 | GUI: ./scripts/gui.sh\033[0m"
./gradlew :thz-api-jvm:bootRun
