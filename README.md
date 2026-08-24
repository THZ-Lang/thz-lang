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
├── Extensions/                  # Extensões para editores
│   └── thz-lsp-vscode/          #   Extensão oficial do VS Code (LSP client)
│
├── JVM/                         # Motor Java 25 — produção
│   ├── thz-core-jvm/            #   Núcleo: Lexer, Parser, Semântico, Runtime, IR, SIMD
│   ├── thz-cli-jvm/             #   CLI + REPL + UberJAR
│   ├── thz-gui-jvm/             #   IDE Desktop Swing
│   ├── thz-api-jvm/             #   REST API (Spring Boot) → Playground Web
│   ├── thz-lsp-jvm/             #   LSP Server (LSP4J) → VS Code Extension
│   └── thz-bench-jvm/           #   Benchmarks JMH (Decimal, Memoria, Layout)
│
└── docs/                        # Documentação da linguagem, EBNF e arquitetura
```

> Os módulos JVM comunicam entre si pela API pública do `thz-core`; as funções gráficas `TELA.*` são registradas por cada apresentação via `BibliotecaPadrao.registrar()`.
```

---

## 🚀 Quick Start

### 0. Orquestrador da raiz (`package.json`)

A raiz do workspace centraliza as operações dos módulos JVM — sem ferramenta extra, apenas npm:

```bash
npm run setup        # compila core + CLI + API + LSP
npm test             # suíte completa: core + gui + api + lsp
npm run test:core    # apenas núcleo JVM

npm run thz -- check JVM/thz-core-jvm/exemplos/faturamento.thz   # CLI
npm run repl                                             # REPL interativo
npm run ide                                              # IDE Desktop Swing

# API REST (Spring Boot) — consume o core Java via HTTP
npm run api:build    # gera o JAR da API
npm run api:run      # http://localhost:8080

# LSP Server (Java) — conecta ao VS Code
npm run lsp:jar      # gera shadow JAR
npm run lsp:run      # inicia servidor LSP via stdio

# Benchmarks JMH
npm run bench        # roda todos os benchmarks
```

---

### 1. Motor Java 25 (produção)

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

# API REST
cd ../thz-api-jvm
./gradlew bootRun    # http://localhost:8080

# LSP Server (VS Code)
cd ../thz-lsp-jvm
./gradlew shadowJar
java -jar target/thz-lsp-2.3.0.jar --stdio

# Benchmarks
cd ../thz-bench-jvm
./gradlew jmh
```

---

## 📚 Documentação

- **Gramática EBNF:** [`docs/GRAMATICA.md`](docs/GRAMATICA.md)
- **Documentação do Motor JVM:** [`thz-core-jvm/README.md`](JVM/thz-core-jvm/README.md), [`thz-cli-jvm/README.md`](JVM/thz-cli-jvm/README.md), [`thz-api-jvm/README.md`](JVM/thz-api-jvm/README.md), [`thz-lsp-jvm/README.md`](JVM/thz-lsp-jvm/README.md)
- **Visão Arquitetural e Roadmap:** [`docs/PROJECT.md`](docs/PROJECT.md)
- **Diretrizes para Agentes de IA:** [`AGENTS.md`](AGENTS.md)
- **Extensão VS Code:** [`thz-lsp-vscode/README.md`](Extensions/thz-lsp-vscode/README.md)

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [`LICENSE`](LICENSE) para mais detalhes.
