# Native Compilation Architecture & LLVM AOT — THZ-LANG

This document describes the native Ahead-of-Time (AOT) compilation architecture of **THZ-LANG** utilizing LLVM Clang.

---

## 1. Native AOT Pipeline

1. **Frontend Parsing & Semantic Validation:** Validates contracts, types, and generates AST.
2. **Intermediate Representation (`GeradorIr`):** Emits standardized `thz-ir/1` and LLVM IR (`.ll`).
3. **Clang Native Toolchain:** Compiles `.ll` combined with `thz_runtime.c` into native standalone executables (`.exe` / ELF binary) without JVM dependencies.
