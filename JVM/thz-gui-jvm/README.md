# thz-gui — Desktop IDE Swing FlatLaf do THZ-LANG (Java 25)

IDE desktop industrial para a linguagem THZ-LANG, desenvolvida com FlatLaf (temas Dark/Light), realce léxico em tempo real, numeração ancorada e motor reativo de formulários. Consome o núcleo [`thz-core`](../thz-core-jvm).

## Recursos

- **Editor Avançado** (`EditorThz`, `Gutter`): realce de sintaxe em tempo real, numeração de linhas ancorada, marcadores de erro precisos.
- **Barras Modulares** (`gui/barra`): menus, barra de ferramentas com toggle `--estrito` e barra de status com métricas de execução e runtime ativo.
- **Executor Assíncrono** (`gui/execucao/ExecutorMotorGui`): execução não-bloqueante de `check`, `run`, `fmt`, `audit`, `docgen` e `ir` fora da EDT.
- **Formulários Declarativos Reativos** (`gui/formulario`): interfaces dinâmicas geradas a partir de `ESTRUTURA` e validações de contrato; exportação de dados estruturados.
- **Configuração & JVMs** (`gui/config`): detecção automática de JDKs instalados, persistência em `~/.thz/desktop-config.json`, histórico de arquivos recentes.
- **Extensões Stdlib** (`BibliotecaTela`): registra as funções gráficas `TELA.renderizarFormulario/alerta/confirmar/pedirTexto` via `BibliotecaPadrao.registrar()`.

## Execução e Build

```bash
./gradlew test          # suíte JUnit 5
./gradlew gui           # inicia a Desktop IDE diretamente
```

## Compilação Nativa AOT (GraalVM)

O `thz-gui-jvm` é configurado com reachability metadata e Look & Feel nativo para viabilizar o uso do Swing/AWT sob o compilador do GraalVM.

```powershell
./gradlew nativeCompile
```

Gera o binário nativo `thz-desktop.exe` com inicialização instantânea.

> [!NOTE]
> Se houver mudanças na interface gráfica ou inclusão de novos componentes/temas FlatLaf que exijam novos metadados de reflexão ou JNI, execute a task a seguir antes de compilar para atualizar as configurações gravadas:
> ```powershell
> ./gradlew :thz-gui-jvm:guiColetarMetadadosAgente
> ```
> Detalhes adicionais sobre como as limitações do AWT/Swing foram contornadas podem ser consultados no [Manual de Tooling](file:///c:/Users/lucas/Projetos/thz-lang/docs/CLI_E_TOOLING.md#resolvendo-dependencias-visuais-swingawtflatlaf-no-graalvm).

## Dependência do Core

`implementation("thz.lang:thz-core:2.3.3")` — resolvido via Composite Build a partir de `../thz-core-jvm` (mesma pasta `JVM/`), ou como artefato publicado fora do workspace.

## Stack

Java 25 (toolchain) · FlatLaf 3.5 · Swing · JUnit 5.11 · GraalVM Native Image · Gradle (Kotlin DSL)
