# AGENTS.md — THZ-LANG CLI

Diretrizes para agentes de IA operando no módulo **thz-cli** (CLI + REPL do motor JVM da THZ-LANG).

## Identidade

* **Projeto:** thz-cli — aplicação Java 25 que consome o módulo irmão `:thz-core` (`implementation(project(":thz-core"))`).
* **Build canônico:** exclusivamente o Gradle Wrapper embutido na raiz do projeto (`gradlew` / `gradlew.bat`).

## Pontos-Chave do Código

* `thz.lang.cli.ThzCli` — entrypoint; registra `BibliotecaConsole.registrar()` no início do `main`.
* `thz.lang.cli.BibliotecaConsole` — implementações console/headless das funções `TELA.*`; `renderizarFormulario` falha com mensagem orientando à IDE Desktop.
* `thz.lang.cli.ThzCli.lancarGuiSeDisponivel()` — lança `thz.lang.gui.ThzGui` **por reflexão** (módulo opcional no classpath); nunca adicionar dependência de compilação com o thz-gui aqui.
* `thz.lang.repl.Repl` — REPL multi-linha; também registra `BibliotecaConsole`.
* `-Dthz.nao_interativo=true` — modo não interativo (respostas padrão sem stdin).

## Comandos

```bash
./gradlew test                                  # testes
./gradlew shadowJar                             # UberJAR → build/libs/ + target/thz-jvm-2.3.0.jar
./gradlew run --args="check <arquivo.thz>"      # executa CLI (workingDir = raiz)
./gradlew cli                                   # atalho para run
```

Scripts legados (`../scripts/build-native.ps1`, `../scripts/build-package.ps1`) esperam `../target/thz-jvm-2.3.0.jar` — mantido pelo `doLast` do `shadowJar`.
