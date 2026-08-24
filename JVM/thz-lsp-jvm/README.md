# thz-lsp-jvm — LSP Server Java

Servidor de Protocolo de Linguagem (LSP) para THZ-LANG, escrito em Java usando LSP4J 0.21.1. Substitui o servidor LSP anterior em Node.js.

## Funcionalidades

| Feature | Status |
|---------|--------|
| `textDocument/didOpen` | ✅ Validação automática |
| `textDocument/didChange` | ✅ Validação incremental |
| `textDocument/didSave` | ✅ Re-validação |
| `textDocument/hover` | ✅ Tipo/assinatura via core |
| `textDocument/completion` | ✅ Keywords + tipos THZ |
| `textDocument/documentSymbol` | ✅ Outline completo |
| `textDocument/definition` | ✅ Go-to-definition |
| `textDocument/formatting` | ✅ Formatação canônica |
| `thz/audit` | ✅ Governança (custom) |
| `thz/ir` | ✅ IR generation (custom) |

## Compilação

```bash
cd JVM/thz-lsp-jvm
./gradlew shadowJar
```

Gera: `target/thz-lsp-2.3.0.jar`

## Execução

### Via stdio (para VS Code)
```bash
java -jar target/thz-lsp-2.3.0.jar --stdio
```

### Via npm
```bash
npm run lsp:jar   # compilar
npm run lsp:run   # executar
```

## Integração com VS Code

A extensão VS Code (`Extensions/thz-lsp-vscode`) foi configurada para usar este servidor Java. Ao instalar a extensão, ela executará automaticamente o `thz-lsp-2.3.0.jar`.

## Arquitetura

```
thz-lsp-jvm/
├── ThzLanguageServer.java     # Entry point (stdio)
├── ThzLanguageServerImpl.java # Core: analyze, hover, symbols, format
├── ThzTextDocumentService.java # Document lifecycle + LSP features
└── ThzWorkspaceService.java   # Workspace handlers
```

Depende de `thz-core-jvm` (núcleo do engine) via composite build do Gradle.
