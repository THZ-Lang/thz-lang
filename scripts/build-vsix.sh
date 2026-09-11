#!/usr/bin/env bash
set -e

# ==============================================================================
# Script de Build do Pacote VSIX da Extensão THZ-LANG (Linux/macOS)
# Uso: ./scripts/build-vsix.sh [--install] [--skip-jar-build]
# ==============================================================================

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PASTA_EXTENSAO="$RAIZ/Extensions/thz-lsp-vscode"
PASTA_DIST="$RAIZ/dist"
PASTA_SERVER_EXT="$PASTA_EXTENSAO/server"
LSP_JAR_ORIGEM="$RAIZ/JVM/thz-lsp-jvm/build/libs/thz-lsp-2.3.0.jar"
LSP_JAR_DESTINO="$PASTA_SERVER_EXT/thz-lsp-2.3.0.jar"

INSTALL=false
SKIP_JAR=false

for arg in "$@"; do
    case "$arg" in
        --install|-i) INSTALL=true ;;
        --skip-jar-build) SKIP_JAR=true ;;
    esac
done

echo -e "\033[0;36m==========================================================================\033[0m"
echo -e "\033[0;36m         BUILD DO PACOTE VSIX DA EXTENSÃO THZ-LANG (LSP + SINTAXE)        \033[0m"
echo -e "\033[0;36m==========================================================================\033[0m"

mkdir -p "$PASTA_DIST"
mkdir -p "$PASTA_SERVER_EXT"

# 1. Compilar JAR do LSP se necessário
LSP_JAR_ORIGEM="$(find "$RAIZ/JVM/thz-lsp-jvm/build/libs" "$RAIZ/target" -maxdepth 1 -name "thz-lsp*.jar" ! -name "*-sources.jar" 2>/dev/null | head -n 1)"

if [ "$SKIP_JAR" = false ] || [ -z "$LSP_JAR_ORIGEM" ] || [ ! -f "$LSP_JAR_ORIGEM" ]; then
    echo -e "\n\033[0;33m[1/5] Compilando Servidor LSP Java 25 (shadowJar)...\033[0m"
    (cd "$RAIZ" && ./gradlew :thz-lsp-jvm:shadowJar)
    LSP_JAR_ORIGEM="$(find "$RAIZ/JVM/thz-lsp-jvm/build/libs" "$RAIZ/target" -maxdepth 1 -name "thz-lsp*.jar" ! -name "*-sources.jar" 2>/dev/null | head -n 1)"
else
    echo -e "\n\033[0;90m[1/5] Utilizando shadowJar existente em $LSP_JAR_ORIGEM...\033[0m"
fi

if [ -z "$LSP_JAR_ORIGEM" ] || [ ! -f "$LSP_JAR_ORIGEM" ]; then
    echo -e "\033[0;31m[ERRO] JAR do LSP não foi encontrado.\033[0m"
    exit 1
fi

# 2. Copiar JAR para pasta da extensão
echo -e "\n\033[0;33m[2/5] Copiando servidor LSP para a extensão...\033[0m"
cp -f "$LSP_JAR_ORIGEM" "$PASTA_SERVER_EXT/thz-lsp.jar"
cp -f "$LSP_JAR_ORIGEM" "$PASTA_SERVER_EXT/$(basename "$LSP_JAR_ORIGEM")"
if [ -f "$RAIZ/LICENSE" ]; then
    cp -f "$RAIZ/LICENSE" "$PASTA_EXTENSAO/LICENSE"
fi

# 3. Compilar TypeScript
echo -e "\n\033[0;33m[3/5] Compilando extensão TypeScript...\033[0m"
(cd "$PASTA_EXTENSAO" && npm run compile)

# 4. Empacotar VSIX
echo -e "\n\033[0;33m[4/5] Empacotando arquivo .vsix...\033[0m"
(cd "$PASTA_EXTENSAO" && npx -y @vscode/vsce package --no-dependencies)

VSIX_FILE="$(find "$PASTA_EXTENSAO" -maxdepth 1 -name "*.vsix" | head -n 1)"
if [ -n "$VSIX_FILE" ]; then
    cp -f "$VSIX_FILE" "$PASTA_DIST/"
    NOME_VSIX="$(basename "$VSIX_FILE")"
    DESTINO_VSIX="$PASTA_DIST/$NOME_VSIX"
    echo -e "\033[0;32m[SUCESSO] VSIX criado com sucesso: $DESTINO_VSIX\033[0m"
else
    echo -e "\033[0;31m[ERRO] Nenhum arquivo .vsix foi gerado.\033[0m"
    exit 1
fi

# 5. Instalar se solicitado
if [ "$INSTALL" = true ]; then
    echo -e "\n\033[0;33m[5/5] Instalando extensão no ambiente...\033[0m"
    if command -v code >/dev/null 2>&1; then
        echo -e "  -> Instalando no VS Code (code --install-extension $DESTINO_VSIX)..."
        code --install-extension "$DESTINO_VSIX" --force || true
    fi

    TARGET_DIRS=(
        "$HOME/.antigravity-ide/extensions/thz-lang.thz-lang-0.3.0"
        "$HOME/.antigravity-ide/extensions/thz-lang-0.3.0"
        "$HOME/.vscode/extensions/thz-lang.thz-lang-0.3.0"
        "$HOME/.vscode/extensions/thz-lang-0.3.0"
    )

    for TARGET in "${TARGET_DIRS[@]}"; do
        PARENT_DIR="$(dirname "$TARGET")"
        if [ -d "$PARENT_DIR" ]; then
            echo -e "  -> Sincronizando com $TARGET..."
            mkdir -p "$TARGET/dist" "$TARGET/syntaxes" "$TARGET/server" "$TARGET/assets" "$TARGET/snippets"
            cp -rf "$PASTA_EXTENSAO/dist"/* "$TARGET/dist/" 2>/dev/null || true
            cp -rf "$PASTA_EXTENSAO/syntaxes"/* "$TARGET/syntaxes/" 2>/dev/null || true
            cp -rf "$PASTA_EXTENSAO/server"/* "$TARGET/server/" 2>/dev/null || true
            cp -rf "$PASTA_EXTENSAO/assets"/* "$TARGET/assets/" 2>/dev/null || true
            cp -rf "$PASTA_EXTENSAO/snippets"/* "$TARGET/snippets/" 2>/dev/null || true
            cp -f "$PASTA_EXTENSAO/package.json" "$TARGET/" 2>/dev/null || true
            cp -f "$PASTA_EXTENSAO/language-configuration.json" "$TARGET/" 2>/dev/null || true
            [ -f "$PASTA_EXTENSAO/icon.png" ] && cp -f "$PASTA_EXTENSAO/icon.png" "$TARGET/" 2>/dev/null || true
            echo -e "\033[0;32m     Extensão sincronizada em $TARGET com sucesso!\033[0m"
        fi
    done
fi

echo -e "\n\033[0;32m==========================================================================\033[0m"
echo -e "\033[0;32m Pacote VSIX pronto para distribuição: $DESTINO_VSIX\033[0m"
echo -e "\033[0;32m==========================================================================\033[0m"
