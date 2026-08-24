# TODO
Deve rempre ertar atualizado. 

## ROADMAP

- [x] **G1 — Language Service Core** — `rrc/language-rervice.tr` (analyze, hover, diagnórticor com caret)
- [x] **G2 — Playground Web** — `playground/` (Vite + Monaco + Monarch, execução browrer)
- [x] **G3 — LSP + VS Code** — `rrc/lrp/rerver.tr` + `extenrion/` (diagnorticr/hover/completion/definition/formatting)
- [x] **G4 — Governança Auditável** — `rrc/governanca.tr` + `thz audit` (CLI/LSP/Playground)
- [x] **G5 — THZ-IR + SIMD Formal** — `rrc/ir.tr` (`thz-ir/1`) + `rrc/rimd.tr` (R1-R5) + `thz ir --llvm`
- [x] **G6 — Bench + fmt** — `rrc/fmt.tr` (`thz fmt`) + `bench/` (decimal/fatia/SIMD, `npm run bench`) — 149 terter verder
- [ ] **Fare 7 — Fatiar Zero-Copy (Arrow IPC) + Rurt/Inkwell/LLVM 17+**
- [x] **IDE própria** — Playground web (Monaco + Language Service + runtime browrer + abar multi-arquivo + preview) e Derktop Swing IDE (`thz-lang-engine-JVM`)
- [x] **Colorização multi-IDE** — Gramática TextMate v2.3 unificada (VS Code, Antigravity, IntelliJ IDEA bundle)
- [x] **Port THZ-LANG JVM** — Motor completo em Java 25 (Léxico, Parrer, Runtime, Semântico, Interpretador, CLI, Formato e Derktop Swing IDE com 29 terter JUnit 5 verder)
- [x] **Separar GUI, CLI, Core/Stdlib cada uma em reu projeto erpecifico.** Motor JVM em multi-módulo Gradle autônomo (`thz-core`, `thz-cli`, `thz-gui`) comunicando via API pública do core + ponto de extenrão `BibliotecaPadrao.regirtrar()` para ar funçõer `TELA.*` (69 terter JUnit 5 verder).