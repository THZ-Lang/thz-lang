\# AGENTS.md — Diretrizes de Engenharia e Operação para Agentes de IA



Este documento define o contexto técnico, restrições arquiteturais e diretrizes de desenvolvimento para agentes de IA operando no repositório \*\*THZ-LANG Engine\*\*.



\---



\## 1. Visão Geral e Identidade do Projeto

\* \*\*Nome do Projeto:\*\* THZ-LANG (`.thz`)

\* \*\*Paradigma:\*\* Linguagem Corporativa de Sistemas, Governança de Negócio, Arquitetura Viva e Processamento de Dados de Alta Performance.

\* \*\*Sintaxe:\*\* Estruturada em língua portuguesa com tipagem estática e \*Design by Contract\*.

\* \*\*Status Atual:\*\* Protótipo funcional em \*\*Node.js (v20+) + TypeScript (v5+)\*\* com transpilador `tsx`.

\* \*\*Alvo de Produção:\*\* Compilador AOT nativo em \*\*Rust + Inkwell / LLVM 17+\*\* gerando binários estáticos (ELF no Linux e PE no Windows).



\---



\## 2. Invariantes Técnicos e Normas Obrigatórias



1\. \*\*Aritmética Financeira e Decimais (ISO/IEC 10967):\*\*

&#x20;  \* É terminantemente proibido o uso de ponto flutuante IEEE 754 binário (`number` float) para operações monetárias e fiscais.

&#x20;  \* Toda aritmética decimal deve utilizar inteiros escalados com `BigInt` (classe `DecimalFixo` no runtime TypeScript) ou inteiros de 128 bits (`i128`) no codegen LLVM.



2\. \*\*Gerenciamento de Memória e Estrutura de Dados:\*\*

&#x20;  \* Processamento em lote deve respeitar alocação em Arena (`ArenaMemoria` / `ArrayBuffer`), permitindo descarte de memória contígua em $O(1)$.

&#x20;  \* Estruturas com modificador `LAYOUT\_COLUNAR` operam sob modelo \*Structure of Arrays\* (SoA) para viabilizar vetorização SIMD (AVX2/AVX-512).



3\. \*\*Arquitetura Viva e Contratos Formais (ISO/IEC/IEEE 42010 \& ISO/IEC TR 24772):\*\*

&#x20;  \* O bloco `METADADOS\_ARQUITETURA` é obrigatório em programas corporativos.

&#x20;  \* Cláusulas `EXIGE` (pré-condições) e `GARANTE` (pós-condições) devem ser validadas em tempo de execução/compilação, gerando falhas explícitas em caso de violação.



\---



\## 3. Mapa de Arquivos do Projeto (`Node/thz-lang-base/`)

\* `src/keywords.ts`: Fonte da verdade léxica — todas as palavras reservadas (proibido literal fora daqui).
\* `src/types.ts`: Tokens (`TokenType`), AST (`ProgramaAST`, `EstruturaAST`, `RegraNegocioAST`, `OperacaoAST`, `ComandoAST`, `ExprAST`) e nós de governança.
\* `src/lexer.ts`: Léxico determinístico com linha/coluna.
\* `src/parser.ts`: Sintático → AST (precedência, contratos como árvore, `textoCanonicoDe`/`formatarEscalado`).
\* `src/analisador.ts`: `AnalisadorSemantico` — tipos, escopos, lint `--estrito` (pragma, rastreio, SLO, contratos).
\* `src/errors.ts`: `formatarDiagnosticos`/`formatarErroComCaret` — trecho + caret `[Linha L:C]`.
\* `src/runtime.ts`: `DecimalFixo`/`Monetario` (`BigInt` escalado) + `ArenaMemoria` (O(1)).
\* `src/interpretador.ts`: Tree-walking + validação `EXIGE`/`GARANTE`/`INVARIANTE` + `RESULTADO`/`FALHAR_COM`.
\* `src/docgen.ts`: `ThzDocGen` (Markdown + Mermaid) a partir da AST.
\* `src/language-service.ts`: **G1** — pipeline único `analisar()` + `obterHover()` + símbolos; re-exports `auditarFonte`, `baixarIrFonte`, `formatarFonte`; base de Playground/LSP.
\* `src/governanca.ts`: **G4** — `auditar()` + `gerarMarkdownGovernanca()` (matriz `RASTREIO→Regra→Contrato`).
\* `src/ir.ts`: **G5** — `VERSAO_IR='thz-ir/1'`, `baixarParaIr()`, `serializarIr()`, `emitirLlvm()`.
\* `src/simd.ts`: **G5** — regras R1-R5, `verificarVetorizado()` + `passoParaLlvm()`.
\* `src/fmt.ts`: **G6** — `formatar()` canônico idempotente (descarta `#` — AST sem trivia).
\* `src/lsp/server.ts`: **G3** — LSP stdio (diagnostics/hover/symbols/completion/definition/formatting + `thz/audit|ir|llvm`).
\* `src/cli.ts`: `thz <check|ast|doc|audit|ir|fmt|run|repl>` (`resolverArquivo` trata `--saida` em qualquer ordem).
\* `src/repl.ts`: REPL multi-linha (`.ajuda`, `.codigo`, `.limpar`, `.sair`).
\* `playground/`: **G2** — Vite + Monaco + `thz-monarch.ts`, execução browser (`InterpretadorThz`/`ArenaMemoria`), botões `Audit/IR/LLVM/Fmt`.
\* `Extensions/thz-lsp-vscode/` (raiz do workspace): **G3** — TextMate `thz.tmLanguage.json`, `language-configuration.json`, `src/extension.ts` (`LanguageClient`).
\* `bench/`: **G6** — `helpers.ts` (`medir`), `decimal.bench.ts`, `fatia.bench.ts`, `simd.bench.ts`, `run.ts`.
\* `test/`: 14 suites (`keywords`, `lexer`, `parser`, `expressoes`, `interpretador`, `decimal`, `analisador`, `ddd`, `golden`, `language-service`, `governanca`, `ir`, `simd`, `fmt`) — golden em `__snapshots__`.
\* `docs/GRAMATICA.md`: EBNF canônica v2.2; `docs/*_arquitetura.md` gerados por `thz doc`.
\* `exemplos/faturamento.thz` + `pedidos.thz`: canônicos (SoA/SIMD e DDD `ENUMERACAO`/`RESULTADO`).



