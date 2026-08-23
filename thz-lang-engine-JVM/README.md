# THZ-LANG Engine — JVM

Motor de Alta Performance **Java 25 + Gradle** para THZ-LANG v2.3 (interpretador tree-walking, compilação de formulários visuais, gerador de documentos e tooling integrado), com paridade comportamental estrita com o motor de referência em Node/TypeScript.

---

## 1. Visão Geral e Pilares Técnicos

O **THZ-LANG Engine JVM** é um ambiente corporativo completo de execução, análise, governança e interface gráfica para programas escritos em THZ-LANG (`.thz`).

### Diretrizes e Invariantes Técnicos:
- **Aritmética Monetária/Fiscal Exata (ISO/IEC 10967):** Uso obrigatório de inteiros escalados com `BigInteger` (`DecimalFixo` e `Monetario`); proibição absoluta de ponto flutuante binário IEEE 754 (`float`/`double`).
- **Gerenciamento por Bloco de Memória Temporária (`USAR_BLOCO_MEMORIA`):** Alocação rápida por bloco contíguo (`BlocoMemoria`) com descarte instantâneo em $O(1)$ ao final do bloco, eliminando pausas e sobrecarga de Garbage Collection em processamentos em lote.
- **Contratos Formais e Arquitetura Viva (DbC):** Validação rigorosa de pré-condições (`EXIGE`), pós-condições (`GARANTE`) e invariantes (`INVARIANTE`) via `AnalisadorSemantico` e `InterpretadorThz`.
- **Motor de Exportação de Documentos Corporativos:** Emissão direta de relatórios oficiais em **PDF**, planilhas **Excel (.xlsx)** e documentos **Word (.docx)** via biblioteca padrão (`DOCUMENTO`) e formulários da IDE.
- **Interface Desktop Declarativa & Formulários Visuais:** Geração automática e moderna de telas Swing (FlatLaf) a partir de estruturas de dados (`ESTRUTURA`) com validação contratual integrada.
- **Idempotência Inteligente:** Cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` com cache transacional LRU/TTL para evitar reprocessamentos duplicados.
- **Manual Oficial da Linguagem:** Consulte o [Manual Oficial da Linguagem THZ-LANG v2.3](docs/MANUAL_LINGUAGEM.md) para a referência completa de sintaxe, tipos, stdlib, contratos e governança.

---

## 2. Requisitos de Ambiente

- **JDK 25 (OpenJDK 25 ou GraalVM JDK 25)**
- Sistema Operacional: Windows 10/11 ou Linux x86_64

---

## 3. Build e Execução (via Gradle Wrapper)

O projeto utiliza **Gradle 9.7 (Kotlin DSL com Gradle Wrapper autônomo)** como sistema canônico oficial.

### Comandos de Compilação e Testes:
```bash
# Compila todo o código-fonte Java 25
./gradlew compileJava

# Executa todos os 68 testes automatizados no JUnit 5
./gradlew test

# Gera o UberJAR executável autônomo (build/libs/thz-jvm-2.3.0.jar e target/thz-jvm-2.3.0.jar)
./gradlew shadowJar

# Validação e checagem completa do projeto
./gradlew check
```

### Comandos de Execução Direta:
```bash
# Iniciar a IDE Desktop Swing moderna (FlatLaf)
./gradlew gui

# Checagem estrita de sintaxe, tipos e contratos
./gradlew run --args="check exemplos/faturamento.thz --estrito"

# Executar programas
./gradlew run --args="run exemplos/faturamento.thz"
./gradlew run --args="run exemplos/colecao/01-ola-mundo.thz"

# Auditoria de Governança G4 e Matriz de Rastreabilidade
./gradlew run --args="audit exemplos/faturamento.thz"

# Geração de Documentação Técnica em Markdown + Diagramas Mermaid
./gradlew run --args="doc exemplos/faturamento.thz --saida docs/"

# Emissão de THZ-IR/1 e LLVM IR preliminar
./gradlew run --args="ir exemplos/faturamento.thz --llvm"

# Formatador canônico idempotente
./gradlew run --args="fmt exemplos/colecao/01-ola-mundo.thz --check"

