# THZ-LANG Engine — JVM

Porto **Java 25 + Gradle** do motor THZ-LANG v2.3 (interpretador tree-walking), com paridade comportamental com o motor original em Node/TypeScript.

## Visão Geral
Este é o motor de execução para THZ-LANG, focado em alta performance na JVM.

### Diretrizes Técnicas
- **Aritmética Monetária/Fiscal**: Uso obrigatório de `DecimalFixo` (BigInteger escalado); proibido ponto flutuante binário IEEE 754.
- **Gerenciamento por Bloco de Memória Temporária**: Alocação rápida por bloco contíguo (`BlocoMemoria`) com descarte automático ao final da execução, eliminando sobrecarga e pausas de Garbage Collector em processamento em lote.
- **Contratos e Arquitetura Viva**: Validação rigorosa de `EXIGE`/`GARANTE`/`INVARIANTE` via `AnalisadorSemantico` e `InterpretadorThz`.
- **Manual Completo da Linguagem**: Consulte o [Manual Oficial da Linguagem THZ-LANG v2.3](docs/MANUAL_LINGUAGEM.md) para referência de sintaxe, operador `<-`, tipos, contratos, stdlib, GUI e ferramentas.


## Requisitos
- JDK 25 (OpenJDK 25)

## Build e Execução
O projeto utiliza **Gradle (Kotlin DSL com Gradle Wrapper autônomo)** como sistema de build canônico.

### Comandos de Build (via Gradle Wrapper)
```bash
./gradlew compileJava    # Compila o código fonte
./gradlew test           # Executa todos os testes automatizados (JUnit 5)
./gradlew shadowJar      # Gera o Uber/Fat JAR em build/libs/ e target/
./gradlew check          # Validação e checagem completa
```

### Comandos de Execução Direta
```bash
# Executa a CLI passando argumentos
./gradlew run --args="check exemplos/agenda.thz"
./gradlew run --args="run exemplos/faturamento.thz"
./gradlew run --args="repl"

# Inicia a IDE Desktop Swing diretamente
./gradlew gui

# Ou execute diretamente pelo JAR compilado
java -jar build/libs/thz-jvm-2.3.0.jar check exemplos/agenda.thz
java -jar build/libs/thz-jvm-2.3.0.jar run   exemplos/agenda.thz
java -jar build/libs/thz-jvm-2.3.0.jar gui
```

### Importar na IDE (IntelliJ / VS Code)
1. **File → Open** e selecione o diretório desta pasta ou o arquivo `build.gradle.kts` (Open as Project).
2. Garanta **Project SDK = 25** (Settings → Project).
3. O Gradle Wrapper (`gradlew.bat`) cuidará automaticamente do download do runtime do Gradle e das dependências.

## Distribuição e Executáveis Autônomos (.exe)

O projeto suporta dois modos de geração de executáveis autônomos para distribuição sem necessidade de Java instalado:

### 1. Pacote Autônomo com `jpackage` (Java 25)
Gera a pasta portátil `dist/thz/` com executáveis `.exe` e JRE 25 embutido:
```powershell
# PowerShell
.\scripts\build-package.ps1

# Ou no Prompt de Comando (CMD)
scripts\build-package.cmd
```
* **Executável CLI:** `dist\thz\thz.exe`
* **Executável GUI (IDE):** `dist\thz\thz-gui.exe`

### 2. Binário Nativo Único via GraalVM Native Image (AOT)
Gera um único arquivo binário estático `dist\bin\thz.exe` (1–3ms de inicialização, zero overhead de JVM):
```powershell
# Pré-requisito: GraalVM JDK 25 + Visual Studio C++ Build Tools
.\scripts\build-native.ps1
```

---

## Testes
O projeto garante paridade com o motor TypeScript original e 100% de conformidade de tipos e contratos:
- `mvn test` (30/30 testes verdes no JUnit 5 / Java 25).
- Suítes em `src/test/java/thz/lang/`:
  - `ContratosInvariantesTest` (4 testes)
  - `DecimalMonetarioTest` (10 testes)
  - `GuiPaletaTest` (4 testes)
  - `InterpretadorTest` (4 testes)
  - `ParidadeTest` (8 testes)

## Estrutura do Projeto (Arquitetura Modular SRP)
```
src/main/java/thz/lang/
├── ast/              # Nós da Árvore de Sintaxe Abstrata (Sealed Records)
├── lexico/           # Analisador Léxico Determinístico (ThzLexer.java, TokenType.java)
├── sintatico/        # Analisador Sintático Recursivo Descendente (ThzParser.java)
├── semantico/        # Analisador Semântico e Verificador de Contratos (AnalisadorSemantico.java)
├── runtime/          # Runtime determinístico (DecimalFixo, Monetario, BlocoMemoria, Idempotencia)
├── interpretador/    # Interpretador Tree-Walking e Stdlib (InterpretadorThz.java, BibliotecaPadrao.java)
├── documento/        # Motor de Exportação Empresarial (PDF, XLSX, DOCX)
├── governanca/       # Auditoria de Arquitetura Viva e Governança G4 (AuditorGovernanca.java)
├── docgen/           # Gerador de Documentação Markdown + Mermaid (ThzDocGen.java)
├── ir/               # Emissão de THZ-IR/1 e LLVM IR preliminar (GeradorIr.java)
├── simd/             # Validador Vetorial SIMD com Regras R1–R5 (ValidadorSimd.java)
├── formato/          # Formatador de Código Idempotente Canônico (Formatador.java)
├── diagnosticos/     # Renderização de diagnósticos com trecho de fonte e caret
├── cli/              # Ponto de entrada CLI (ThzCli.java)
├── repl/             # Shell REPL multi-linha interativo (Repl.java)
└── gui/              # IDE Desktop Modularizada
    ├── ThzGui.java                   # Orquestrador da janela principal
    ├── EditorThz.java                # Editor com realce de sintaxe nativo
    ├── Gutter.java                   # Calha de numeração de linhas
    ├── PaletaThz.java                # Paletas de cores para temas Claro e Escuro
    ├── GaleriaExemplos.java          # Varredura e montagem da galeria de exemplos
    ├── barra/                        # Componentes de interface
    │   ├── BarraMenuGui.java         # Barra de Menus (Arquivo, Editar, Ver, Ações, Ajuda)
    │   ├── BarraFerramentasGui.java  # Toolbar e Header
    │   └── BarraStatusGui.java       # Indicador de linha/coluna, lint e JVM
    ├── execucao/                     # Despacho assíncrono do motor
    │   └── ExecutorMotorGui.java     # Worker de verificação, execução e compilação
    ├── formulario/                   # Motor de Interfaces Declarativas
    │   ├── RenderizadorFormularioSwing.java # Orquestrador de formulários dinâmicos
    │   ├── FabricaCamposFormulario.java     # Fábrica de widgets especializados
    │   ├── PainelTabelaFatia.java           # Visualização de coleções FATIA[...]
    │   └── ExportadorFormularioGui.java     # Exportação de formulários para documentos
    └── config/                       # Persistência e detecção de ambiente
        ├── ConfiguracaoDesktop.java
        ├── GerenciadorConfiguracao.java
        ├── DetectorJvm.java
        └── DialogoConfiguracaoJvm.java
```

