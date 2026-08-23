# PROGRESSO DE IMPLEMENTAÇÃO — THZ-LANG Engine JVM25

> Registro consolidado do estado da implementação. Atualizado em **2026-08-23**.
> Espelho Node/TS de referência: `../thz-lang-engine` (v2.3 "Núcleo Generalista", 159 testes).

---

## 1. Visão Geral

| Item | Valor |
|---|---|
| **Artefato Canônico** | `build/libs/thz-jvm-2.3.0.jar` / `target/thz-jvm-2.3.0.jar` (shaded FatJAR, ~18 MB com FlatLaf, Apache POI e OpenPDF) |
| **Plataforma** | Java 25 (OpenJDK 25) + Gradle 9.7 (Kotlin DSL `build.gradle.kts` + Wrapper) |
| **Testes Automatizados** | `./gradlew test` — **68/68 testes verdes (100%)** no JUnit 5 |
| **Pacotes** | `thz.lang.{ast, lexico, sintatico, semantico, runtime, interpretador, documento, governanca, docgen, ir, simd, formato, diagnosticos, cli, repl, gui}` |
| **Entradas de Execução** | CLI `ThzCli` (`check/run/fmt/ast/audit/doc/ir/repl/gui`), IDE Desktop `ThzGui`, REPL multi-linha |
| **Distribuição Portátil** | `dist/thz/` (`thz.exe` e `thz-gui.exe` via `jpackage`) e `dist/bin/thz.exe` (AOT GraalVM Native Image) |

### Comandos Canônicos de Build e Execução:
```powershell
# Execução da suíte de testes
.\gradlew test

# Iniciar a IDE Desktop
.\gradlew gui

# Executar programas e comandos CLI
.\gradlew run --args="check exemplos\faturamento.thz --estrito"
.\gradlew run --args="run exemplos\faturamento.thz"
.\gradlew run --args="audit exemplos\faturamento.thz"
.\gradlew run --args="doc exemplos\faturamento.thz"
.\gradlew run --args="ir exemplos\faturamento.thz --llvm"
```

---

## 2. Fases do Motor — TODAS CONCLUÍDAS ✅

| Fase | Escopo | Componentes e Arquivos-Chave | Status |
|---|---|---|:---:|
| **F0** | Fundações & Build | Gradle 9.7 com Kotlin DSL (`build.gradle.kts`), Java 25 toolchain, ShadowJar plugin, JUnit 5.11, dependências FlatLaf, Apache POI e OpenPDF | ✅ |
| **F1** | Scanner Léxico | `TokenType` (63 tokens canônicos), `Token`, `CategoriaPalavra`, `PalavrasReservadas` (fonte única da verdade), `ThzLexer` determinístico com rastreio de linha:coluna e descarte de BOM UTF-8 (`U+FEFF`) | ✅ |
| **F2** | AST & Parser | `ExprAst` sealed (12 nós), `ComandoAst` sealed (12 nós), `ProgramaAst` + records de alto nível; `ThzParser` recursivo descendente com precedência estrita e formatação canônica | ✅ |
| **F3** | Runtime Determinístico | `DecimalFixo` (BigInteger escalado, half-even ISO/IEC 10967 sem float), `Monetario` (ISO-4217), `DataThz/DataHoraThz` (algoritmos temporais Hinnant), `BlocoMemoria` (memória contígua efêmera $O(1)$) e `RegistroIdempotencia` (cache LRU/TTL) | ✅ |
| **F4** | Análise Semântica | `TipoThz`, `Tipos`, `CategoriaTipo`, `AnalisadorSemantico` (tabela de tipos, escopos aninhados, 28+ assinaturas stdlib, lint estrito e validação quantificada $\forall$ sobre `FATIA`) | ✅ |
| **F5** | Interpretador & Stdlib | `ValorThz` sealed (12 registros), `Escopo`, sinais de controle `SinalRetorne/SinalFalhar`, `InterpretadorThz` com avaliação de contratos em tempo de execução e stdlib (`TEXTO`, `MATEMATICA`, `DATA`, `FATIA`, `DOCUMENTO`, `TELA`) | ✅ |
| **F6** | Tooling & Formatação | `Formatador` canônico idempotente, `JsonEscritor` determinístico, `ThzCli` (subcomandos `check`, `run`, `fmt`, `ast`, `audit`, `doc`, `ir`, `repl`, `gui`) e `Repl` interativo | ✅ |
| **F7** | Paridade e Golden Tests | 68 testes JUnit 5 cobrindo lexer, parser, runtime, contratos, stdlib, idempotência, SIMD, IR, governança e formulários visuais | ✅ |

---

## 3. Módulos Avançados e Extensões Corporativas

### Governança G4 e Matriz de Rastreabilidade (`thz.lang.governanca`)
* Implementado por `AuditorGovernanca` e `RelatorioAuditoria`.
* Mapeamento completo `RASTREIO_REQUISITO → Regra → Contrato`, conferência de SLOs e conformidade regulatória.
* Saída em Markdown institucional e JSON estruturado via CLI `thz audit` e botão `🛡️ Auditoria` na IDE.

### Geração de Documentação Técnica (`thz.lang.docgen`)
* Implementado por `ThzDocGen`.
* Extrai metadados arquiteturais, regras, contratos e estruturas para Markdown com diagramas de classes e diagramas de fluxo Mermaid.js.
* Disponível via CLI `thz doc` e botão `📘 Doc` na IDE.

