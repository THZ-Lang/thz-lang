#!/usr/bin/env bash
# ==============================================================================
# Script de Empacotamento THZ-LANG Engine JVM (jpackage / Java 25) no Linux
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$DIR/../../.." && pwd)"
cd "$RAIZ"

VERSAO="2.4.0"
if [ -f "$RAIZ/version.txt" ]; then
    VERSAO="$(cat "$RAIZ/version.txt" | tr -d '[:space:]')"
fi

SKIP_TESTS=false
for arg in "$@"; do
    case "$arg" in
        --pular-testes|-x) SKIP_TESTS=true ;;
    esac
done

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG Engine JVM - Gerador de Pacote jpackage (Linux)\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

# 1. Resolver jpackage
JPACKAGE_BIN="${JAVA_HOME:+$JAVA_HOME/bin/jpackage}"
if [ -z "$JPACKAGE_BIN" ] || [ ! -x "$JPACKAGE_BIN" ]; then
    JPACKAGE_BIN="$(command -v jpackage || true)"
fi

if [ -z "$JPACKAGE_BIN" ] || [ ! -x "$JPACKAGE_BIN" ]; then
    echo -e "\033[0;31m[ERRO] jpackage do Java 25 não foi encontrado no PATH.\033[0m"
    exit 1
fi

echo -e "\033[0;32m[OK] Usando jpackage: $JPACKAGE_BIN\033[0m"

# 2. Build Gradle
echo -e "\n\033[0;33m[1/3] Compilando Shaded JAR com Gradle...\033[0m"
GRADLE_ARGS=( ":thz-cli-jvm:shadowJar" )
if [ "$SKIP_TESTS" = true ]; then
    GRADLE_ARGS+=( "-x" "test" )
fi
./gradlew "${GRADLE_ARGS[@]}"

JAR_PATH="$RAIZ/target/thz-jvm.jar"
if [ ! -f "$JAR_PATH" ]; then
    JAR_PATH="$(find "$RAIZ/target" "$RAIZ/JVM/thz-cli-jvm/build/libs" -name "thz-jvm*.jar" ! -name "*-sources.jar" | head -n 1)"
fi

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo -e "\033[0;31m[ERRO] JAR da CLI não foi encontrado em target/.\033[0m"
    exit 1
fi
echo -e "\033[0;32m[OK] JAR pronto: $JAR_PATH\033[0m"

# 3. Preparar diretório de entrada para jpackage
APP_INPUT_DIR="$RAIZ/target/jpackage-input"
rm -rf "$APP_INPUT_DIR"
mkdir -p "$APP_INPUT_DIR"
cp -f "$JAR_PATH" "$APP_INPUT_DIR/thz-engine.jar"

DIST_DIR="$RAIZ/dist"
DEST_DIR="$DIST_DIR/thz"
rm -rf "$DEST_DIR"
mkdir -p "$DIST_DIR"

# 4. Executar jpackage para gerar App-Image
echo -e "\n\033[0;33m[2/3] Gerando pacote autônomo com jpackage (Linux)...\033[0m"
"$JPACKAGE_BIN" \
    --type app-image \
    --input "$APP_INPUT_DIR" \
    --dest "$DIST_DIR" \
    --name thz \
    --main-jar thz-engine.jar \
    --main-class thz.lang.cli.ThzCli \
    --app-version "$VERSAO" \
    --vendor "THZ-LANG Team" \
    --description "THZ-LANG Engine e Desktop IDE" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED"

echo -e "\n\033[0;32m=================================================\033[0m"
echo -e "\033[0;32m EMPACOTAMENTO LINUX CONCLUÍDO COM SUCESSO!\033[0m"
echo -e " Executável: $DEST_DIR/bin/thz"
echo -e "\033[0;32m=================================================\033[0m"
