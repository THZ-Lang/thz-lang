# ADR-001 — LLVM vs Cranelift/QBE for Native AOT Compilation

**Date:** 2025-08-25 · **Status:** Accepted · **Context:** Selection of Native AOT Backend for `DATA_PIPELINE` and `PROGRAM` (Zero-JVM).

## Context

We required a compilation backend capable of emitting native x86_64 (and future ARM64) machine code from `GeradorIr.emitirLlvm`, supporting cross-compilation for Windows PE / Linux ELF and static linking against `src/runtime/thz_runtime.c`. Candidates considered: LLVM (Clang), Cranelift (Rust), and QBE.

## Decision

Adopt **LLVM + Clang** (`scripts/build-llvm.ps1` via `clang -target x86_64-... -c` combined with `gcc -O3 thz_runtime.c`).

## Consequences

- **Pros:** Mature target triple and data layout specs, automatic `-O3` optimization with SLP loop vectorization, cross PE/ELF compilation out of the box, native `i128` hardware support for exact decimal calculations, and rich toolchain ecosystem (`clang`, `llc`, `opt`).
- **Cons:** External dependency on `clang`/`llvm`, larger build distribution than minimalistic compilers like QBE.
- **Rejected Alternatives:** Cranelift (primarily optimized for WASM JIT, lacked mature vector `i128` handling), QBE (lightweight but lacks auto-vectorization and native Windows PE output).

## Validation

Automated CI executes `clang -target x86_64-unknown-linux-gnu -c` + `gcc -O3` producing verified native executables.
