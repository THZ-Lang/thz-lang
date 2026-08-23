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
├── thz-lang-engine/            # Motor TypeScript / Node.js (v2.2+)
│   ├── src/                    # Lexer, Parser, Semântico, Runtime, IR, SIMD, LSP
│   ├── playground/             # Playground Web (Vite + Monaco Editor + Monarch)
│   ├── extension/              # Extensão VS Code (TextMate + Language Server)
│   ├── bench/                  # Benchmarks (Decimal, Fatia, SIMD)
│   ├── docs/                   # Gramática EBNF canônica e docs de arquitetura
│   ├── exemplos/               # Programas canônicos (faturamento, pedidos, agenda)
│   └── test/                   # 159 testes unitários e golden snapshots
│
└── thz-lang-engine-JVM/        # Motor Java 25 / JVM
    ├── pom.xml                 # Build canônico Maven
    ├── src/main/java/thz/lang/ # Implementação Java 25 (AST, Lexer, Parser, Runtime, GUI)
    ├── src/test/java/thz/lang/ # Testes JUnit 5 (paridade comportamental com TypeScript)
    └── exemplos/               # Galeria de exemplos e coleções de testes
```

---

## 🚀 Quick Start

### 1. Motor TypeScript / Node.js (`thz-lang-engine`)

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

### 2. Motor Java 25 (`thz-lang-engine-JVM`)

**Requisitos:** OpenJDK 25 e Maven 3.9+.

```bash
cd thz-lang-engine-JVM

# Compilar e executar testes JUnit 5
mvn clean test

# Gerar o JAR executável
mvn package

# Executar verificação ou rodar arquivo
java -jar target/thz-jvm-2.3.0.jar check exemplos/agenda.thz
java -jar target/thz-jvm-2.3.0.jar run   exemplos/agenda.thz

# Iniciar IDE Gráfica Swing (com galeria de exemplos)
java -jar target/thz-jvm-2.3.0.jar gui

# Iniciar REPL
java -jar target/thz-jvm-2.3.0.jar repl
```

---

## 📚 Documentação

- **Gramática EBNF:** [`thz-lang-engine/docs/GRAMATICA.md`](thz-lang-engine/docs/GRAMATICA.md)
- **Documentação do Motor Node/TS:** [`thz-lang-engine/README.md`](thz-lang-engine/README.md)
- **Documentação do Motor JVM:** [`thz-lang-engine-JVM/README.md`](thz-lang-engine-JVM/README.md)
- **Visão Arquitetural e Roadmap:** [`PROJECT.md`](PROJECT.md)
- **Diretrizes para Agentes de IA:** [`AGENTS.md`](AGENTS.md)
- **Extensão VS Code:** [`thz-lang-engine/extension/README.md`](thz-lang-engine/extension/README.md)

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [`LICENSE`](LICENSE) para mais detalhes.
