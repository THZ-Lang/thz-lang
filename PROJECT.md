\# THZ-LANG: Enterprise Architecture \& Data Systems Language



Linguagem de programação corporativa orientada a domínio (DDD), contratos formais de governança e alta taxa de transferência de dados, projetada para unir a legibilidade do português estruturado com a performance de execução em código de máquina nativo.



```

┌────────────────────────────────────────────────────────────────────────┐

│                              THZ-LANG                                  │

│        (Enterprise Architecture \& High-Performance Data Engine)        │

└───────┬────────────────────────┬───────────────────────────────┬───────┘

&#x20;       │                        │                               │

&#x20;       ▼                        ▼                               ▼

┌───────────────┐        ┌───────────────┐               ┌───────────────┐

│   GOVERNANÇA  │        │  ARQUITETURA  │               │ ALTA EFICIÊNCIA│

│  E CONTRATOS  │        │   E METADADOS │               │    DE DADOS   │

├───────────────┤        ├───────────────┤               ├───────────────┤

│ • DDD Nativo  │        │ • Doc Viva    │               │ • SIMD / AVX  │

│ • Tipos Dec/Mo│        │ • C4 / Arch   │               │ • Layout SoA  │

│ • ISO 4217    │        │ • OpenAPI/Spec│               │ • Zero-Copy   │

│ • Invariantes │        │ • Rastreio Req│               │ • Arenas Mem  │

└───────────────┘        └───────────────┘               └───────────────┘

```



\---



\## 1. Pilares da Linguagem



1\. \*\*Aritmética Exata de Domínio:\*\* Suporte nativo a `DECIMAL(P, S)` e `MONETARIO(ISO)` sem imprecisões binárias (ISO/IEC 10967 / ISO 4217).

2\. \*\*Contratos Formais e Invariantes:\*\* Cláusulas de pré-condição (`EXIGE`) e pós-condição (`GARANTE`) integradas à semântica da linguagem.

3\. \*\*Engenharia Orientada a Dados (DoD):\*\* Modificador `LAYOUT\_COLUNAR` (\*Structure of Arrays\*), loops vetorizados (`VETORIZAR\_PARA ... PASSO\_SIMD`) e gerenciamento de memória em blocos contíguos (`USAR\_BLOCO\_MEMORIA ARENA\_EPHEMERAL`).

4\. \*\*Arquitetura Viva:\*\* Extração direta de metadados arquiteturais (ISO/IEC/IEEE 42010), diagramas C4 e Mermaid a partir da AST.



\---



\## 2. Código Canônico de Referência (`exemplos/faturamento.thz`)



```thz
VERSAO_LINGUAGEM "2.2"

PROGRAMA ProcessamentoFaturamentoLote

METADADOS_ARQUITETURA
    DOMINIO: "LogisticaEFaturamento"
    SUBDOMINIO: "FaturamentoLote"
    CAMADA: "Dominio"
    VERSAO: "2.2.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "15ms"
    CONFORMIDADE: "SOX-404", "LGPD-Art7"
FIM_METADADOS

ESTRUTURA ItemFatura LAYOUT_COLUNAR
    id_transacao        : UUID
    codigo_produto      : TEXTO
    quantidade          : NATURAL32
    valor_unitario      : DECIMAL(12, 4)
    aliquota_imposto    : DECIMAL(5, 2)
    valor_total_liquido : DECIMAL(14, 4)
    INVARIANTE valor_total_liquido >= 0.0000
FIM_ESTRUTURA

REGRA_NEGOCIO CalculoTributarioLote
    IDENTIFICADOR_REGRA: "BR-FISCAL-2026-08"
    RASTREIO_REQUISITO: "REQ-FISCAL-9102"
    DESCRICAO: "Aplica isenção para insumos essenciais e calcula ICMS/PIS/COFINS em lote vetorizado."

    CONTRATO_ENTRADA
        EXIGE itens.quantidade > 0
        EXIGE itens.valor_unitario >= 0.0000
    FIM_CONTRATO_ENTRADA

    CONTRATO_SAIDA
        GARANTE itens.valor_total_liquido >= 0.0000
    FIM_CONTRATO_SAIDA

    OPERACAO ProcessarVetorizado(itens: FATIA[ItemFatura]) : DECIMAL(18, 4)
    INICIO
        VARIAVEL acumulador_tributos : DECIMAL(18, 4) <- 0.0000
        VETORIZAR_PARA item EM itens PASSO_SIMD 8
            VARIAVEL bruto          : DECIMAL(18, 4) <- item.quantidade * item.valor_unitario
            VARIAVEL fator_aliquota : DECIMAL(18, 4) <- item.aliquota_imposto / 100
            VARIAVEL imposto_item   : DECIMAL(18, 4) <- bruto * fator_aliquota
            item.valor_total_liquido <- bruto + imposto_item
            acumulador_tributos <- acumulador_tributos + imposto_item
            EXIBA "[ITEM " + item.codigo_produto + "] Qtd: " + item.quantidade
        FIM_PARA
        RETORNE acumulador_tributos
    FIM
FIM_REGRA_NEGOCIO

FIM_PROGRAMA
```



