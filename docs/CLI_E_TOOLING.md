# Manual do CLI, Tooling & Suporte a IDEs — THZ-LANG (v0.4.0)

Este manual cobre a ferramenta unificada de linha de comando (`thz`), o servidor de desenvolvimento com *live reload* (`thz dev`), a Desktop IDE Swing FlatLaf (`thz gui`), o Agente Autônomo de IA (`thz agent`), o Protocolo Oficial de Liberação (`thz release`), a auditoria de arquitetura viva (`thz audit`), o Servidor de Linguagem (LSP), o depurador nativo DAP, a extensão para VS Code e a compilação nativa AOT Dual-OS.

---

## 🛠️ 1. Ferramenta de Linha de Comando (`thz`)

A CLI do THZ-LANG é distribuída através do subprojeto `JVM/thz-cli-jvm` e também em binários nativos autônomos gerados via GraalVM Native Image (`dist/bin/thz.exe`). Ela aceita subcomandos especializados para análise, execução, homologação, assistência de IA, formatação, auditoria e compilação.

### Sintaxe Geral:
```bash
thz <subcomando> [arquivo.thz|arquivo.thzui] [opções]
```

### 📋 Tabela de Subcomandos Oficiais (17 Comandos)

| Subcomando | Descrição | Opções Principais | Exemplo de Uso |
| :--- | :--- | :--- | :--- |
| **`check`** | Análise léxica, sintática, semântica e verificação de contratos | `--estrito` | `thz check pedido.thz --estrito` |
| **`run`** | Execução tree-walking de programas canônicos ou modernos (`main`) | `--arg k=v` | `thz run faturamento.thz` |
| **`dev`** | Servidor de desenvolvimento com recarga automática (*Live Reload*) | `--porta <num>` | `thz dev faturamento.thz` |
| **`audit`** | Matriz de governança e rastreabilidade de requisitos funcionais | `--git`, `--json`, `--saida` | `thz audit pedido.thz --git` |
| **`release`** / **`liberar`** | Protocolo Oficial de Liberação com homologação e assinatura SHA-256 | `--dados <arq>`, `--laudo` | `thz release faturamento.thz --dados homolog.json` |
| **`agent`** | Inicia o Agente Autônomo de Codificação e RAG em terminal | `--modelo`, `--api`, `--yes` | `thz agent --modelo llama3` |
| **`init`** | Inicializa novo projeto com manifesto canônico `thz.config.json` | — | `thz init meu_projeto` |
| **`compile`** | Compilação nativa AOT de um programa `.thz` via LLVM Clang | `--alvo`, `--saida` | `thz compile app.thz` |
| **`compile-all`** | Compilação AOT de todos os exemplos e programas do workspace | — | `thz compile-all` |
| **`fmt`** | Formatador canônico idempotente de código-fonte | `--check`, `--escrever` | `thz fmt --escrever pedido.thz` |
| **`doc`** | Gerador de documentação viva em Markdown + Mermaid | `--saida <dir>` | `thz doc faturamento.thz --saida docs/` |
| **`ui`** | Renderizador e gerador de UIs declarativas (`.thzui`) em HTML5 | `--html` | `thz ui dashboard.thzui --html` |
| **`ir`** | Emissão de THZ-IR (`thz-ir/1`) e LLVM IR estático | `--llvm`, `--saida` | `thz ir programa.thz --llvm` |
| **`ast`** | Exportação da Árvore Sintática Abstrata em JSON estruturado | — | `thz ast programa.thz` |
| **`livro`** / **`manual`** | Compilação de todos os manuais técnicos em PDF unificado | `--saida <pdf>` | `thz livro --saida dist/MANUAL.pdf` |
| **`repl`** | Shell interativo multi-linha (.ajuda, .codigo, .limpar, .sair) | — | `thz repl` |
| **`gui`** | Inicia a Desktop IDE nativa Swing + FlatLaf | — | `thz gui` |

---

## 🚀 2. Uso Detalhado dos Principais Subcomandos

### 2.1 Verificação Semântica & Lint (`thz check`)
Valida tipagem, contratos `EXIGE`/`GARANTE`, invariantes e conformidade normativa:
```bash
# Análise padrão
thz check exemplos/faturamento.thz

# Modo estrito (exige metadados de arquitetura, rastreio de requisitos e conformidade)
thz check exemplos/faturamento.thz --estrito
```

### 2.2 Protocolo de Liberação para Produção (`thz release` / `thz liberar`)
Executa o protocolo formal de homologação de 4 etapas (P01..P04):
1. **P01:** Validação sintática e semântica com barreira estrita.
2. **P02 & P03:** Ingestão de dados reais de homologação e execução dos testes positivos e negativos.
3. **P04:** Vinculação criptográfica SHA-256 de integridade e emissão de laudo auditável.
```bash
thz release exemplos/faturamento.thz --dados dados/homologacao.json --laudo dist/laudo_liberacao.txt
```

### 2.3 Agente Autônomo de IA em Terminal (`thz agent`)
Inicia o assistente de desenvolvimento orientado a objetivos no terminal:
```bash
# Modo interativo padrão:
thz agent

# Com auto-aprovação de comandos:
thz agent --yes

# Conectando com endpoint compatível com OpenAI:
thz agent --api https://api.openai.com/v1 --api-key sk-... --modelo gpt-4o
```

