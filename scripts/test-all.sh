#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Test All - Executa todos os testes JVM + relatórios (Linux/macOS)
# Uso: ./scripts/test-all.sh [all|core|cli|gui|api|lsp|bench] [--watch]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

MODULE="${1:-all}"
WATCH=false

for arg in "$@"; do
    case "$arg" in
        --watch|-w) WATCH=true ;;
    esac
done

case "$MODULE" in
    core)  TASK=":thz-core-jvm:test" ;;
    cli)   TASK=":thz-cli-jvm:test" ;;
    gui)   TASK=":thz-gui-jvm:test" ;;
    api)   TASK=":thz-api-jvm:test" ;;
    lsp)   TASK=":thz-lsp-jvm:test" ;;
    bench) TASK=":thz-bench-jvm:test" ;;
    all|*) TASK="test" ;;
esac

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Testes: $MODULE ($TASK)\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

if [ "$WATCH" = true ]; then
    ./gradlew $TASK --continuous --parallel
else
    ./gradlew $TASK --parallel
    echo -e "\033[0;32m[OK] Testes de $MODULE passaram com sucesso!\033[0m"
    echo -e "\033[0;90mRelatórios disponíveis em: JVM/*/build/reports/tests/test/index.html\033[0m"
fi
