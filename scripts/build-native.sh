#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Engine - Compilação Nativa GraalVM Native Image (Linux)
# Gera os binários:
#   1. dist/bin/thz      (CLI ultrarrápida do THZ-LANG)
#   2. dist/bin/thz-gui  (Desktop IDE Swing + FlatLaf nativa)
#
# Uso: ./scripts/build-native.sh [--pular-testes] [--apenas-cli] [--apenas-gui]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

PULAR_TESTES=false
APENAS_CLI=false
APENAS_GUI=false

for arg in "$@"; do
    case "$arg" in
        --pular-testes|-x) PULAR_TESTES=true ;;
        --apenas-cli) APENAS_CLI=true ;;
        --apenas-gui) APENAS_GUI=true ;;
    esac
done

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Compilação Nativa GraalVM (Linux)   \033[0m"
echo -e "\033[0;36m=================================================\033[0m"

if ! command -v native-image >/dev/null 2>&1; then
    echo -e "\033[0;31m[ERRO] native-image (GraalVM) não foi encontrado no PATH.\033[0m"
    echo -e "Instale o GraalVM JDK 25 e configure JAVA_HOME e PATH."
    exit 1
fi

DIST_BIN="$RAIZ/dist/bin"
mkdir -p "$DIST_BIN"

# 1. Compilar CLI Nativa
if [ "$APENAS_GUI" = false ]; then
    echo -e "\n\033[0;33m[1/2] Compilando CLI Nativa (thz) via GraalVM...\033[0m"
    GRADLE_ARGS=( ":thz-cli-jvm:nativeCompile" )
    if [ "$PULAR_TESTES" = true ]; then
        GRADLE_ARGS+=( "-x" "test" )
    fi
    ./gradlew "${GRADLE_ARGS[@]}"

    CLI_NATIVE_SRC="$RAIZ/JVM/thz-cli-jvm/build/native/nativeCompile/thz-cli-jvm"
    if [ -f "$CLI_NATIVE_SRC" ]; then
        cp -f "$CLI_NATIVE_SRC" "$DIST_BIN/thz"
        chmod +x "$DIST_BIN/thz"
        echo -e "\033[0;32m[OK] CLI Nativa publicada: $DIST_BIN/thz\033[0m"
    fi
fi

# 2. Compilar GUI Nativa
if [ "$APENAS_CLI" = false ]; then
    echo -e "\n\033[0;33m[2/2] Compilando Desktop IDE Nativa (thz-gui) via GraalVM...\033[0m"
    GRADLE_ARGS=( ":thz-gui-jvm:nativeCompile" )
    if [ "$PULAR_TESTES" = true ]; then
        GRADLE_ARGS+=( "-x" "test" )
    fi
    ./gradlew "${GRADLE_ARGS[@]}"

    GUI_NATIVE_SRC="$RAIZ/JVM/thz-gui-jvm/build/native/nativeCompile/thz-gui-jvm"
    if [ -f "$GUI_NATIVE_SRC" ]; then
        cp -f "$GUI_NATIVE_SRC" "$DIST_BIN/thz-gui"
        chmod +x "$DIST_BIN/thz-gui"
        echo -e "\033[0;32m[OK] GUI Nativa publicada: $DIST_BIN/thz-gui\033[0m"
    fi
fi

echo -e "\n\033[0;32m=================================================\033[0m"
echo -e "\033[0;32m COMPILAÇÃO NATIVA CONCLUÍDA!\033[0m"
echo -e " Binários gerados em: $DIST_BIN/"
echo -e "\033[0;32m=================================================\033[0m"
