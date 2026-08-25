# Changelog — THZ-LANG

Todas as mudanças notáveis deste projeto são documentadas aqui. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e versionamento [SemVer 2.0.0](https://semver.org/lang/pt-BR/).

---

## [3.0.0] - 2026-08-25 (WebAssembly Target & Arquitetura Unificada)

### Adicionado
- **Manifesto de Configuração Centralizada (`thz.config.json` / `thz.json`):** Padronização de projeto, dialeto (`pt-BR`), drivers de banco e mensageria, IA e conformidade de governança, com comando `thz init` e auto-detecção no CLI.
- **Resolução e Pesquisa Inteligente Recursiva de Recursos (`ThzLocalizadorRecursos`):** Motor centralizado de busca progressiva e recursiva para arquivos-fonte `.thz`/`.thzui`, módulos (`IMPORTAR "..."`), manifestos de projeto e bancos SQLite com subida hierárquica à raiz e varredura profunda com descarte de diretórios ignorados.
- **Bridge Universal de Mensageria (`MENSAGERIA.*`):** Suporte transparente a RabbitMQ (AMQP/REST), Apache Kafka, AWS SQS, AWS SNS e Barramento Embutido (Virtual Threads) com auto-detecção de brokers locais e failover de zero latência.
- **Bridge Universal de Banco de Dados & ORM JPA-Like (`BANCO.*`):** Persistência automatizada (`BANCO.salvar`, `BANCO.buscarPorId`, `BANCO.removerPorId`, `BANCO.criarTabela`), Raw SQL de alta performance (`BANCO.consultar`, `BANCO.executar`, `BANCO.consultarValor`, `BANCO.iniciarTransacao`, `BANCO.confirmarTransacao`, `BANCO.cancelarTransacao`, `BANCO.executarScript`) e Busca Semântica Vetorial KNN (`BANCO.consultarVetorial`).
- **Target WebAssembly (WASM):** Módulo `src/runtime_rs/src/wasm.rs` e alvo `Alvo.WEBASSEMBLY` no `ThzCompilerDriver` para execução universal em navegadores e Edge Workers.
- **Rust Embutido (`BLOCO_NATIVO_RUST`):** Suporte sintático para blocos de código nativo Rust diretamente em arquivos `.thz`.
- **Toolchain Rust Portátil:** Scripts `scripts/setup-rust.ps1` e `setup-rust.sh` para provisionamento standalone em `.tools/rust`.
- **Exemplos Canônicos:** `exemplos/mensageria_conectores_hibridos.thz`, `exemplos/banco_jpa_orm_vetorial.thz`, `exemplos/banco_rawsql_avancado.thz`, `exemplos/regra_wasm.thz` e `exemplos/rust_embutido.thz`.
- **Documentação Formal:** `docs/CONECTORES_BANCO_E_MENSAGERIA.md`.

### Alterado
- **Consolidação Arquitetural:** Rust (`src/runtime_rs/`) estabelecido como o único runtime nativo oficial, aposentando código C legado (`src/runtime/`).
- **Isolamento de Node.js:** Node.js mantido exclusivamente dentro de `Extensions/thz-lsp-vscode/`.

---

## [2.9.0] - 2026-08-25 (Debugger Nativo DAP)

### Adicionado
- **Servidor DAP (Debug Adapter Protocol):** `ThzDapServer.java` e hook `ThzDebugListener.java` com suporte a breakpoints, Step Over, Continue, StackTrace e inspeção de variáveis no VS Code e IDE Desktop.
- **Suíte de Testes:** `ThzDapServerTest.java`.

---

## [2.8.0] - 2026-08-25 (Mensageria Reativa & EDA Async)

### Adicionado
- **Barramento Reativo de Eventos:** `ThzBarramentoEventos.java` com RingBuffer lock-free e despacho em Virtual Threads do Java 25.
- **Módulo `MENSAGERIA.*`:** Funções `MENSAGERIA.publicar`, `MENSAGERIA.consumir`, `MENSAGERIA.tamanhoFila`, `MENSAGERIA.limparTopico`.
- **Exemplo Canônico:** `exemplos/streaming_eventos.thz` e testes `MensageriaReativaTest.java`.

---

## [2.7.0] - 2026-08-25 (Consultas Tipadas LINQ / Query DSL)

### Adicionado
- **Sintaxe de Consulta Tipada:** Palavras-chave `CONSULTAR`, `DE`, `ONDE`, `ORDENAR_POR`, `LIMITE`, `PULAR`, `ASC`, `DESC`.
- **Funções de Fatias:** `FATIA.tamanho`, `FATIA.primeiro`, `FATIA.ultimo`, `FATIA.vazia`.
- **Exemplo Canônico:** `exemplos/consultas_linq.thz` e testes `ConsultaTipadaTest.java`.

---

## [2.6.0] - 2026-08-25 (IA & Machine Learning On-Device - Zero Python)

### Adicionado
- **Embeddings Determinísticos:** Extração semântica com hash FNV-1a 64-bit, tri-gramas morfológicos e normalização Euclidiana $L_2$ (`ThzIaEngine.java` / `ml.rs`).
- **ML Tabular:** Regressão linear e classificação logística sigmoide sem dependências de Python (`ThzMlEngine.java`).
- **Módulo `IA.*` e `ML.*`:** `IA.embedding`, `IA.similaridade`, `ML.classificar`, `ML.predizer`.
- **Exemplo Canônico:** `exemplos/ia_rag_local.thz` e testes `ThzIaMlTest.java`.

---

### Adicionado

- **Documentação extensa (esta release):**
  - `docs/ARQUITETURA_COMPILACAO_NATIVA.md` — tratado GraalVM/LLVM/IR/IL/AOT (7062w, 4 apêndices, JMH)
  - `docs/SELF_HOSTING.md` — pipeline `THZ→THZ-IR→LLVM IR`, bootstrap, paridade `ThzLexer.java`↔`lexer.thz`
  - `docs/RUNTIME_NATIVO.md` — ABI Dual-OS, `ThzArena`, `HeapAlloc` vs `malloc`, linking `clang→gcc`, `thz_webview2.c`
  - `docs/PIPELINE_DADOS.md` — `FONTE_ENTRADA`/`TRANSFORMACAO`/`DESTINO_SAIDA`, conectores, streaming Virtual Threads
  - `docs/TELA_THZUI.md` — DSL `.thzui`, `TELA.*`/`WEBVIEW.*`/`UI.*`, Swing vs WebView
  - `docs/DEPLOYMENT.md` — `jpackage`/`GraalVM`/`LLVM`, Docker `scratch`/`alpine`, `dist/` vs `target`, CI gates
  - `docs/GUIA_PERFORMANCE.md` — SoA/SIMD/Arena tuning, escolha `PASSO_SIMD`, JMH
  - `docs/TESTES_E_BENCHMARKS.md` — JUnit 5 112 testes, goldens, paridade TS↔JVM, `write-tests` skill
  - `docs/LSP_VSCODE.md` + `docs/API_REST.md` — `ThzLanguageServerImpl.java` + `ThzController.java` 11 endpoints
  - `docs/ADRs/` (5 ADRs: LLVM vs Cranelift, Arena vs GC, i128 vs double, GraalVM vs jpackage, Swing vs WebView)
  - `docs/TROUBLESHOOTING.md` — FAQ por área (build, SIMD, arena, PIPELINE, TELA, LSP, GraalVM, LLVM, Docker)
  - `EXEMPLOS_E_PADROES.md` 5→12 receitas (cobrindo `exemplos/*.thz` + `*.thzui` reais)
  - `INTELLIJ_SETUP.md` reescrito (JDK 25 + Gradle composite + TextMate + run configs)
- Cross-links em `README.md`, `apresentacao_tecnica.md`, `CLI_E_TOOLING.md` para novo corpus.

### Alterado

- `README.md:182` — Documentação Oficial com nova entrada `ARQUITETURA_COMPILACAO_NATIVA`
- `docs/EXEMPLOS_E_PADROES.md` — expandido com 7 receitas novas (`PIPELINE_DADOS`, `BIBLIOTECA`, arquivos, segurança, temporal, arena, `TESTE`)

---

## [2.3.3] - 2026-08-24

- `thz-core-jvm 2.3.3` — `DecimalFixo` half-even, `BlocoMemoria` O(1), `ValidadorSimd` R1-R5
- `thz-cli-jvm 2.3.3` / `thz-gui-jvm 2.3.3` — GraalVM `native-image` com FlatLaf, `jpackage` `dist/thz/`
- `thz-lsp-jvm` — LSP4J `ThzLanguageServerImpl` + `thz/*` custom
- `thz-bench-jvm` — JMH `DecimalBench`/`LayoutBench`/`BlocoMemoriaBench`
- `thz-api-jvm` — Spring Boot `ThzController` 11 endpoints

## [2.2.0] — 2026-08-10 (Self-Hosting & LLVM AOT)

- `compilador/*.thz` — `tokens.thz`/`ast.thz`/`lexer.thz`/`parser.thz`/`codegen.thz`/`driver.thz`
- `src/runtime/thz_runtime.c` Dual-OS + `scripts/build-llvm.ps1` cross PE/ELF
- `CompiladorSelfHostTest.java` 7 testes `AUDITORIA`/`EXECUCAO_JVM`/`LLVM`
- `PIPELINE_DADOS` arquétipo, `thz dev`, `thz audit --git`

## [2.1.0] — 2026-07-20 (Multi-módulo & Desktop IDE)

- Gradle composite `JVM/thz-*-jvm`, `thz-gui-jvm` Swing+FlatLaf (`ThzGui`, `EditorThz`, `Gutter`)
- `MANUAL_LINGUAGEM.md` v2.4, `GRAMATICA.md` EBNF, `CONFORMIDADE_E_NORMAS.md`

## [2.0.0] — 2026-06-01 (Fundação JVM 25)

- `ThzLexer`/`ThzParser`/`AnalisadorSemantico`/`InterpretadorThz`/`GeradorIr`/`ThzDocGen`, `exemplos/faturamento.thz`

---

> **Roadmap pós-2.4.0:** `TODO.md:29` — Arrow IPC Zero-Copy, AVX-512/Neon, Kafka/Spark/Delta Lake (`PIPELINE_DADOS`), WASM doc viva.

