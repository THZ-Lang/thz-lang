#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG Engine - Compilação AOT de todos os programas fonte .thz (Linux)
# Uso: ./scripts/build-all.sh [--force-legado]
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

FORCE_LEGADO=false
if [ "$1" == "--force-legado" ]; then
    FORCE_LEGADO=true
fi

echo -e "\033[0;36m==========================================================================\033[0m"
echo -e "\033[0;36m THZ-LANG Engine - COMPILAÇÃO AOT NATIVA EM LOTE (Linux ELF)             \033[0m"
echo -e "\033[0;36m==========================================================================\033[0m"

shopt -s nullglob
FONTES=( "$RAIZ"/compilador/*.thz "$RAIZ"/exemplos/*.thz )

SUCESSOS=0
TOTAL=0

for fonte in "${FONTES[@]}"; do
    nome="$(basename "$fonte")"
    if [[ "$nome" =~ _gui ]] && [ "$FORCE_LEGADO" = false ]; then
        continue
    fi
    ((TOTAL++))
done

echo -e "\033[0;33m[INFO] $TOTAL arquivos .thz selecionados para compilação nativa.\033[0m\n"

for fonte in "${FONTES[@]}"; do
    nome="$(basename "$fonte")"
    if [[ "$nome" =~ _gui ]] && [ "$FORCE_LEGADO" = false ]; then
        continue
    fi

    echo -e "\033[0;90m--------------------------------------------------------------------------\033[0m"
    echo -e "\033[0;33mCompilando ($((SUCESSOS + 1))/$TOTAL): $nome...\033[0m"

    if bash "$RAIZ/scripts/build-llvm.sh" "$fonte"; then
        ((SUCESSOS++))
    else
        echo -e "\033[0;31m[ERRO] Falha ao compilar $nome\033[0m"
    fi
done

echo -e "\n\033[0;32m==========================================================================\033[0m"
echo -e "\033[0;32m COMPILAÇÃO EM LOTE CONCLUÍDA!\033[0m"
echo -e " Total Processado: $SUCESSOS / $TOTAL arquivos compilados com sucesso."
echo -e " Binários publicados em: $RAIZ/dist/bin/"
echo -e "\033[0;32m==========================================================================\033[0m"
