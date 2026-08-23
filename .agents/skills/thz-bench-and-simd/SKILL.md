---
name: thz-bench-and-simd
description: >-
  Use this skill when implementing, verifying, or benchmarking high-performance features in THZ-LANG, including SIMD vectorization, Structure-of-Arrays (SoA) layouts, and ephemeral memory arenas.
---

# THZ-LANG — Guia de Alta Performance (SIMD, SoA & Arenas)

Este guia orienta o desenvolvimento e validação de funcionalidades de alta performance no motor **THZ-LANG**.

---

## 1. Princípios de Alta Performance

1. **Estruturas de Dados Orientadas a Dados (DoD):**
   - O modificador `LAYOUT_COLUNAR` converte registros (*Array of Structures* - AoS) em *Structure of Arrays* (SoA).
   - O layout colunar viabiliza leitura e escrita contígua na memória, permitindo carregamento direto em registradores SIMD (AVX2/AVX-512).

2. **Regras Formais de Vetorização (R1 a R5):**
   O analisador semântico e o validador SIMD (`src/simd.ts`) verificam estritamente:
   - **R1:** O laço deve usar `VETORIZAR_PARA` com limites inteiros determinísticos.
   - **R2:** Todos os acessos a campos no corpo do laço devem pertencer a estruturas com `LAYOUT_COLUNAR`.
   - **R3:** O `PASSO_SIMD` deve ser uma potência de 2 entre 4 e 64 (padrão: 8).
   - **R4:** O corpo não pode conter controle de fluxo divergente ou não-vetorizável (`ENQUANTO`, `SE` complexo, `RETORNE`).
   - **R5:** Atribuições a variáveis fora do laço são permitidas apenas se forem operações canônicas de redução (ex: acumulação em soma).

3. **Gerenciamento de Memória em Arena (`USAR_BLOCO_MEMORIA`):**
   - Alocações temporárias em lote devem ocorrer dentro do bloco `USAR_BLOCO_MEMORIA ARENA_EPHEMERAL`.
   - Ao final do bloco, a memória é descartada em tempo contínuo $O(1)$ sem overhead de garbage collection.

---

## 2. Emissão de THZ-IR e LLVM

O compilador intermediário converte blocos vetorizados para THZ-IR (`thz-ir/1`):
- `src/ir.ts`: Converte AST para nós de IR (`baixarParaIr`) e serializa (`serializarIr`).
- `src/ir.ts:emitirLlvm()`: Gera representação em LLVM IR contendo loops com metadados de vetorização (`vector.body`, `<8 x double>`, etc.).

### Comandos de Inspeção de IR

```bash
cd thz-lang-engine

# Visualizar THZ-IR estruturado
npm run thz:ir

# Inspecionar emissão em LLVM IR
npm run thz:ir:llvm
```

---

## 3. Execução de Benchmarks

A suíte de benchmarks avalia a performance de operações críticas:
- `bench/decimal.bench.ts`: Operações aritméticas escaladas vs floats vs BigInt nativo.
- `bench/fatia.bench.ts`: Indexação e fatiamento de coleções com alocação em Arena.
- `bench/simd.bench.ts`: Comparação de taxa de transferência entre loops escalares e vetorizados.

### Comandos de Benchmark

```bash
cd thz-lang-engine

# Executar todos os benchmarks
npm run bench

# Executar benchmark específico
npm run bench:decimal
npm run bench:simd
```
