# Native Runtime & LLVM AOT Compilation — THZ-LANG

This document outlines the standalone C runtime (`src/runtime/thz_runtime.c`) and the LLVM Clang Ahead-of-Time (AOT) compilation model.

---

## 1. Native Architecture

THZ-LANG binaries can be compiled directly to standalone native machine code without any JVM dependency:
- **Dual-OS C Runtime:** Cross-platform Win32 and POSIX compatibility.
- **Direct 128-bit Scaled Arithmetic:** Native `i128` integer operations for financial calculations.
- **Hardware SIMD:** LLVM vectorized vector types for high-throughput batch loops.