# Shell REPL interativo multi-linha
./gradlew run --args="repl"
```

### Executar diretamente pelo JAR compilado:
```bash
java -jar build/libs/thz-jvm-2.3.0.jar gui
java -jar build/libs/thz-jvm-2.3.0.jar check exemplos/faturamento.thz
java -jar build/libs/thz-jvm-2.3.0.jar run   exemplos/faturamento.thz
```

---

## 4. Distribuição e Executáveis Nativos (.exe)

O projeto suporta dois modos de geração de executáveis autônomos sem necessidade de Java instalado no cliente:

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
Gera um único arquivo binário estático `dist\bin\thz.exe` (inicialização instantânea em ~2ms, zero overhead de runtime):
```powershell
# Pré-requisito: GraalVM JDK 25 + Visual Studio C++ Build Tools
.\scripts\build-native.ps1
```

---

## 5. Suíte de Testes Automatizados (JUnit 5)

A suíte conta com **68 testes unitários e de integração (100% verdes)** cobrindo todos os módulos do sistema:

| Classe de Teste | Quantidade | Escopo de Validação |
|---|:---:|---|
| `ParidadeTest` | 8 | Paridade estrita com o motor TS: lexer, parser, datas Hinnant, tolerância a BOM UTF-8, formatador idempotente e validação integral de todos os exemplos da galeria. |
| `DecimalMonetarioTest` | 10 | Aritmética bancária half-even ISO/IEC 10967, precisão decimal sem float, multiplicação de monetário por escalar e moedas ISO-4217. |
| `ContratosInvariantesTest` | 4 | Validação estática e em tempo de execução de `EXIGE` (pré-condições), `GARANTE` (pós-condições) e `INVARIANTE` em estruturas de dados. |
| `DocumentosTest` | 5 | Geração programática de relatórios em PDF (OpenPDF), planilhas Excel XLSX e documentos Word DOCX (Apache POI). |
| `FormularioGuiTest` | 5 | Renderizador de formulários Swing, binding de campos, testes visuais de widgets e ciclo de submissão. |
| `GovernancaTest` | 4 | Matriz de rastreabilidade de requisitos G4, auditoria formal de conformidade (SOX, PCI-DSS) e relatórios Markdown/JSON. |
| `GuiPaletaTest` | 4 | Cobertura de todos os 63 `TokenType` nas paletas ESCURO e CLARO, e ciclo de vida da interface gráfica Swing. |
| `IdempotenciaTest` | 5 | Memoização transacional com `RegistroIdempotencia`, auditoria G4 e emissão de IR/LLVM idempotentes. |
| `InterpretadorTest` | 4 | Execução de operações, laços de controle (`SE`, `ENQUANTO`, `PARA`), fatias dinâmicas e stdlib (`TEXTO`, `MATEMATICA`). |
| `IrSimdTest` | 4 | Representação intermediária `thz-ir/1`, validação estrita das regras vetoriais SIMD R1 a R5 e emissão de LLVM IR textual. |
| `BlocoMemoriaTest` | 4 | Alocação sequencial contígua, descarte $O(1)$ e validação de limites de capacidade da memória efêmera. |
| `ConfiguracaoDesktopTest` | 4 | Serialização/desserialização JSON de configurações de desktop (`~/.thz/desktop-config.json`) e histórico de arquivos recentes. |
| `DetectorJvmTest` | 5 | Detecção automática de JVMs instaladas (Scoop, SDKMAN, Program Files, JAVA_HOME), validação e persistência. |
| `DocGenTest` | 2 | Geração automatizada de documentação técnica em Markdown com diagramas de classes e fluxo Mermaid.js. |

---

## 6. Estrutura Arquitetural do Projeto

```
src/main/java/thz/lang/
├── ast/              # Nós da Árvore de Sintaxe Abstrata (Sealed Records imutáveis)
├── lexico/           # Analisador Léxico Determinístico (ThzLexer, Token, TokenType, PalavrasReservadas)
├── sintatico/        # Analisador Sintático Recursivo Descendente (ThzParser)
├── semantico/        # Analisador Semântico, Tipos e Verificador de Contratos (AnalisadorSemantico, Tipos)
├── runtime/          # Runtime determinístico (DecimalFixo, Monetario, BlocoMemoria, RegistroIdempotencia)
├── interpretador/    # Interpretador Tree-Walking e Stdlib (InterpretadorThz, BibliotecaPadrao, Escopo)
├── documento/        # Motor de Exportação Empresarial (MotorDocumentos, GeradorPdf, GeradorXlsx, GeradorDocx)
├── governanca/       # Auditoria de Governança G4 e Matriz de Rastreabilidade (AuditorGovernanca, RelatorioAuditoria)
├── docgen/           # Gerador de Documentação Markdown + Mermaid (ThzDocGen)
├── ir/               # Emissão de THZ-IR/1 e LLVM IR preliminar (GeradorIr, IrPrograma)
├── simd/             # Validador Vetorial SIMD com Regras R1–R5 (ValidadorSimd)
├── formato/          # Formatador de Código Idempotente Canônico e JSON (Formatador, JsonEscritor)
├── diagnosticos/     # Renderização de diagnósticos com trecho de fonte e caret (Diagnosticos)
├── cli/              # Ponto de entrada CLI (ThzCli)
├── repl/             # Shell REPL multi-linha interativo (Repl)
└── gui/              # IDE Desktop Modularizada em FlatLaf (Dark/Light)
    ├── ThzGui.java                   # Janela principal e orquestrador de eventos
    ├── EditorThz.java                # Editor de código com realce léxico em tempo real
    ├── Gutter.java                   # Calha de numeração de linhas ancorada ao layout
    ├── PaletaThz.java                # Paletas de cores profissionais para temas Claro e Escuro
    ├── GaleriaExemplos.java          # Varredura dinâmica e carregamento de exemplos
    ├── barra/                        # Componentes de interface (Menu, Toolbar, StatusBar)
    ├── execucao/                     # Despacho assíncrono do motor (ExecutorMotorGui)
    ├── formulario/                   # Motor declarativo de telas desktop (RenderizadorFormularioSwing)
    └── config/                       # Persistência e detecção de JVMs (ConfiguracaoDesktop, DetectorJvm)
```

