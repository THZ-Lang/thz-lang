# ADR-002 — Ephemeral Memory Arenas O(1) vs Garbage Collection for Batch Data

**Date:** 2025-08-25 · **Status:** Accepted · **Context:** Memory management in `USE_MEMORY_BLOCK` vs traditional GC.

## Context

Streaming and batch data pipelines (`DATA_PIPELINE`) require deterministic p99 latency and high SoA/SIMD throughput. Traced Garbage Collectors (G1, ZGC) introduce unpredictable pauses, memory fragmentation, and cache pollution when allocating short-lived row items. Alternatives considered: Traditional GC, Linear Arenas, and raw off-heap pointers.

## Decision

Adopt **Linear Bump-Pointer Arenas** (`ThzArena {buffer, capacity, offset}`) with constant-time allocation (`offset += bytes`) and $O(1)$ batch disposal (`offset = 0`), supported uniformly across JVM and native LLVM IR (`@thz_arena_alloc`).

## Consequences

- **Pros:** Ultra-fast $O(1)$ allocation and deallocation, cache-friendly contiguous layout, zero mark-and-sweep overhead, and immune to memory fragmentation.
- **Cons:** Fixed capacity pool (requires sizing tuning), no individual item deallocation within an active arena block.
- **Measured Benchmark:** Bump-pointer allocation achieves 5× to 20× higher throughput compared to traditional object heap allocation.

## Rejected Alternatives

Traditional GC (unpredictable latency spikes), manual off-heap raw pointers (lacks safe bounds checking, violating ISO/IEC TR 24772).
