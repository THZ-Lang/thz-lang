# PROJECT.md — THZ-LANG Engine JVM

## Identidade
- **Nome:** thz-lang-engine-jvm
- **Versão:** 2.3.0 (paridade com THZ-LANG Engine TS)
- **Stack:** Java 25 (records, sealed interfaces, pattern matching), Gradle (Kotlin DSL + Wrapper), JUnit 5

## Mapa de pacotes (`src/main/java/thz/lang/`)
- `lexico` — `TokenType`, `Token`, `CategoriaPalavra`, `PalavrasReservadas`, `ThzLexer`
- `sintatico` — `ThzParser`, `TextoCanonico`
- `ast` — `ProgramaAst`, `EstruturaAst`, `RegraNegocioAst`, `OperacaoAst`, `ProcedimentoAst` + nós `ExprAst`/`ComandoAst`
- `semantico` — `AnalisadorSemantico`, `TipoThz`, `CategoriaTipo`, `OpcoesAnalise`
- `runtime` — `DecimalFixo`, `ModoArredondamento`, `Monetario`, `DataThz`, `DataHoraThz`, `BlocoMemoria`
- `interpretador` — `InterpretadorThz`, `ValorThz` (sealed ×12), `Escopo`, `BibliotecaPadrao`
- `documento` — `GeradorPdf`, `GeradorXlsx`, `GeradorDocx`, `MotorDocumentos`
- `formato` — `Formatador`, `JsonEscritor`
- `diagnosticos` — `Diagnosticos`, `DiagnosticoEntrada`
- `governanca` — `AuditorGovernanca`, `RelatorioAuditoria` (G4)
- `docgen` — `ThzDocGen` (Markdown + diagramas Mermaid)
- `ir` — `IrPrograma`, `GeradorIr` (THZ-IR/1 & LLVM IR preliminar G5)
- `simd` — `ValidadorSimd`, `ResultadoValidacaoSimd` (Regras R1 a R5 G5)
- `cli` — `ThzCli` (comandos check/run/fmt/ast/audit/doc/ir/repl/gui)
- `repl` — `Repl` (shell interativo multi-linha)
- `gui` — `ThzGui`, `EditorThz`, `PaletaThz`, `Gutter`, `GaleriaExemplos` (IDE Desktop FlatLaf)
- `gui.barra` — `BarraMenuGui`, `BarraFerramentasGui`, `BarraStatusGui`
- `gui.execucao` — `ExecutorMotorGui`
- `gui.formulario` — `RenderizadorFormularioSwing`, `FabricaCamposFormulario`, `PainelTabelaFatia`, `ExportadorFormularioGui`
- `gui.config` — `ConfiguracaoDesktop`, `GerenciadorConfiguracao`, `DetectorJvm`, `DialogoConfiguracaoJvm`


## Invariantes
1. Aritmética decimal/monetária exata em BigInteger escalado (proibido ponto flutuante binário IEEE 754).
2. Gerenciamento por bloco de memória temporária (`BlocoMemoria`) com descarte automático ao final da execução.
3. Contratos formais (`EXIGE`, `GARANTE`, `INVARIANTE`) validados estática e dinamicamente.

