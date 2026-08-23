# PROJECT.md — THZ-LANG Engine JVM

## Identidade
- **Nome:** thz-lang-engine-jvm
- **Versão:** 2.3.0 (paridade com THZ-LANG Engine TS)
- **Stack:** Java 25 (records, sealed interfaces, pattern matching), Maven 3.9, JUnit 5
- **Build alternativo marcado:** Gradle (wrapper + build.gradle.kts com `java`/`application`/`shadow`) — migração drop-in, sem tocar src.

## Mapa de pacotes (`src/main/java/thz/lang/`)
- `lexico` — `TokenType`, `Token`, `CategoriaPalavra`, `PalavrasReservadas`, `ThzLexer`
- `sintatico` — `ThzParser`, `TextoCanonico`
- `ast` — `ProgramaAst`, `EstruturaAst`, `RegraNegocioAst`, `OperacaoAst`, `ProcedimentoAst` + nós `ExprAst`/`ComandoAst`
- `semantico` — `AnalisadorSemantico`, `TipoThz`, `CategoriaTipo`
- `runtime` — `DecimalFixo`, `ModoArredondamento`, `Monetario`, `DataThz`, `DataHoraThz`, `ArenaMemoria`
- `interpretador` — `InterpretadorThz`, `ValorThz` (sealed), `Escopo`, `BibliotecaPadrao`
- `formato` — `Formatador`, `JsonEscritor`
- `diagnosticos` — `Diagnosticos`
- `cli` — `ThzCli`
- `repl` — `Repl`

## Invariantes (mesmos do TS)
1. Decimal em BigInteger escalado (proibido double para dinheiro).
2. Arena O(1), descarte contíguo.
3. Contratos EXIGE/GARANTE validados em tempo de execução.
