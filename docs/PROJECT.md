# THZ-LANG: Enterprise Architecture & Data Systems Language

Linguagem de programação corporativa orientada a domínio (DDD), contratos formais de governança e alta taxa de transferência de dados, projetada para unir a legibilidade do português estruturado com a performance de execução em código de máquina nativo.

```
┌────────────────────────────────────────────────────────────────────────┐
│                              THZ-LANG                                  │
│        (Enterprise Architecture & High-Performance Data Engine)        │
└───────┬────────────────────────┬───────────────────────────────┬───────┘
        │                        │                               │
        ▼                        ▼                               ▼
┌───────────────┐        ┌───────────────┐               ┌───────────────┐
│   GOVERNANÇA  │        │  ARQUITETURA  │               │ ALTA EFICIÊNCIA│
│  E CONTRATOS  │        │   E METADADOS │               │    DE DADOS   │
├───────────────┤        ├───────────────┤               ├───────────────┤
│ • DDD Nativo  │        │ • Doc Viva    │               │ • SIMD / AVX  │
│ • Tipos Dec/Mo│        │ • C4 / Arch   │               │ • Layout SoA  │
│ • ISO 4217    │        │ • OpenAPI/Spec│               │ • Zero-Copy   │
│ • Invariantes │        │ • Rastreio Req│               │ • Arenas Mem  │
│ • SOX/LGPD    │        │ • Big Data    │               │ • AOT Clang   │
└───────────────┘        └───────────────┘               └───────────────┘
```

---

## 1. Pilares da Linguagem

1. **Aritmética Exata de Domínio:** Suporte nativo a `DECIMAL(P, S)` e `MONETARIO(ISO)` sem imprecisões binárias (ISO/IEC 10967 / ISO 4217).
2. **Contratos Formais e Invariantes:** Cláusulas de pré-condição (`EXIGE`), pós-condição (`GARANTE`) e invariantes de entidade (`INVARIANTE`) integradas à semântica da linguagem.
3. **Engenharia Orientada a Dados (DoD):** Modificador `LAYOUT_COLUNAR` (*Structure of Arrays*), loops vetorizados (`VETORIZAR_PARA ... PASSO_SIMD`) e gerenciamento de memória em blocos contíguos (`USAR_BLOCO_MEMORIA`).
4. **Arquitetura Viva & Big Data:** Extração direta de metadados arquiteturais (ISO/IEC/IEEE 42010), diagramas C4 e suporte a pipelines de dados em lote e tempo real (`PIPELINE_DADOS`).
5. **Autonomia AOT & Self-Hosting:** Compilador self-hosted em THZ (`compilador/*.thz`), backend LLVM IR/Clang 22, runtime nativo C Dual-OS (`src/runtime/thz_runtime.c`) e geração de binários nativos sem dependência de JVM.

### 1.1 Escopo da Governança Corporativa

Para evitar burocracia desnecessária, `METADADOS_ARQUITETURA` e `REGRA_NEGOCIO` não precisam ser obrigatórios em todos os arquivos THZ-LANG. Eles são obrigatórios em programas corporativos, módulos de domínio e componentes sujeitos a rastreabilidade, auditoria ou contratos formais.

Bibliotecas utilitárias, ferramentas internas e scripts pequenos podem usar uma forma reduzida da linguagem, mantendo a tipagem, a segurança financeira e os contratos quando aplicáveis. Essa distinção preserva a essência corporativa da THZ-LANG sem impor cerimônia arquitetural a código simples.

---

## 2. Código Canônico de Referência (`exemplos/faturamento.thz`)