### THZ-IR e Validação SIMD G5 (`thz.lang.ir` e `thz.lang.simd`)
* Implementado por `GeradorIr`, `IrPrograma` e `ValidadorSimd`.
* Baixamento da AST para `thz-ir/1`, validação estrita das 5 regras de vetorização SIMD (R1–R5) e emissão de LLVM IR textual preliminar.
* Disponível via CLI `thz ir [--llvm]` e botão `🧩 IR` na IDE.

### Motor de Exportação de Documentos Empresariais (`thz.lang.documento`)
* Implementado por `MotorDocumentos`, `GeradorPdf` (OpenPDF), `GeradorXlsx` (Apache POI) e `GeradorDocx` (Apache POI).
* Geração programática de relatórios em PDF com paginação e cabeçalhos corporativos, planilhas Excel XLSX zebradas com auto-ajuste de colunas e documentos Word DOCX estruturados.
* Integrado à Stdlib (`DOCUMENTO.exportar*`) e aos formulários da IDE Desktop.

### Idempotência Inteligente de Larga Escala (`thz.lang.runtime.RegistroIdempotencia`)
* Suporte nativo às cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` no cabeçalho de `REGRA_NEGOCIO`.
* Cache transacional LRU com TTL e descarte $O(1)$, evitando re-execuções desnecessárias e garantindo auditabilidade.

---

## 4. IDE Desktop Swing (`thz.lang.gui`)

* **Visual Moderno:** FlatLaf (Dark/Light) com alternância dinâmica de temas sem reiniciar.
* **Editor Inteligente (`EditorThz`):** Realce léxico em tempo real, auto-indentação, fechamento de aspas e sublinhado de erros com debounce de 300ms.
* **Gutter Preciso (`Gutter`):** Numeração de linhas ancorada ao modelo de layout da view (`modelToView2D`).
* **Motor Declarativo de Formulários (`RenderizadorFormularioSwing`):** Geração dinâmica de formulários a partir de estruturas `ESTRUTURA`, mapeamento automático de widgets (campos de texto, senhas, seletores de cor, seletores de arquivos, sliders, spinners, combos, radios, tabelas dinâmicas para `FATIA[Estrutura]`, listas de seleção múltipla) e exportação de dados para PDF/XLSX/DOCX.
* **Painel de Configuração de JVM (`DialogoConfiguracaoJvm` e `DetectorJvm`):** Detecção automática de JVMs instaladas no sistema (Scoop, SDKMAN, Program Files, JAVA_HOME), validação de executáveis e persistência.
* **Persistência de Ambiente (`ConfiguracaoDesktop`):** Armazenamento em JSON (`~/.thz/desktop-config.json`) de tema, modo estrito, dimensões e histórico de arquivos recentes.
* **Galeria de Exemplos (`GaleriaExemplos`):** Varredura e carregamento instantâneo de programas de exemplo no editor com tooltips explicativos.

---

## 5. Inventário da Suíte de Testes (68 Testes JUnit 5)

```
src/test/java/thz/lang/
├── ParidadeTest.java                 [8 testes]  (Lexer, Parser, Datas Hinnant, Exemplos Canônicos, Fmt Idempotente, BOM UTF-8, Galeria)
├── DecimalMonetarioTest.java        [10 testes]  (Aritmética Half-Even, Decimais Exatos, Monetário ISO-4217, Escalares)
├── ContratosInvariantesTest.java     [4 testes]  (EXIGE, GARANTE, INVARIANTE de Estrutura e Invariantes Pós-Mutação)
├── DocumentosTest.java               [5 testes]  (Geração de PDF, XLSX, DOCX, Stdlib e Fachada Unificada)
├── FormularioGuiTest.java            [5 testes]  (Renderização Swing, Binding de Contratos, Widgets e Showcase)
├── GovernancaTest.java               [4 testes]  (Matriz RASTREIO, Conformidade SOX/PCI, Relatórios Markdown/JSON)
├── GuiPaletaTest.java                [4 testes]  (Cobertura de 63 TokenTypes nas Paletas Dark/Light e Ciclo Swing)
├── IdempotenciaTest.java             [5 testes]  (RegistroIdempotencia, Auditoria G4, Interpretador e IR Idempotentes)
├── InterpretadorTest.java            [4 testes]  (Laços SE/ENQUANTO/PARA, Fatias, Stdlib TEXTO/MATEMATICA)
├── IrSimdTest.java                   [4 testes]  (Emissão THZ-IR/1, Validador SIMD R1-R5 e Emissão LLVM IR)
├── BlocoMemoriaTest.java             [4 testes]  (Alocação Sequencial Contígua, Descarte O(1) e Limites de Bloco)
├── ConfiguracaoDesktopTest.java      [4 testes]  (Serialização/Desserialização JSON, Histórico Recente e Resiliência)
├── DetectorJvmTest.java              [5 testes]  (Detecção de JVMs no SO, Validação e Persistência de Caminho)
└── docgen/DocGenTest.java            [2 testes]  (Geração de Markdown com Diagramas Mermaid)
```

---

## 6. Problemas Conhecidos e Decisões de Design

| Item | Situação / Decisão |
|---|---|
| `RESULTADO[T,E]` sem campos diretos | **Por Design:** Paridade estrita com o motor TS — formatado como `SUCESSO(...)` ou `FALHA(...)` via `EXIBA`. |
| Acesso indexado em cadeia (`lote[i].campo`) | **Por Design:** Requer variável intermediária (`VARIAVEL item <- lote[i]; item.campo`). |
| Injeção de `LOTE` demo na CLI | **Por Design:** Operações que recebem `FATIA[Estrutura]` recebem lote canônico populado pela CLI/GUI com validação de invariantes. |
| GraalVM Native Image | **Operacional:** Requer GraalVM JDK 25 e ferramentas de compilação C/C++ instaladas. |




