# THZ-LANG

<div align="center">

[![CI](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 25](https://img.shields.io/badge/Java-25%20LTS-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Rust](https://img.shields.io/badge/Rust-Runtime%20C%20ABI-DEA584.svg?logo=rust&logoColor=white)](src/runtime_rs)
[![LLVM Clang](https://img.shields.io/badge/LLVM-Clang%20AOT-red.svg?logo=llvm&logoColor=white)](https://llvm.org/)
[![Containers](https://img.shields.io/badge/Containers-Docker%20%7C%20Podman-2496ED.svg?logo=docker&logoColor=white)](docs/DOCKER_PODMAN_DEVCONTAINER.md)
[![Dev Containers](https://img.shields.io/badge/Dev%20Containers-Ready-blueviolet.svg?logo=visualstudiocode&logoColor=white)](.devcontainer/devcontainer.json)
[![Version](https://img.shields.io/badge/version-﻿0.4.0-blue.svg)](version.txt)
[![Status](https://img.shields.io/badge/Status-ativo%20%7C%20v0.4.0-green.svg)](#-visÃ£o-geral)

### A Linguagem Corporativa para Sistemas CrÃ­ticos, GovernanÃ§a Viva e Dados em Escala

**AritmÃ©tica Financeira Exata (ISO/IEC 10967) â€¢ Design by Contract â€¢ MemÃ³ria em Arena $O(1)$ â€¢ SIMD AVX-512 â€¢ Paradigma Dual â€¢ AOT Nativo â€¢ IA On-Device**

---

[VisÃ£o Geral](#-visÃ£o-geral) â€¢
[O Que Torna o THZ Ãšnico?](#-o-que-torna-o-thz-Ãºnico) â€¢
[Arquitetura do Ecossistema](#-arquitetura-do-ecossistema) â€¢
[O Showcase One-Shot](#-o-showcase-one-shot-todos-os-recursos-em-aÃ§Ã£o) â€¢
[Paradigma Dual](#-paradigma-dual-corporativo-vs-moderno) â€¢
[Quick Start](#-quick-start-em-3-passos) â€¢
[CLI & Ferramentas](#-manual-de-comandos-da-cli-17-comandos) â€¢
[Mapa do Monorepo](#-estrutura-do-monorepo) â€¢
[DocumentaÃ§Ã£o Completa](#-documentaÃ§Ã£o-oficial)

</div>

---

## ðŸŒŸ VisÃ£o Geral

O **THZ-LANG** (`.thz`, `.thzui`) Ã© uma linguagem de programaÃ§Ã£o corporativa de sistemas projetada para eliminar o abismo histÃ³rico entre as **regras de negÃ³cio do mundo corporativo** e a **engenharia de software de ultra-alta performance**.

Tradicionalmente, empresas precisam escolher entre linguagens de alto nÃ­vel legÃ­veis (onde regras de negÃ³cio sÃ£o expressivas, mas a performance e o controle de memÃ³ria sÃ£o limitados) ou linguagens de baixo nÃ­vel (rÃ¡pidas, porÃ©m excessivamente complexas para auditorias e analistas). O **THZ-LANG** resolve essa equaÃ§Ã£o combinando:

1. **GovernanÃ§a e Contratos como CÃ³digo:** ClÃ¡usulas executÃ¡veis de prÃ© e pÃ³s-condiÃ§Ãµes (`EXIGE`/`GARANTE`), invariantes e metadados arquiteturais de rastreabilidade (SOX, BACEN, LGPD) integrados nativamente na AST.
2. **AritmÃ©tica Financeira DeterminÃ­stica (ISO/IEC 10967 & ISO 4217):** ProibiÃ§Ã£o categÃ³rica de ponto flutuante binÃ¡rio IEEE 754 para moedas e decimais, garantindo precisÃ£o absoluta com arredondamento bancÃ¡rio meio-par (*Half-Even*).
3. **Engenharia Orientada a Dados (DoD):** AlocaÃ§Ã£o linear em Arenas contÃ­guas descartÃ¡veis em $O(1)$, layout colunar *Structure of Arrays* (SoA) e laÃ§os vetorizados via instruÃ§Ãµes SIMD de hardware (AVX2/AVX-512).
4. **Paradigma Dual Moderno ("Kotlin/Rust Corporativo"):** FluÃªncia completa tanto na clÃ¡ssica sintaxe corporativa estruturada em lÃ­ngua portuguesa quanto na moderna sintaxe concisa com chaves `{ ... }`, `fn`, `struct`, `var`, `val`, `=` e `:=`.
5. **Autonomia AOT e Soberania TecnolÃ³gica:** CompilaÃ§Ã£o direta para cÃ³digo de mÃ¡quina nativo (.exe / .elf) via LLVM Clang linkando com o runtime de alta performance em Rust (`src/runtime_rs`), sem qualquer dependÃªncia de JVM em produÃ§Ã£o.
6. **Tooling Industrial Completo:** Desktop IDE moderna em Swing FlatLaf, servidor LSP com depurador DAP nativo, assistente autÃ´nomo de cÃ³digo em terminal (`thz agent`) e protocolo oficial de liberaÃ§Ã£o para produÃ§Ã£o (`thz release`).

---

## ðŸ’Ž O Que Torna o THZ Ãšnico?

| Pilar | Abordagem Tradicional (Java / Python / Go) | Abordagem THZ-LANG | BenefÃ­cio Real |
| :--- | :--- | :--- | :--- |
| **AritmÃ©tica MonetÃ¡ria** | `double`/`float` geram dÃ­zimas; `BigDecimal` gera overhead massivo no Heap | Inteiros escalados `DecimalFixo` e `i128` nativos com escala fixa | **Zero desvio financeiro** com velocidade de inteiros de hardware |
| **Contratos de NegÃ³cio** | AnotaÃ§Ãµes `@Valid`, `assert` desligado ou `if` disperso | ClÃ¡usulas formais de primeira classe: `EXIGE`, `GARANTE`, `INVARIANTE` | **Auditoria 100% verificÃ¡vel** em tempo de compilaÃ§Ã£o e execuÃ§Ã£o |
| **Processamento Massivo** | `List<Objeto>` com ponteiros espalhados, cache misses e GC pauses | `LAYOUT_COLUNAR` (SoA), `USAR_BLOCO_MEMORIA` (Arena $O(1)$) e SIMD | **VetorizaÃ§Ã£o AVX nativa** e descarte de memÃ³ria contÃ­gua em 0ms |
| **Arquitetura Viva** | DocumentaÃ§Ã£o estÃ¡tica desatualizada no Confluence | `METADADOS_ARQUITETURA` e `RASTREIO_REQUISITO` compilÃ¡veis | **Rastreabilidade regulatÃ³ria** (SOX, BACEN, LGPD) ligada ao cÃ³digo |
| **IA & Machine Learning** | DependÃªncia de ecossistemas pesados em Python / C++ | Embeddings determinÃ­sticos e ML tabular on-device (`IA.*`, `ML.*`) | **Zero latÃªncia externa**, zero custos de API e soberania LGPD |
| **Entrega para ProduÃ§Ã£o** | Deploys baseados em pipelines opacos de CI | Protocolo criptogrÃ¡fico `thz release` com evidÃªncias SHA-256 | **Laudo formal auditÃ¡vel** emitido a cada homologaÃ§Ã£o |

---

## ðŸ›ï¸ Arquitetura do Ecossistema

O THZ-LANG adota uma arquitetura unificada de camadas onde a **experiÃªncia do desenvolvedor**, o **nÃºcleo de governanÃ§a** e os **mecanismos de execuÃ§Ã£o** operam em perfeita sintonia:

```mermaid
flowchart TB
    subgraph DEV_TOOLS["ðŸ’» Camada de ApresentaÃ§Ã£o & Ferramentas"]
        direction LR
        VSCODE["ðŸ”Œ VS Code / Antigravity<br/>(LSP4J + DAP + TextMate)"]
        GUI["ðŸ–¥ï¸ Desktop IDE FlatLaf<br/>(thz gui / Swing Dark-Light)"]
        CLI["âš¡ CLI Unificada & REPL<br/>(thz run / dev / check / repl)"]
        AGENT["ðŸ¤– Agente AutÃ´nomo de IA<br/>(thz agent / ReAct + RAG)"]
        API["ðŸŒ Spring Boot REST API<br/>(thz-api-jvm / Porta 8080)"]
    end

    subgraph CORE_ENGINE["â˜• NÃºcleo do Compilador (Java 25 Multi-MÃ³dulo)"]
        direction TB
        PARSER["LÃ©xico & Parser Dual<br/>(CanÃ´nico PT-BR & Moderno Kotlin/Rust)"]
        AST["Ãrvore de Sintaxe Abstrata (AST)<br/>Sealed Records & Metadados de Arquitetura"]
        SEMANTIC["AnÃ¡lise SemÃ¢ntica & Design by Contract<br/>(EXIGE â€¢ GARANTE â€¢ INVARIANTE â€¢ Tipagem EstÃ¡tica)"]
        
        subgraph MODULES["Bibliotecas Nativas & Conectores"]
            direction LR
            BRASIL["ðŸ‡§ðŸ‡· BRASIL.*<br/>(PIX, Boletos, CEPs, CPF)"]
            DATA["ðŸ“Š DATA & DAX.*<br/>(ETL, Planilhas, EstatÃ­stica)"]
            CONNECT["ðŸ—„ï¸ BANCO & MENSAGERIA<br/>(JPA, RawSQL, Kafka, SQS)"]
            IA_ML["ðŸ§  IA & ML.*<br/>(Embeddings, RegressÃ£o, Cosine)"]
        end

        IR_GEN["Gerador de CÃ³digo IntermediÃ¡rio<br/>(THZ-IR/1 & LLVM IR Builder)"]
        RELEASE_PROT["Protocolo de LiberaÃ§Ã£o<br/>(thz release â€¢ Assinatura SHA-256)"]

        PARSER --> AST --> SEMANTIC --> MODULES --> IR_GEN
        SEMANTIC --> RELEASE_PROT
    end

    subgraph RUNTIMES["âš™ï¸ Runtimes & Alvos de ExecuÃ§Ã£o"]
        direction LR
        subgraph JVM_TARGET["Pipeline JVM 25"]
            INTERP["Interpretador de Alta VazÃ£o"]
            VTHREADS["Project Loom Virtual Threads"]
            DOCS_PDF["Motor Corporativo OpenPDF/POI"]
        end

        subgraph NATIVE_TARGET["Pipeline Nativo AOT (Zero JVM)"]
            LLVM_CLANG["LLVM Clang Compiler<br/>(scripts/build-llvm.ps1/.sh)"]
            RUST_RUNTIME["ðŸ¦€ Runtime Nativo Rust (src/runtime_rs)<br/>â€¢ Alocador de Arena O(1)<br/>â€¢ VetorizaÃ§Ã£o SIMD AVX2/AVX-512<br/>â€¢ Criptografia Militar Argon2id/AES-GCM<br/>â€¢ Bridge WebAssembly W3C"]
            LLVM_CLANG --> RUST_RUNTIME
        end
    end

    subgraph ARTIFACTS["ðŸ“¦ Artefatos Finais Gerados"]
        direction LR
        ELF_PE["BinÃ¡rios Nativos AutÃ´nomos<br/>(.exe PE Windows / .elf Linux)"]
        WASM_BIN["MÃ³dulo WebAssembly<br/>(.wasm Browser / Edge)"]
        VSIX_PKG["ExtensÃ£o Oficial IDE<br/>(.vsix para VS Code/Antigravity)"]
        BOOK_PDF["Manual TÃ©cnico Unificado<br/>(MANUAL_THZ_LANG.pdf)"]
    end

    DEV_TOOLS --> CORE_ENGINE
    IR_GEN --> NATIVE_TARGET
    AST --> JVM_TARGET
    NATIVE_TARGET --> ELF_PE
    NATIVE_TARGET --> WASM_BIN
    DEV_TOOLS --> VSIX_PKG
    JVM_TARGET --> BOOK_PDF

    classDef primary fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff;
    classDef highlight fill:#0f172a,stroke:#10b981,stroke-width:2px,color:#fff;
    classDef rust fill:#2d1b14,stroke:#f97316,stroke-width:2px,color:#fff;
    classDef output fill:#111827,stroke:#8b5cf6,stroke-width:2px,color:#fff;

    class DEV_TOOLS primary;
    class CORE_ENGINE highlight;
    class RUST_RUNTIME rust;
    class ARTIFACTS output;
```

---

## ðŸ”¥ O Showcase One-Shot: Todos os Recursos em AÃ§Ã£o!

Abaixo estÃ¡ um programa completo demonstrando em um Ãºnico arquivo a expressividade, o rigor matemÃ¡tico e a engenharia de alta performance do **THZ-LANG**:

```thz
programa SistemaFinanceiroConsolidado {

    // 1. ARQUITETURA VIVA: GovernanÃ§a, Rastreio RegulatÃ³rio e Metadados Formais
    metadados {
        SISTEMA: "LiquidacaoInstantanea"
        MODULO: "MotorTributarioESIMD"
        DOMINIO: "Financeiro"
        VERSAO: "0.4.0"
        SLO_LATENCIA_MS: 10
        CONFORMIDADE: "ISO-10967", "BACEN-Res4893", "LGPD-Art7"
    }

    // 2. ENGENHARIA ORIENTADA A DADOS (DoD): Layout Colunar (Structure-of-Arrays) para SIMD
    struct TransacaoLote LAYOUT_COLUNAR {
        id: String,
        cpfTitular: String,
        quantidade: Int,
        valorUnitario: Decimal(18, 2),
        aliquotaImposto: Decimal(5, 4),
        valorLiquido: Decimal(18, 2)
    }

    // 3. FUNÃ‡Ã•ES PURAS & DESIGN BY CONTRACT
    fn calcularTarifa(base: Decimal(18, 2)): Decimal(18, 2) = base * 0.0015

    regra ProcessarLoteFiscal {
        RASTREIO_REQUISITO: "REQ-PIX-2026"
        exige: tamanho(lote) > 0
        garante: lote.valorLiquido[0] >= 0.00

        operacao ExecutarCalculo(lote: TransacaoLote) {
            // 4. MEMÃ“RIA EM ARENA O(1): AlocaÃ§Ã£o contÃ­gua sem Garbage Collection
            usar_bloco_memoria {
                
                // 5. VETORIZAÃ‡ÃƒO SIMD: AceleraÃ§Ã£o vetorial por hardware (AVX2 / AVX-512)
                vetorizar_para i de 0 ate tamanho(lote) - 1 passo_simd 8 {
                    var bruto = lote.quantidade[i] * lote.valorUnitario[i]
                    var imposto = bruto * lote.aliquotaImposto[i]
                    lote.valorLiquido[i] = bruto - imposto
                }

            }
        }
    }

    // 6. PROCEDIMENTO PRINCIPAL (PONTO DE ENTRADA)
    fn main(): Int {
        print "=== THZ-LANG v0.4.0: MOTOR EXECUTIVO & PERFORMANCE ==="

        // 7. BRASIL DIGITAL: ValidaÃ§Ã£o fiscal de documentos e geraÃ§Ã£o de PIX EMVco
        var cpfValido = BRASIL.validarCPF("123.456.789-00")
        var payloadPix = BRASIL.gerarPix("chave@empresa.com.br", 1500.50, "SP", "PAGTO-01")
        print "Payload PIX Copia e Cola:"
        print payloadPix

        // 8. CONSULTAS TIPADAS INTEGRADAS (LINQ / Query DSL)
        var contasAprovadas = consultar Conta
                              onde saldo > 1000.00 e status = "ATIVO"
                              ordenar_por saldo desc
                              limite 10

        // 9. CONECTORES DE BANCO & MENSAGERIA DISTRIBUÃDA
        BANCO.executar("UPDATE parametros SET ultimo_processamento = CURRENT_TIMESTAMP")
        MENSAGERIA.publicar("topico.liquidacoes", "Lote processado com sucesso!")

        // 10. IA & MACHINE LEARNING ON-DEVICE (Zero DependÃªncia de Python)
        var embedding1 = IA.embedding("TransaÃ§Ã£o Financeira Suspeita")
        var embedding2 = IA.embedding("TransferÃªncia de Alto Valor")
        var similaridade = IA.similaridade(embedding1, embedding2)
        print "Similaridade SemÃ¢ntica Calculada On-Device:"
        print similaridade

        print "Processamento concluÃ­do com sucesso e conformidade ISO/IEC 10967 garantida."
        retorne 0
    }
}
```

> [!TIP]
> O cÃ³digo acima compila e executa **tanto no interpretador JVM 25** com Virtual Threads quanto em **binÃ¡rio nativo compilado AOT via LLVM Clang (.exe / .elf)**, linkando com o runtime Rust sem alteraÃ§Ã£o de uma Ãºnica linha de cÃ³digo.

---

## âš–ï¸ Paradigma Dual: Corporativo vs Moderno

O THZ-LANG entende que diferentes contextos exigem diferentes nÃ­veis de cerimÃ´nia. Por isso, a linguagem oferece **Paridade 1:1** entre o estilo clÃ¡ssico corporativo e o estilo moderno conciso:

<div align="center">

| Recurso | Estilo Corporativo ClÃ¡ssico (PT-BR) | Estilo Moderno ("Kotlin/Rust Corporativo") |
| :--- | :--- | :--- |
| **DeclaraÃ§Ã£o de MÃ³dulo** | `PROGRAMA NEGOCIO Faturamento ... FIM_PROGRAMA` | `programa Faturamento { ... }` |
| **Metadados de Arquitetura** | `METADADOS_ARQUITETURA ... FIM_METADADOS` | `metadados { ... }` |
| **DefiniÃ§Ã£o de Tipos** | `ESTRUTURA Pedido ... FIM_ESTRUTURA` | `struct Pedido { ... }` |
| **DeclaraÃ§Ã£o de VariÃ¡vel** | `VARIAVEL total : INTEIRO <- 100` | `var total: Int = 100` ou `var total = 100` |
| **Constante ImutÃ¡vel** | `CONSTANTE taxa : DECIMAL <- 0.05` | `val taxa: Decimal = 0.05` |
| **FunÃ§Ãµes & ExpressÃµes** | `FUNCAO somar(a: INT): INT ... FIM_FUNCAO` | `fn somar(a: Int, b: Int): Int = a + b` |
| **Condicionais** | `SE condicao ENTAO ... SENAO ... FIM_SE` | `se condicao { ... } senao { ... }` |
| **LaÃ§os de RepetiÃ§Ã£o** | `PARA i DE 0 ATE 10 PASSO 1 ... FIM_PARA` | `para i de 0 ate 10 passo 1 { ... }` |
| **SaÃ­da no Console** | `EXIBA "Mensagem"` | `print "Mensagem"` |
| **Retorno de FunÃ§Ã£o** | `RETORNE total` | `ret total` ou `retorne total` |

</div>

---

## âš¡ Quick Start em 3 Passos

### 1. Clonar o RepositÃ³rio
```bash
git clone https://github.com/thz-lang/thz-lang.git
cd thz-lang
```

### 2. Verificar o Ambiente e Executar os Testes
O projeto conta com scripts multiplataforma e o wrapper oficial do Gradle:

```bash
# DiagnÃ³stico completo do ambiente (Java 25, Clang, Rust, Node, etc.)
powershell -ExecutionPolicy Bypass -File scripts/health-check.ps1  # Windows
./scripts/health-check.sh                                         # Linux / macOS

# Executar a suÃ­te de testes JUnit 5
./gradlew test
```

### 3. Rodar seu Primeiro Programa
```bash
# ExecuÃ§Ã£o na JVM com hot reload e anÃ¡lise semÃ¢ntica:
./gradlew cli --args="run exemplos/gestao_pedidos_moderno.thz"

# Iniciar a Desktop IDE moderna Swing + FlatLaf:
./gradlew gui

# Iniciar o Agente AutÃ´nomo de IA no terminal:
./gradlew cli --args="agent"
```

---

## ðŸ› ï¸ Manual de Comandos da CLI (17 Comandos)

A ferramenta de linha de comando (`thz`) Ã© a central de operaÃ§Ãµes do ecossistema. Use `./gradlew cli --args="<comando>"` ou invoque o binÃ¡rio nativo `thz`:

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                      THZ-LANG CLI COMMAND SUITE                        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

| Comando | DescriÃ§Ã£o | Sintaxe TÃ­pica |
| :--- | :--- | :--- |
| **`check`** | AnÃ¡lise lÃ©xica, sintÃ¡tica, verificaÃ§Ã£o de contratos e lint | `thz check pedido.thz --estrito` |
| **`run`** | ExecuÃ§Ã£o rÃ¡pida de programas (canÃ´nicos ou com `fn main`) | `thz run faturamento.thz` |
| **`dev`** | Servidor de desenvolvimento com Live Reload instantÃ¢neo | `thz dev faturamento.thz` |
| **`audit`** | Matriz de governanÃ§a, auditoria e rastreabilidade com Git | `thz audit pedido.thz --git` |
| **`release`** / **`liberar`** | Protocolo de homologaÃ§Ã£o com evidÃªncias e assinatura SHA-256 | `thz release fat.thz --dados homolog.json` |
| **`agent`** | Inicia o Agente AutÃ´nomo de IA para codificaÃ§Ã£o e RAG | `thz agent --modelo llama3` |
| **`init`** | Inicializa um novo projeto criando o manifesto `thz.config.json` | `thz init meu_sistema` |
| **`compile`** | CompilaÃ§Ã£o AOT de um arquivo para cÃ³digo de mÃ¡quina nativo | `thz compile app.thz --alvo ambos` |
| **`compile-all`** | CompilaÃ§Ã£o em lote de todos os programas do projeto | `thz compile-all` |
| **`fmt`** | Formatador de cÃ³digo idempotente | `thz fmt --escrever pedido.thz` |
| **`doc`** | GeraÃ§Ã£o de documentaÃ§Ã£o viva em Markdown e diagramas Mermaid | `thz doc pedido.thz --saida docs/` |
| **`ui`** | Renderizador e compilador de formulÃ¡rios `.thzui` em HTML5 | `thz ui tela.thzui --html` |
| **`ir`** | InspeÃ§Ã£o da representaÃ§Ã£o intermediÃ¡ria THZ-IR e LLVM IR | `thz ir app.thz --llvm` |
| **`ast`** | Dump estruturado da Ãrvore de Sintaxe Abstrata (AST) em JSON | `thz ast app.thz` |
| **`livro`** / **`manual`** | CompilaÃ§Ã£o de todos os tratados tÃ©cnicos em PDF unificado | `thz livro --saida dist/MANUAL.pdf` |
| **`repl`** | Shell interativo multi-linha com inspeÃ§Ã£o em tempo real | `thz repl` |
| **`gui`** | InicializaÃ§Ã£o da Desktop IDE Swing FlatLaf | `thz gui` |

---

## ðŸš€ CompilaÃ§Ã£o Nativa AOT (Zero DependÃªncia de JVM)

Para ambientes de produÃ§Ã£o com contÃªineres mÃ­nimos (`scratch`/`alpine`), o THZ-LANG disponibiliza compilaÃ§Ã£o AOT que gera binÃ¡rios nativos autÃ´nomos linkando diretamente com o runtime em Rust:

```bash
# No Windows (PowerShell): Gera executÃ¡vel PE (.exe)
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz exemplos/gestao_pedidos_moderno.thz

# No Linux (Bash): Gera executÃ¡vel ELF (.elf)
./scripts/build-llvm.sh exemplos/gestao_pedidos_moderno.thz

# Executar o binÃ¡rio de cÃ³digo de mÃ¡quina nativo gerado:
./dist/bin/gestao_pedidos_moderno.exe   # Windows
./dist/bin/gestao_pedidos_moderno.elf   # Linux
```

> [!NOTE]
> Os binÃ¡rios nativos gerados nÃ£o contÃªm bytecode, nÃ£o necessitam do runtime Java instalado e inicializam em menos de **3 milissegundos**, consumindo menos de **12 MB** de memÃ³ria residente.

---

## ðŸ³ Docker, Podman & Dev Containers

Execute e desenvolva sem necessidade de instalar dependÃªncias locais:

```bash
# Subir a API REST e o ambiente em contÃªiner (auto-detecta Podman ou Docker):
npm run docker:up

# REPL interativo dentro do contÃªiner:
npm run docker:repl

# Testes automatizados dentro do contÃªiner:
npm run docker:test
```
ðŸ‘‰ [Consulte o Guia Completo de Docker, Podman e Dev Containers](docs/DOCKER_PODMAN_DEVCONTAINER.md)

---

## ðŸ§± Estrutura do Monorepo

```
thz-lang/
â”œâ”€â”€ compilador/                 # ðŸš€ Compilador Self-Hosted escrito em THZ (.thz)
â”œâ”€â”€ exemplos/                   # ðŸ’¡ ColeÃ§Ã£o canÃ´nica e moderna de exemplos reais
â”œâ”€â”€ scripts/                    # ðŸ› ï¸ SuÃ­te multiplataforma de automaÃ§Ã£o (.ps1 e .sh)
â”œâ”€â”€ src/
â”‚   â””â”€â”€ runtime_rs/             # ðŸ¦€ Runtime Nativo Oficial em Rust com C ABI (Arena, SIMD, Crypto, WASM)
â”œâ”€â”€ JVM/                        # â˜• Monorepo Java 25 (Gradle Composite Build)
â”‚   â”œâ”€â”€ thz-core-jvm/           # NÃºcleo: Lexer, Parser, AST, SemÃ¢ntico, Runtime, DecimalFixo, IR, DAP
â”‚   â”œâ”€â”€ thz-cli-jvm/            # CLI unificada (17 comandos), REPL e Dev Server
â”‚   â”œâ”€â”€ thz-gui-jvm/            # Desktop IDE Swing FlatLaf (Editor, Gutter, FormulÃ¡rios)
â”‚   â”œâ”€â”€ thz-lsp-jvm/            # Servidor LSP oficial (LSP4J)
â”‚   â”œâ”€â”€ thz-agent-jvm/          # Agente AutÃ´nomo de IA em terminal (ReAct, RAG, Tools)
â”‚   â”œâ”€â”€ thz-bench-jvm/          # Microbenchmarks de alta precisÃ£o JMH
â”‚   â””â”€â”€ thz-api-jvm/            # API REST Spring Boot para integraÃ§Ã£o externa
â”œâ”€â”€ Extensions/
â”‚   â””â”€â”€ thz-lsp-vscode/         # ðŸ”Œ ExtensÃ£o oficial para VS Code e Antigravity IDE
â”œâ”€â”€ docs/                       # ðŸ“– Manuais tÃ©cnicos formais, tratados arquiteturais e ADRs
â””â”€â”€ dist/                       # ðŸ“¦ Pacotes e binÃ¡rios finais gerados (.exe, .elf, .jar, .vsix, .pdf)
```

---

## ðŸ“– DocumentaÃ§Ã£o Oficial

Explore a suÃ­te completa de documentaÃ§Ã£o tÃ©cnica do ecossistema:

<div align="center">

| Categoria | Documento | ConteÃºdo Principal |
| :--- | :--- | :--- |
| **Fundamentos** | ðŸ“˜ [**Manual Completo da Linguagem**](docs/MANUAL_LINGUAGEM.md) | Guia completo da sintaxe, tipagem, contratos e regras |
| **Arquitetura** | âš™ï¸ [**Arquitetura de CompilaÃ§Ã£o Nativa**](docs/ARQUITETURA_COMPILACAO_NATIVA.md) | Tratado completo de IR/IL, LLVM Clang, GraalVM e AOT |
| **Performance** | ðŸ§± [**Runtime Nativo Rust**](docs/RUNTIME_NATIVO.md) | Rust C ABI, Arenas $O(1)$, SIMD AVX-512 e WASM |
| **Performance** | âš¡ [**Guia de Performance & Tuning**](docs/GUIA_PERFORMANCE.md) | Ajustes finos de SoA, SIMD, Arenas e mÃ©tricas JMH |
| **Conectores** | ðŸ—„ï¸ [**Banco de Dados & Mensageria**](docs/CONECTORES_BANCO_E_MENSAGERIA.md) | ORM JPA-like, Raw SQL, Busca KNN, RabbitMQ, Kafka e SQS |
| **InovaÃ§Ã£o** | ðŸ‡§ðŸ‡· [**Brasil Digital & Snapshot Engine**](docs/BRASIL_DIGITAL_E_SNAPSHOT_ENGINE.md) | PIX EMVco, Boletos Febraban, CEPs offline `.thzdbi` e Snapshots |
| **Analytics** | ðŸ“Š [**Engenharia de Dados & Analytics**](docs/ENGENHARIA_DE_DADOS_E_ANALYTICS.md) | MÃ³dulos DAX, estatÃ­stica descritiva, PROCV e sanitizaÃ§Ã£o |
| **Big Data** | ðŸŒŠ [**Pipelines de Dados Massivos**](docs/PIPELINE_DADOS.md) | IngestÃ£o, transformaÃ§Ã£o colunar e streaming reativo |
| **Interface** | ðŸ–¼ï¸ [**DSL Visual TELA / .thzui**](docs/TELA_THZUI.md) | ConstruÃ§Ã£o declarativa de interfaces grÃ¡ficas e WebView |
| **Tooling** | ðŸ› ï¸ [**Manual de CLI & Ferramentas**](docs/CLI_E_TOOLING.md) | ReferÃªncia completa dos 17 comandos da CLI e IDEs |
| **Conformidade**| ðŸ›ï¸ [**Conformidade & Normas TÃ©cnicas**](docs/CONFORMIDADE_E_NORMAS.md) | AderÃªncia a ISO/IEC 10967, ISO 4217, ISO 42010 e LGPD |
| **DecisÃµes** | ðŸ“š [**ADRs (Registros Arquiteturais)**](docs/ADRs/README.md) | 6 decisÃµes formais (LLVM, Arenas, i128, FlatLaf, Sintaxe...) |
| **EstratÃ©gia** | ðŸ—ºï¸ [**Roadmap EstratÃ©gico**](docs/ROADMAP.md) | Os 5 pilares de evoluÃ§Ã£o tÃ©cnica rumo Ã  v1.0.0 estÃ¡vel |
| **GovernanÃ§a** | ðŸ“¦ [**Changelog Oficial**](CHANGELOG.md) | HistÃ³rico de releases em conformidade com SemVer 2.0.0 |
| **Comunidade** | ðŸ¤ [**Guia de ContribuiÃ§Ã£o**](CONTRIBUTING.md) | Normas e diretrizes para colaboradores e desenvolvedores |

</div>

---

## âš–ï¸ LicenÃ§a

Este projeto estÃ¡ licenciado sob a [LicenÃ§a MIT](LICENSE) â€” livre para uso corporativo, acadÃªmico e comercial.

