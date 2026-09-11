# Ubiquitous Language Glossary — THZ-LANG

In accordance with **Domain-Driven Design (DDD)** and technical specification standards, this glossary defines the domain vocabulary used across the **THZ-LANG** engine in both English and Portuguese.

---

## Technical & Domain Terms

| English Term | Portuguese Equivalent | Formal Definition |
| :--- | :--- | :--- |
| **Living Architecture** | Arquitetura Viva | Syntactically embedded architectural metadata (`ARCHITECTURE_METADATA`) evaluated during compilation. |
| **Design by Contract** | Design por Contrato | Formal method establishing verifiable preconditions (`REQUIRES`), postconditions (`ENSURES`), and invariants. |
| **Exact Scaled Decimal** | Decimal Fixo Exato | Fixed-point numerical arithmetic representing quantities as scaled integers to avoid IEEE 754 precision errors. |
| **Banker's Rounding** | Arredondamento Bancário | Half-Even rounding (ISO/IEC 10967) that rounds halfway values to the nearest even integer. |
| **Ephemeral Arena** | Bloco de Memória / Arena | Contiguous memory allocation pool that can be reset or released in $O(1)$ constant time. |
| **Columnar Layout** | Layout Colunar (SoA) | Structure-of-Arrays memory arrangement enabling SIMD AVX2/AVX-512 hardware vectorization. |
| **Dual Dialect** | Dialeto Duplo | Dual single-dialect keyword tables (`en-US` and `pt-BR`) generating unified AST representations. |
