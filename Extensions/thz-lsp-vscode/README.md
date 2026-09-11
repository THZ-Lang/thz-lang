# THZ-LANG — VS Code & Antigravity IDE Extension

Extensão oficial para **VS Code**, **Antigravity IDE** e IDEs compatíveis com TextMate / LSP, trazendo suporte completo a desenvolvimento para **THZ-LANG** (`.thz`, `.thzui`).

---

## 🌟 Funcionalidades

| Recurso | Descrição |
|---|---|
| **Realce Léxico Completo** | Gramática TextMate com suporte a `PROGRAMA`, `BIBLIOTECA`, `PIPELINE_DADOS`, `ESTRUTURA`, `REGRA_NEGOCIO`, `TELA`, tipos numéricos exatos, comentários `//`, `#` e `/* */` |
| **Diagnósticos em Tempo Real** | Validação sintática e semântica com precisão `[Linha L:C]` e modo `--estrito` |
| **Hover & Assinaturas** | Tipos e parâmetros de estruturas, campos, enums, contratos `EXIGE`/`GARANTE` e stdlib |
| **Autocompletion Contextual** | Palavras-chave, conectores de pipeline, tipos nativos e templates |
| **Navegação de Símbolos** | Outline hierárquico com `DocumentSymbol` e `Go-to-Definition` |
| **Indentação Assistida** | Quatro espaços, avanço automático após `:`, retorno correto em `senao`/`capture` e diagnóstico de tabs ou recuos que alterariam a árvore |
| **Formatação Canônica** | Formatação idempotente habilitada por padrão ao salvar, ao colar ou via `Format Document` (`Shift+Alt+F`) |
| **Governança & IR** | Comandos `THZ: Mostrar Auditoria de Governança`, `THZ: Mostrar IR` e `THZ: Mostrar LLVM IR` |

Quando houver indentação ambígua, o LSP destaca a linha e suspende a formatação
automática para não transformar um recuo acidental em uma árvore sintática
diferente. Depois de corrigido o diagnóstico, salvar o arquivo aplica novamente
a formatação canônica.

---

## 🚀 Como Gerar e Instalar o Pacote `.vsix`

O repositório inclui automação completa para empacotar a extensão junto ao servidor LSP Java 25:

### 1. Gerar o Pacote `.vsix`:
```powershell
# Via script dedicado:
powershell.exe -ExecutionPolicy Bypass -File scripts/build-vsix.ps1

# Ou via npm:
npm run vsix:build
```

O arquivo gerado é salvo em `dist/thz-lang-0.3.0.vsix`.

### 2. Gerar e Instalar Automaticamente:
```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/build-vsix.ps1 -Instalar
```

### 3. Instalação Manual no VS Code ou Antigravity IDE:
- **No Terminal:**
  ```bash
  code --install-extension dist/thz-lang-0.3.0.vsix
  ```
- **Na Interface Gráfica:**
  1. Abra o painel de **Extensions** (`Ctrl+Shift+X`).
  2. Clique no menu de três pontos (`...`) no canto superior do painel.
  3. Selecione **"Install from VSIX..."** e aponte para `dist/thz-lang-0.3.0.vsix`.

---

## 🛠️ Estrutura da Extensão

```
Extensions/thz-lsp-vscode/
├── syntaxes/
│   └── thz.tmLanguage.json       # Gramática TextMate (Realce sintático)
├── server/
│   └── thz-lsp-2.3.0.jar         # Servidor LSP Java 25 empacotado
├── src/
│   └── extension.ts              # Cliente LSP (LanguageClient)
├── dist/
│   └── extension.js              # Bundle compilado da extensão
├── language-configuration.json   # Pares de fechamento e comentários
└── package.json                  # Manifesto da extensão
```
