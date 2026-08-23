# TODO — THZ-LANG Engine JVM

## Marcos Concluídos (Núcleo v2.3 & Tooling) ✅

* [x] **Fundações & Build Canônico:** Gradle 9.7 com Kotlin DSL (`build.gradle.kts`), Gradle Wrapper autônomo, Java 25 toolchain e JUnit 5.11.
* [x] **Fase 1 — Scanner Léxico:** `TokenType` (63 tokens canônicos), `PalavrasReservadas`, `ThzLexer` com rastreio de linha/coluna e descarte de BOM UTF-8.
* [x] **Fase 2 — AST & Parser:** Árvore com sealed records (`ProgramaAst`, `EstruturaAst`, `RegraNegocioAst`, `OperacaoAst`, `ProcedimentoAst`, `ExprAst`, `ComandoAst`) e `ThzParser` recursivo descendente.
* [x] **Fase 3 — Runtime Determinístico:** `DecimalFixo` (aritmética decimal bancária half-even ISO/IEC 10967 sem float), `Monetario` (ISO-4217), `DataThz/DataHoraThz` (algoritmos Hinnant) e `BlocoMemoria` (memória contígua efêmera $O(1)$).
* [x] **Fase 4 — Análise Semântica:** `AnalisadorSemantico`, tabela de tipos estáticos, resolução de escopos, verificação de contratos universais $\forall$ sobre `FATIA` e lint `--estrito`.
* [x] **Fase 5 — Interpretador & Stdlib:** `InterpretadorThz`, despacho polimórfico de nós da AST, validação dinâmica de contratos (`EXIGE`/`GARANTE`/`INVARIANTE`) e stdlib completa (`TEXTO`, `MATEMATICA`, `DATA`, `FATIA`, `DOCUMENTO`, `TELA`).
* [x] **Fase 6 — Tooling, CLI & REPL:** `Formatador` canônico idempotente, `JsonEscritor` determinístico, `ThzCli` (`check`, `run`, `fmt`, `ast`, `audit`, `doc`, `ir`, `repl`, `gui`) e `Repl` interativo multi-linha.
* [x] **Fase 7 — Suíte de Testes & Paridade:** **68/68 testes automatizados verdes** no JUnit 5 garantindo paridade total com o motor TypeScript de referência.
* [x] **G4 Governança & Rastreabilidade:** `AuditorGovernanca` gerando matriz `RASTREIO_REQUISITO → Regra → Contrato`, conferência de SLOs e relatórios Markdown/JSON (`thz audit` e botão na IDE).
* [x] **DocGen:** `ThzDocGen` gerando documentação arquitetural em Markdown com diagramas de classes e diagramas de fluxo Mermaid.js (`thz doc` e botão na IDE).
* [x] **G5 THZ-IR & Validação SIMD:** `GeradorIr` (`thz-ir/1`), emissor LLVM IR textual e `ValidadorSimd` com regras vetoriais R1 a R5 (`thz ir` e botão na IDE).
* [x] **IDE Desktop Swing (FlatLaf):**
  * Editor com realce léxico em tempo real, auto-indentação e sublinhado de erros com debounce de 300ms.
  * Gutter com numeração de linhas ancorada ao modelo de layout da View.
  * Temas profissionais Escuro e Claro com chaveamento dinâmico em tempo de execução.
  * Galeria de exemplos com varredura dinâmica e carregamento instantâneo.
  * Persistência de configurações desktop em JSON (`~/.thz/desktop-config.json`).
  * Diálogo modal de detecção automática, validação e configuração de JVMs locais (Scoop, SDKMAN, Program Files, JAVA_HOME).
* [x] **Motor de Formulários Visuais Declarativos (`TELA`):** Renderização automática de interfaces gráficas a partir de estruturas `ESTRUTURA`, mapeamento automático de widgets (texto, senha, seletor de arquivos, cores, sliders, spinners, combos, radios, tabelas dinâmicas para `FATIA[Estrutura]`, listas de seleção múltipla) e exportação de dados.
* [x] **Motor de Exportação de Documentos Corporativos (`DOCUMENTO`):** Emissão direta de relatórios institucionais em PDF (OpenPDF), planilhas Excel XLSX (Apache POI) e documentos Word DOCX (Apache POI).
* [x] **Idempotência Inteligente de Larga Escala:** Cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` com cache transacional LRU/TTL (`RegistroIdempotencia`).
* [x] **Empacotamento e Distribuição:**
  * Script `scripts/build-package.ps1` (`jpackage`) gerando pasta autônoma `dist/thz/` com executáveis `thz.exe` e `thz-gui.exe` com JRE 25 embutido.
  * Script `scripts/build-native.ps1` gerando binário estático único AOT `dist/bin/thz.exe` via GraalVM Native Image.

---

## Próximos Passos & Extensões Futuras

- [ ] **LSP Stdio:** Servidor Language Server Protocol sobre stdio para integração direta com VS Code, NeoVim e Helix.
- [ ] **Extensão VS Code para a JVM:** Conector para executar a CLI/LSP do motor JVM diretamente no VS Code.
- [ ] **Otimizações no Codegen LLVM:** Emissão de binários nativos AOT via backend LLVM Inkwell/Rust compartilhado.

