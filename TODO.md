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
- [ ] **Evolução Self-Hosting & Autonomia Total (Zero JVM):** Trabalhar na branch **`feat/self-hosting-llvm-autonomy`** para o avanço da suíte `compilador/*.thz`, codegen LLVM e bootstrap do compilador nativo autônomo.
- [ ] **Otimizar RenderizadorSwing (thz-gui-jvm):** Refatorar `RenderizadorFormularioSwing.java` para evitar chamadas caras como `frame.pack()` dentro do método `renderizar()` principal (que roda em loop/atualização).

    * _Ação:_ Remover `frame.pack()` do loop e chamar apenas quando realmente necessário (construção inicial, redimensionamento explícito, ou após alterar tamanho/layout de forma que o conteúdo exceda o viewport). Avaliar se redimensionamento automático deve ser desabilitado ou feito sob demanda via listener de resize.
- [ ] **Consertar renderização do MenuBar (thz-gui-jvm):** No `RenderizadorFormularioSwing.java`, a chamada `jframe.setJMenuBar(menuBar)` está fora do bloco `if (SwingUtilities.isEventDispatchThread(...))` e está dentro do loop de renderização. Isso causa re-criação e reposição desnecessária do menu a cada frame, além de poder falhar se chamada não estiver na EDT.
    * _Ação:_ Mover a criação e atribuição do menu para o bloco `if (SwingUtilities.isEventDispatchThread(...))` e chamar `jframe.setJMenuBar(menuBar)` apenas uma vez (antes do loop) ou quando o menu for de fato alterado.
- [ ] **Padronizar comandos do Gradle em `build.gradle.kts`:** O projeto tem comandos duplicados ou com nomes diferentes para a mesma ação (ex: `thz-cli` com `run` e `thz` com `thz`).