# AGENTS.md — THZ-LANG JVM Engine

Diretrizes de Engenharia e Operação para Agentes de IA operando no repositório **THZ-LANG Engine JVM**.

---

## 1. Visão Geral e Identidade do Projeto
* **Nome do Projeto:** THZ-LANG JVM Engine (`.thz`)
* **Paradigma:** Linguagem Corporativa de Sistemas, Governança de Negócio, Arquitetura Viva, Precisão Fiscal e Processamento de Alto Desempenho.
* **Stack Canônica:** Java 25 (OpenJDK 25) + Gradle 9.7 (Kotlin DSL `build.gradle.kts` + Gradle Wrapper) + FlatLaf + Apache POI + OpenPDF + JUnit 5.
* **Paridade:** Paridade comportamental estrita e golden tests com o motor TypeScript de referência (`thz-lang-engine`).

---

## 2. Invariantes Técnicos e Normas Obrigatórias

1. **Aritmética Financeira e Decimais (ISO/IEC 10967):**
   * É terminantemente proibido o uso de ponto flutuante binário IEEE 754 (`float`/`double`) para operações monetárias e fiscais.
   * Toda aritmética decimal deve utilizar inteiros escalados com `BigInteger` (classe `DecimalFixo` e `Monetario` no runtime Java).

2. **Gerenciamento por Bloco de Memória Temporária (`USAR_BLOCO_MEMORIA` / `BlocoMemoria`):**
   * Processamento em lote e escopos temporários utilizam alocação sequencial em bloco de memória contíguo (`BlocoMemoria`).
   * Toda a memória utilizada no bloco é liberada de forma limpa e automática ao final da execução (`liberarTudo()`), sem sobrecarga de Garbage Collection nem fragmentação.

3. **Arquitetura Viva e Contratos Formais (ISO/IEC/IEEE 42010 & ISO/IEC TR 24772):**
   * O bloco `METADADOS_ARQUITETURA` é obrigatório em programas corporativos no modo `--estrito`.
   * Cláusulas `EXIGE` (pré-condições), `GARANTE` (pós-condições) e `INVARIANTE` são validadas estática e dinamicamente via `AnalisadorSemantico` e `InterpretadorThz`.

4. **Idempotência Inteligente:**
   * Cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` no cabeçalho de `REGRA_NEGOCIO` são auditadas e armazenadas em cache transacional LRU/TTL com descarte $O(1)$ (`RegistroIdempotencia`).

5. **Build Canônico e Uso Exclusivo do Gradle Wrapper Embutido:**
   * É obrigatório utilizar exclusivamente o **Gradle Wrapper embutido no projeto** (`.\gradlew` no Windows PowerShell / `./gradlew` no Linux/macOS).
   * Não utilize o binário `gradle` global do sistema para evitar conflitos de versão e de toolchain.
   * Arquivo de configuração oficial: `build.gradle.kts`.

6. **Sintaxe Canônica em Português e Diagnósticos:**
   * Palavras reservadas vivem em `thz.lang.lexico.PalavrasReservadas` (fonte única da verdade). Proibido literal fora daqui.
   * Erros sintáticos, léxicos e semânticos seguem o padrão `[Erro <Categoria>][Linha L:C]` com renderização por caret via `thz.lang.diagnosticos.Diagnosticos`.

---

## 3. Mapa de Pacotes (`src/main/java/thz/lang/`)

* `ast/`: Nós da Árvore de Sintaxe Abstrata como sealed records (`ProgramaAst`, `EstruturaAst`, `RegraNegocioAst`, `OperacaoAst`, `ProcedimentoAst`, `ExprAst`, `ComandoAst`).
* `lexico/`: Scanner determinístico com rastreio de linha/coluna e tolerância a BOM UTF-8 (`ThzLexer`, `TokenType`, `PalavrasReservadas`).
* `sintatico/`: Parser recursivo descendente (`ThzParser`) gerando AST com precedência formal de operadores.
* `semantico/`: Verificador de tipos, escopos léxicos, contratos quantificados $\forall$ sobre `FATIA` e lint estrito (`AnalisadorSemantico`, `Tipos`, `AssinaturasStdlib`).
* `runtime/`: Núcleo de execução numérica (`DecimalFixo`, `Monetario`, `ModoArredondamento`), temporal (`DataThz`, `DataHoraThz`), memória (`BlocoMemoria`) e memoização (`RegistroIdempotencia`).
* `interpretador/`: Interpretador tree-walking com validação contratual em tempo de execução, despacho de operações e biblioteca padrão (`InterpretadorThz`, `BibliotecaPadrao`, `Escopo`, `ValorThz`).
* `documento/`: Motor de exportação de documentos empresariais com formatação institucional (`MotorDocumentos`, `GeradorPdf`, `GeradorXlsx`, `GeradorDocx`).
* `governanca/`: Auditoria formal de conformidade, SLOs e matriz `RASTREIO_REQUISITO → Regra → Contrato` (`AuditorGovernanca`, `RelatorioAuditoria`).
* `docgen/`: Gerador de documentação técnica em Markdown com diagramas de classes e fluxo Mermaid.js (`ThzDocGen`).
* `ir/`: Emissão de representação intermediária `thz-ir/1` e LLVM IR (`GeradorIr`, `IrPrograma`).
* `simd/`: Validador de transformações vetoriais com verificação estrita das regras R1 a R5 (`ValidadorSimd`).
* `formato/`: Formatador canônico idempotente e serializador JSON determinístico (`Formatador`, `JsonEscritor`).
* `diagnosticos/`: Formatador de erros com apontador caret e trecho de código (`Diagnosticos`).
* `cli/`: Ponto de entrada de linha de comando (`ThzCli`).
* `repl/`: Console interativo multi-linha (`Repl`).
* `gui/`: IDE Desktop Swing completa com realce de sintaxe em tempo real, numeração de linhas ancorada, galeria de exemplos, alternância de temas (FlatLaf) e formulários declarativos:
  * `gui/barra/`: Barra de Menus, Barra de Ferramentas e Barra de Status.
  * `gui/execucao/`: Executor assíncrono do motor (`ExecutorMotorGui`).
  * `gui/formulario/`: Renderizador dinâmico de formulários (`RenderizadorFormularioSwing`, `FabricaCamposFormulario`, `PainelTabelaFatia`, `ExportadorFormularioGui`).
  * `gui/config/`: Detecção e configuração de JVMs locais (`DetectorJvm`, `DialogoConfiguracaoJvm`) e persistência desktop (`ConfiguracaoDesktop`, `GerenciadorConfiguracao`).

---

## 4. Comandos de Build e Teste

```bash
# Compilar fontes Java 25
./gradlew compileJava

# Executar suíte completa de testes automatizados JUnit 5
./gradlew test

# Gerar UberJAR executável autônomo (build/libs/thz-jvm-2.3.0.jar e target/thz-jvm-2.3.0.jar)
./gradlew shadowJar

# Iniciar a IDE Desktop Swing
./gradlew gui

# Executar a CLI com argumentos
./gradlew run --args="check exemplos/faturamento.thz"
./gradlew run --args="run exemplos/colecao/01-ola-mundo.thz"
./gradlew run --args="audit exemplos/faturamento.thz"
./gradlew run --args="doc exemplos/faturamento.thz"
./gradlew run --args="ir exemplos/faturamento.thz --llvm"
./gradlew run --args="fmt exemplos/colecao/01-ola-mundo.thz --check"
./gradlew run --args="repl"
```
