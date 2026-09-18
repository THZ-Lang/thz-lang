#!/usr/bin/env bash
# ==============================================================================
# setup-rust.sh — Instalação portátil e autônoma do Rust Toolchain em .tools/rust
# ==============================================================================

set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$RAIZ/.tools/rust"
export CARGO_HOME="$TOOLS_DIR/cargo"
export RUSTUP_HOME="$TOOLS_DIR/rustup"
BIN_DIR="$CARGO_HOME/bin"

echo "=========================================================================="
echo " THZ-LANG — PROVISIONAMENTO PORTATIL DE TOOLCHAIN RUST (.tools/rust)"
echo "=========================================================================="

if [ -f "$BIN_DIR/cargo" ]; then
    echo "[OK] Rust/Cargo portátil já está instalado em: $BIN_DIR"
    "$BIN_DIR/rustc" --version
    "$BIN_DIR/cargo" --version
    exit 0
fi

mkdir -p "$TOOLS_DIR" "$CARGO_HOME" "$RUSTUP_HOME"

echo "[1/2] Baixando e executando rustup-init..."
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --no-modify-path --profile minimal

echo "[2/2] Configuração concluída!"
export PATH="$BIN_DIR:$PATH"
"$BIN_DIR/rustc" --version
"$BIN_DIR/cargo" --version
echo "=========================================================================="
