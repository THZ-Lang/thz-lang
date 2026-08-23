# TODO
Deve sempre estar atualizado. 

## ROADMAP

- [x] **G1 — Language Service Core** — `src/language-service.ts` (analyze, hover, diagnósticos com caret)
- [x] **G2 — Playground Web** — `playground/` (Vite + Monaco + Monarch, execução browser)
- [x] **G3 — LSP + VS Code** — `src/lsp/server.ts` + `extension/` (diagnostics/hover/completion/definition/formatting)
- [x] **G4 — Governança Auditável** — `src/governanca.ts` + `thz audit` (CLI/LSP/Playground)
- [x] **G5 — THZ-IR + SIMD Formal** — `src/ir.ts` (`thz-ir/1`) + `src/simd.ts` (R1-R5) + `thz ir --llvm`
- [x] **G6 — Bench + fmt** — `src/fmt.ts` (`thz fmt`) + `bench/` (decimal/fatia/SIMD, `npm run bench`) — 149 testes verdes
- [ ] **Fase 7 — Fatias Zero-Copy (Arrow IPC) + Rust/Inkwell/LLVM 17+**
- [x] **IDE própria** — Playground web (Monaco + Language Service + runtime browser + abas multi-arquivo + preview) e Desktop Swing IDE (`thz-lang-engine-JVM`)
- [x] **Colorização multi-IDE** — Gramática TextMate v2.3 unificada (VS Code, Antigravity, IntelliJ IDEA bundle)
- [x] **Port THZ-LANG JVM** — Motor completo em Java 25 (Léxico, Parser, Runtime, Semântico, Interpretador, CLI, Formato e Desktop Swing IDE com 29 testes JUnit 5 verdes)
- [x] **Separar GUI, CLI, Core/Stdlib cada uma em seu projeto especifico.** Motor JVM em multi-módulo Gradle autônomo (`thz-core`, `thz-cli`, `thz-gui`) comunicando via API pública do core + ponto de extensão `BibliotecaPadrao.registrar()` para as funções `TELA.*` (69 testes JUnit 5 verdes).