```thz
PROGRAMA NEGOCIO ProcessamentoFaturamentoLote

METADADOS_ARQUITETURA
    SISTEMA: "FaturamentoCore"
    MODULO: "MotorCalculo"
    DOMINIO: "Financeiro"
    SLO_LATENCIA_MS: 15
    CRITICIDADE: "ALTA"
    CONFORMIDADE: "SOX-404", "LGPD-Art7"
FIM_METADADOS

ESTRUTURA ItemFatura LAYOUT_COLUNAR
    id_transacao        : TEXTO
    codigo_produto      : TEXTO
    quantidade          : INTEIRO
    valor_unitario      : DECIMAL(12, 4)
    aliquota_imposto    : DECIMAL(5, 2)
    valor_total_liquido : DECIMAL(14, 4)
    INVARIANTE valor_total_liquido >= 0.0000
FIM_ESTRUTURA

REGRA_NEGOCIO CalculoTributarioLote
    RASTREIO_REQUISITO: "REQ-FISCAL-9102"

    EXIGE: tamanho(itens) > 0

    GARANTE: acumulador_tributos >= 0.0000

    INICIO
        VARIAVEL acumulador_tributos : DECIMAL(18, 4) <- 0.0000
        VETORIZAR_PARA i DE 0 ATE tamanho(itens) - 1 PASSO_SIMD 8
            VARIAVEL bruto : DECIMAL(18, 4) <- itens.quantidade[i] * itens.preco_unitario[i]
            VARIAVEL fator_aliquota : DECIMAL(18, 4) <- itens.aliquota_imposto[i] / 100
            VARIAVEL imposto_item : DECIMAL(18, 4) <- bruto * fator_aliquota
            itens.valor_total_liquido[i] <- bruto + imposto_item
            acumulador_tributos <- acumulador_tributos + imposto_item
        FIM_VETORIZAR
        RETORNAR acumulador_tributos
    FIM
FIM_REGRA_NEGOCIO

FIM_PROGRAMA
```

---

## 3. Documentação — Mapa do Corpus

| Doc | Escopo | Leitura |
| :--- | :--- | :--- |
| [`MANUAL_LINGUAGEM.md`](MANUAL_LINGUAGEM.md) | Linguagem completa (tipos, contratos, SIMD, PIPELINE, TELA, stdlib) | Referência diária |
| [`ARQUITETURA_COMPILACAO_NATIVA.md`](ARQUITETURA_COMPILACAO_NATIVA.md) | **Tratado** GraalVM/LLVM/IR/IL/AOT, velocidade & segurança (7062w, 4 apêndices) | Arquitetura |
| [`SELF_HOSTING.md`](SELF_HOSTING.md) | Compilador em THZ, bootstrap `THZ→THZ-IR→LLVM`, paridade Java | Self-hosting |
| [`RUNTIME_NATIVO.md`](RUNTIME_NATIVO.md) | `thz_runtime.c`/`thz_webview2.c`, ABI Dual-OS, Arena, linking `clang→gcc` | Runtime |
| [`PIPELINE_DADOS.md`](PIPELINE_DADOS.md) | `PIPELINE_DADOS` (FONTE/TRANSFORMACAO/DESTINO), conectores, streaming | Big Data |
| [`TELA_THZUI.md`](TELA_THZUI.md) | `TELA`/`.thzui` DSL, `TELA.*`/`WEBVIEW.*`, Swing vs WebView | UI |
| [`GUIA_PERFORMANCE.md`](GUIA_PERFORMANCE.md) | SoA/SIMD/Arena tuning, escolha `PASSO_SIMD`, JMH | Performance |
| [`TESTES_E_BENCHMARKS.md`](TESTES_E_BENCHMARKS.md) | JUnit 5 112 testes, goldens, paridade, JMH, `write-tests` | Qualidade |
| [`LSP_VSCODE.md`](LSP_VSCODE.md) / [`API_REST.md`](API_REST.md) | LSP4J + VS Code + Spring Boot 11 endpoints | Tooling |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | `jpackage`/GraalVM/LLVM, Docker, `dist/`/`target`, CI `audit --git` | Deploy |
| [`EXEMPLOS_E_PADROES.md`](EXEMPLOS_E_PADROES.md) | 12 receitas de `exemplos/*.thz` + `*.thzui` reais | Receitas |
| [`INTELLIJ_SETUP.md`](INTELLIJ_SETUP.md) | JDK 25 + Gradle composite + TextMate + run configs | IDE |
| [`ADRs/`](ADRs/README.md) | 5 ADRs (LLVM vs Cranelift, Arena vs GC, i128 vs double...) | Decisões |
| [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) | FAQ por área (build, SIMD, arena, LSP, GraalVM, LLVM, Docker) | Suporte |
| [`CLI_E_TOOLING.md`](CLI_E_TOOLING.md) | `thz check/run/dev/fmt/doc/audit/ui/ir/gui` + IDE/LSP | CLI |
| [`GRAMATICA.md`](GRAMATICA.md) / [`CONFORMIDADE_E_NORMAS.md`](CONFORMIDADE_E_NORMAS.md) / [`DIRETRIZES_QUALIDADE.md`](DIRETRIZES_QUALIDADE.md) | EBNF, ISO 10967/4217/42010/TR24772, V&V | Normas |
| [`RELATORIO-EVOLUCAO.md`](RELATORIO-EVOLUCAO.md) + [`CHANGELOG.md`](../CHANGELOG.md) | Linha do tempo + changelog SemVer | Histórico |

