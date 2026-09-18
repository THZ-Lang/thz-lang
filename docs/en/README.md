# THZ-LANG

<div align="center">

[![CI](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml/badge.svg)](https://github.com/thz-lang/thz-lang/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
[![Rust](https://img.shields.io/badge/Rust-Runtime%20C%20ABI-DEA584.svg)](../../src/runtime_rs)
[![LLVM](https://img.shields.io/badge/LLVM-Clang%20AOT-red.svg)](https://llvm.org/)
[![Version](https://img.shields.io/badge/version-0.4.0-blue.svg)](../../version.txt)

**Corporate Systems Programming Language, Business Domain Governance (DDD), Living Architecture, and High-Performance Data Processing.**

[Overview](#-overview) •
[Pillars](#-architectural-pillars) •
[Modern Dual Paradigm](#-modern-dual-paradigm) •
[Canonical Example](#-canonical-examples) •
[Quick Start](#-quick-start) •
[Official Documentation](#-official-documentation)

</div>

---

## 🌟 Overview

**THZ-LANG** (`.thz`, `.thzui`) is a domain-driven systems programming language (DDD) featuring statically-typed grammar, formal built-in governance contracts, dual dialects (English and Brazilian Portuguese), and high-performance native Ahead-of-Time (AOT) compilation. It bridges executive business clarity with contiguous columnar memory layout, ephemeral arenas, and SIMD vectorization.

The repository unifies:
1. **Core & Tooling Engine (Java 25):** Multi-module Gradle build encompassing core semantics (`thz-core-jvm`), unified CLI (`thz-cli-jvm`), Swing FlatLaf Desktop IDE (`thz-gui-jvm`), Language Server Protocol (`thz-lsp-jvm`), Autonomous AI Coding Agent (`thz-agent-jvm`), JMH Benchmarks (`thz-bench-jvm`), and REST API (`thz-api-jvm`).
2. **Official Native Rust Runtime (`src/runtime_rs/`):** High-performance native layer exporting pure C ABI for $O(1)$ memory arenas, SIMD vectorization (AVX2/AVX-512), military-grade cryptography (Argon2id/AES-256-GCM), on-device ML/embeddings, and WebAssembly (WASM).
3. **Native AOT Pipeline (Zero JVM in Production):** LLVM Clang Dual-OS backend (`scripts/build-llvm.ps1` and `scripts/build-llvm.sh`) compiling `.thz` sources directly into standalone executables (.exe / .elf) statically linked with the Rust runtime.
4. **Modern Dual Paradigm ("Corporate Kotlin/Rust"):** Ergonomic syntax (`var`, `val`, `let`, `fn`, `struct`, `ret`, `print`, `{ ... }`, `=`, `:=`) coexisting in 100% harmony and parity with classical corporate syntax (`PROGRAM`, `STRUCTURE`, `BUSINESS_RULE`, etc.).
5. **Living Architecture & AI Tooling:** Evidence-based production release protocol (`thz release`) and autonomous in-terminal AI coding assistant (`thz agent`).

---

## 💎 Architectural Pillars

| Pillar | Principle | Technical Implementation |
| :--- | :--- | :--- |
| **1. Business Governance** | Living Architecture & Traceability | First-class AST node `ARCHITECTURE_METADATA`, requirement traceability tags, and production release protocol (`thz release`). |
| **2. Design by Contract (DbC)** | Provable System Invariants | Formal preconditions (`REQUIRES` / `EXIGE`), postconditions (`ENSURES` / `GARANTE`), structure invariants, and idempotency guarantees. |
| **3. Exact Financial Arithmetic** | Zero Float Approximations | Strict **ISO/IEC 10967** adherence: 100% exact scaled decimal integers (`DecimalFixo`), ISO 4217 multi-currency safety, and Half-Even banker's rounding. |
| **4. High Performance & SIMD** | Continuous Vectorized Pipelines | Ephemeral memory arenas ($O(1)$ allocation/deallocation), Structure-of-Arrays (`COLUMNAR_LAYOUT` / `LAYOUT_COLUNAR`), and AVX2/AVX-512 vectorization (`VECTORIZE_FOR`). |
| **5. Native Rust Runtime** | Provable Memory Safety & Speed | Pure C ABI export from `src/runtime_rs/` for LLVM Clang linking, Project Panama, and WebAssembly targets. |
| **6. Autonomous AI Coding Agent** | Sovereign On-Device Developer | Embedded terminal agent (`thz agent`) with ReAct goal-driven loop, approval gate, file patching tools, and local/API LLM support. |

---

## 🚀 Modern Dual Paradigm

THZ-LANG supports modern compact notation alongside classic declarative grammar:

```thz
programa GestaoPedidosModerno {
    metadados {
        DOMINIO: "Logistics"
        CAMADA: "Domain"
        VERSAO: "0.4.0"
        AUTOR: "THZ Engineering"
    }

    struct ItemPedido {
        id: Int,
        descricao: String,
        quantidade: Int,
        precoUnitario: Int
    }

    fn calcularBonus(pontos: Int): Int = pontos * 10

    fn main(): Int {
        var limite = 500
        var totalVendas = 0

        para i de 1 ate 5 passo 1 {
            totalVendas = totalVendas + 100
        }

        se totalVendas >= limite {
            print "Sales target achieved!"
        } senao {
            print "Below target."
        }

        var bonus = calcularBonus(5)
        print "Total Sales:"
        print totalVendas
        print "Calculated Bonus:"
        print bonus

        retorne totalVendas + bonus
    }
}
```

---

## ⚡ Quick Start

### Installation & CLI Usage

```bash
# Run unit tests across all JVM modules
./gradlew test

# Static analysis and strict contract checking
./gradlew cli --args="check exemplos/faturamento.thz --estrito"

# Run program (interpreted mode or top-level main function)
./gradlew cli --args="run exemplos/gestao_pedidos_moderno.thz"

# Launch in-terminal autonomous AI coding assistant
./gradlew cli --args="agent"

# Run formal production release verification with real data
./gradlew cli --args="release exemplos/faturamento.thz --dados dados/homologacao.json"

# Launch the Desktop Swing FlatLaf IDE
./gradlew gui

# Compile standalone native executable via LLVM Clang (linking Rust runtime):
# Linux:
./scripts/build-llvm.sh exemplos/gestao_pedidos_moderno.thz
# Windows (PowerShell):
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz exemplos/gestao_pedidos_moderno.thz
```

---

## 📖 Official Documentation

- 📘 [**Language Manual**](MANUAL_LANGUAGE.md) — Comprehensive guide on syntax, contracts, and type system.
- ⚙️ [**Native Compilation Architecture**](NATIVE_COMPILATION_ARCHITECTURE.md) — Deep dive into IR/IL, LLVM Clang, and AOT compilation.
- 🧱 [**Native Rust Runtime**](NATIVE_RUNTIME.md) — Rust C ABI, memory arenas, and SIMD internals.
- 🏛️ [**Compliance & Standards**](COMPLIANCE_AND_STANDARDS.md) — ISO/IEC 10967, ISO 4217, and RFC conformance.
- 🔌 [**LSP & VS Code Extension**](LSP_VSCODE.md) — Language Server Protocol and VS Code setup.
- 📚 [**ADRs Index**](ADRs/README.md) — Architectural Decision Records.
- 🤝 [**Contributing Guide**](CONTRIBUTING.md) — Guidelines for engine contributors.
