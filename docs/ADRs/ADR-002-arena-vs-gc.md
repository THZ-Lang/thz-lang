# ADR-002 — Arena O(1) vs GC para Lotes

**Data:** 2025-08-25 · **Status:** Aceito · **Contexto:** `USAR_BLOCO_MEMORIA` / `BlocoMemoria.java:21` vs `new` + GC.

## Contexto

`PIPELINE_DADOS` streaming precisa latência p99 estável e throughput SoA/SIMD. GC (G1/ZGC) pausa, fragmenta e polui cache com objetos `ItemFatura` individuais. Alternativas: GC, Arena linear, off-heap `Unsafe`.

## Decisão

**Arena linear bump-pointer** — `ThzArena {buffer,capacidade,offset}` (`src/runtime/thz_runtime.c:37`), `HeapAlloc`/`malloc` por arena, `alocar = offset+=bytes` (`BlocoMemoria.java:50`), `liberarTudo = offset=0` (`BlocoMemoria.java:69` / `thz_arena_free_all` `thz_runtime.c:47`), LLVM IR `call @thz_arena_alloc(1048576)` (`GeradorIr.java:395`).

## Consequências

- **Prós:** Alocação/liberação O(1), cache-friendly (contíguo), sem `mark/sweep`, sem `OutOfMemory` por fragmentação, `getPorcentagemUso()` (`BlocoMemoria.java:97`) para tuning.
- **Contras:** Capacidade fixa (`Limite excedido`, `BlocoMemoria.java:56`), sem `free` individual, sem coleta de não-alcançáveis.
- **Medido:** `BlocoMemoriaBench.java:17` — bump 5–20× mais throughput que `new Object()` (`GUIA_PERFORMANCE.md:4`).

## Alternativas rejeitadas

GC (imprevisível), `Unsafe` off-heap (sem bounds check, inseguro TR 24772).

