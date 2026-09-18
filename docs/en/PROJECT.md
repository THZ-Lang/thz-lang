# Project Technical Architecture & Overview — THZ-LANG

This document details the architectural foundation, engineering principles, and multi-tier module hierarchy of the **THZ-LANG** ecosystem.

---

## 1. System Vision & Architecture

THZ-LANG is engineered as a domain-centric corporate systems language with formal verification, contract governance (DbC), and native low-latency execution capabilities.

### Multi-Tier Engine Hierarchy
1. **`thz-core-jvm`:** Lexical scanner, AST parser, semantic analyzer, arena runtime, and decimal engine.
2. **`thz-cli-jvm`:** Developer command-line interface, REPL environment, and dev servers.
3. **`thz-gui-jvm`:** Professional Swing FlatLaf Desktop IDE with code editor, syntax gutter, and interactive runner.
4. **`thz-lsp-jvm`:** Language Server Protocol implementation powered by Eclipse LSP4J.
5. **`thz-bench-jvm`:** JMH micro-benchmark test harness.
6. **`thz-native (LLVM)`:** Standalone C runtime and Clang AOT code generator.

---

## 2. Technical Invariants

1. **Exact Scaled Arithmetic (ISO/IEC 10967):** Zero floating-point representation drift. All decimals use integer scaling with Banker's Rounding (*Half-Even*).
2. **Memory Safety & Ephemeral Arenas:** Contiguous memory arena buffers allow $O(1)$ batch disposal without garbage collection stalls.
3. **Dual Dialects:** Single-dialect purity across Brazilian Portuguese (`pt-BR`) and international English (`en-US`).