\---



\## 3. Comandos do Ecossistema CLI



```bash
npm run thz:check                 # léxico + sintaxe + semântica
npm run thz:check -- --estrito    # + lint estrito (pragma/rastreio/SLO/contratos)
npm run thz:run                   # primeira OPERACAO com INICIO…FIM (Arena + ponto fixo)
npm run thz:repl                  # REPL multi-linha (.ajuda, .codigo, .limpar, .sair)
npm run thz:doc                   # docs/<Programa>_arquitetura.md (Markdown + Mermaid)
npm run thz:audit                 # matriz RASTREIO→Regra→Contrato
npm run thz:audit -- --json       # JSON + --saida <path> --estrito
npm run thz:ir                    # THZ-IR thz-ir/1 (JSON)
npm run thz:ir:llvm               # LLVM IR (--llvm --saida out.ll)
npm run fmt -- --check            # checa formatação canônica (CI)
npm run fmt -- --escrever         # reescreve canônico (descarta comentários #)
npm run bench                     # suite completa (~5 s)
npm run playground                # Vite + Monaco (http://localhost:5173)
npm run lsp                       # LSP stdio (dist/lsp/server.js --stdio)
```

> `thz fmt` é idempotente e preserva semântica; `thz audit/ir/fmt --saida` aceita argumentos em qualquer ordem.

<details><summary>CLI direto (sem npm)</summary>

