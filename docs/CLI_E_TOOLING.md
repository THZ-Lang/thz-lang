# Manual do CLI, Tooling & Suporte a IDEs — THZ-LANG (v2.4.0)

Este manual cobre a ferramenta de linha de comando (`thz`), o Servidor de Linguagem (LSP), a Extensão para VS Code e a integração com IDEs no ecossistema THZ-LANG.

---

## 🛠️ 1. Ferramenta de Linha de Comando (`thz`)

A CLI do THZ-LANG é distribuída através do subprojeto `thz-cli-jvm`. Ela aceita subcomandos para análise, formatação, execução, auditoria e renderização gráfica.

### Sintaxe Geral:
```bash
thz <subcomando> <arquivo.thz|arquivo.thzui> [opções]
```

### 📋 Subcomandos Suportados

| Subcomando | Descrição | Opções Principais | Exemplo |
| :--- | :--- | :--- | :--- |
| **`check`** | Análise sintática e verificação semântica de tipos | `--estrito` | `thz check pedido.thz --estrito` |
| **`run`** | Execução tree-walking do programa | — | `thz run faturamento.thz` |
| **`fmt`** | Formatador de código canônico e idempotente | `--check`, `--escrever`, `--saida` | `thz fmt --escrever pedido.thz` |
| **`doc`** | Gerador de documentação viva em Markdown + Mermaid | `--saida <dir>` | `thz doc faturamento.thz --saida docs/` |
| **`audit`** | Auditoria de governança e rastreabilidade | `--json`, `--saida`, `--estrito` | `thz audit pedido.thz --estrito` |
| **`ui`** | Renderizador e gerador de UIs declarativas (`.thzui`) | `--html` | `thz ui dashboard.thzui --html` |
| **`ir`** | Emissão de THZ-IR / LLVM IR estático | `--llvm`, `--saida` | `thz ir programa.thz --llvm` |
| **`ast`** | Exportação da Árvore Sintática Abstrata em JSON | — | `thz ast programa.thz` |
| **`repl`** | Shell interativo multi-linha | — | `thz repl` |

---

## 🚀 2. Uso Detalhado dos Subcomandos CLI

### 2.1 Verification & Lint (`thz check`)
Valida tipagem, escopo de variáveis e contratos.
```bash
# Análise padrão
thz check exemplos/faturamento.thz

# Modo estrito (exige metadados de arquitetura, rastreio e SLO)
thz check exemplos/faturamento.thz --estrito
```

### 2.2 Formatador Canônico (`thz fmt`)
Garante que o código siga 100% o padrão visual da linguagem.
```bash
# Verificar se o arquivo necessita de formatação
thz fmt exemplos/faturamento.thz --check

# Formatar e reescrever o arquivo no local
thz fmt exemplos/faturamento.thz --escrever
```

### 2.3 Renderização Gráfica UI (`thz ui`)
Exporta arquivos `.thzui` para HTML5 semântico com Glassmorphism CSS + JavaScript Bridge.
```bash
thz ui exemplos/faturamento_dashboard.thzui --html > dashboard.html
```

### 2.4 Auditoria de Governança (`thz audit`)
Gera matriz de rastreabilidade entre requisitos, regras de negócio e contratos `EXIGE`/`GARANTE`.
```bash
# Exibir relatório em Markdown
thz audit exemplos/pedidos.thz

# Exportar relatório estruturado em JSON
thz audit exemplos/pedidos.thz --json --saida relatorio.json
```

---

## 🔌 3. Extensão VS Code & Servidor LSP

O repositório inclui suporte oficial de linguagem para o Visual Studio Code via protocolo LSP (*Language Server Protocol*).

### Recursos Disponíveis na Extensão:
- **Syntax Highlighting:** Suporte a arquivos `.thz` e `.thzui` via TextMate Grammar.
- **Diagnósticos em Tempo Real:** Sublinhado vermelho/amarelo para erros sintáticos/semânticos.
- **Auto-completar & Hover:** Sugestões contextuais e exibição de assinaturas de procedimentos ao passar o mouse.
- **Formatação ao Salvar (Ctrl+S):** Integração com `thz fmt`.
- **Comandos customizados:** `THZ: Mostrar Auditoria`, `THZ: Gerar IR/LLVM`.

### Instalação da Extensão VS Code:
```bash
cd Extensions/thz-lsp-vscode
npm install
npm run extension:compile
```

---

## ☕ 4. Execução e Build via Gradle Monorepo

No ambiente de desenvolvimento, você pode utilizar os wrappers Gradle da raiz:

```bash
# Executar todos os testes do monorepo JVM
./gradlew test

# Executar a CLI via Gradle
./gradlew :thz-cli-jvm:run --args="check exemplos/faturamento.thz"

# Iniciar o servidor LSP via stdio
./gradlew :thz-lsp-jvm:run
```

---

## ⚡ 5. Compilação Nativa AOT (Zero JVM Runtime)

O THZ-LANG disponibiliza automação para compilação AOT nativa de programas `.thz` diretamente para binários executáveis `.exe` (PE no Windows / ELF no Linux):

### 5.1 Compilação Nativa via LLVM IR + Clang Dual-OS (`scripts/build-llvm.ps1`)
Compila qualquer arquivo `.thz` gerando **automaticamente ambos os binários nativos** (Windows `.exe` e Linux `.elf`) em um único comando usando LLVM Clang + MinGW GCC + Runtime C Dual-OS (`src/runtime/thz_runtime.c`):

```powershell
# Gera automaticamente AMBOS os binários (.exe para Windows e .elf para Linux):
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz JVM/thz-core-jvm/exemplos/compilador/driver.thz
```

**Resultados Gerados Simultaneamente em `dist/bin/`:**
- **Windows Executável:** `dist/bin/driver.exe`
- **Linux Executável:** `dist/bin/driver.elf`

### 5.2 Compilação Nativa via GraalVM Native Image (`JVM/thz-cli-jvm/scripts/build-native.ps1`)
Compila a CLI inteira em um binário nativo estático pré-compilado:

```powershell
powershell.exe -ExecutionPolicy Bypass -File JVM/thz-cli-jvm/scripts/build-native.ps1 -PularTestes
```

> **Nota de Toolchain:** A compilação nativa AOT do THZ-LANG via LLVM utiliza 100% **LLVM Clang + MinGW-w64** (instaláveis via Scoop), eliminando completamente a necessidade de instalar pacotes pesados de Visual Studio MSVC.
