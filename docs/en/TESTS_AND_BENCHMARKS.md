# Tests & Micro-Benchmark Suite — THZ-LANG

This document outlines the testing methodologies, benchmark frameworks (JMH), and verification suites ensuring correctness and performance across THZ-LANG components.

---

## 1. Test Architecture

- **Unit & Contract Testing:** JUnit 5 test suites covering parsing, AST generation, semantic validation, and runtime execution.
- **JMH Micro-Benchmarks:** High-resolution latency and throughput benchmarks (`thz-bench-jvm`) measuring arena allocation, SoA layout iteration, and SIMD vectorization.
- **Differential Dialect Verification:** Validating that equivalent `pt-BR` and `en-US` programs produce identical bytecodes and execution results.
