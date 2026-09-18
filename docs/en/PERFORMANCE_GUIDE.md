# Performance & Optimization Guide — THZ-LANG

This guide provides optimization strategies for achieving sub-millisecond execution latencies with THZ-LANG.

---

## 1. Core Optimizations

1. **Structure-of-Arrays (SoA):** Use `COLUMNAR_LAYOUT` on batch structures to enable contiguous CPU cache lines and SIMD auto-vectorization.
2. **Ephemeral Arenas:** Allocate temporary batch calculations inside `USE_MEMORY_BLOCK` to bypass GC allocation costs.
3. **Dead Code Elimination & Constant Folding:** The AST optimizer evaluates static arithmetic and eliminates dead control paths at compile time.
