# thz-gui — IDE Desktop Swing do THZ-LANG (Java 25)

IDE desktop completa para a linguagem THZ-LANG, com FlatLaf (temas Dark/Light), realce léxico em tempo real e motor declarativo de formulários. Consome o núcleo [`thz-core`](../thz-core).

## Recursos

- **Editor** (`EditorThz`, `Gutter`): realce léxico em tempo real, numeração de linhas ancorada, marcação de erros.
- **Barras modulares** (`gui/barra`): menus, ferramentas (toggle `--estrito`) e status com métricas e JVM ativa.
- **Executor assíncrono** (`gui/execucao/ExecutorMotorGui`): check, run, fmt, audit, docgen e IR sem travar a UI.
- **Formulários declarativos** (`gui/formulario`): telas geradas a partir de `ESTRUTURA` + contratos; exportação de dados.
- **Configuração** (`gui/config`): detecção de JVMs instaladas, persistência em `~/.thz/desktop-config.json`, histórico de arquivos recentes.
- **Extensões stdlib** (`BibliotecaTela`): registra as funções gráficas `TELA.renderizarFormulario/alerta/confirmar/pedirTexto` via `BibliotecaPadrao.registrar()`.

## Build

```bash
./gradlew test          # suíte JUnit 5
./gradlew gui           # inicia a IDE Desktop
```

> Os exemplos GUI (`*_gui.thz`) estão em `exemplos/` e são carregados pela Galeria da IDE.

## Dependência do core

`implementation("thz.lang:thz-core:2.3.3")` — resolvido via Composite Build a partir de `../thz-core-jvm` (mesma pasta `JVM/`), ou como artefato publicado fora do workspace.

## Stack

Java 25 (toolchain) · FlatLaf 3.5 · Swing · JUnit 5.11 · Gradle (Kotlin DSL)