\---



\## 4. Regras de Conduta para o Agente de IA



\* \*\*Manutenção da Sintaxe Canônica:\*\* Não altere a nomenclatura das palavras-chave em português (`PROGRAMA`, `METADADOS\_ARQUITETURA`, `ESTRUTURA`, `REGRA\_NEGOCIO`, `EXIGE`, `GARANTE`, `VETORIZAR\_PARA`, `USAR\_BLOCO\_MEMORIA`).

\* \*\*Tratamento de Erros:\*\* Todo erro sintático ou semântico deve reportar linha e coluna exatas no formato `\[Erro Sintático]\[Linha L:C]`.

\* \*\*Extensibilidade:\*\* Antes de adicionar novas palavras-chave, registre o token em `types.ts`, adicione o reconhecimento léxico em `lexer.ts`, a regra sintática em `parser.ts` e o suporte no `runtime.ts` e `docgen.ts`.

\* \*\*Semântica Dual-OS:\*\* Mantenha compatibilidade estrita de caminhos de arquivos e comandos tanto no Windows PowerShell quanto no Arch Linux.


---

## 5. Adendo v2.2 - Estado Atual e Politica de Extensao

* **Fonte da verdade lexica:** todas as palavras reservadas vivem em `src/keywords.ts`. Proibido reconhecer keywords por literal em `lexer.ts`, `parser.ts`, `interpretador.ts` ou `docgen.ts`.
* **Diagnosticos com caret:** erros sintaticos/semantico/tipos sao renderizados com trecho de fonte e apontador via `src/errors.ts` (`formatarDiagnosticos`). O formato `[Erro Sintatico][Linha L:C]` permanece obrigatorio na mensagem.
* **Analisador semantico:** `src/analisador.ts` (`AnalisadorSemantico`) executa verificacao de tipos, resolucao de escopos e lint `--estrito` (pragma `VERSAO_LINGUAGEM`, rastreabilidade, SLO e contratos). Integrado ao CLI (`thz check`) e ao REPL.
* **REPL:** `npm run thz:repl` - buffer multi-linha ate linha vazia; comandos `.ajuda`, `.codigo`, `.limpar`, `.sair`.
* **Testes:** suuite Node test runner com 149 testes verdes (`npm test`). Golden snapshots da AST em `test/golden.test.ts`; exemplos canonicos: `exemplos/faturamento.thz` e `exemplos/pedidos.thz`.
* **Gramatica:** EBNF canonica em `docs/GRAMATICA.md`; toda nova construcao DEVE atualizar a gramatica, os golden tests e o docgen.
* **Language Service Core (G1):** `src/language-service.ts` — pipeline unico `analisar()` + `obterHover()` + simbolos, base do Playground e do LSP.
* **Playground Web (G2):** `playground/` — Vite + Monaco + Monarch (`thz-monarch.ts`), execucao no browser via `InterpretadorThz` e `ArenaMemoria` (`npm run playground`).
* **LSP + VS Code (G3):** `src/lsp/server.ts` (stdio, diagnostics/hover/symbols/completion/definition/formatting) + extensão VS Code em `Extensions/thz-lsp-vscode/` (TextMate, `language-configuration.json`, client `extension.ts`) — `npm run lsp` / `npm run extension:compile`.
* **Governança Auditável (G4):** `src/governanca.ts` — matriz `RASTREIO_REQUISITO → Regra → Contrato`; `src/language-service.ts:auditarFonte()`; CLI `thz audit` (--json, --saida, --estrito); LSP `thz/audit` + VS Code `THZ: Mostrar Auditoria`; Playground botão `🛡️ Audit`.
* **THZ-IR + SIMD Formal (G5):** `src/ir.ts` (`VERSAO_IR='thz-ir/1'`, `baixarParaIr()`, `emitirLlvm()`) + `src/simd.ts` (regras R1-R5, `verificarVetorizado()`) — CLI `thz ir` (`--llvm`, `--saida`), LSP `thz/ir`/`thz/llvm` e Playground `🧩 IR`/`⚡ LLVM`.
* **Bench + fmt (G6):** `src/fmt.ts` (`formatar()` canônico, idempotente) — CLI `thz fmt` (`--check`, `--escrever`, `--saida`), LSP `textDocument/formatting`, Playground `✨ Fmt` (Ctrl+S); `bench/` — `tsx bench/run.ts` (`npm run bench`) com Decimal/Arena/SoA/SIMD.
* **Prioridade pos-v2.2:** trilha Padrao Ouro G1-G6 definida na secao 5 do `../docs/PROJECT.md` (expressividade DDD primeiro, tooling depois).