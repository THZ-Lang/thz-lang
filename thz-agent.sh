#!/usr/bin/env bash
# ==============================================================================
# thz-agent.sh — THZ-Agent direto (sem Gradle)
# Uso: ./thz-agent.sh [opções]
#      ./thz-agent.sh --modelo caminho/modelo.gguf
#      ./thz-agent.sh --api https://api.openai.com/v1 --api-key sk-...
# ==============================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$DIR/target/thz-jvm.jar"

# Auto-detectar Rust portátil
if [ -f "$DIR/.tools/rust/cargo/bin/cargo" ]; then
    export PATH="$DIR/.tools/rust/cargo/bin:$PATH"
fi

# Verificar se o JAR existe
if [ ! -f "$JAR" ]; then
    echo "[THZ-Agent] Shadow JAR não encontrado. Buildando..."
    "$DIR/gradlew" :thz-cli-jvm:shadowJar --no-daemon -q
    if [ ! -f "$JAR" ]; then
        echo "[THZ-Agent] ERRO: Falha ao buildar o JAR."
        exit 1
    fi
fi

# Executar direto via java -jar (sem Gradle no meio)
exec java \
    "-Dfile.encoding=UTF-8" \
    "-Dstdout.encoding=UTF-8" \
    "-Dstderr.encoding=UTF-8" \
    --enable-native-access=ALL-UNNAMED \
    -jar "$JAR" \
    agent "$@"
