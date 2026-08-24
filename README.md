# THZ-LANG

<div align="center">

[![CI](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
[![Status](https://img.shields.io/badge/Testes-100%25%20PASSED-brightgreen.svg)](#-suíte-de-testes)

**Linguagem Corporativa de Sistemas, Governança de Negócio, Arquitetura Viva e Processamento de Dados de Alta Performance.**

[Visão Geral](#-visão-geral) •
[Pilares](#-pilares-da-linguagem) •
[Glossário Ubíquo](#-glossário-de-linguagem-ubíqua) •
[Arquétipos](#-arquétipos-de-módulo) •
[Exemplo Canônico](#-exemplo-canônico) •
[Quick Start](#-quick-start-5-minutos) •
[Documentação Completa](#-documentação-oficial)

</div>

---

## 🌟 Visão Geral

**THZ-LANG** (`.thz`, `.thzui`) é uma linguagem de programação orientada a domínio (DDD) com sintaxe estruturada em língua portuguesa, tipagem estática forte e contratos formais de governança integrados. Ela foi projetada para unir a legibilidade executiva com a eficiência de processamento de dados contíguos e vetorização SIMD.

O repositório unifica os motores de execução em Java (JVM), ferramentas CLI, serviços de linguagem (LSP), extensão para VS Code e renderização gráfica de UI.

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
│ • Decimais    │        │ • C4 / Arch   │               │ • Layout SoA  │
│ • ISO 4217    │        │ • OpenAPI/Spec│               │ • Zero-Copy   │
│ • Invariantes │        │ • Rastreio Req│               │ • Arenas Mem  │
└───────────────┘        └───────────────┘               └───────────────┘
```

---

## 💎 Pilares da Linguagem

1. **Aritmética Exata de Domínio (ISO/IEC 10967 & ISO 4217):** Proibição estrita de ponto flutuante binário IEEE 754 para valores monetários e decimais. Todo cálculo utiliza inteiros escalados com representação decimal exata (`DECIMAL` e `MONETARIO`).
2. **Design by Contract & Governança Integrada:** Cláusulas formais de pré-condição (`EXIGE`), pós-condição (`GARANTE`) e invariantes de entidade (`INVARIANTE`) validadas em tempo de compilação e execução.
3. **Engenharia Orientada a Dados (DoD):** Suporte nativo a layout colunar (*Structure of Arrays* via `LAYOUT_COLUNAR`), laços vetorizados (`VETORIZAR_PARA ... PASSO_SIMD`) e gerenciamento de blocos em Arena contígua (`USAR_BLOCO_MEMORIA`).
4. **Arquitetura Viva & UIs Declarativas:** Extração automática de metadados arquiteturais (`METADADOS_ARQUITETURA`), rastreabilidade de requisitos (`RASTREIO_REQUISITO`) e suporte nativo a arquivos de interface gráfica (`.thzui` e `TELA`).

---

## 📚 Glossário de Linguagem Ubíqua

No THZ-LANG, a **Linguagem Ubíqua (DDD)** é compilável. O vocabulário alinha analistas de negócio e engenheiros:

- **`REGRA_NEGOCIO`**: Unidade discreta de lógica corporativa auditável.
- **`EXIGE` / `GARANTE`**: Pré e pós-condições executáveis de contratos de negócio.
- **`INVARIANTE`**: Regra de integridade absoluta mantida por uma entidade de domínio.
- **`RASTREIO_REQUISITO`**: Vínculo entre a especificação funcional (`"REQ-FIN-001"`) e a implementação.
- **`DECIMAL` / `MONETARIO`**: Aritmética financeira exata sem aproximações de ponto flutuante.
- **`LAYOUT_COLUNAR` / `PASSO_SIMD`**: Estruturas otimizadas para processamento colunar de alta performance.

👉 [Consulte o Glossário Completo de Linguagem Ubíqua](file:///c:/Users/lucas/Projetos/thz-lang/docs/GLOSSARIO_LINGUAGEM_UBIQUA.md)

---

## 🏷️ Arquétipos de Módulo

Em THZ-LANG v2.4, cada arquivo declara seu propósito arquitetural explícito com um terminador obrigatório pareado:

| Arquétipo | Finalidade | Terminador |
| :--- | :--- | :--- |
| `PROGRAMA NEGOCIO` | Processamento de regras de negócio e serviços backend | `FIM_PROGRAMA` |
| `PROGRAMA VISUAL` | Aplicações gráficas interativas | `FIM_PROGRAMA` |
| `PROGRAMA ARQUITETURA` | Especificações e diagramação de arquitetura de software | `FIM_PROGRAMA` |
| `BIBLIOTECA` | Módulos utilitários e funções reutilizáveis | `FIM_BIBLIOTECA` |
| `EXTENSAO` | Módulos de extensão do ecossistema | `FIM_EXTENSAO` |
| `FERRAMENTA` | Utilitários de linha de comando e scripts | `FIM_FERRAMENTA` |
| `TESTE` | Suites de testes automatizados integrados | `FIM_TESTE` |
| `TELA` | Componentes de interface gráfica declarativa (`.thzui`) | `FIM_TELA` |

---

## 📝 Exemplo Canônico

```thz
PROGRAMA NEGOCIO ProcessamentoFaturamentoLote
VERSAO_LINGUAGEM "2.4"

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

    VETORIZAR_PARA i DE 0 ATE tamanho(itens) - 1 PASSO_SIMD 8
        itens.subtotal[i] <- itens.quantidade[i] * itens.preco_unitario[i]
    FIM_VETORIZAR
FIM_REGRA_NEGOCIO

FIM_PROGRAMA
```

---

## ⚡ Quick Start (5 minutos)

### Pré-requisitos
- **Java 25** (JDK 25 ou GraalVM JDK 25)
- **Gradle 8.x** (incluso via Wrapper `./gradlew`)

### 1. Clonar o Repositório
```bash
git clone https.github.com/thz-lang/thz-lang.git
cd thz-lang
```

### 2. Executar a Suíte de Testes
```bash
./gradlew test
```

### 3. Validar e Executar um Código THZ
```bash
# Análise semântica
./gradlew :thz-cli-jvm:run --args="check exemplos/faturamento.thz"

# Executar programa
./gradlew :thz-cli-jvm:run --args="run exemplos/faturamento.thz"

# Renderizar tela .thzui em HTML5
./gradlew :thz-cli-jvm:run --args="ui exemplos/faturamento_dashboard.thzui --html"
```

---

## 📖 Documentação Oficial

Explore os guias detalhados da documentação:

- 📘 [**Manual Completo da Linguagem**](file:///c:/Users/lucas/Projetos/thz-lang/docs/MANUAL_LINGUAGEM.md) — Guia do iniciante ao avançado sobre sintaxe, tipos, contratos, UI e SIMD.
- 📖 [**Glossário de Linguagem Ubíqua**](file:///c:/Users/lucas/Projetos/thz-lang/docs/GLOSSARIO_LINGUAGEM_UBIQUA.md) — Termos universais de negócio, governança, arquitetura e dados em português.
- 📐 [**Gramática Formal EBNF (v2.4)**](file:///c:/Users/lucas/Projetos/thz-lang/docs/GRAMATICA.md) — Especificação rigorosa da linguagem.
- 🛠️ [**CLI, Tooling & IDEs**](file:///c:/Users/lucas/Projetos/thz-lang/docs/CLI_E_TOOLING.md) — Manual do `thz check/run/fmt/doc/audit/ui/ir`, LSP e extensão VS Code.
- 💡 [**Exemplos & Padrões**](file:///c:/Users/lucas/Projetos/thz-lang/docs/EXEMPLOS_E_PADROES.md) — Receitas de código DDD, SIMD, HTTP e UIs.
- 🤝 [**Guia de Contribuição**](file:///c:/Users/lucas/Projetos/thz-lang/CONTRIBUTING.md) — Diretrizes para desenvolvedores do monorepo.

---

## 🧱 Estrutura do Monorepo

```
thz-lang/
├── JVM/                        # Monorepo de motores e ferramentas Java 25
│   ├── thz-core-jvm/           # Núcleo: Léxico, Parser, Semântico, Interpretador, UI & IR
│   ├── thz-cli-jvm/            # Ferramenta de linha de comando (`thz`)
│   ├── thz-gui-jvm/            # Swing FlatLaf Desktop & Native Webview
│   ├── thz-lsp-jvm/            # Servidor LSP (Language Server Protocol)
│   ├── thz-api-jvm/            # Biblioteca HTTP / Rest API
│   └── thz-bench-jvm/          # Benchmarks JMH (SIMD, Arenas, Decimais)
├── Extensions/
│   └── thz-lsp-vscode/         # Extensão oficial para o Visual Studio Code
├── docs/                       # Documentação técnica e manuais
└── exemplos/                   # Códigos de exemplo em `.thz` e `.thzui`
```

---

## ⚖️ Licença

Este projeto está licenciado sob a Licença MIT — consulte o arquivo [LICENSE](LICENSE) para mais detalhes.
