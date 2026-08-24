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

## OPEN items :

- [x] **Melhorar interação entre CLI e GUI:** `thz.ps1` e `thz.cmd` agora delegam `gui` diretamente para `:thz-gui-jvm:gui`, separando completamente o ciclo de vida do CLI do ciclo de vida da Desktop IDE Swing.
- [x] **Resolução de paridade e qualidade de GUI:** O Swing + FlatLaf no `thz-gui-jvm` é o padrão oficial universal para garantir paridade visual 1:1 no Windows, Linux e macOS, dispensando pontes Win32/GTK parciais.
- [x] **Swing no GraalVM Native Image:** Configurado `-Djava.awt.headless=false` e o plugin `org.graalvm.buildtools.native` com `native-image-agent` no `thz-gui-jvm/build.gradle.kts`, gerando automaticamente `reachability-metadata.json` para compilação AOT de aplicações Swing.

- [x] **Evolução Self-Hosting & Autonomia Total (Zero JVM):** Implementada a suíte `compilador/*.thz` (`tokens.thz`, `ast.thz`, `lexer.thz`, `parser.thz`, `codegen.thz`, `driver.thz`) e expandido o runtime C Dual-OS (`src/runtime/thz_runtime.c`) com rotinas nativas de I/O de arquivo e manipulação de strings. Pipeline AOT LLVM Clang validado com geração de binário nativo `driver.exe` x86_64 funcional.
- [x] **Otimização do RenderizadorFormularioSwing:** O `RenderizadorFormularioSwing` foi auditado e opera de forma estritamente reativa em uma única passagem na EDT, invocando `frame.pack()` somente durante o dimensionamento inicial no método `ajustarTamanhoECentralizar()`.
- [x] **Renderização de MenuBar & Ciclo de Vida da GUI:** Em `ThzGui.java`, o `setJMenuBar` é chamado uma única vez na inicialização dentro da EDT via `SwingUtilities.invokeLater`, garantindo estabilidade e integridade em todas as plataformas.
- [x] **Padronizar comandos do Gradle em `build.gradle.kts`:** Definidas tasks aggregation de alto nível na raiz (`./gradlew cli`, `./gradlew gui`, `./gradlew jmh`, `./gradlew check`, `./gradlew test`), permitindo execução direta e limpa a partir de qualquer ponto do projeto.