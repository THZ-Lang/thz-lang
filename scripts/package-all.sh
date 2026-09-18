#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Package All - Empacotamento de Distribuição no Linux
# Uso: ./scripts/package-all.sh [--skip-tests] [--with-native] [--with-llvm] [--with-vsix]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

SKIP_TESTS=false
WITH_NATIVE=false
WITH_LLVM=false
WITH_VSIX=false

for arg in "$@"; do
    case "$arg" in
        --skip-tests|-x) SKIP_TESTS=true ;;
        --with-native) WITH_NATIVE=true ;;
        --with-llvm) WITH_LLVM=true ;;
        --with-vsix) WITH_VSIX=true ;;
    esac
done

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Package All (Linux)\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

# 1. Build JVM shadowJars
echo -e "\n\033[0;33m[1/4] Compilando shadowJars JVM...\033[0m"
GRADLE_ARGS=( ":thz-cli-jvm:shadowJar" ":thz-lsp-jvm:shadowJar" )
if [ "$SKIP_TESTS" = true ]; then
    GRADLE_ARGS+=( "-x" "test" )
fi
./gradlew "${GRADLE_ARGS[@]}"

# 2. GraalVM Native (opcional)
if [ "$WITH_NATIVE" = true ]; then
    echo -e "\n\033[0;33m[2/4] Compilando binários nativos GraalVM...\033[0m"
    NATIVE_ARGS=()
    if [ "$SKIP_TESTS" = true ]; then
        NATIVE_ARGS+=( "--pular-testes" )
    fi
    bash "$RAIZ/scripts/build-native.sh" "${NATIVE_ARGS[@]}" || echo -e "\033[0;33m[AVISO] native-image falhou (opcional)\033[0m"
fi

# 3. LLVM AOT (opcional)
if [ "$WITH_LLVM" = true ]; then
    echo -e "\n\033[0;33m[3/4] Compilando programas AOT via LLVM...\033[0m"
    bash "$RAIZ/scripts/build-all.sh" || echo -e "\033[0;33m[AVISO] build-llvm falhou (opcional)\033[0m"
fi

# 4. VSIX Extension (opcional)
if [ "$WITH_VSIX" = true ]; then
    echo -e "\n\033[0;33m[4/4] Empacotando extensão VS Code .vsix...\033[0m"
    bash "$RAIZ/scripts/build-vsix.sh"
fi

echo -e "\n\033[0;32m=================================================\033[0m"
echo -e "\033[0;32m PACKAGE ALL CONCLUÍDO COM SUCESSO!\033[0m"
echo -e " Artefatos disponíveis em: dist/ e JVM/*/build/libs/"
echo -e "\033[0;32m=================================================\033[0m"
