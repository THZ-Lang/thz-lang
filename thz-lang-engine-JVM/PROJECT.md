# PROJECT.md — THZ-LANG Engine JVM

## Identidade
- **Nome:** THZ-LANG Engine JVM (`thz-lang-engine-jvm`)
- **Versão da Linguagem:** 2.3.0 (paridade estrita com THZ-LANG Engine TS)
- **Stack Tecnológica:** Java 25 (records selados, pattern matching, APIs modernas), Gradle 9.7 (Kotlin DSL + Wrapper autônomo), FlatLaf 3.5 (UI moderna), Apache POI 5.3 (XLSX/DOCX), OpenPDF 2.0 (PDF), JUnit 5.11 (testes unitários e de integração).

---

## Mapa de Pacotes (`src/main/java/thz/lang/`)

| Pacote | Classes Principais | Responsabilidade Arquitetural |
|---|---|---|
| `ast` | `ProgramaAst`, `EstruturaAst`, `RegraNegocioAst`, `OperacaoAst`, `ProcedimentoAst`, `ExprAst`, `ComandoAst`, `EnumeracaoAst`, `InvarianteAst`, `MetadadosArquiteturaAst` | Definição da Árvore de Sintaxe Abstrata como sealed hierarchies e records imutáveis. |
| `lexico` | `ThzLexer`, `Token`, `TokenType`, `PalavrasReservadas`, `CategoriaPalavra`, `ErroLexico` | Scanner léxico determinístico com preservação de linha/coluna, 63 tokens e tolerância a BOM UTF-8. |
| `sintatico` | `ThzParser` | Parser recursivo descendente com precedência canônica de operadores e reconstrução de expressões. |
| `semantico` | `AnalisadorSemantico`, `Tipos`, `TipoThz`, `CategoriaTipo`, `AssinaturasStdlib`, `EscopoTipos`, `OpcoesAnalise`, `ErroSemantico` | Análise de tipos estáticos, escopos léxicos, validação de contratos universais $\forall$ sobre `FATIA` e lint estrito. |
| `runtime` | `DecimalFixo`, `Monetario`, `ModoArredondamento`, `DataThz`, `DataHoraThz`, `BlocoMemoria`, `RegistroIdempotencia`, `ErroDecimal`, `ErroMonetario`, `ErroData` | Runtime determinístico: aritmética de ponto fixo sem float (ISO/IEC 10967), datas Hinnant (ISO-8601), bloco contíguo de memória e memoização LRU/TTL. |
| `interpretador` | `InterpretadorThz`, `BibliotecaPadrao`, `Escopo`, `ValorThz`, `InjetorLoteDemo`, `SinalRetorne`, `SinalFalhar`, `ErroContrato`, `ErroExecucao` | Interpretador tree-walking com execução de contratos em runtime, tratamento de sinais e stdlib (`TEXTO`, `MATEMATICA`, `DATA`, `FATIA`, `DOCUMENTO`, `TELA`). |
| `documento` | `MotorDocumentos`, `GeradorPdf`, `GeradorXlsx`, `GeradorDocx` | Subsistema de emissão institucional de relatórios corporativos em PDF, planilhas Excel e documentos Word a partir de dados estruturados. |
| `governanca` | `AuditorGovernanca`, `RelatorioAuditoria` | Auditoria formal de Governança G4, matriz de rastreabilidade `RASTREIO_REQUISITO → Regra → Contrato`, SLOs e conformidade regulatória. |
| `docgen` | `ThzDocGen` | Geração de documentação técnica em Markdown enriquecida com diagramas de classes e diagramas de fluxo Mermaid.js. |
| `ir` | `IrPrograma`, `GeradorIr` | Emissão de representação intermediária `thz-ir/1` e geração de LLVM IR textual preliminar. |
| `simd` | `ValidadorSimd`, `ResultadoValidacaoSimd` | Validação formal de laços vetorizáveis (`VETORIZAR_PARA`) sob as regras R1 a R5 (SoA, contiguidade, pureza operacional, passo SIMD). |
| `formato` | `Formatador`, `JsonEscritor` | Formatador canônico idempotente e serializador JSON determinístico para árvores AST e relatórios. |
| `diagnosticos` | `Diagnosticos`, `DiagnosticoEntrada` | Renderizador unificado de diagnósticos com trechos de código e apontador visual de caret (`[Linha L:C]`). |
| `cli` | `ThzCli` | Ponto de entrada de linha de comando com subcomandos `check`, `run`, `fmt`, `ast`, `audit`, `doc`, `ir`, `repl` e `gui`. |
| `repl` | `Repl` | Shell interativo REPL com suporte a entrada multi-linha, buffer e comandos `.ajuda`, `.codigo`, `.limpar` e `.sair`. |
| `gui` | `ThzGui`, `EditorThz`, `PaletaThz`, `Gutter`, `GaleriaExemplos` | IDE Desktop moderna em FlatLaf (Dark/Light) com realce léxico em tempo real, numeração de linhas por view, abas de saída e galeria de exemplos integrada. |
| `gui.barra` | `BarraMenuGui`, `BarraFerramentasGui`, `BarraStatusGui` | Componentes modulares da interface gráfica (menus de ações, barra de ferramentas e barra de status com métricas e JVM ativa). |
| `gui.execucao` | `ExecutorMotorGui` | Trabalhador assíncrono para execução sem travamento da UI de checagens, execução, formatação, auditoria, docgen e IR. |
| `gui.formulario` | `RenderizadorFormularioSwing`, `FabricaCamposFormulario`, `PainelTabelaFatia`, `ExportadorFormularioGui` | Motor declarativo de telas desktop a partir de estruturas `ESTRUTURA`, mapeamento automático de widgets e exportação para arquivos. |
| `gui.config` | `ConfiguracaoDesktop`, `GerenciadorConfiguracao`, `DetectorJvm`, `DialogoConfiguracaoJvm` | Persistência de configurações desktop (`~/.thz/desktop-config.json`) e detecção automática de JVMs instaladas no sistema operacional. |

---

## Invariantes e Pilares Técnicos

1. **Aritmética Exata (ISO/IEC 10967):** Uso estrito de `BigInteger` escalado em `DecimalFixo` e `Monetario`. Proibição absoluta de ponto flutuante binário IEEE 754.
2. **Gerenciamento por Bloco de Memória Temporária:** Descarte instantâneo e contíguo de blocos de memória temporária (`BlocoMemoria`) em $O(1)$ sem pressão sobre o GC.
3. **Contratos Formais e Arquitetura Viva:** Cláusulas `EXIGE`, `GARANTE`, `INVARIANTE` e metadados arquiteturais obrigatórios validados estática e dinamicamente.
4. **Idempotência Inteligente:** Cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` integradas ao analisador semântico, interpretador, auditoria G4 e tooling.
5. **Build Canônico:** Construção, testes e empacotamento unificados via Gradle (`build.gradle.kts` / `./gradlew`).

