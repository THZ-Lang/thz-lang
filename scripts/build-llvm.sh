#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Engine - Compilador AOT Nativo (LLVM IR -> ELF Linux)
# Uso: ./scripts/build-llvm.sh <arquivo.thz> [--saida <binario>] [--force-legado]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

if [ -z "$1" ]; then
    echo "Uso: ./scripts/build-llvm.sh <arquivo.thz> [--saida <caminho_elf>]"
    exit 1
fi

ARQUIVO_THZ="$1"
shift

SAIDA=""
FORCE_LEGADO=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --saida|-o) SAIDA="$2"; shift 2 ;;
        --force-legado) FORCE_LEGADO=true; shift ;;
        *) shift ;;
    esac
done

if [ ! -f "$ARQUIVO_THZ" ]; then
    echo -e "\033[0;31m[ERRO] Arquivo fonte não encontrado: $ARQUIVO_THZ\033[0m"
    exit 1
fi

NOME_BASE="$(basename "$ARQUIVO_THZ" .thz)"
DIST_BIN="$RAIZ/dist/bin"
mkdir -p "$DIST_BIN"

# Bloqueio GUI legado se aplicável
if [[ "$NOME_BASE" =~ _gui ]] && [ "$FORCE_LEGADO" = false ]; then
    echo -e "\033[0;33m=================================================\033[0m"
    echo -e "\033[0;33m [AVISO] $NOME_BASE contém interface gráfica.\033[0m"
    echo -e " Use o comando recomendado para executar interfaces:"
    echo -e "   ./scripts/run.sh run $ARQUIVO_THZ"
    echo -e "   ./scripts/gui.sh"
    echo -e "\033[0;33m=================================================\033[0m"
fi

LLVM_FILE="$DIST_BIN/$NOME_BASE.ll"
RUNTIME_C="$RAIZ/src/runtime/thz_runtime.c"
OBJ_LIN="$DIST_BIN/$NOME_BASE-lin.o"
ELF_LIN="${SAIDA:-$DIST_BIN/$NOME_BASE.elf}"

CLANG_BIN="${CLANG:-clang}"
GCC_BIN="${CC:-gcc}"

echo -e "\033[0;36m=================================================\033[0m"
echo -e "\033[0;36m THZ-LANG - Compilação AOT Nativa (Linux ELF)\033[0m"
echo -e "\033[0;36m Fonte: $ARQUIVO_THZ -> $ELF_LIN\033[0m"
echo -e "\033[0;36m=================================================\033[0m"

# 1. Gerar LLVM IR
echo -e "\n\033[0;33m[1/3] Gerando LLVM IR a partir do fonte THZ...\033[0m"
./gradlew :thz-cli-jvm:run --args="ir \"$ARQUIVO_THZ\" --llvm --saida \"$LLVM_FILE\""

# 2. Compilar Objeto com Clang
echo -e "\n\033[0;33m[2/3] Compilando objeto ELF com LLVM Clang...\033[0m"
$CLANG_BIN -c "$LLVM_FILE" -o "$OBJ_LIN"

# 3. Linkar Executável com Runtime C Dual-OS
echo -e "\n\033[0;33m[3/3] Linkando binário nativo ELF Linux...\033[0m"
$GCC_BIN -O3 "$OBJ_LIN" "$RUNTIME_C" -o "$ELF_LIN" -lm -lpthread

chmod +x "$ELF_LIN"

echo -e "\n\033[0;32m[OK] Executável nativo Linux gerado com sucesso: $ELF_LIN\033[0m"