### 2.4 Servidor de Desenvolvimento com Live Reload (`thz dev`)
Monitora o arquivo-fonte e revalida/reexecuta instantaneamente a cada alteração salva:
```bash
thz dev exemplos/faturamento.thz
```

### 2.5 Auditoria de Governança Integrada com Git (`thz audit`)
Gera a matriz de rastreabilidade entre requisitos funcionais, regras de negócio e cláusulas contratuais:
```bash
# Auditoria contextual com base nas modificações do Git:
thz audit exemplos/pedidos.thz --git

# Exportar relatório estruturado em JSON para pipelines de CI:
thz audit exemplos/pedidos.thz --json --saida relatorio_auditoria.json
```

### 2.6 Formatador Canônico (`thz fmt`)
Aplica a formatação canônica da linguagem:
```bash
thz fmt exemplos/faturamento.thz --escrever
```

---

## 🖥️ 3. Desktop IDE — Swing + FlatLaf (`thz gui`)

O módulo `JVM/thz-gui-jvm` provê uma Desktop IDE industrial completa:

- **Editor com Realce em Tempo Real (`EditorThz`):** Suporte à sintaxe canônica e dual moderna, numeração de linhas ancorada (`Gutter`) e marcadores visuais de erro.
- **Barra de Ferramentas Integrada:** Ações rápidas para Análise (`check`), Execução (`run`), Formatação (`fmt`), Auditoria (`audit`), Documentação (`doc`) e Emissão de IR (`ir`).
- **Formulários Dinâmicos (`RenderizadorFormularioSwing`):** Renderização reativa de interfaces a partir de definições `ESTRUTURA` e validações de contrato.
- **Look & Feel Nativo Universal:** FlatLaf Dark/Light com consistência universal no Windows, Linux e macOS.

Para iniciar a IDE:
```bash
./gradlew gui
# ou via CLI:
thz gui
```

---

## 🔌 4. Extensão VS Code & Servidor LSP

O ecossistema disponibiliza suporte oficial ao Language Server Protocol (LSP) e depuração nativa (DAP) via `JVM/thz-lsp-jvm` e a extensão [`Extensions/thz-lsp-vscode`](../Extensions/thz-lsp-vscode):

- **Syntax Highlighting & TextMate:** Suporte completo para `.thz` e `.thzui`.
- **Diagnósticos em Tempo Real:** Sublinhado de erros com posição cirúrgica `[Linha L:C]`.
- **Depurador Nativo (DAP):** Breakpoints, step over/into, call stack e inspeção de variáveis.
- **Hover & Auto-complete:** Assinaturas de tipos, estruturas, enumerações e funções da stdlib.
- **Go-to-Definition & Document Symbols:** Navegação direta no workspace.

---

## ⚡ 5. Compilação Nativa AOT (Zero JVM em Produção)

O THZ-LANG disponibiliza pipelines de compilação nativa Ahead-Of-Time (AOT):

### 5.1 Compilação AOT via LLVM Clang & Runtime Rust (`scripts/build-llvm.ps1` / `.sh`)
Compila programas `.thz` diretamente em binários executáveis de código de máquina nativo (.exe no Windows e .elf no Linux) utilizando LLVM Clang, MinGW GCC e linkando estaticamente com o runtime oficial em Rust (`src/runtime_rs/`):

```bash
# No Windows (PowerShell):
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz exemplos/gestao_pedidos_moderno.thz

# No Linux (Bash):
./scripts/build-llvm.sh exemplos/gestao_pedidos_moderno.thz
```

**Artefatos Gerados em `dist/bin/`:**
- `dist/bin/gestao_pedidos_moderno.exe` (Executável PE nativo para Windows)
- `dist/bin/gestao_pedidos_moderno.elf` (Executável ELF nativo para Linux)

### 5.2 Compilação AOT da Tooling via GraalVM Native Image
Compila a CLI e a Desktop GUI em binários nativos de inicialização instantânea:

```powershell
# Compilar a CLI nativa (thz.exe):
powershell.exe -ExecutionPolicy Bypass -File JVM/thz-cli-jvm/scripts/build-native.ps1 -PularTestes

# Compilar a Desktop GUI nativa (thz-desktop.exe):
./gradlew :thz-gui-jvm:nativeCompile
```

---

## ☕ 6. Atalhos do Gradle e Scripts Multiplataforma

Tarefas de alto nível disponíveis na raiz:

```bash
./gradlew test         # Executa toda a suíte de testes JUnit 5
./gradlew cli          # Executa a CLI interativa
./gradlew gui          # Inicia a Desktop IDE Swing FlatLaf
./gradlew jmh          # Executa a suíte de benchmarks JMH
./gradlew livro        # Compila os manuais em PDF unificado
```

Suíte de scripts paralelos disponíveis em `scripts/`:
- `test-all.sh` / `test-all.ps1`: Executa todos os testes do monorepo
- `build-all.sh` / `build-all.ps1`: Compilação completa do ecossistema
- `dev.sh` / `dev.ps1`: Inicialização do ambiente de desenvolvimento
- `build-vsix.sh` / `build-vsix.ps1`: Empacotamento da extensão VS Code
- `sync-version.sh` / `sync-version.ps1`: Sincronização global da versão SemVer 2.0.0
