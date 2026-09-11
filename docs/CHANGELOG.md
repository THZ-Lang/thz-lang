# Changelog — THZ-LANG

Todas as mudanças notáveis deste projeto são documentadas aqui. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e versionamento [SemVer 2.0.0](https://semver.org/lang/pt-BR/).

---

## [2.4.0] - 2026-08-25

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

