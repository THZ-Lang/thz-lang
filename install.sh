#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG — Instalador Oficial de 1 Linha para Linux / macOS / WSL
# Uso: curl -fsSL https://raw.githubusercontent.com/THZ-Lang/thz-lang/main/install.sh | bash
# ==============================================================================

set -euo pipefail

REPO="THZ-Lang/thz-lang"
DESTINO_RAIZ="$HOME/.thz"
DESTINO_BIN="$DESTINO_RAIZ/bin"

echo "================================================="
echo "    Instalador Oficial da Linguagem THZ-LANG     "
echo "================================================="

echo -e "\n[1/3] Identificando pacote oficial de distribuicao..."
TAG=$(curl -s "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/' || echo "v0.5.0")
if [ -z "$TAG" ]; then TAG="v0.5.0"; fi

echo "[OK] Versao alvo: $TAG"
mkdir -p "$DESTINO_BIN"

echo -e "\n[2/3] Baixando pacote executavel..."
URL="https://github.com/$REPO/releases/download/$TAG/thz-linux-x64.tar.gz"
TEMP_TAR=$(mktemp /tmp/thz-install.XXXXXX.tar.gz)

if curl -fsSL "$URL" -o "$TEMP_TAR"; then
    tar -xzf "$TEMP_TAR" -C "$DESTINO_RAIZ"
    rm -f "$TEMP_TAR"
else
    echo "[!] Pacote nativo Linux em compilacao ou use a CLI JAR."
fi

echo -e "\n[3/3] Configurando PATH..."
PROFILE="$HOME/.bashrc"
if [ -f "$HOME/.zshrc" ]; then PROFILE="$HOME/.zshrc"; fi

if ! grep -q "$DESTINO_BIN" "$PROFILE" 2>/dev/null; then
    echo "export PATH=\"$DESTINO_BIN:\$PATH\"" >> "$PROFILE"
    echo "[OK] Adicionado ao $PROFILE"
fi

echo "================================================="
echo "       THZ-LANG INSTALADO COM SUCESSO!           "
echo "================================================="
echo " Reinicie seu terminal ou execute: source $PROFILE"
echo " Teste com: thz --versao"
