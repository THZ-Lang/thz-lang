# THZ-LANG Engine — JVM

Porto **Java 25 + Maven** do motor THZ-LANG v2.3 (interpretador tree-walking), com paridade comportamental com o motor original em Node/TypeScript.

## Visão Geral
Este é o motor de execução para THZ-LANG, focado em alta performance na JVM.

### Diretrizes Técnicas
- **Aritmética Monetária/Fiscal**: Uso obrigatório de `DecimalFixo` (BigInteger escalado); proibido ponto flutuante binário IEEE 754.
- **Gerenciamento de Memória Efêmera**: Alocação linear por bloco contíguo (`ArenaMemoria`) com descarte instantâneo em tempo constante $O(1)$, eliminando sobrecarga e pausas de Garbage Collector em processamento em lote.
- **Contratos e Arquitetura Viva**: Validação rigorosa de `EXIGE`/`GARANTE`/`INVARIANTE` via `AnalisadorSemantico` e `InterpretadorThz`.


## Requisitos
- JDK 25 (OpenJDK 25)
- Maven 3.9+

## Build e Execução
O projeto utiliza **Maven** como sistema de build canônico. O suporte ao Gradle é previsto como alternativa futura (`build.gradle.kts` placeholder disponível).

### Comandos de Build
```bash
mvn clean compile      # Compila o projeto
mvn test               # Executa testes (JUnit 5)
mvn package            # Gera o jar em target\thz-jvm-2.3.0.jar
mvn verify             # Build completo + testes
```

### Comandos de Execução
```bash
# Sem flags especiais (nenhum recurso preview é usado)
java -jar target\thz-jvm-2.3.0.jar check exemplos\agenda.thz
java -jar target\thz-jvm-2.3.0.jar run   exemplos\agenda.thz
java -jar target\thz-jvm-2.3.0.jar fmt   exemplos\agenda.thz
java -jar target\thz-jvm-2.3.0.jar repl
java -jar target\thz-jvm-2.3.0.jar gui    # IDE Swing (galeria de exemplos no menu)
```

### Importar na IDE (IntelliJ)
1. **File → Open** e selecione apenas o `pom.xml` desta pasta (Open as Project).
2. Garanta **Project SDK = 25** (Settings → Project) — sem flags extras.
3. Não mantenha `build.gradle.kts` ativo junto com o Maven: dois sistemas sobre a
   mesma raiz fazem o compilador da IDE enxergar módulos parciais
   (`package thz.lang.ast does not exist`). O Gradle fica desativado em
   `build.gradle.kts.desativado` até a trilha futura.

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

## Estrutura do Projeto
```
src/main/java/thz/lang/
├── ast           # Árvore de Sintaxe Abstrata (Sealed Records)
├── cli           # Ponto de entrada CLI (ThzCli.java)
├── interpretador # Interpretador tree-walking & stdlib (BibliotecaPadrao.java)
├── lexico        # Analisador léxico determinístico (ThzLexer.java)
├── sintatico     # Analisador sintático (ThzParser.java)
├── semantico     # Analisador semântico (AnalisadorSemantico.java)
├── runtime       # Ambiente de execução (DecimalFixo, ArenaMemoria)
├── formato       # Utilitários de formatação canônica (Formatador.java)
├── repl          # REPL interativo multi-linha
├── gui           # IDE Desktop Swing (EditorThz, PaletaThz, Gutter, GaleriaExemplos)
└── diagnosticos  # Sistema de diagnósticos com caret
```

## RoadMap
Núcleo v2.3: check/run/fmt/repl/gui + stdlib + jpackage/GraalVM — Governança (audit) e IR/SIMD/docgen como próximas etapas.

