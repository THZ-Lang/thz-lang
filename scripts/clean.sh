#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Clean - Limpa builds, caches e binários gerados (Linux/macOS)
# Uso: ./scripts/clean.sh [--deep] [--dist-only]
# ==============================================================================

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

DEEP=false
DIST_ONLY=false

for arg in "$@"; do
    case "$arg" in
        --deep) DEEP=true ;;
        --dist-only) DIST_ONLY=true ;;
    esac
done

if [ "$DIST_ONLY" = true ]; then
    echo -e "\033[0;33m[clean] Removendo dist/ e target/...\033[0m"
    rm -rf "$RAIZ/dist" "$RAIZ/target"
    echo -e "\033[0;32m[OK] dist/ e target/ limpos.\033[0m"
    exit 0
fi

echo -e "\033[0;33m[clean] Executando ./gradlew clean...\033[0m"
./gradlew clean 2>&1 | tail -n 5 || true

echo -e "\033[0;90m[clean] Removendo pastas de build locais...\033[0m"
rm -rf "$RAIZ"/JVM/*/build "$RAIZ/build" "$RAIZ/target" "$RAIZ/dist"

if [ "$DEEP" = true ]; then
    echo -e "\033[0;33m[clean --deep] Removendo cache local .gradle/...\033[0m"
    rm -rf "$RAIZ/.gradle"
fi

echo -e "\033[0;32m[OK] Limpeza concluída com sucesso!\033[0m"
