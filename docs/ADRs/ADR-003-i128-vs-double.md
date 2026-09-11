# ADR-003 — `i128` Escalado vs `double` para DECIMAL

**Data:** 2025-08-25 · **Status:** Aceito · **Contexto:** ISO/IEC 10967 + ISO 4217 para `DECIMAL(P,S)` / `MONETARIO(M)`.

## Contexto

Fiscal exige `0.1 + 0.2 == 0.30` exato, sem `0.30000000000000004` de `double` (IEEE 754). Candidatos: `double`, `BigDecimal` (JVM), `i128` escalado (LLVM), `BigInteger` (JVM).

## Decisão

- **JVM:** `DecimalFixo.java:13` `BigInteger valorEscalado + int escala` + `half-even` (`DecimalFixo.java:118`, `BANCARIO`).
- **Nativo:** `i128` LLVM (`GeradorIr.java:505` `DECIMAL→i128`, `MONETARIO→i128`), `thz_exiba_i128(low,high,scale)` (`thz_runtime.c:137`).
- **Regra:** proibição estática de `float`/`double` em fiscal (`AnalisadorSemantico.java`, `CONFORMIDADE_E_NORMAS.md:21`).

## Consequências

- **Prós:** Exatidão ISO 10967, sem NaN/Inf, vetorizável como `i128`, `versao` 1:1 entre JVM e nativo.
- **Contras:** `double` 10–50× mais rápido (FPU, `DecimalBench.java:40`), mas errado — `GUIA_PERFORMANCE.md:5` compensa com SoA/SIMD/Arena.
- **TODO:** `thz_exiba_i128` hoje só imprime `low` (`thz_runtime.c:140`) — formatar `high`+`scale`.

