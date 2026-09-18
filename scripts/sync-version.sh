#!/usr/bin/env bash
# ==============================================================================
# Sincronizador Automático de Versão — THZ-LANG Engine
# Fonte Única da Verdade (Single Source of Truth): version.txt
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$DIR/.." && pwd)"

ARQ_VERSAO="$RAIZ/version.txt"
if [ ! -f "$ARQ_VERSAO" ]; then
    echo "[ERRO] Arquivo version.txt não encontrado na raiz: $RAIZ" >&2
    exit 1
fi

VERSAO="$(cat "$ARQ_VERSAO" | tr -d '[:space:]')"
if [ -z "$VERSAO" ]; then
    echo "[ERRO] version.txt está vazio." >&2
    exit 1
fi

echo "=================================================================="
echo " Sincronizando versão global do THZ-LANG: v$VERSAO"
echo " Fonte da verdade: $ARQ_VERSAO"
echo "=================================================================="

# 1. thz.config.json
CONFIG_JSON="$RAIZ/thz.config.json"
if [ -f "$CONFIG_JSON" ]; then
    sed -i -E "s/(\"versao\":\s*\")[^\"]+(\")/\1$VERSAO\2/" "$CONFIG_JSON"
    echo "  [OK] thz.config.json -> $VERSAO"
fi

# 2. Rust Runtime (Cargo.toml)
CARGO_TOML="$RAIZ/../thz-runtime-rs/Cargo.toml"
if [ ! -f "$CARGO_TOML" ]; then CARGO_TOML="$RAIZ/src/runtime_rs/Cargo.toml"; fi
if [ -f "$CARGO_TOML" ]; then
    sed -i -E "s/^(version\s*=\s*\")[^\"]+(\")/\1$VERSAO\2/" "$CARGO_TOML"
    echo "  [OK] $CARGO_TOML -> $VERSAO"
fi

# 3. Rust Runtime (Cargo.lock)
CARGO_LOCK="$RAIZ/../thz-runtime-rs/Cargo.lock"
if [ ! -f "$CARGO_LOCK" ]; then CARGO_LOCK="$RAIZ/src/runtime_rs/Cargo.lock"; fi
if [ -f "$CARGO_LOCK" ]; then
    sed -i -E "/name = \"thz_runtime_rs\"/{n;s/version = \"[^\"]+\"/version = \"$VERSAO\"/}" "$CARGO_LOCK"
    echo "  [OK] $CARGO_LOCK -> $VERSAO"
fi

# 4. WASM Bridge (wasm.rs)
WASM_RS="$RAIZ/../thz-runtime-rs/src/wasm.rs"
if [ ! -f "$WASM_RS" ]; then WASM_RS="$RAIZ/src/runtime_rs/src/wasm.rs"; fi
if [ -f "$WASM_RS" ]; then
    sed -i -E "s/CString::new\(\"[^\"]+-WASM\"\)/CString::new(\"$VERSAO-WASM\")/g" "$WASM_RS"
    echo "  [OK] $WASM_RS -> $VERSAO-WASM"
fi

# 5. VS Code Extension (package.json)
PKG_JSON="$RAIZ/../thz-vscode/package.json"
if [ ! -f "$PKG_JSON" ]; then PKG_JSON="$RAIZ/Extensions/thz-lsp-vscode/package.json"; fi
if [ -f "$PKG_JSON" ]; then
    sed -i -E "s/(\"version\":\s*\")[^\"]+(\")/\1$VERSAO\2/" "$PKG_JSON"
    echo "  [OK] $PKG_JSON -> $VERSAO"
fi

# 6. VS Code Extension (package-lock.json)
PKG_LOCK="$RAIZ/../thz-vscode/package-lock.json"
if [ ! -f "$PKG_LOCK" ]; then PKG_LOCK="$RAIZ/Extensions/thz-lsp-vscode/package-lock.json"; fi
if [ -f "$PKG_LOCK" ]; then
    sed -i -E "0,/\"version\": \"[^\"]+\"/s/\"version\": \"[^\"]+\"/\"version\": \"$VERSAO\"/" "$PKG_LOCK"
    sed -i -E "/\"\": \{/{n;n;s/\"version\": \"[^\"]+\"/\"version\": \"$VERSAO\"/}" "$PKG_LOCK"
    echo "  [OK] $PKG_LOCK -> $VERSAO"
fi

# 7. VS Code Extension (extension.ts Cockpit)
EXT_TS="$RAIZ/../thz-vscode/src/extension.ts"
if [ ! -f "$EXT_TS" ]; then EXT_TS="$RAIZ/Extensions/thz-lsp-vscode/src/extension.ts"; fi
if [ -f "$EXT_TS" ]; then
    sed -i -E "s/'THZ-LANG [0-9]+\.[0-9]+\.[0-9]+'/'THZ-LANG $VERSAO'/g" "$EXT_TS"
    echo "  [OK] $EXT_TS -> THZ-LANG $VERSAO"
fi

# 8. Sincronizar version.txt em todos os módulos irmãos
for vfile in "$RAIZ"/../thz-*/version.txt; do
    if [ -f "$vfile" ]; then
        echo "$VERSAO" > "$vfile"
        echo "  [OK] $vfile -> $VERSAO"
    fi
done

# 8. README.md (Badge)
README_MD="$RAIZ/README.md"
if [ -f "$README_MD" ]; then
    sed -i -E "s/badge\/version-[0-9]+\.[0-9]+\.[0-9]+-blue\.svg/badge\/version-$VERSAO-blue.svg/g" "$README_MD"
    echo "  [OK] README.md badge -> $VERSAO"
fi

echo "=================================================================="
echo " Sincronização concluída com sucesso!"
echo "=================================================================="