```bash
npx tsx src/cli.ts check  exemplos/faturamento.thz --estrito
npx tsx src/cli.ts audit  exemplos/faturamento.thz --json --saida out.json
npx tsx src/cli.ts ir     exemplos/faturamento.thz --llvm --saida out.ll
npx tsx src/cli.ts fmt    exemplos/faturamento.thz --check
npx tsx src/cli.ts doc    exemplos/faturamento.thz
npx tsx src/cli.ts run    exemplos/faturamento.thz
```
</details>
```



\---



\## 4. Roadmap de Evolução Técnica



- [x] **Fase 1:** Prototipagem da gramática, Lexer, Parser AST e Runtime em TypeScript.
- [x] **Fase 2:** Aritmética decimal de ponto fixo via `BigInt` e emulação de Arena de Memória.
- [x] **Fase 3:** Geração de documentação viva da AST (Mermaid + Markdown).
- [x] **Fase 4 (v2.2):** Expressões com precedência, comandos executáveis, contratos como árvore, interpretador real, análise semântica e lint `--estrito`.
- [x] **Fase 5 (v2.2):** Expressividade DDD — `ENUMERACAO`, `RESULTADO[T,E]` + `FALHAR_COM`, `INVARIANTE` em `ESTRUTURA`; golden snapshots da AST.
- [x] **Fase 6 (G1):** Language Service Core — src/language-service.ts (analyze, símbolos, hover, diagnósticos com caret).
- [x] **Fase 6 (G2):** Playground Web — playground/ (Monaco + Monarch + Language Service + Runtime no browser).
- [x] **Fase 6 (G3):** LSP + Extensão VS Code — src/lsp/server.ts + Extensions/thz-lsp-vscode/ (hover, diagnósticos, símbolos, completion, go-to-definition).
- [x] **Fase 6 (G4):** Governança Auditável — `src/governanca.ts` + `thz audit` (CLI), LSP `thz/audit` e Playground `🛡️ Audit`.
- [x] **Fase 6 (G5):** THZ-IR + Semântica SIMD Formal — `src/ir.ts` + `src/simd.ts` (IR `thz-ir/1`, verificação R1-R5, `thz ir` com `--llvm`).
- [x] **Fase 6 (G6):** Bench + fmt — `src/fmt.ts` (`thz fmt`) + `bench/` (decimal/fatias/SIMD) — formatador canônico e benchmarks comparativos.
- [ ] **Fase 7:** Fatias Zero-Copy (Arrow IPC), port para `thz.pest` e codegen nativo Rust + Inkwell (LLVM 17+).

---

## 5. Trilha Padrão Ouro (pós-v2.2) — Marcos G1 a G6

Prioridade aprovada: **expressividade DDD primeiro**, tooling em seguida.

| Marco | Entrega | Status |
|---|---|---|
| **G1 — Language Service Core** | API única sobre lexer/parser/analisador (`analyze(source)`), diagnósticos com caret, símbolos e hover de tipos | ✅ Concluído |
| **G2 — Playground Web** | Editor Monaco com gramática THZ, execução no browser via interpretador TS, exemplos carregáveis | ✅ Concluído |
| **G3 — LSP + Extensão VS Code** | Servidor LSP alimentado pelo G1; publicação da extensão | ✅ Concluído |
| **G4 — Governança Auditável** | Matriz `RASTREIO_REQUISITO → Regra → Contrato → Teste golden`; relatório de cobertura contratual | ✅ Concluído |
| **G5 — THZ-IR + Semântica SIMD Formal** | IR intermediário estável, regras de vetorização verificáveis, ponte para LLVM | ✅ Concluído |
| **G6 — Bench + fmt** | Benchmarks comparativos (decimal/fatias/SIMD) e formatador canônico `thz fmt` | ✅ Concluído |

### Estado consolidado v2.2

- Gramática EBNF canônica em `docs/GRAMATICA.md`; palavras reservadas centralizadas em `src/keywords.ts` (política estrita).
- Interpretador tree-walking completo com contratos EXIGE/GARANTE (quantificador universal sobre fatias).
- Aritmética exata: `DecimalFixo(P,S)` paramétrico (meio-par bancário padrão) e `Monetario` ISO 4217 sem mistura de moedas.
- Análise semântica com verificação de tipos e lint `--estrito` (pragma, rastreabilidade, SLO e contratos obrigatórios).
- Diagnósticos com trecho de fonte e caret (`src/errors.ts`); REPL interativo (`npm run thz:repl`); golden tests da AST.
- Suíte atual: **149 testes verdes** (`npm test`) — incluindo `test/language-service.test.ts` + `test/governanca.test.ts` + `test/ir.test.ts` + `test/simd.test.ts` + `test/fmt.test.ts` (G1/G4/G5/G6).
- Language Service Core: `src/language-service.ts` expõe `analisar()`, `obterHover()`, `posicaoParaOffset/offsetParaPosicao` e `tokenNoCursor`; base para Playground (G2) e LSP (G3).
- Playground Web: `playground/` (Vite + Monaco + Monarch) com execução browser via `InterpretadorThz` e `ArenaMemoria` (`npm run playground`); agora com botões `🛡️ Audit`, `🧩 IR`/`⚡ LLVM` e `✨ Fmt` (Ctrl+S).
- LSP + VS Code: `src/lsp/server.ts` (stdio, diagnostics/hover/symbols/completion/definition/formatting) + extensão em `Extensions/thz-lsp-vscode/` (TextMate, LanguageClient) — `npm run lsp` / `npm run extension:compile`.
- Governança Auditável: `src/governanca.ts` (`auditar()`, `gerarMarkdownGovernanca()`) — matriz `RASTREIO_REQUISITO → Regra → Contrato`; CLI `thz audit` + LSP `thz/audit` + Playground `🛡️ Audit`.
- THZ-IR + SIMD Formal: `src/ir.ts` (`VERSAO_IR='thz-ir/1'`, `baixarParaIr()`, `emitirLlvm()`) + `src/simd.ts` (R1-R5, `verificarVetorizado()`) — CLI `thz ir` (`--llvm`, `--saida`), LSP `thz/ir`/`thz/llvm`, Playground `🧩 IR`/`⚡ LLVM`.
- Bench + fmt: `src/fmt.ts` (`formatar()` canônico, idempotente, preserva semântica; comentários `#` não preservados — AST sem trivia) — CLI `thz fmt` (`--check`, `--escrever`, `--saida`), LSP `textDocument/formatting`, Playground `✨ Fmt` + `bench/` (decimal BigInt vs Number, SoA/AoS/Arena, SIMD `VETORIZAR_PARA` N=100/1k/10k e `PASSO_SIMD` 4/8/16) — `npm run bench`.