# THZ-LANG

<div align="center">

[![CI](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Node.js](https://img.shields.io/badge/Node.js-20%2B-green.svg)](https://nodejs.org/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5%2B-blue.svg)](https://www.typescriptlang.org/)
[![Status](https://img.shields.io/badge/Testes-188%20Verdes-brightgreen.svg)](#)

**Linguagem Corporativa de Sistemas, Governança de Negócio, Arquitetura Viva e Processamento de Dados de Alta Performance.**

[Visão Geral](#-visão-geral) •
[Pilares](#-pilares-da-linguagem) •
[Exemplo Canônico](#-exemplo-canônico) •
[Estrutura](#-estrutura-do-repositório) •
[Quick Start](#-quick-start) •
[Documentação](#-documentação)

</div>

---

## 🌟 Visão Geral

**THZ-LANG** (`.thz`) é uma linguagem de programação orientada a domínio (DDD) com sintaxe estruturada em língua portuguesa, tipagem estática forte e contratos formais de governança integrados. Ela foi projetada para unir a legibilidade executiva com a eficiência de processamento de dados contíguos e vetorização SIMD.

O repositório unifica os motores de execução, tooling de desenvolvimento, serviços de linguagem (LSP), extensão para VS Code, Playground Web e IDE gráfica desktop.

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
└───────────────┘        └───────────────┘               └───────────────┘
```

---

## 💎 Pilares da Linguagem

1. **Aritmética Exata de Domínio (ISO/IEC 10967 & ISO 4217):**
   Proibição estrita de ponto flutuante binário IEEE 754 para valores monetários e decimais. Todo cálculo utiliza inteiros escalados com representação decimal exata (`DECIMAL` e `MONETARIO`).
2. **Design by Contract & Governança Integrada:**
   Cláusulas formais de pré-condição (`EXIGE`), pós-condição (`GARANTE`) e invariantes de entidade (`INVARIANTE`) validadas em tempo de compilação e execução.
3. **Engenharia Orientada a Dados (DoD):**
   Suporte nativo a layout colunar (*Structure of Arrays* via `LAYOUT_COLUNAR`), laços vetorizados (`VETORIZAR_PARA ... PASSO_SIMD`) e gerenciamento de blocos em Arena contígua (`USAR_BLOCO_MEMORIA`).
4. **Arquitetura Viva:**
   Extração automática de metadados arquiteturais (`METADADOS_ARQUITETURA`), rastreabilidade de requisitos (`RASTREIO_REQUISITO`) e geração de documentação em Markdown com diagramas Mermaid.

---

## 📝 Exemplo Canônico

```thz
PROGRAMA ProcessamentoFaturamentoLote
VERSAO_LINGUAGEM "2.2"

METADADOS_ARQUITETURA
    SISTEMA: "FaturamentoCore"
    MODULO: "MotorCalculo"
    DOMINIO: "Financeiro"
    SLO_LATENCIA_MS: 50
    CRITICIDADE: "ALTA"
FIM_METADADOS

ESTRUTURA ItemFatura LAYOUT_COLUNAR
    id: TEXTO
    quantidade: INTEIRO
    preco_unitario: DECIMAL(12, 2)
    subtotal: DECIMAL(12, 2)
FIM_ESTRUTURA

REGRA_NEGOCIO CalcularSubtotais
    RASTREIO_REQUISITO: "REQ-FIN-001"
    EXIGE: tamanho(itens) > 0
    GARANTE: PARA_TODO(itens, item -> item.subtotal >= 0.00)

    VETORIZAR_PARA i DE 0 ATE tamanho(itens) - 1 PASSO_SIMD 8
        itens.subtotal[i] <- itens.quantidade[i] * itens.preco_unitario[i]
    FIM_VETORIZAR
FIM_REGRA_NEGOCIO
```

---

## 📁 Estrutura do Repositório

```
thz-lang/
├── PROJECT.md                  # Visão, pilares, roadmap e especificações
├── AGENTS.md                   # Diretrizes para agentes de IA e invariantes
├── TODO.md                     # Próximas etapas de evolução
├── CONTRIBUTING.md             # Guia de contribuição
├── LICENSE                     # Licença MIT
│
├── Extensions/
│   └── thz-lsp-vscode/          # Extensão VS Code (TextMate + LanguageClient; servidor via thz-lang-engine)
│
├── thz-lang-engine/             # Motor TypeScript / Node.js (v2.2+)
│   ├── src/                    # Lexer, Parser, Semântico, Runtime, IR, SIMD, LSP
│   ├── playground/             # Playground Web (Vite + Monaco Editor + Monarch)
│   ├── bench/                  # Benchmarks (Decimal, Fatia, SIMD)
│   ├── docs/                   # Gramática EBNF canônica e docs de arquitetura
│   ├── exemplos/               # Programas canônicos (faturamento, pedidos, agenda)
│   └── test/                   # 159 testes unitários e golden snapshots
│
├── JVM/                         # Motor Java 25 — três projetos Gradle autônomos
│   ├── thz-core-jvm/            #   núcleo/stdlib (java-library + maven-publish)
│   ├── thz-cli-jvm/             #   CLI + REPL + UberJAR
│   └── thz-gui-jvm/             #   IDE Desktop Swing

> Os módulos JVM comunicam entre si pela API pública do `thz-core`; as funções gráficas `TELA.*` são registradas por cada apresentação via `BibliotecaPadrao.registrar()`.
```

---

## 🚀 Quick Start

### 0. Orquestrador da raiz (`package.json`)

A raiz do workspace centraliza as operações mais comuns dos quatro módulos — sem ferramenta extra, apenas npm:

```bash
npm run setup        # instala deps TS + publica core no Maven Local + gera UberJAR
npm test             # suítes completas: TypeScript + thz-core + thz-gui
npm run test:ts      # apenas motor TypeScript (160 testes)
npm run test:core    # apenas núcleo JVM
npm run test:gui     # apenas IDE Desktop

npm run thz -- check JVM/thz-core-jvm/exemplos/faturamento.thz   # CLI com args naturais*
npm run repl                                             # REPL interativo
npm run ide                                              # abre a IDE Desktop Swing

npm run core:publish    # publica thz.lang:thz-core no ~/.m2
npm run cli:jar         # gera JVM/thz-cli-jvm/target/thz-jvm-2.3.0.jar
```

> \* O wrapper `scripts/thz.js` gera o UberJAR automaticamente na primeira execução, se ainda não existir.

### 1. Motor TypeScript / Node.js (`thz-lang-base`)

**Requisitos:** Node.js 20+ e npm.

```bash
cd thz-lang-engine

# Instalar dependências
npm install

# Executar a suíte de testes (159 testes)
npm test

# Executar checagem estrita de tipos e contratos
npm run thz:check -- --estrito

# Executar programa canônico
npm run thz:run

# Iniciar o REPL interativo
npm run thz:repl

# Iniciar o Playground Web no navegador (http://localhost:5173)
npm run playground

# Executar benchmarks
npm run bench
```

#### Comandos do CLI (`thz`):
- `thz check <arquivo.thz> [--estrito]` — Análise léxica, sintática e semântica.
- `thz run <arquivo.thz>` — Executa o programa via interpretador.
- `thz fmt <arquivo.thz> [--escrever]` — Formatador canônico idempotente.
- `thz doc <arquivo.thz> [--saida <dir>]` — Gera documentação viva com diagramas Mermaid.
- `thz audit <arquivo.thz> [--json]` — Matriz de governança e auditoria de requisitos.
- `thz ir <arquivo.thz> [--llvm]` — Baixa para THZ-IR e emite código LLVM.

---

### 2. Motor Java 25 (`JVM/thz-core-jvm` / `JVM/thz-cli-jvm` / `JVM/thz-gui-jvm`)

Três projetos Gradle autônomos na pasta `JVM/` do workspace, comunicando pela API pública do `thz-core` (as funções gráficas `TELA.*` são registradas por cada apresentação via `BibliotecaPadrao.registrar()`). Requisitos: OpenJDK 25 (Gradle Wrapper embutido em cada projeto).

```bash
# Núcleo — testes e publicação no Maven Local
cd JVM/thz-core-jvm
./gradlew test publishToMavenLocal

# CLI — UberJAR executável (target/thz-jvm-2.3.0.jar)
cd ../thz-cli-jvm
./gradlew shadowJar
java -jar target/thz-jvm-2.3.0.jar check ../thz-core-jvm/exemplos/agenda.thz
java -jar target/thz-jvm-2.3.0.jar run   ../thz-core-jvm/exemplos/colecao/01-ola-mundo.thz
java -jar target/thz-jvm-2.3.0.jar repl

# IDE Desktop Swing
cd ../thz-gui-jvm
./gradlew gui
```

---

## 📚 Documentação

- **Gramática EBNF:** [`Node/thz-lang-base/docs/GRAMATICA.md`](Node/thz-lang-base/docs/GRAMATICA.md)
- **Documentação do Motor Node/TS:** [`Node/thz-lang-base/README.md`](Node/thz-lang-base/README.md)
- **Documentação do Motor JVM:** [`thz-core-jvm/README.md`](JVM/thz-core-jvm/README.md), [`thz-cli-jvm/README.md`](JVM/thz-cli-jvm/README.md) e [`thz-gui-jvm/README.md`](JVM/thz-gui-jvm/README.md)
- **Visão Arquitetural e Roadmap:** [`PROJECT.md`](PROJECT.md)
- **Diretrizes para Agentes de IA:** [`AGENTS.md`](AGENTS.md)
- **Extensão VS Code:** [`thz-lsp-vscode/README.md`](Extensions/thz-lsp-vscode/README.md)

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [`LICENSE`](LICENSE) para mais detalhes.
