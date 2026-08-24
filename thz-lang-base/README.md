# THZ-LANG Engine — v2.2 (Padrão Ouro G1-G6)

Linguagem corporativa em português estruturado com tipagem estática, *Design by Contract* e processamento de dados de alta performance — legibilidade de domínio + execução nativa.

> **Status:** Protótipo funcional Node.js 20+ · TypeScript 5+ · `tsx` · **149 testes verdes** · Alvo: compilador AOT Rust + Inkwell/LLVM 17+ (ELF/PE).

---

## Pilares

1. **Aritmética exata** — `DECIMAL(P,S)` e `MONETARIO(ISO)` via `BigInt` escalado (`DecimalFixo`, `Monetario` ISO 4217). Proibido `number` float para monetário (ISO/IEC 10967).
2. **Contratos formais** — `EXIGE`/`GARANTE`/`INVARIANTE` + `RESULTADO[T,E]`/`FALHAR_COM`, validados em tempo de execução e no `AnalisadorSemantico --estrito`.
3. **Alta performance** — `LAYOUT_COLUNAR` (SoA), `VETORIZAR_PARA ... PASSO_SIMD`, `USAR_BLOCO_MEMORIA ARENA_EPHEMERAL` (descarte O(1)), ponte para THZ-IR/LLVM.
4. **Arquitetura viva** — `METADADOS_ARQUITETURA` (ISO/IEC/IEEE 42010) → Markdown + Mermaid (`ThzDocGen`), matriz de governança `RASTREIO_REQUISITO → Regra → Contrato`.

## Exemplos canônicos

- `exemplos/faturamento.thz` — lote vetorizado com `LAYOUT_COLUNAR`, `INVARIANTE` e `VETORIZAR_PARA PASSO_SIMD 8`.
- `exemplos/pedidos.thz` — expressividade DDD: `ENUMERACAO`, `INVARIANTE` de estrutura, `RESULTADO[StatusPedido,TEXTO]` + `FALHAR_COM`.

## Quick start

```bash
npm install
npm test                          # 149 testes (Node test runner)
npm run thz:check                 # léxico + sintaxe + semântica
npm run thz:check -- --estrito    # + lint pragma/rastreio/SLO/contratos
npm run thz:run                   # executa faturamento.thz (Arena + ponto fixo)
npm run thz:repl                  # REPL multi-linha (.ajuda, .codigo, .limpar, .sair)
```

## CLI — `thz <comando> [arquivo] [opções]`

| Comando | Descrição | Opções |
|---|---|---|
| `check` | Valida léxico/sintaxe/semântica e imprime caret em erros | `--estrito` |
| `ast` | Imprime AST em JSON (BigInt → string) | |
| `doc` | Gera `docs/<Programa>_arquitetura.md` (Markdown + Mermaid) | |
| `audit` | Matriz `RASTREIO → Regra → Contrato`; cobertura contratual | `--json`, `--saida <path>`, `--estrito` |
| `ir` | Emite THZ-IR `thz-ir/1` (JSON) ou LLVM IR | `--llvm`, `--saida <path>` |
| `fmt` | Formatador canônico idempotente | `--check`, `--escrever`/`-w`, `--saida <path>` |
| `run` | Executa a primeira `OPERACAO` com corpo `INICIO…FIM` | |
| `repl` | REPL interativo | |

> `fmt` descarta comentários `#` (AST sem trivia) — é estável (`fmt(fmt(x))==fmt(x)`) e preserva semântica.
> Ordem de flags é livre: `thz fmt --saida out.thz arquivo.thz` e `thz fmt arquivo.thz --saida out.thz` são equivalentes.

```bash
npx tsx src/cli.ts check exemplos/faturamento.thz --estrito
npx tsx src/cli.ts audit exemplos/faturamento.thz --json --saida out.json
npx tsx src/cli.ts ir exemplos/faturamento.thz --llvm --saida out.ll
npx tsx src/cli.ts fmt exemplos/faturamento.thz --check
npx tsx src/cli.ts fmt exemplos/faturamento.thz --escrever
```

Atalhos `package.json`: `thz:check`, `thz:run`, `thz:repl`, `thz:doc`, `thz:audit`, `thz:ir`, `thz:ir:llvm`, `fmt`, `fmt:check`, `bench`, `playground`, `lsp`, `extension:compile`.

## Language Service, Playground, LSP e VS Code

