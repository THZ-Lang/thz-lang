# Contributing to THZ-LANG

Thank you for your interest in contributing to the **THZ-LANG** ecosystem!

---

## 1. Development Guidelines

1. **Branch for Self-Hosting & LLVM Autonomy:** When working on self-hosting compiler tasks (`compilador/`), LLVM codegen, or the native C runtime (`thz_runtime.c`), always switch to the `feat/self-hosting-llvm-autonomy` branch.
2. **Dual-Dialect Purity:** Ensure single-dialect purity is preserved. Do not mix English and Portuguese keywords in the same file.
3. **Exact Arithmetic:** Never use IEEE 754 floating-point types (`float` / `double`) for monetary calculations. Always use scaled integers (`DecimalFixed` / `i128`).
4. **Zero Regression:** All tests (`./gradlew test`) must pass with 100% success before submitting changes.
