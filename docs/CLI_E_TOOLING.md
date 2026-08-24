# Manual do CLI, Tooling & Suporte a IDEs — THZ-LANG (v2.4.0)

Este manual cobre a ferramenta de linha de comando (`thz`), o servidor de desenvolvimento com *live reload* (`thz dev`), a IDE Desktop (`thz gui` / `THZ-STUDIO`), a auditoria integrada com Git (`thz audit --git`), o Servidor de Linguagem (LSP), a extensão para VS Code e a compilação nativa AOT.

---

## 🛠️ 1. Ferramenta de Linha de Comando (`thz`)

A CLI do THZ-LANG é distribuída através do subprojeto `thz-cli-jvm` e também em binários nativos autônomos gerados via GraalVM Native Image (`thz.exe`). Ela aceita subcomandos para análise, execução, desenvolvimento, formatação, auditoria e renderização.

### Sintaxe Geral:
```bash
thz <subcomando> [arquivo.thz|arquivo.thzui] [opções]
```

### 📋 Tabela de Subcomandos

| Subcomando | Descrição | Opções Principais | Exemplo de Uso |
| :--- | :--- | :--- | :--- |
| **`check`** | Análise léxica, sintática e semântica com verificação de tipos | `--estrito` | `thz check pedido.thz --estrito` |
| **`run`** | Execução tree-walking do programa principal | — | `thz run faturamento.thz` |
| **`dev`** | Servidor de desenvolvimento com recarga automática (*Live Reload*) | `--porta <num>` | `thz dev faturamento.thz` |
| **`audit`** | Matriz de governança e rastreabilidade de requisitos | `--git`, `--json`, `--saida`, `--estrito` | `thz audit pedido.thz --git` |
| **`fmt`** | Formatador canônico idempotente de código-fonte | `--check`, `--escrever`, `--saida` | `thz fmt --escrever pedido.thz` |
| **`doc`** | Gerador de documentação viva em Markdown + Mermaid | `--saida <dir>` | `thz doc faturamento.thz --saida docs/` |
| **`ui`** | Renderizador e gerador de UIs declarativas (`.thzui`) | `--html` | `thz ui dashboard.thzui --html` |
| **`gui`** | Inicia a Desktop IDE nativa Swing + FlatLaf | — | `thz gui` |
| **`ir`** | Emissão de THZ-IR (`thz-ir/1`) e LLVM IR estático | `--llvm`, `--saida` | `thz ir programa.thz --llvm` |
| **`ast`** | Exportação da Árvore Sintática Abstrata em JSON | — | `thz ast programa.thz` |
| **`repl`** | Shell interativo multi-linha | — | `thz repl` |

---

## 🚀 2. Uso Detalhado dos Subcomandos CLI

### 2.1 Verificação Semântica & Lint (`thz check`)
Valida tipagem, escopo de variáveis, contratos `EXIGE`/`GARANTE` e invariantes.
```bash
# Análise padrão
thz check exemplos/faturamento.thz

# Modo estrito (exige metadados de arquitetura, rastreio de requisitos e SLO)
thz check exemplos/faturamento.thz --estrito
```

### 2.2 Servidor de Desenvolvimento com Live Reload (`thz dev`)
Monitora o arquivo-fonte e reexecuta/revalida instantaneamente a cada alteração salva:
```bash
thz dev exemplos/faturamento.thz
```

### 2.3 Auditoria de Governança Integrada com Git (`thz audit`)
Gera a matriz de rastreabilidade entre requisitos funcionais, regras de negócio e cláusulas contratuais. A flag `--git` audita os arquivos modificados no stage ou no último commit:
```bash
# Exibir relatório textual de conformidade
thz audit exemplos/pedidos.thz

# Auditoria contextual com base no histórico/diff do Git
thz audit exemplos/pedidos.thz --git

# Exportar relatório estruturado em JSON para pipelines de CI
thz audit exemplos/pedidos.thz --json --saida relatorio_auditoria.json
```

### 2.4 Formatador Canônico (`thz fmt`)
Aplica a formatação padrão da linguagem com indentação de 4 espaços, palavras-chave em caixa alta e quebras estruturadas:
```bash
# Verificar conformidade de formatação sem alterar o arquivo
thz fmt exemplos/faturamento.thz --check

# Formatar e sobrescrever o arquivo original
thz fmt exemplos/faturamento.thz --escrever
```

