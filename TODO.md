# TODO — THZ-LANG Roadmap & Backlog

> Estado revisado em 02/09/2026: o caminho oficial é JVM para compilação/interpretação e Rust para runtime/AOT/performance; `compilador/` é apenas experimento hospedado.

Este documento mantém o estado atualizado dos marcos alcançados e das metas futuras do ecossistema **THZ-LANG**.

---

## 🗺️ ROADMAP DE EVOLUÇÃO

- [x] **G1 — Language Service Core** — `src/language-service.ts` (analyze, hover, diagnósticos com caret)
- [x] **G2 — Playground Web** — `playground/` (Vite + Monaco + Monarch, execução browser)
- [x] **G3 — LSP + VS Code** — `src/lsp/server.ts` + `Extensions/thz-lsp-vscode/` (diagnósticos, hover, completion, definition, formatting)
- [x] **G4 — Governança Auditável** — `src/governanca.ts` + `thz audit` (CLI/LSP/Playground/Git integration)
- [x] **G5 — THZ-IR + SIMD Formal** — `src/ir.ts` (`thz-ir/1`) + `src/simd.ts` (R1-R5) + `thz ir --llvm`
- [x] **G6 — Benchmarks & Formatação Canônica** — `src/fmt.ts` (`thz fmt`) + `JVM/thz-bench-jvm` (JMH benchmarks)
- [x] **Motor JVM 25 Multi-Módulo** — Estrutura Gradle modular autônoma (`thz-core-jvm`, `thz-cli-jvm`, `thz-gui-jvm`, `thz-lsp-jvm`, `thz-bench-jvm`, `thz-api-jvm`)
- [x] **Desktop IDE Nativa Swing + FlatLaf** — Interface gráfica moderna com paridade visual 1:1, temas Dark/Light, realce em tempo real, gutter e formulários dinâmicos
- [x] **Compilação GraalVM Native Image** — Suporte a binários nativos de CLI (`thz.exe`) e Desktop GUI (`thz-desktop.exe`) com FlatLaf AOT Look & Feel
- [x] **Arquétipo de Big Data Pipelines** — `PIPELINE_DADOS` para ingestão/transformação em lote (*Batch*) e tempo real (*Streaming*) com conectores heterogêneos
- [x] **Servidor de Desenvolvimento Live Reload** — Comando `thz dev` para desenvolvimento com hot reload automático
- [x] **Integração de Governança com Git** — Comando `thz audit --git` para verificação de conformidade em diffs e commits
- [ ] **Experimento self-hosted hospedado** — `compilador/*.thz`; ainda contém stubs e não tem paridade com o parser JVM
- [ ] **Pipeline AOT experimental via LLVM Clang + runtime Rust** — gera artefatos nativos a partir do host JVM; bootstrap/self-hosting ainda não validado
- [x] **CI/CD Modernizado** — GitHub Actions workflow para Java 25, Gradle multi-módulo e compilação LLVM Clang AOT

---

## 🎯 PRÓXIMAS METAS (BACKLOG)

- [ ] **Fase 7 — Fatias Zero-Copy com Apache Arrow IPC & Rust/Inkwell Integration:**
  - Exportação e ingestão de tabelas de memória contíguas interoperáveis via Arrow Flight / Plasma.
  - Otimizações vetoriais adicionais para conjuntos de instruções AVX-512 e ARM Neon.
- [ ] **Expansão dos Conectores de Dados Nativos:**
  - Drivers nativos de conexão para Apache Kafka, Apache Spark e Delta Lake em `PIPELINE_DADOS`.
- [ ] **Gerador de Documentação Interativa em WebAssembly:**
  - Exportação de portais de documentação viva com simulador de código embutido via WASM.
