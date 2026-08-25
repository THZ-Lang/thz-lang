# Changelog — THZ-LANG

Todas as mudanças notáveis deste projeto são documentadas aqui. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e versionamento [SemVer 2.0.0](https://semver.org/lang/pt-BR/).

---

## 1.0.0 (2026-08-25)


### ⚠ BREAKING CHANGES

* consolidate JVM engine back into monorepo (thz-lang-jvm)

### Features

* add architecture documentation generator, expanded test suite, and additional domain-specific language examples ([75f6f78](https://github.com/THZ-Lang/thz-lang/commit/75f6f78b761fb0bc95928a349256326babed6523))
* add workspace skills for testing, language design and SIMD benchmarks ([e68b23e](https://github.com/THZ-Lang/thz-lang/commit/e68b23e035cf3105fcc67a1fad51eddb129fb1c3))
* adding vscode extension vsix, updating readme and more docs. ([5829ca0](https://github.com/THZ-Lang/thz-lang/commit/5829ca0859e8c9aad46d6c7dec5ddbf0beb85561))
* **core:** implement AST optimizer, panic-mode parser recovery, LSP references/rename, and reactive UI data-binding ([3b72f8a](https://github.com/THZ-Lang/thz-lang/commit/3b72f8a2127d9930a4f0ad757cfc57b8753eae5f))
* GraalVM Native with Look And Feel working. ([27a0e21](https://github.com/THZ-Lang/thz-lang/commit/27a0e2142b09e4299b79f972fdb2ad61f8db6235))
* implement core engine components, runtime memory management, SIMD validation, and GUI configuration utilities ([cb45cce](https://github.com/THZ-Lang/thz-lang/commit/cb45ccea4714729bfb0958eb8af6d95e2c4f3587))
* implement core language infrastructure, including compiler toolchain, language server, GUI editor, and standard library components ([10210b1](https://github.com/THZ-Lang/thz-lang/commit/10210b1a64cd4fe0f67f3b1c750dd59b818d74c8))
* implement JVM engine with GUI support, document generation, and example showcase ([eef298e](https://github.com/THZ-Lang/thz-lang/commit/eef298e504a535f1c5c6b227ad247d5ecb83a847))
* implement modular Swing-based IDE with interpreter integration for THZ-LANG ([583bb25](https://github.com/THZ-Lang/thz-lang/commit/583bb25479705a3f9b207d130b637a80ea9b573a))
* implement VS Code LSP extension and add CLI/GUI support for THZ-LANG. Minor improves. ([fa98830](https://github.com/THZ-Lang/thz-lang/commit/fa98830eaa7b334f22686313a895c3f76cd54863))
* initial commit of THZ-LANG ecosystem (TypeScript & Java 25 engines) ([4be0693](https://github.com/THZ-Lang/thz-lang/commit/4be0693a95d2244fcd90c3ad67a9ac18abb745c7))
* initialize thz-gui-jvm build configuration and add application.yml for thz-api-jvm ([93dbdce](https://github.com/THZ-Lang/thz-lang/commit/93dbdcee9a8e9a9a31aa0bc8beb9c0ac1d02f716))
* Introduce comprehensive documentation and enhance project structure for THZ-LANG ([12f475c](https://github.com/THZ-Lang/thz-lang/commit/12f475c3c1ff6235097414242191532d8398efd0))
* **jvm:** add module archetypes, import clauses, result pattern and jvm core tooling ([6f5cca8](https://github.com/THZ-Lang/thz-lang/commit/6f5cca8870d63a045440c00006ab8a2aa9e972b3))
* marco alcançado : thz-lang independente (não precisa de outros kits) ; compilação autonoma alcançada. ([22c9aef](https://github.com/THZ-Lang/thz-lang/commit/22c9aefe0051cf2617db383ba1d356fc8c2175ad))
* **pipeline:** add PIPELINE_DADOS archetype and Big Data streaming/batch ingestion engine ([9116fde](https://github.com/THZ-Lang/thz-lang/commit/9116fde80acbf48d4cacd53b2d99d5d75ce20475))
* **tooling:** add THZ-STUDIO ide, thz dev server with live reload, and thz audit --git governance integration ([f19c241](https://github.com/THZ-Lang/thz-lang/commit/f19c241b11867027fb805e61faba6af98564df10))


### Bug Fixes

* verification pass after workspace regrouping ([b4afe0a](https://github.com/THZ-Lang/thz-lang/commit/b4afe0a27c155e72933876c8aba6fb5c2a0e3bcf))


### Code Refactoring

* consolidate JVM engine back into monorepo (thz-lang-jvm) ([096a6fb](https://github.com/THZ-Lang/thz-lang/commit/096a6fb02261bc0de951c270b70d65e591e7d87e))

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