---

## 3. Comandos do Ecossistema CLI

```bash
# Execução via Gradle:
./gradlew cli --args="check exemplos/faturamento.thz --estrito" # Análise estrita
./gradlew cli --args="run exemplos/faturamento.thz"             # Execução
./gradlew cli --args="dev exemplos/faturamento.thz"             # Dev server com live reload
./gradlew cli --args="audit exemplos/faturamento.thz --git"     # Auditoria vinculada ao Git
./gradlew cli --args="doc exemplos/faturamento.thz"             # Documentação viva (Markdown + Mermaid)
./gradlew cli --args="ir exemplos/faturamento.thz --llvm"       # Emissão de LLVM IR
./gradlew cli --args="fmt exemplos/faturamento.thz --escrever"  # Formatação canônica
./gradlew gui                                                  # Desktop IDE Swing + FlatLaf
./gradlew jmh                                                  # Benchmarks JMH
./gradlew test                                                 # Suíte completa de testes JUnit 5
```

---

## 4. Roadmap de Evolução Técnica

- [x] **Fase 1:** Prototipagem da gramática, Lexer, Parser AST e Runtime em TypeScript.
- [x] **Fase 2:** Aritmética decimal de ponto fixo via `BigInt` e emulação de Arena de Memória.
- [x] **Fase 3:** Geração de documentação viva da AST (Mermaid + Markdown).
- [x] **Fase 4 (v2.2):** Expressões com precedência, comandos executáveis, contratos como árvore, interpretador real, análise semântica e lint `--estrito`.
- [x] **Fase 5 (v2.2):** Expressividade DDD — `ENUMERACAO`, `RESULTADO[T,E]` + `FALHAR_COM`, `INVARIANTE` em `ESTRUTURA`; golden snapshots da AST.
- [x] **Fase 6 (G1–G6):** Language Service Core, Playground Web, LSP Java + VS Code, Governança auditável, THZ-IR/LLVM e Benchmarks.
- [x] **Fase Multi-Módulo JVM 25:** Separação estrita em `thz-core`, `thz-cli`, `thz-gui`, `thz-lsp`, `thz-bench` e `thz-api`.
- [x] **Fase Desktop IDE Industrial:** IDE Swing FlatLaf (`ThzGui`, `EditorThz`, `Gutter`, `RenderizadorFormularioSwing`, paleta de ações e formulários reativos).
- [x] **Fase GraalVM Native & Look and Feel:** Compilação AOT de CLI (`thz.exe`) e GUI (`thz-desktop.exe`) com metadata reachability.
- [x] **Fase Big Data Pipeline Archetype:** Ingestão massiva `PIPELINE_DADOS` com fontes/destinos heterogêneos e transformação vetorizada.
- [x] **Fase Self-Hosting & Autonomia Total:** Compilador self-hosted em THZ (`compilador/*.thz`) + Pipeline AOT LLVM Clang Dual-OS (`scripts/build-llvm.ps1`) com runtime C nativo (`src/runtime/thz_runtime.c`), gerando binários executáveis independentes.
- [ ] **Fase 7:** Fatias Zero-Copy com Apache Arrow IPC e otimizações vetoriais AVX-512.
