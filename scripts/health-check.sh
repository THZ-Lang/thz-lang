#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Health Check - Diagnóstico do Ambiente (Linux/macOS)
# Verifica: Java 25, GraalVM, LLVM/Clang, GCC, Node.js, Gradle Wrapper
# Uso: ./scripts/health-check.sh
# ==============================================================================

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG -- Health Check (Linux/macOS)\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

OK=true

test_cmd() {
    local cmd="$1"
    local desc="$2"
    if command -v "$cmd" >/dev/null 2>&1; then
        echo -e "\033[0;32m[OK] $desc : $(command -v "$cmd")\033[0m"
        return 0
    else
        echo -e "\033[0;31m[FALTA] $desc ($cmd) não encontrado\033[0m"
        return 1
    fi
}

# 1. Java 25
if test_cmd "java" "Java (JDK)"; then
    java -version 2>&1 | head -n 1 | sed 's/^/      /'
else
    OK=false
fi

# 2. GraalVM Native Image
if test_cmd "native-image" "GraalVM Native Image"; then
    native-image --version 2>&1 | head -n 1 | sed 's/^/      /'
else
    echo -e "\033[0;33m[AVISO] native-image não encontrado no PATH (opcional para binários nativos AOT)\033[0m"
fi

# 3. LLVM / Clang
if test_cmd "clang" "LLVM Clang"; then
    clang --version 2>&1 | head -n 1 | sed 's/^/      /'
fi

# 4. GCC
if test_cmd "gcc" "GNU GCC"; then
    gcc --version 2>&1 | head -n 1 | sed 's/^/      /'
fi

# 5. Node.js e npm
if test_cmd "node" "Node.js"; then
    node -v | sed 's/^/      /'
else
    OK=false
fi

if test_cmd "npm" "npm"; then
    npm -v | sed 's/^/      /'
fi

# 6. Gradle Wrapper
if [ -f "$RAIZ/gradlew" ]; then
    echo -e "\033[0;32m[OK] Gradle Wrapper : $RAIZ/gradlew\033[0m"
    chmod +x "$RAIZ/gradlew"
else
    echo -e "\033[0;31m[FALTA] gradlew não encontrado na raiz\033[0m"
    OK=false
fi

# 7. Módulos do Projeto
for dir in "JVM/thz-core-jvm" "JVM/thz-cli-jvm" "JVM/thz-gui-jvm" "JVM/thz-lsp-jvm" "JVM/thz-api-jvm" "exemplos" "compilador"; do
    if [ -d "$RAIZ/$dir" ]; then
        echo -e "\033[0;32m[OK] Módulo $dir\033[0m"
    else
        echo -e "\033[0;31m[FALTA] Módulo $dir não encontrado\033[0m"
        OK=false
    fi
done

echo -e "\033[0;36m=================================================\033[0m"
if [ "$OK" = true ]; then
    echo -e "\033[0;32m Ambiente 100% OK -- pronto para ./scripts/setup.sh\033[0m"
else
    echo -e "\033[0;31m Ambiente possui pendências listadas acima.\033[0m"
fi
