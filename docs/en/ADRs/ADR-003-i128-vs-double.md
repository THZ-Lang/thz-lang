# ADR-003 — Scaled `i128` vs Binary `double` for DECIMAL Arithmetic

**Date:** 2025-08-25 · **Status:** Accepted · **Context:** ISO/IEC 10967 and ISO 4217 exact financial compliance.

## Context

Financial and fiscal systems require exact decimal representation where `0.10 + 0.20 == 0.30` without binary floating-point drift (`0.30000000000000004`). Candidates considered: `double` (IEEE 754), `BigDecimal` (JVM), `i128` scaled integer (LLVM), and `BigInteger` scaled integer (JVM).

## Decision

- **JVM Engine:** `DecimalFixed` using `BigInteger` scaled value + integer scale with Half-Even Banker's Rounding.
- **Native LLVM Backend:** `i128` scaled integer representation mapping `DECIMAL` and `MONETARY` directly to 128-bit hardware registers.
- **Compiler Rule:** Static rejection of `float` and `double` in all monetary computation paths.

## Consequences

- **Pros:** 100% ISO/IEC 10967 compliance, zero float inaccuracies, and perfect arithmetic parity between JVM and LLVM native executables.
- **Cons:** Slightly lower raw throughput than hardware FPUs, mitigated through Structure-of-Arrays (SoA) and SIMD batching.
