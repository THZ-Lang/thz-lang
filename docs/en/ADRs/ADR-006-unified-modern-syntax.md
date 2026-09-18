# ADR-006 — Unified Modern Syntax Without Parallel Dialect

**Status:** Accepted  
**Date:** 2026-08-31  

## Context

THZ-LANG possesses proprietary corporate constructs — `ARCHITECTURE_METADATA` / `METADADOS_ARQUITETURA`, `BUSINESS_RULE` / `REGRA_NEGOCIO`, formal contracts, exact decimals, memory arenas, and columnar layout — that cannot be diluted by superficial modernization of syntax.

The parser previously supported typed declarations with `:` and initialization with `<-`, but legacy formatting inserted spaces before `:`. Furthermore, modern ergonomics (`var`, `val`, `fn`, `struct`, `{ ... }`, `=`, `:=`) required seamless integration into the core AST without fracturing the compiler or creating parallel incompatible languages.

## Decision

1. **Single Unified Language:** THZ-LANG remains a single cohesive language. Modern constructs are first-class and compile to the same underlying AST nodes without maintaining separate incompatible dialects.
2. **Ergonomic Declarations:** Both canonical declarations (`VARIAVEL nome: TIPO <- expr`) and modern declarations (`var nome: TIPO = expr` or `var nome = expr`) are natively parsed and supported.
3. **Dedicated Function Representation:** Functions (`fn` / `FUNCAO`) are first-class AST nodes with explicit return types, representing pure, reusable calculation rather than replacing orchestration procedures (`PROCEDIMENTO`) or audited business operations (`OPERACAO`).
4. **End-to-End Parity:** No new syntax keyword or construct is considered complete until lexer, parser, AST, semantic analyzer, interpreter, formatter, DocGen, and AOT backends preserve identical semantics.
5. **Backwards Compatibility:** Legacy syntax forms remain 100% supported and tested across the entire regression test suite.

## Consequences

- The canonical formatter manages code formatting without forcefully rewriting developer source files.
- Line and column error reporting retains absolute accuracy across both classical and modern syntaxes.
- Exact monetary and decimal invariants (`DecimalFixo` / scaled 128-bit integers) remain fully enforced, strictly prohibiting IEEE 754 floating-point operations.
