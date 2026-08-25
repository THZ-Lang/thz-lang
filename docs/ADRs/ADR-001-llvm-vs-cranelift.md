# ADR-001 — LLVM vs Cranelift/QBE para AOT Nativo

**Data:** 2025-08-25 · **Status:** Aceito · **Contexto:** Escolha do backend AOT para `PIPELINE_DADOS` e `PROGRAMA` (Zero-JVM).

## Contexto

Precisávamos de um backend que emitisse código nativo x86_64 (e futuro ARM64) a partir de `GeradorIr.emitirLlvm` (`JVM/thz-core-jvm/src/main/java/thz/lang/ir/GeradorIr.java:221`), com cross-compilação Windows PE / Linux ELF e linking contra `src/runtime/thz_runtime.c`. Candidatos: LLVM (Clang 22), Cranelift (Rust), QBE.

## Decisão

**LLVM + Clang** (`scripts/build-llvm.ps1:71` `clang -target x86_64-... -c` + `gcc -O3 thz_runtime.c`).

## Consequências

- **Prós:** `target triple`/`datalayout` maduros, `-O3` + SLP vectorizer gratuitos, cross PE/ELF sem toolchain extra, `i128` nativo para `DECIMAL`, ecossistema (`clang`, `llc`, `opt`, `llvm-mc`).
- **Contras:** Dependência `clang`/`llvm` (via `scoop` ou `apt`), build mais pesado que QBE, IR textual verboso.
- **Alternativas rejeitadas:** Cranelift (ótimo para JIT WASM, mas sem `i128` vetorial maduro e sem PE/ELF cross tão simples), QBE (leve mas sem autovetorização e sem `target` Windows MSVC).

## Validação

`ci.yml:90` `clang -target x86_64-unknown-linux-gnu -c` + `gcc -O3` → `./driver.elf` executa.

