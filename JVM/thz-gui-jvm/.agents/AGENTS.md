# AGENTS.md — THZ-LANG GUI

Diretrizes para agentes de IA operando no módulo **thz-gui** (IDE Desktop Swing do motor JVM da THZ-LANG).

## Identidade

* **Projeto:** thz-gui — IDE Desktop Java 25 (Swing + FlatLaf 3.5) que consome o módulo irmão `:thz-core` (`implementation(project(":thz-core"))`).
* **Build canônico:** exclusivamente o Gradle Wrapper embutido na raiz do projeto (gradlew / gradlew.bat).

## Pontos-Chave do Código

* `thz.lang.gui.ThzGui` — janela principal; `main` chama `BibliotecaTela.registrar()` **antes** de qualquer execução de código THZ.
* `thz.lang.gui.BibliotecaTela` — registra as funções gráficas `TELA.renderizarFormulario/alerta/confirmar/pedirTexto` na stdlib do core (`BibliotecaPadrao.registrar()`); o core permanece livre de Swing.
* `gui/execucao/ExecutorMotorGui` — despacho assíncrono das operações do motor.
* `gui/formulario/RenderizadorFormularioSwing` — formulários declarativos a partir de `ESTRUTURA`; validação de contratos `EXIGE` na submissão.
* `gui/config/DetectorJvm` + `ConfiguracaoDesktop` — detecção/persistência de JVMs e preferências (`~/.thz/desktop-config.json`).
* Testes GUI rodam com `-Dthz.nao_interativo=true` (ver `FormularioGuiTest#setup`).

## Estrutura

```
src/main/java/thz/lang/gui/
├── ThzGui, EditorThz, Gutter, PaletaThz, GaleriaExemplos
├── BibliotecaTela          # extensões stdlib TELA.*
├── barra/                  # BarraMenuGui, BarraFerramentasGui, BarraStatusGui
├── execucao/               # ExecutorMotorGui
├── formulario/             # RenderizadorFormularioSwing, FabricaCamposFormulario,
│                           #   PainelTabelaFatia, ExportadorFormularioGui
└── config/                 # DetectorJvm, DialogoConfiguracaoJvm,
                            #   ConfiguracaoDesktop, GerenciadorConfiguracao
```

## Comandos

```bash
./gradlew test    # testes JUnit 5
./gradlew gui     # inicia a IDE Desktop
```

Exemplos GUI carregáveis pela Galeria ficam em `exemplos/*_gui.thz`.
