# THZ-LANG — VS Code Extension (G1-G6)

Extensão alimentada pelo **Language Service Core (G1)** via LSP — `src/lsp/server.ts` (stdio).

## Funcionalidades

| Recurso | Detalhe |
|---|---|
| **Diagnósticos** | `[Linha L:C]` com caret → `Diagnostic` (léxico/sintático/semântico + lint `--estrito`) |
| **Hover** | Tipos/assinaturas de `ESTRUTURA`, `campo`, `ENUMERACAO`, `RESULTADO[T,E]`, `variável` e `item.q` |
| **Símbolos** | `DocumentSymbol` (programa, estruturas/campos/invariantes, enumerações, regras e operações) |
| **Go-to-Definition** | Mapeia `simbolosDe()` → `Location` |
| **Completion** | Keywords (`PALAVRAS_RESERVADAS`) e tipos (`DECIMAL`, `MONETARIO`, `FATIA`, `RESULTADO`…) |
| **Formatting** | `textDocument/formatting` via `src/fmt.ts` (Shift+Alt+F; descarta comentários `#`) |
| **Governança** | Comando `THZ: Mostrar Auditoria` → `thz/audit` (matriz `RASTREIO→Regra→Contrato`) |
| **THZ-IR / LLVM** | Comandos `THZ: Mostrar IR` (`thz/ir`, `thz-ir/1`) e `THZ: Mostrar LLVM` (`thz/llvm`) |

Gramática TextMate: `syntaxes/thz.tmLanguage.json`. Configurações: `thz-lang.lintEstrito` (ativa `--estrito` no `analisar()`) e `thz-lang.trace.server` (`off`/`messages`/`verbose`).

## Requisitos

- VS Code ≥ 1.85
- Engine compilado: `dist/lsp/server.js` (gerado por `npm run lsp:build` na raiz do engine)

## Dev

```bash
# 1) motor TS — servidor LSP (thz-lang-engine/)
npm install
npm run lsp:build          # compila src/lsp/server.ts → dist/lsp/server.js

# 2) extensão (esta pasta, thz-lsp-vscode/)
npm install
npm run compile            # compila src/extension.ts → dist/extension.js

# abrir a extensão como Extension Development Host (F5)
code --extensionDevelopmentPath=<raiz>/thz-lsp-vscode <raiz>/thz-lang-engine
# ou
cd thz-lang-engine && npm run playground   # alternativa web (Vite + Monaco)
```

Servidor em `stdio`: `npm run lsp` → `node dist/lsp/server.js --stdio`.

## Empacotamento

```bash
npm run extension:package  # → thz-lang.vsix (requer @vscode/vsce)
```

Para publicar fora do dev, garanta que `dist/lsp/server.js` está incluído no `vsix` (o `extension/package.json` resolve via `../dist/lsp/server.js`; se necessário: `Copy-Item dist/lsp/* extension/server -Force` antes de `vsce package`).

## Comandos

- `THZ: Mostrar Auditoria` (`thz.showAudit`)
- `THZ: Mostrar IR (thz-ir/1)` (`thz.showIr`)
- `THZ: Mostrar LLVM IR` (`thz.showLlvm`)
- Formatação nativa (menu **Formatar Documento**)

## Playground (G2) — referência cruzada

Os mesmos Language Service + Runtime do LSP alimentam `playground/` — botões `🛡️ Audit`, `🧩 IR`, `⚡ LLVM`, `✨ Fmt` e execução browser com `ArenaMemoria`.

## Licença

Protótipo — uso interno.
