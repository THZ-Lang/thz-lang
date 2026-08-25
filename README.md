# THZ-LANG

<div align="center">

[![CI](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
[![LLVM](https://img.shields.io/badge/LLVM-Clang%20AOT-red.svg)](https://llvm.org/)
[![Docker & Podman](https://img.shields.io/badge/Containers-Docker%20%7C%20Podman-2496ED.svg)](docs/DOCKER_PODMAN_DEVCONTAINER.md)
[![Dev Containers](https://img.shields.io/badge/Dev%20Containers-Ready-blueviolet.svg)](.devcontainer/devcontainer.json)
[![Status](https://img.shields.io/badge/Testes-100%25%20PASSED-brightgreen.svg)](#-suíte-de-testes)

**Linguagem Corporativa de Sistemas, Governança de Negócio, Arquitetura Viva e Processamento de Dados de Alta Performance.**

[Visão Geral](#-visão-geral) •
[Pilares](#-pilares-da-linguagem) •
[Glossário Ubíquo](#-glossário-de-linguagem-ubíqua) •
[Arquétipos](#-arquétipos-de-módulo) •
[Exemplo Canônico](#-exemplo-canônico) •
[Quick Start](#-quick-start-5-minutos) •
[Docker & Podman](docs/DOCKER_PODMAN_DEVCONTAINER.md) •
[Documentação Oficial](#-documentação-oficial)

</div>

---

## 🌟 Visão Geral

**THZ-LANG** (`.thz`, `.thzui`) é uma linguagem de programação orientada a domínio (DDD) com sintaxe estruturada em língua portuguesa, tipagem estática forte, contratos formais de governança integrados e compilação nativa de alta performance. Ela foi projetada para unir a legibilidade executiva com a eficiência de processamento de dados contíguos e vetorização SIMD.

O repositório unifica os motores de execução em Java 25 (JVM Multi-módulo), o compilador self-hosted em THZ, o backend de compilação nativa AOT via LLVM Clang, ferramentas CLI (`thz`), Desktop IDE Swing FlatLaf (`thz gui`), serviços de linguagem (LSP) e extensão para VS Code.

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
│ • SOX / LGPD  │        │ • UI .thzui   │               │ • AOT Clang   │
└───────────────┘        └───────────────┘               └───────────────┘
```

---

## 💎 Pilares da Linguagem

1. **Aritmética Exata de Domínio (ISO/IEC 10967 & ISO 4217):** Proibição estrita de ponto flutuante binário IEEE 754 para valores monetários e decimais. Todo cálculo utiliza inteiros escalados com representação decimal exata (`DECIMAL` e `MONETARIO`).
2. **Design by Contract & Governança Integrada:** Cláusulas formais de pré-condição (`EXIGE`), pós-condição (`GARANTE`) e invariantes de entidade (`INVARIANTE`) validadas em tempo de compilação e execução.
3. **Engenharia Orientada a Dados (DoD):** Suporte nativo a layout colunar (*Structure of Arrays* via `LAYOUT_COLUNAR`), laços vetorizados (`VETORIZAR_PARA ... PASSO_SIMD`) e gerenciamento de blocos em Arena contígua (`USAR_BLOCO_MEMORIA`).
4. **Arquitetura Viva & UIs Declarativas:** Extração automática de metadados arquiteturais (`METADADOS_ARQUITETURA`), rastreabilidade de requisitos (`RASTREIO_REQUISITO`) e suporte nativo a arquivos de interface gráfica (`.thzui` e `TELA`).
5. **Autonomia Total & Runtime Rust:** Compilação nativa AOT Dual-OS via LLVM Clang linkando com o runtime de alta performance em Rust (`src/runtime_rs`), sem dependência de JVM em produção.
6. **IA & Machine Learning Soberano (Zero Python):** Embeddings determinísticos, similaridade de cosseno e modelos tabulares em sub-milissegundos on-device (`IA.*`, `ML.*`).
7. **Consultas Tipadas Nativas (LINQ / Query DSL):** `CONSULTAR ... ONDE ... ORDENAR_POR ... LIMITE` com validação de tipos em tempo de compilação.
8. **Mensageria Reativa & Streaming (EDA):** Barramento de eventos assíncrono de ultra-alta taxa de transferência (`MENSAGERIA.*`).
9. **Depuração Nativa (DAP):** Protocolo DAP para depuração passo a passo com breakpoints no VS Code e na Desktop IDE.
10. **Target Universal WebAssembly (WASM):** Execução nativa no navegador e Edge Workers com precisão ISO/IEC 10967.

---

## 📚 Glossário de Linguagem Ubíqua

No THZ-LANG, a **Linguagem Ubíqua (DDD)** é compilável. O vocabulário alinha analistas de negócio e engenheiros:

- **`REGRA_NEGOCIO`**: Unidade discreta de lógica corporativa auditável.
- **`EXIGE` / `GARANTE`**: Pré e pós-condições executáveis de contratos de negócio.
- **`INVARIANTE`**: Regra de integridade absoluta mantida por uma entidade de domínio.
- **`RASTREIO_REQUISITO`**: Vínculo entre a especificação funcional (`"REQ-FIN-001"`) e a implementação.
- **`DECIMAL` / `MONETARIO`**: Aritmética financeira exata sem aproximações de ponto flutuante.
- **`PIPELINE_DADOS`**: Arquétipo de processamento massivo de dados em lote (*Batch*) ou tempo real (*Streaming*).
- **`LAYOUT_COLUNAR` / `PASSO_SIMD`**: Estruturas otimizadas para processamento colunar de alta performance.

👉 [Consulte o Glossário Completo de Linguagem Ubíqua](docs/GLOSSARIO_LINGUAGEM_UBIQUA.md)

---

## 🏷️ Arquétipos de Módulo

Em THZ-LANG v2.4, cada arquivo declara seu propósito arquitetural explícito com um terminador obrigatório pareado:

| Arquétipo | Finalidade | Terminador |
| :--- | :--- | :--- |
| `PROGRAMA NEGOCIO` | Processamento de regras de negócio e serviços backend | `FIM_PROGRAMA` |
| `PROGRAMA VISUAL` | Aplicações gráficas interativas | `FIM_PROGRAMA` |
| `PROGRAMA ARQUITETURA` | Especificações e diagramação de arquitetura de software | `FIM_PROGRAMA` |
| `PIPELINE_DADOS` | Ingestão e transformação Big Data (Streaming & Batch) | `FIM_PIPELINE` |
| `BIBLIOTECA` | Módulos utilitários e funções reutilizáveis | `FIM_BIBLIOTECA` |
| `EXTENSAO` | Módulos de extensão do ecossistema | `FIM_EXTENSAO` |
| `FERRAMENTA` | Utilitários de linha de comando e scripts | `FIM_FERRAMENTA` |
| `TESTE` | Suítes de testes automatizados integrados | `FIM_TESTE` |
| `TELA` | Componentes de interface gráfica declarativa (`.thzui`) | `FIM_TELA` |

---

## 📝 Exemplo Canônico

```thz
PROGRAMA NEGOCIO FaturamentoVendas

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
- **Java 25** (OpenJDK 25 ou GraalVM JDK 25)
- **Gradle 8.x** (incluso via `./gradlew`)
- **LLVM Clang & MinGW GCC** (Opcional, para compilação nativa AOT)
- **MSVC - Ferramentas de Build do Visual Studio 2019+ (no caso for Windows)

### 1. Clonar o Repositório
```bash
git clone https://github.com/thz-lang/thz-lang.git
cd thz-lang
```

### 2. Executar a Suíte de Testes
```bash
./gradlew test
```

### 3. Comandos Principais da CLI
```bash
# Análise semântica e verificação de contratos
./gradlew cli --args="check exemplos/faturamento.thz"

# Executar programa
./gradlew cli --args="run exemplos/faturamento.thz"

# Servidor de desenvolvimento com Live Reload
./gradlew cli --args="dev exemplos/faturamento.thz"

# Auditoria de governança integrada com Git
./gradlew cli --args="audit exemplos/faturamento.thz --git"

# Iniciar a IDE Desktop Swing + FlatLaf
./gradlew gui

# Executar benchmarks JMH
./gradlew jmh
```

### 4. Compilação Nativa AOT (Zero Dependência de JVM)
```bash
# Compilar qualquer fonte .thz em binário nativo (.exe PE / .elf) via LLVM Clang:
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz compilador/driver.thz

# Executar o binário nativo autônomo gerado:
./dist/bin/driver.exe
```

### 5. Execução 100% Plug & Play via Docker, Podman e Devcontainers
```bash
# Sobe a API REST na porta 8080 (auto-detecta Podman ou Docker):
npm run docker:up

# Executa o REPL interativo dentro do contêiner:
npm run docker:repl

# Executa testes unitários dentro do contêiner:
npm run docker:test

# Ou abra diretamente no VS Code via Dev Containers (F1 > Reopen in Container)!
```
👉 [Consulte o Guia Completo de Docker, Podman e Devcontainers](docs/DOCKER_PODMAN_DEVCONTAINER.md)

---

## 📖 Documentação Oficial

Explore os guias detalhados da documentação:

- 🗄️ [**Conectores de Banco & Mensageria Distribuída**](docs/CONECTORES_BANCO_E_MENSAGERIA.md) — Manifesto `thz.config.json`, JPA/ORM, Raw SQL, Busca Vetorial KNN, RabbitMQ, Kafka e AWS SQS/SNS.
- 🗺️ [**Roadmap Estratégico (v2.6 ~ v3.0)**](docs/ROADMAP.md) — Os 5 pilares: IA Nativa On-Device, LINQ Tipado, Streaming EDA, WebAssembly e Debugger DAP.
- 📘 [**Manual Completo da Linguagem**](docs/MANUAL_LINGUAGEM.md) — Guia do iniciante ao avançado sobre sintaxe, tipos, contratos, UI e SIMD.
- ⚙️ [**Arquitetura de Compilação Nativa — GraalVM, LLVM, IR/IL e Código de Máquina**](docs/ARQUITETURA_COMPILACAO_NATIVA.md) — Tratado completo sobre IR/IL, LLVM, GraalVM Native Image, geração AOT, velocidade (SIMD/SoA/Arenas) e execução segura.
- 🔁 [**Self-Hosting**](docs/SELF_HOSTING.md) — Compilador em THZ, bootstrap `THZ→THZ-IR→LLVM`, paridade Java↔THZ.
- 🧱 [**Runtime Nativo**](docs/RUNTIME_NATIVO.md) — ABI Dual-OS, Arenas, `thz_runtime.c`/`thz_webview2.c`, linking `clang→gcc`.
- 🌊 [**PIPELINE_DADOS**](docs/PIPELINE_DADOS.md) — Guia Big Data (FONTE/TRANSFORMACAO/DESTINO, conectores, streaming).
- 🖼️ [**TELA / .thzui**](docs/TELA_THZUI.md) — DSL declarativa, `TELA.*`/`WEBVIEW.*`, Swing vs WebView.
- 🚀 [**Deployment**](docs/DEPLOYMENT.md) — `jpackage`/GraalVM/LLVM, Docker, `dist/`/`target`, CI `audit --git`.
- ⚡ [**Guia de Performance**](docs/GUIA_PERFORMANCE.md) — SoA/SIMD/Arena tuning, escolha `PASSO_SIMD`, JMH.
- 🧪 [**Testes & Benchmarks**](docs/TESTES_E_BENCHMARKS.md) — JUnit 5, goldens, paridade, JMH, skill `write-tests`.
- 🔌 [**LSP & VS Code**](docs/LSP_VSCODE.md) / [**API REST**](docs/API_REST.md) — LSP4J + VS Code + Spring Boot 11 endpoints.
- 📖 [**Glossário de Linguagem Ubíqua**](docs/GLOSSARIO_LINGUAGEM_UBIQUA.md) — Termos universais de negócio, governança, arquitetura e dados em português.
- 📐 [**Gramática Formal EBNF (v2.4)**](docs/GRAMATICA.md) — Especificação rigorosa da linguagem.
- 🛠️ [**CLI, Tooling & IDEs**](docs/CLI_E_TOOLING.md) — Manual do `thz check/run/dev/fmt/doc/audit/ui/ir`, Desktop IDE, LSP e extensão VS Code.
- 🏛️ [**Conformidade e Normas Técnicas**](docs/CONFORMIDADE_E_NORMAS.md) — Adesão a ISO/IEC 10967, ISO 4217, ISO/IEC/IEEE 42010, RFCs e JSRs.
- 💡 [**Exemplos & Padrões**](docs/EXEMPLOS_E_PADROES.md) — 12 receitas de `exemplos/*.thz` + `*.thzui` reais.
- 🛠️ [**IntelliJ Setup**](docs/INTELLIJ_SETUP.md) — JDK 25 + Gradle composite + TextMate + run configs.
- 📚 [**ADRs**](docs/ADRs/README.md) — 5 decisões (LLVM vs Cranelift, Arena vs GC, i128 vs double...).
- 🆘 [**Troubleshooting**](docs/TROUBLESHOOTING.md) — FAQ por área (build, SIMD, LSP, GraalVM, LLVM, Docker).
- 📦 [**Changelog**](CHANGELOG.md) — Histórico SemVer.
- 📊 [**Relatório de Evolução Histórica**](docs/RELATORIO-EVOLUCAO.md) — Trajetória de desenvolvimento e marcos alcançados.
- 🤝 [**Guia de Contribuição**](CONTRIBUTING.md) — Diretrizes para desenvolvedores do ecossistema.

---

## 🧱 Estrutura do Monorepo

```
thz-lang/
├── compilador/                 # 🚀 Compilador Self-Hosted em .thz (driver, lexer, parser, codegen, ast, tokens)
├── exemplos/                   # 💡 Códigos de exemplo canônicos em .thz e .thzui
├── scripts/                    # 🛠️ Scripts de automação de build AOT (.exe / .elf)
├── src/
│   └── runtime/
│       └── thz_runtime.c       # 🌐 Runtime Nativo C Dual-OS (Windows Win32 / Linux POSIX)
├── JVM/                        # ☕ Monorepo do Engine JVM
│   ├── thz-core-jvm/           # Núcleo: Lexer, Parser, AST, Semântico, Runtime, DecimalFixo, IR, DocGen
│   ├── thz-cli-jvm/            # CLI executável, REPL interativo e Dev Server
│   ├── thz-gui-jvm/            # IDE Desktop Swing + FlatLaf (Editor, Gutter, Formulários Dinâmicos)
│   ├── thz-lsp-jvm/            # Language Server Protocol (LSP4J)
│   ├── thz-bench-jvm/          # Suíte de Benchmarks JMH
│   └── thz-api-jvm/            # API REST Spring Boot
├── Extensions/
│   └── thz-lsp-vscode/         # 🔌 Extensão oficial para o Visual Studio Code
├── docs/                       # 📖 Documentação técnica formal e manuais
└── dist/                       # 📦 Binários nativos executáveis gerados (.exe, .elf)
```

---

## ⚖️ Licença

Este projeto está licenciado sob a Licença MIT — consulte o arquivo [LICENSE](LICENSE) para mais detalhes.