| Camada | Local | Comando |
|---|---|---|
| **G1 Language Service Core** | `src/language-service.ts` — `analisar()`, `obterHover()`, `auditarFonte()`, `baixarIrFonte()`, `formatarFonte()` | — |
| **G2 Playground Web** | `playground/` (Vite + Monaco + Monarch `thz-monarch.ts`) executa no browser via `InterpretadorThz` + `ArenaMemoria` | `npm run playground` / `playground:build` / `playground:preview` |
| **G3 LSP + extensão** | `src/lsp/server.ts` (stdio, diagnostics/hover/symbols/completion/definition/formatting) + `extension/` (TextMate, `language-configuration.json`) | `npm run lsp` / `npm run extension:compile` / `npm run extension:package` |
| **G4 Governança** | `src/governanca.ts` — CLI `thz audit`, LSP `thz/audit`, VS Code `THZ: Mostrar Auditoria`, Playground `🛡️ Audit` | — |
| **G5 THZ-IR + SIMD** | `src/ir.ts` (`thz-ir/1`, `baixarParaIr`, `emitirLlvm`) + `src/simd.ts` (R1-R5, `verificarVetorizado`) — CLI `thz ir`, LSP `thz/ir`/`thz/llvm`, Playground `🧩 IR`/`⚡ LLVM` | — |
| **G6 Bench + fmt** | `src/fmt.ts` + `bench/` (`decimal/fatia/simd`, `bench/run.ts`) — `npm run bench` | — |

Playground expõe: `▶ Executar`, `🔍 Check`, `🔎 Estrito`, `📄 Doc`, `🛡️ Audit`, `🧩 IR`, `⚡ LLVM`, `✨ Fmt` (Ctrl+S).

## Benchmarks

```bash
npm run bench              # suite completa (≈5 s)
npm run bench:decimal      # BigInt escalado vs Number (baseline não-exata)
npm run bench:fatia        # SoA scan vs AoS + Arena O(1)
npm run bench:simd         # VETORIZAR_PARA N=100/1k/10k + PASSO 4/8/16
```

## Estrutura do projeto

```
src/
  keywords.ts          fonte da verdade léxica (proibido literal fora daqui)
  types.ts             TokenType, ProgramaAST, EstuturaAST, RegraNegocioAST…
  lexer.ts             léxico determinístico com linha/coluna
  parser.ts            sintático → AST (precedência, contratos como árvore)
  analisador.ts        verificação de tipos, escopos e lint --estrito
  errors.ts            Diagnostico com caret (formatarDiagnosticos)
  runtime.ts           DecimalFixo, Monetario, ArenaMemoria
  interpretador.ts     tree-walking + EXIGE/GARANTE/INVARIANTE/RESULTADO
  docgen.ts            ThzDocGen (Markdown + Mermaid)
  language-service.ts  pipeline único analisar() base do Playground/LSP
  governanca.ts        auditar() + gerarMarkdownGovernanca()
  ir.ts                THZ-IR thz-ir/1 + emitirLlvm()
  simd.ts              regras R1-R5 de VETORIZAR_PARA
  fmt.ts               formatar() canônico idempotente
  lsp/server.ts        servidor LSP stdio
  cli.ts               thz <check|ast|doc|audit|ir|fmt|run|repl>
  repl.ts              REPL multi-linha
playground/            Vite + Monaco + Monarch
extension/             TextMate + LanguageClient (VS Code)
bench/                 decimal/fatia/simd + helpers.ts/run.ts
test/                  14 suites, golden snapshots da AST (__snapshots__)
exemplos/              faturamento.thz, pedidos.thz
docs/                  GRAMATICA.md (EBNF), *_arquitetura.md
```

## Gramática e política de palavras reservadas

- EBNF canônica em `docs/GRAMATICA.md` (v2.2) — qualquer nova construção deve atualizar gramática + golden tests + docgen.
- Todas as keywords vivem em `src/keywords.ts`; proibido reconhecer keyword por literal em `lexer.ts`/`parser.ts`/`interpretador.ts`/`docgen.ts`.
- Erros reportam `[Erro Sintático][Linha L:C]` com trecho + caret via `src/errors.ts`.

## Desenvolvimento

```bash
npm test
npx tsc --noEmit
npx tsc -p extension/tsconfig.json --noEmit
npm run playground:build
npm run lsp:build && npm run extension:compile
npm run bench
npx tsx src/cli.ts fmt exemplos/faturamento.thz --check
```

Compatível Windows PowerShell 5.1 e Arch Linux (sem `head`/`cd` em workdir via parâmetro).

## Roadmap

Fases 1-3 (gramática/runtime/doc), Fase 4-5 (v2.2, DDD), **G1-G6 Padrão Ouro concluídos**. Próxima: **Fase 7** — Fatias Zero-Copy (Arrow IPC), `thz.pest` e codegen Rust + Inkwell/LLVM 17+.

## Licença

Sem licença publicada — uso interno / protótipo.
