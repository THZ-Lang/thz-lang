# THZ-LANG

**Corporate Systems Programming Language, Business Domain Governance (DDD), Living Architecture, and High-Performance Data Processing.**

Overview • Pillars • Ubiquitous Glossary • Archetypes • Canonical Example • Quick Start • Official Documentation

---

## Overview

**THZ-LANG** (`.thz`, `.thzui`) is a domain-driven systems programming language (DDD) featuring statically-typed grammar, formal built-in governance contracts, and high-performance native compilation. It was engineered to bridge executive readability with the raw efficiency of contiguous memory processing, ephemeral arenas, and SIMD vectorization.

The repository unifies Java 25 execution engines (JVM Multi-module), a self-hosted THZ compiler (`compilador/`), a native AOT compilation backend via LLVM Clang, full CLI tooling (`thz`), a FlatLaf Swing Desktop IDE (`thz gui`), language servers (LSP), and an official VS Code extension.

---

## Architectural Pillars

| Pillar | Principle | Technical Implementation |
| :--- | :--- | :--- |
| **1. Business Governance** | Living Architecture & Traceability | First-class AST node `ARCHITECTURE_METADATA`, requirement traceability tags, and Git diff compliance auditing. |
| **2. Design by Contract (DbC)** | Provable System Invariants | Formal preconditions (`REQUIRES`), postconditions (`ENSURES`), structure invariants, and idempotency guarantees. |
| **3. Exact Financial Arithmetic** | Zero Float Approximations | Strict **ISO/IEC 10967** adherence: 100% exact scaled decimal integers (`DecimalFixed`), ISO 4217 multi-currency safety, and Half-Even banker's rounding. |
| **4. High Performance & SIMD** | Continuous Vectorized Pipelines | Ephemeral memory arenas ($O(1)$ allocation/deallocation), Structure-of-Arrays (`COLUMNAR_LAYOUT`), and AVX2/AVX-512 vectorization (`VECTORIZE_FOR`). |
| **5. Dual Dialects & Portability** | Universal Global Portability | Dual canonical dialects (`pt-BR` and `en-US`), Cross-platform JVM 25 / Native AOT (Linux, Windows, macOS, Docker). |

---

## Dual Dialect Architecture (EN-US & PT-BR)

THZ-LANG natively supports two single-dialect modes:
- **`LANGUAGE: en-US`**: International standard English syntax.
- **`LINGUAGEM: pt-BR`**: Brazilian Portuguese structured business syntax.

### Keyword Equivalence Reference

```thz
# LANGUAGE: en-US
PROGRAM OrderBilling

ARCHITECTURE_METADATA
    DOMAIN: "Finance"
    VERSION: "2.4.0"
    AUTHOR: "Lucas Thomaz"
    MAX_LATENCY_SLO: "5ms"
    COMPLIANCE: "ISO-10967", "BACEN-Res4893"
END_METADATA

STRUCTURE InvoiceItem
    id : TEXT
    quantity : INTEGER
    unitPrice : DECIMAL(18, 2)
END_STRUCTURE

BUSINESS_RULE ProcessBilling
    INPUT_CONTRACT
        REQUIRES quantity > 0
        REQUIRES unitPrice > 0.00
    END_INPUT_CONTRACT

    OUTPUT_CONTRACT
        ENSURES total >= unitPrice
    END_OUTPUT_CONTRACT

    OPERATION Execute(item: InvoiceItem, taxRate: DECIMAL(5, 4)) : DECIMAL(18, 2)
    BEGIN
        VARIABLE subtotal : DECIMAL(18, 2) <- item.unitPrice * item.quantity
        VARIABLE tax : DECIMAL(18, 2) <- subtotal * taxRate
        VARIABLE total : DECIMAL(18, 2) <- subtotal + tax
        IF total > 1000.00 THEN
            PRINT "High priority invoice"
        END_IF
        RETURN total
    END
END_BUSINESS_RULE

END_PROGRAM
```

---

## Quick Start

### Installation & CLI Usage

```bash
# Verify program syntax & contracts
./thz check exemplos/faturamento.thz

# Execute script with JVM engine
./thz run exemplos/faturamento.thz

# Launch the Desktop IDE
./thz gui

# Generate documentation & audit reports
./thz doc exemplos/faturamento.thz
./thz audit --git

# Compile complete bilingual PDF books
npm run livro
```
