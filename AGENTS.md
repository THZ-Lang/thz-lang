# AGENTS.md — Diretrizes de Engenharia e Operação para Agentes de IA

Este documento define o contexto técnico, restrições arquiteturais e diretrizes de desenvolvimento para agentes de IA operando no repositório **THZ-LANG Engine**.

---

## 1. Visão Geral e Identidade do Projeto

* **Nome do Projeto:** THZ-LANG (`.thz`, `.thzui`)
* **Paradigma:** Linguagem Corporativa de Sistemas, Governança de Negócio (DDD), Arquitetura Viva e Processamento de Dados de Alta Performance.
* **Sintaxe:** Estruturada em língua portuguesa com tipagem estática e *Design by Contract*.

### Papéis do Ecossistema (Arquitetura Unificada Rota 1):
1. **Core & Tooling Principal (JVM):** Multi-módulo Java 25 (`JVM/thz-core-jvm`, `thz-cli-jvm`, `thz-gui-jvm`, `thz-lsp-jvm`, `thz-bench-jvm`, `thz-api-jvm`).
2. **Desktop IDE:** Interface gráfica moderna em Swing + FlatLaf com paridade universal em todas as plataformas (`JVM/thz-gui-jvm`).
3. **Runtime Nativo de Alta Performance:** Rust (`src/runtime_rs/` + `dist/native/`) com C ABI pura para SIMD, Arena de Memória, Criptografia e WebAssembly (WASM).
4. **Compilação AOT & Performance:** Backend LLVM Clang AOT (`scripts/build-llvm.ps1`) para gerar binários nativos de negócio linkando com o runtime Rust.
5. **VS Code Extension:** Extensão oficial isolada (`Extensions/thz-lsp-vscode`).

---

## 2. Invariantes Técnicos e Normas Obrigatórias

1. **Aritmética Financeira e Decimais (ISO/IEC 10967):**
   * É terminantemente proibido o uso de ponto flutuante IEEE 754 binário (`float` / `double` / `number`) para operações monetárias e fiscais.
   * Toda aritmética decimal utiliza inteiros escalados com `BigInt` (classe `DecimalFixo` no runtime TypeScript/Java) ou inteiros de 128 bits (`i128`) no codegen LLVM, com arredondamento bancário meio-par (*Half-Even*).

2. **Gerenciamento de Memória e Estrutura de Dados:**
   * Processamento em lote deve respeitar alocação em Arena (`ArenaMemoria` / `BlocoMemoria`), permitindo descarte de memória contígua em $O(1)$.
   * Estruturas com modificador `LAYOUT_COLUNAR` operam sob modelo *Structure of Arrays* (SoA) para viabilizar vetorização SIMD (AVX2/AVX-512).

3. **Arquitetura Viva e Contratos Formais (ISO/IEC/IEEE 42010 & ISO/IEC TR 24772):**
   * O bloco `METADADOS_ARQUITETURA` é obrigatório em programas corporativos.
   * Cláusulas `EXIGE` (pré-condições) e `GARANTE` (pós-condições) devem ser validadas em tempo de execução/compilação, gerando falhas explícitas em caso de violação.

---

## 3. Mapa de Estrutura do Projeto

* `JVM/thz-core-jvm/`: Núcleo da linguagem em Java 25 (Léxico, Sintático, AST, Semântico, Runtime de Arena, DecimalFixo, Interpretador, IR, Governança, DocGen, DAP).
* `JVM/thz-cli-jvm/`: Interface de linha de comando (`ThzCli`), REPL e servidor de desenvolvimento.
* `JVM/thz-gui-jvm/`: Desktop IDE completa em Swing + FlatLaf (`ThzGui`, Editor com syntax highlighting, gutter, toolbar, formulários visuais).
* `JVM/thz-lsp-jvm/`: Servidor de protocolo de linguagem (LSP) para IDEs.
* `JVM/thz-bench-jvm/`: Suíte de benchmarks JMH para micro-otimizações.
* `src/runtime_rs/`: Único runtime nativo oficial em Rust (Arena, SIMD, Crypto, ML, WASM, C ABI).
* `dist/native/`: Binários pré-compilados (.dll, .so, .wasm) do runtime nativo.
* `Extensions/thz-lsp-vscode/`: Extensão oficial do VS Code (TextMate Grammar + Language Client).
* `compilador/`: Compilador self-hosted escrito na própria linguagem THZ (`driver.thz`, `lexer.thz`, `parser.thz`, `codegen.thz`).
* `scripts/`: Scripts PowerShell de automação (`build-llvm.ps1`, `setup-rust.ps1`, `gui.ps1`, `test-all.ps1`).
* `exemplos/`: Programas canônicos de teste e demonstração (`faturamento.thz`, `pedidos.thz`, `rust_embutido.thz`, `streaming_eventos.thz`, `regra_wasm.thz`).

---

## 4. Regras de Conduta e Diretrizes de Engenharia

* **Branch para Self-Hosting e Autonomia LLVM:** Quando for trabalhar em tarefas relacionadas ao compilador self-hosted (`compilador/`), codegen LLVM, runtime C (`thz_runtime.c`) ou autonomia total (Zero JVM), **SEMPRE alterne para a branch `feat/self-hosting-llvm-autonomy`**.
* **Manutenção da Sintaxe Canônica:** Não altere a nomenclatura das palavras-chave em português (`PROGRAMA`, `METADADOS_ARQUITETURA`, `ESTRUTURA`, `REGRA_NEGOCIO`, `EXIGE`, `GARANTE`, `VETORIZAR_PARA`, `USAR_BLOCO_MEMORIA`).
* **Tratamento de Erros:** Todo erro sintático ou semântico deve reportar linha e coluna exatas no formato `[Erro Sintático][Linha L:C]`.
* **Idioma:** Toda documentação, mensagens e testes DEVEM estar em português do Brasil (PT-BR).
* **Semântica Dual-OS:** Mantenha compatibilidade estrita de caminhos de arquivos e comandos tanto no Windows quanto no Linux.


---

## 5. Conformidade Normativa 1 a 1

* **ISO/IEC 10967:** Proibição de float binário para decimais/moedas. Aritmética 100% exata via `DecimalFixo` com arredondamento bancário meio-par (*Half-Even*).
* **ISO 4217:** Validação rigorosa de códigos de moedas alfa-3 e proibição de operações monetárias diretas entre moedas distintas sem conversão explícita.
* **ISO/IEC/IEEE 42010:** Preservação obrigatória de metadados de arquitetura no nó `METADADOS_ARQUITETURA` da AST.
* **ISO/IEC TR 24772:** Mitigação de vulnerabilidades de linguagem via alocação contígua em arena (`USAR_BLOCO_MEMORIA`) com checagem rigorosa de limites.
* **RFC 4122 / RFC 8259 / SemVer 2.0.0:** Conformidade universal de UUID v4, JSON UTF-8 e versionamento semântico.

## 6. Diretrizes de Testes e Documentação
* Todos os arquivos de documentação DEVEM estar em português do Brasil (PT-BR).
* Todos os arquivos de código-fonte e testes DEVEM seguir as diretrizes do AGENTS.md.