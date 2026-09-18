# Contributing to THZ-LANG

Thank you for your interest in contributing to the **THZ-LANG** ecosystem!

---

## 1. Development Guidelines

1. **Branch for Self-Hosting & LLVM Autonomy:** When working on self-hosting compiler tasks (`compilador/`), LLVM codegen, or the native Rust runtime (`src/runtime_rs/`), always switch to the `feat/self-hosting-llvm-autonomy` branch.
2. **Dual-Dialect & Modern Syntax Purity:** Ensure single-dialect purity is preserved. Do not mix English and Portuguese keywords in the same file. Both classic and modern dual syntaxes are first-class citizens.
3. **Exact Arithmetic (ISO/IEC 10967):** Never use IEEE 754 floating-point types (`float` / `double`) for monetary or fiscal calculations. Always use scaled integers (`DecimalFixo` / `i128`).
4. **Single Source of Truth Versioning:** All module versions are governed strictly by `version.txt` (SemVer 2.0.0).
5. **Zero Regression:** All tests (`./gradlew test` / `./scripts/test-all.sh`) must pass with 100% success before submitting changes.
