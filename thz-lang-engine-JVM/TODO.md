# TODO — THZ-LANG JVM25

* [x] Fundações — pom.xml Java 25 + JUnit5, esqueleto de pacotes
* [x] Fase 1 — Léxico (TokenType, PalavrasReservadas, ThzLexer) + tolerância a BOM
* [x] Fase 2 — AST + Parser (sealed records, textoCanonicoDe)
* [x] Fase 3 — Runtime (DecimalFixo half-even, Monetario ISO4217, DataThz/DataHoraThz Hinnant, ArenaMemoria)
* [x] Fase 4 — Semântica (Tipos, AnalisadorSemantico com contratos ∀ sobre FATIA + lint estrito)
* [x] Fase 5 — Interpretador (ValorThz sealed ×12, stdlib 28 fn, LER/VETORIZAR/PARA/arena)
* [x] Fase 6 — CLI (check/ast/fmt/run/repl/**gui**) + REPL multi-linha + Formatador idempotente
* [x] Fase 7 — Paridade JUnit (mvn verify **11/11**)
* [x] GUI Swing: realce zero-dep (EditorThz+PaletaThz), FlatLaf dark/light, gutter alinhado ao View, marcação de erros
* [x] Coleção de exemplos `exemplos/colecao` (01–10) + índice README + galeria no menu Exemplos da IDE
* [x] Manifesto jar `Enable-Native-Access: ALL-UNNAMED` (sem warnings JDK25/FlatLaf)

## Notas

- Versão alvo sobre a JVM25, baseada no `thz-lang-engine` Node/TS.
- Build atual: **Maven**. **Gradle marcado como alternativa futura** (`build.gradle.kts.placeholder`
  com receita java/application/shadow). Migração sem tocar no código-fonte.
- Histórico detalhado: ver **PROGRESSO.md**.

## Próximo (ordem proposta)

- [ ] G4 Governança — `auditar()` + matriz RASTREIO→Regra→Contrato (`thz audit`)
- [ ] G5 IR — `baixarParaIr()` (`thz-ir/1`) + SIMD R1–R5 (`verificarVetorizado`)
- [ ] docgen Markdown+Mermaid (`thz doc`) + botão "📘 Doc" na IDE
- [x] IDE: ações Novo/Salvar Como, execução de OPERACAO parametrizada (lote demo / diálogo interativo) e polimento de temas
- [ ] Empacotamento nativo via `jpackage` (Windows/Linux)


## Fase futura (fora do núcleo v2.3)

- LSP stdio + extensão VS Code (espelho do G3 TS)
- Playground web não se aplica à trilha JVM (substituído pela IDE Swing)
