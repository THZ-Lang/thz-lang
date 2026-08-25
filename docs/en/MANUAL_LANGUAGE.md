# Official THZ-LANG Language Manual (v2.4.0)

Welcome to the **Official THZ-LANG Reference Manual**, the domain-driven systems programming language designed for **Business Governance**, **Design by Contract (DbC)**, **Living Architecture**, **High-Integrity Financial Arithmetic (ISO/IEC 10967)**, and **High-Performance Data Engineering (SIMD / Ephemeral Arenas)**.

---

## 📚 Table of Contents
1. [Design Philosophy & Dual-Dialect Architecture (EN-US & PT-BR)](#1-design-philosophy--dual-dialect-architecture-en-us--pt-br)
2. [Dialect Directives & Syntax Equivalences](#2-dialect-directives--syntax-equivalences)
3. [Living Architecture & Metadata (`ARCHITECTURE_METADATA`)](#3-living-architecture--metadata-architecture_metadata)
4. [Data Types & Exact Arithmetic (ISO/IEC 10967 & ISO 4217)](#4-data-types--exact-arithmetic-isoiec-10967--iso-4217)
5. [Global Module Syntax & Archetypes](#5-global-module-syntax--archetypes)
6. [Structures, Enums, and Columnar Layouts (SoA)](#6-structures-enums-and-columnar-layouts-soa)
7. [Design by Contract (DbC) & Governance](#7-design-by-contract-dbc--governance)
8. [Variables, Functions, and Control Flow](#8-variables-functions-and-control-flow)
9. [Idiomatic Error Handling (`RESULT`)](#9-idiomatic-error-handling-result)
10. [Data Engineering: Memory Arenas and SIMD](#10-data-engineering-memory-arenas-and-simd)
11. [Big Data Pipelines (`DATA_PIPELINE`)](#11-big-data-pipelines-data_pipeline)
12. [Declarative UI DSL (`.thzui`)](#12-declarative-ui-dsl-thzui)
13. [Cryptographic Security & BACEN / LGPD Compliance](#13-cryptographic-security--bacen--lgpd-compliance)
14. [Exhaustive Standard Library Reference (Stdlib API)](#14-exhaustive-standard-library-reference-stdlib-api)

---

## 1. Design Philosophy & Dual-Dialect Architecture (EN-US & PT-BR)

THZ-LANG resolves the disconnect between business domain modeling (Domain-Driven Design), executive architectural governance, and high-performance systems engineering.

### Architectural Highlights
* **Single AST Representation:** Both `pt-BR` and `en-US` dialects parse into the identical unified Abstract Syntax Tree and intermediate bytecode (`thz-ir/1`), allowing zero overhead across native LLVM compilation, JIT execution, and SIMD pipelines.
* **Strict Single-Dialect Purity:** A source file configured as `LANGUAGE: en-US` strictly prohibits Portuguese keywords, and vice-versa, preventing linguistic confusion in codebases.

---

## 2. Dialect Directives & Syntax Equivalences

### Header Directive (Lines 1–2)
- `LANGUAGE: en-US` — Explicitly selects English dialect.
- `LINGUAGEM: pt-BR` — Selects Brazilian Portuguese dialect (default if omitted).

### Keyword Mapping Table

| Category | EN-US (`LANGUAGE: en-US`) | PT-BR (`LINGUAGEM: pt-BR`) |
| :--- | :--- | :--- |
| **Archetype** | `PROGRAM`, `LIBRARY`, `MODULE`, `EXTENSION`, `TEST`, `TOOL`, `SCREEN` | `PROGRAMA`, `BIBLIOTECA`, `MODULO`, `EXTENSAO`, `TESTE`, `FERRAMENTA`, `TELA` |
| **Metadata** | `ARCHITECTURE_METADATA`, `DOMAIN`, `AUTHOR`, `VERSION`, `LAYER`, `MAX_LATENCY_SLO` | `METADADOS_ARQUITETURA`, `DOMINIO`, `AUTOR`, `VERSAO`, `CAMADA`, `SLO_LATENCIA_MAXIMA` |
| **Contracts** | `BUSINESS_RULE`, `REQUIRES`, `ENSURES`, `INVARIANT`, `IDEMPOTENT` | `REGRA_NEGOCIO`, `EXIGE`, `GARANTE`, `INVARIANTE`, `IDEMPOTENTE` |
| **Memory** | `USE_MEMORY_BLOCK`, `COLUMNAR_LAYOUT`, `VECTORIZE_FOR` | `USAR_BLOCO_MEMORIA`, `LAYOUT_COLUNAR`, `VETORIZAR_PARA` |
| **Control** | `IF`, `THEN`, `ELSE`, `WHILE`, `DO`, `FOR`, `FROM`, `TO`, `STEP` | `SE`, `ENTAO`, `SENAO`, `ENQUANTO`, `FACA`, `PARA`, `DE`, `ATE`, `PASSO` |
| **I/O & Flow**| `PRINT`, `RETURN`, `READ`, `FAIL_WITH` | `EXIBA`, `RETORNE`, `LER`, `FALHAR_COM` |
| **Terminators**| `END_PROGRAM`, `END_STRUCTURE`, `END_RULE`, `END_IF`, `END_FOR` | `FIM_PROGRAMA`, `FIM_ESTRUTURA`, `FIM_REGRA`, `FIM_SE`, `FIM_PARA` |

---

## 3. Living Architecture & Metadata (`ARCHITECTURE_METADATA`)

Every logical compilation unit can declare formal architectural governance tags validated by the semantic analyzer and automatically exported to C4 architecture diagrams and Markdown documentation.

```thz
# LANGUAGE: en-US
ARCHITECTURE_METADATA
    DOMAIN: "BillingEngine"
    SUBDOMAIN: "TaxCalculation"
    LAYER: "Domain"
    VERSION: "2.4.0"
    AUTHOR: "Lucas Thomaz"
    MAX_LATENCY_SLO: "5ms"
    COMPLIANCE: "ISO-10967", "BACEN-Res4893", "LGPD-Art46"
END_METADATA
```

---

## 4. Data Types & Exact Arithmetic (ISO/IEC 10967 & ISO 4217)

THZ-LANG strictly eliminates floating-point representation bugs in financial, tax, and domain-critical computing.

| Type | Declaration Syntax | Description / Invariant |
| :--- | :--- | :--- |
| **`INTEGER`** | `INTEGER` | Signed 64-bit standard integer. |
| **`INTEGER32`** | `INTEGER32` | Signed 32-bit integer for indices and SIMD vector alignment. |
| **`DECIMAL(P, S)`**| `DECIMAL(18, 4)` | Exact scaled decimal with total precision $P$ and scale $S$. |
| **`MONETARY(M)`** | `MONETARY(USD)` | ISO 4217 currency-tagged monetary amount. Prohibits cross-currency operations without conversion. |
| **`TEXT`** | `TEXT` | Native UTF-8 string with full Unicode escape support (`\uXXXX`). |
| **`BOOLEAN`** | `BOOLEAN` | Boolean value (`TRUE` or `FALSE`). |
| **`UUID`** | `UUID` | RFC 4122 128-bit Universally Unique Identifier. |
| **`DATE`** | `DATE` | Civil Date (Year, Month, Day). |
| **`DATETIME`** | `DATETIME` | Timestamp (Year, Month, Day, Hour, Minute, Second). |
| **`LIST[T]`** | `LIST[INTEGER]` | Contiguous indexed homogeneous array. |
| **`RESULT[T, E]`** | `RESULT[Order, TEXT]` | Monadic control channel returning `SUCCESS(T)` or `ERROR(E)`. |

---

## 5. Design by Contract (DbC) & Provable Invariants

Business rules are bound by formal preconditions (`REQUIRES`) and postconditions (`ENSURES`) evaluated deterministically at runtime.

```thz
# LANGUAGE: en-US
BUSINESS_RULE CalculateDiscount
    RULE_ID: "BR-DISC-001"
    REQUIREMENT_TRACE: "REQ-FIN-402"
    DESCRIPTION: "Calculates volume discount for approved corporate accounts"
    IDEMPOTENT: TRUE

    INPUT_CONTRACT
        REQUIRES totalAmount > 0.00
        REQUIRES discountPercentage >= 0.00
        REQUIRES discountPercentage <= 0.50
    END_INPUT_CONTRACT

    OUTPUT_CONTRACT
        ENSURES discountedTotal <= totalAmount
        ENSURES discountedTotal >= 0.00
    END_OUTPUT_CONTRACT

    OPERATION Apply(totalAmount: DECIMAL(18, 2), discountPercentage: DECIMAL(5, 4)) : DECIMAL(18, 2)
    BEGIN
        VARIABLE discountValue : DECIMAL(18, 2) <- totalAmount * discountPercentage
        VARIABLE discountedTotal : DECIMAL(18, 2) <- totalAmount - discountValue
        RETURN discountedTotal
    END
END_BUSINESS_RULE
```

---

## 6. High-Performance Data Engineering: Arenas & SIMD

Contiguous ephemeral memory arenas permit $O(1)$ batch memory disposal without garbage collection pauses.

```thz
# LANGUAGE: en-US
USE_MEMORY_BLOCK BatchProcessing
    VECTORIZE_FOR item IN batch.items SIMD_STEP 8
        item.tax <- item.price * 0.15
    END_FOR
END_MEMORY_BLOCK
```