### 2.5 Renderização Gráfica UI (`thz ui`)
Exporta arquivos `.thzui` para HTML5 semântico com tema Glassmorphism moderno e ponte JavaScript (`window.thz`):
```bash
thz ui exemplos/faturamento_dashboard.thzui --html > dashboard.html
```

---

## 🖥️ 3. Desktop IDE — Swing + FlatLaf (`thz gui` / `THZ-STUDIO`)

O módulo `JVM/thz-gui-jvm` provê uma IDE desktop profissional:

- **Editor com Realce em Tempo Real (`EditorThz`):** Destaque de sintaxe léxica, numeração de linhas ancorada (`Gutter`) e marcadores visuais de erro.
- **Barra de Ferramentas Integrada (`BarraFerramentasGui`):** Botões de ação rápida para Análise (`check`), Execução (`run`), Formatação (`fmt`), Auditoria (`audit`), Documentação (`doc`) e Emissão de IR (`ir`).
- **Formulários Dinâmicos (`RenderizadorFormularioSwing`):** Geração reativa de interfaces a partir de definições `ESTRUTURA` e validações de contrato.
- **Look & Feel Nativo Universal:** FlatLaf Dark/Light com consistência 1:1 no Windows, Linux e macOS.

Para iniciar a IDE:
```bash
# Via Gradle:
./gradlew gui

# Via CLI:
thz gui
```

---

## 🔌 4. Extensão VS Code & Servidor LSP

O ecossistema disponibiliza suporte oficial ao Language Server Protocol (LSP) via `JVM/thz-lsp-jvm`:

### Recursos do LSP:
- **Syntax Highlighting:** Suporte TextMate para `.thz` e `.thzui`.
- **Diagnósticos em Tempo Real:** Sublinhado de erros léxicos, sintáticos e de tipos com posição precisa `[Linha L:C]`.
- **Hover & Auto-complete:** Assinaturas de tipos, estruturas, enumerações e funções da stdlib.
- **Go-to-Definition & Document Symbols:** Navegação direta para definições de regras, estruturas e procedimentos.
- **Formatação ao Salvar (Ctrl+S):** Integração transparente com `thz fmt`.

### Instalação da Extensão VS Code:
```bash
cd Extensions/thz-lsp-vscode
npm install
npm run compile
```

---

## ⚡ 5. Compilação Nativa AOT (Zero JVM Runtime)

O THZ-LANG disponibiliza dois fluxos de compilação nativa Ahead-Of-Time (AOT):

### 5.1 Compilação AOT via LLVM Clang Dual-OS (`scripts/build-llvm.ps1`)
Compila qualquer programa `.thz` diretamente em binários de código de máquina nativo (.exe no Windows e .elf no Linux) utilizando LLVM Clang, MinGW GCC e o Runtime C Dual-OS (`src/runtime/thz_runtime.c`):

```powershell
# Compilar qualquer fonte .thz em binários nativos autônomos:
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz compilador/driver.thz
```

**Artefatos Gerados em `dist/bin/`:**
- `dist/bin/driver.exe` (Executável PE nativo para Windows)
- `dist/bin/driver.elf` (Executável ELF nativo para Linux)

### 5.2 Compilação AOT da Tooling via GraalVM Native Image
Compila a CLI e a Desktop GUI em binários nativos de inicialização instantânea (< 5ms):

```powershell
# Compilar a CLI nativa (thz.exe):
powershell.exe -ExecutionPolicy Bypass -File JVM/thz-cli-jvm/scripts/build-native.ps1 -PularTestes

# Compilar a Desktop GUI nativa (thz-desktop.exe):
./gradlew :thz-gui-jvm:nativeCompile
```

---

## ☕ 6. Atalhos do Gradle Monorepo

Tarefas de alto nível disponíveis a partir da raiz:

```bash
./gradlew test         # Executa toda a suíte de testes JUnit 5
./gradlew cli          # Executa a CLI interativa
./gradlew gui          # Inicia a Desktop IDE Swing FlatLaf
./gradlew jmh          # Executa a suíte de benchmarks JMH
./gradlew shadowJar    # Gera os pacotes executáveis UberJAR
```
