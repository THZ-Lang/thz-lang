# TODO — THZ-LANG JVM

* [x] Fundações — pom.xml Java 25 + JUnit5, esqueleto de pacotes
* [x] Fase 1 — Léxico (TokenType, PalavrasReservadas, ThzLexer) + tolerância a BOM
* [x] Fase 2 — AST + Parser (sealed records, textoCanonicoDe)
* [x] Fase 3 — Runtime (DecimalFixo half-even, Monetario ISO4217, DataThz/DataHoraThz Hinnant, BlocoMemoria)
* [x] Fase 4 — Semântica (Tipos, AnalisadorSemantico com contratos ∀ sobre FATIA + lint estrito)
* [x] Fase 5 — Interpretador (ValorThz sealed ×12, stdlib 28 fn, LER/VETORIZAR/PARA/bloco de memória)
* [x] Fase 6 — CLI (check/ast/fmt/run/repl/**gui**) + REPL multi-linha + Formatador idempotente
* [x] Fase 7 — Paridade JUnit (**69/69**)
* [x] GUI Swing: realce zero-dep (EditorThz+PaletaThz), FlatLaf dark/light, gutter alinhado ao View, marcação de erros
* [x] Coleção de exemplos `exemplos/colecao` (01–11) + índice README + galeria no menu Exemplos da IDE
* [x] Manifesto jar `Enable-Native-Access: ALL-UNNAMED` (sem warnings JDK25/FlatLaf)
* [x] Compilação nativa AOT GraalVM Native Image (`scripts/build-native.ps1`)
* [x] Empacotamento de distribuição via `jpackage` (`scripts/build-package.ps1` gerando pasta autônoma `dist/thz` com `thz.exe`, `thz-gui.exe` e `thz-desktop.exe`)
* [x] G4 Governança — `auditar()` + matriz RASTREIO→Regra→Contrato (`thz audit` / botão "🛡️ Auditoria" na IDE)
* [x] DocGen — Documentação Markdown com diagramas Mermaid (`thz doc` / botão "📘 Doc" na IDE)
* [x] G5 THZ-IR & SIMD — `baixarParaIr()` (`thz-ir/1`), emissão LLVM e validador SIMD R1–R5 (`thz ir` / botão "🧩 IR" na IDE)
* [x] IDE: ações Novo/Salvar Como, execução de OPERACAO parametrizada (lote demo / diálogo interativo) e polimento de temas
* [x] Persistência de Configuração Desktop: armazenamento em JSON (~/.thz/desktop-config.json) de tema, modo estrito, dimensões de janela, divisor e histórico de arquivos recentes
* [x] Menu de Configuração de JVM: diálogo modal de detecção/seleção/teste de JVMs locais (Scoop, SDKMAN, Program Files, JAVA_HOME) com persistência e diagnóstico
* [x] Idempotência Inteligente de Larga Escala: cláusulas `IDEMPOTENTE` e `CHAVE_IDEMPOTENCIA` integradas em todos os níveis
* [x] Motor de Exportação de Documentos Corporativos: geração de PDF, XLSX (Excel) e DOCX (Word) na Stdlib e GUI
* [x] Migração de Build System: Gradle canônico com Kotlin DSL (`build.gradle.kts`) e Gradle Wrapper

## Notas

- Versão alvo sobre a JVM, baseada no `thz-lang-engine` Node/TS.
- Build atual: **Gradle (Kotlin DSL com Gradle Wrapper)**.
  com receita java/application/shadow). Migração sem tocar no código-fonte.
- Histórico detalhado: ver **PROGRESSO.md**.




## Fase futura (fora do núcleo v2.3)

- LSP stdio + extensão VS Code (espelho do G3 TS)
- Playground web não se aplica à trilha JVM (substituído pela IDE Desktop Swing)

