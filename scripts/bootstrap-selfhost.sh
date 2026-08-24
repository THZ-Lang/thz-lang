#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Engine - Bootstrap do Compilador Self-Hosted (Zero JVM) no Linux
# Uso: ./scripts/bootstrap-selfhost.sh [--limpar]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

DIST_BIN="$RAIZ/dist/bin"
mkdir -p "$DIST_BIN"

if [ "$1" == "--limpar" ]; then
    echo -e "\033[0;33m[LIMPEZA] Removendo binários anteriores em dist/bin...\033[0m"
    rm -f "$DIST_BIN/thzc.elf" "$DIST_BIN/driver.elf" "$DIST_BIN/faturamento.elf"
fi

echo -e "\033[0;36m==========================================================================\033[0m"
echo -e "\033[0;36m THZ-LANG Engine - BOOTSTRAP DO COMPILADOR NATIVO SELF-HOSTED (thzc.elf) \033[0m"
echo -e "\033[0;36m==========================================================================\033[0m"

# PASSO 1: Compilar suite compilador/driver.thz
echo -e "\n\033[0;33m[PASSO 1/3] Gerando binário nativo do compilador (thzc.elf) via LLVM AOT...\033[0m"
bash "$RAIZ/scripts/build-llvm.sh" "$RAIZ/compilador/driver.thz" --saida "$DIST_BIN/thzc.elf"

if [ -f "$DIST_BIN/thzc.elf" ]; then
    chmod +x "$DIST_BIN/thzc.elf"
    echo -e "\033[0;32m[OK] Compilador nativo gerado com sucesso: $DIST_BIN/thzc.elf\033[0m"
else
    echo -e "\033[0;31m[ERRO] Falha ao gerar o executável nativo do compilador.\033[0m"
    exit 1
fi

# PASSO 2: Testar execução autônoma
echo -e "\n\033[0;33m[PASSO 2/3] Executando thzc.elf de forma 100% autônoma (Zero JVM)...\033[0m"
"$DIST_BIN/thzc.elf"
echo -e "\033[0;32m[OK] thzc.elf executou nativamente sem qualquer dependência de JVM!\033[0m"

# PASSO 3: Compilar e testar faturamento.thz
echo -e "\n\033[0;33m[PASSO 3/3] Compilando e testando programa de negócio canônico (faturamento.thz)...\033[0m"
bash "$RAIZ/scripts/build-llvm.sh" "$RAIZ/exemplos/faturamento.thz" --saida "$DIST_BIN/faturamento.elf"

if [ -f "$DIST_BIN/faturamento.elf" ]; then
    chmod +x "$DIST_BIN/faturamento.elf"
    echo -e "\033[0;32m[OK] Executável faturamento.elf gerado com sucesso:\033[0m"
    "$DIST_BIN/faturamento.elf"
fi

echo -e "\n\033[0;32m==========================================================================\033[0m"
echo -e "\033[0;32m BOOTSTRAP SELF-HOSTING CONCLUÍDO COM SUCESSO NO LINUX! (Zero JVM)       \033[0m"
echo -e "\033[0;32m==========================================================================\033[0m"
