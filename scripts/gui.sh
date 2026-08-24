#!/usr/bin/env bash
# ==============================================================================
# THZ-LANG GUI - Lança a Desktop IDE oficial (Swing + FlatLaf)
# Uso: ./scripts/gui.sh
# ==============================================================================
set -e

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

echo -e "\033[0;36m[GUI] Iniciando Desktop IDE oficial THZ-LANG (Swing + FlatLaf)...\033[0m"
./gradlew :thz-gui-jvm:gui

