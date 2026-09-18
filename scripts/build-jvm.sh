#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Build JVM - Compila todos os módulos JVM (Linux/macOS)
# Uso: ./scripts/build-jvm.sh [--skip-tests] [--no-parallel]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

ARGS_GRADLE=("build")

for arg in "$@"; do
    case "$arg" in
        --skip-tests|-x) ARGS_GRADLE+=("-x" "test") ;;
        --no-parallel) ;;
        *) ARGS_GRADLE+=("$arg") ;;
    esac
done

if [[ ! " ${ARGS_GRADLE[*]} " =~ " --no-parallel " ]]; then
    ARGS_GRADLE+=("--parallel")
fi

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Build JVM\033[0m"
echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;90m Gradle: ./gradlew ${ARGS_GRADLE[*]}\033[0m"

./gradlew "${ARGS_GRADLE[@]}"

echo -e "\n\033[0;32m[OK] Build JVM concluído com sucesso!\033[0m"
find "$RAIZ/JVM" -type f -path "*/build/libs/*.jar" -exec ls -lh {} + 2>/dev/null || true
