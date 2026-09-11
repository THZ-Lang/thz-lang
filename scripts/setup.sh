#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Setup - One-Click Bootstrap no Linux/macOS
# Uso: ./scripts/setup.sh [--skip-tests]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

SKIP_TESTS=false
if [ "$1" == "--skip-tests" ] || [ "$1" == "-x" ]; then
    SKIP_TESTS=true
fi

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Setup (One Click) no Linux/macOS      \033[0m"
echo -e "\033[0;36m=================================================\033[0m"

# 1. Health check
echo -e "\n\033[0;33m[0/4] Verificando ambiente...\033[0m"
bash "$RAIZ/scripts/health-check.sh" || true

# 2. Publicar thz-core no mavenLocal
echo -e "\n\033[0;33m[1/4] Publicando thz-core (mavenLocal)...\033[0m"
./gradlew :thz-core-jvm:publishToMavenLocal --parallel

# 3. Compilar shadowJars
echo -e "\n\033[0;33m[2/4] Compilando módulos JVM...\033[0m"
GRADLE_ARGS=( ":thz-cli-jvm:shadowJar" ":thz-gui-jvm:classes" ":thz-api-jvm:classes" ":thz-lsp-jvm:shadowJar" )
if [ "$SKIP_TESTS" = true ]; then
    GRADLE_ARGS+=( "-x" "test" )
fi
./gradlew "${GRADLE_ARGS[@]}"

# 4. Testes
if [ "$SKIP_TESTS" = false ]; then
    echo -e "\n\033[0;33m[3/4] Executando testes unitários...\033[0m"
    ./gradlew :thz-core-jvm:test :thz-cli-jvm:test :thz-gui-jvm:test :thz-lsp-jvm:test :thz-api-jvm:test
fi

# 5. Resumo
CLI_JAR="$(find "$RAIZ/JVM/thz-cli-jvm/build/libs" -name "thz-jvm-*.jar" 2>/dev/null | head -n 1 || true)"
echo -e "\n\033[0;32m=================================================\033[0m"
echo -e "\033[0;32m SETUP CONCLUÍDO COM SUCESSO!\033[0m"
if [ -n "$CLI_JAR" ]; then
    echo -e " JAR CLI : $CLI_JAR"
fi
echo -e "\033[0;32m-------------------------------------------------\033[0m"
echo -e " Próximos passos:"
echo -e "   ./scripts/run.sh run exemplos/faturamento.thz"
echo -e "   ./scripts/gui.sh"
echo -e "   ./scripts/build-jvm.sh"
echo -e "   ./scripts/build-vsix.sh"
echo -e "\033[0;32m=================================================\033[0m"
