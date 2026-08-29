# THZ-LANG — Instruções do Projeto

## Sobre
THZ-LANG é uma linguagem de programação corporativa com syntax em português,
focada em DDD (Domain-Driven Design), Design by Contract e alta performance.

## Estrutura do Projeto
- `compilador/` — Compilador self-hosting escrito em THZ-LANG
- `src/runtime_rs/` — Runtime nativo em Rust (Arena, SIMD, Crypto, LLM)
- `JVM/` — Multi-module Java 25 (core, cli, gui, lsp, bench, api, agent)
- `Extensions/` — Extensão VS Code (TypeScript)
- `exemplos/` — 39+ exemplos .thz
- `docs/` — Documentação (PT-BR e EN)
- `dados/` — Bancos SQLite locais

## Convenções de Código
- Java 25 com records, pattern matching, virtual threads
- Rust com FFI C-compatible (`extern "C"`) para binding com Java
- Português para keywords da linguagem, inglês para código interno
- Design by Contract: `EXIGE`, `GARANTE`, `INVARIANTE` em operações
- Testes: JUnit 5 (Java) e cargo test (Rust)

## Build
- JVM: `./gradlew build` (composite build com 6+ módulos)
- Rust: `cargo build --release` em `src/runtime_rs/`
- CLI: `./gradlew :thz-cli-jvm:run -- <comando>`
- Native: GraalVM native-image ou LLVM Clang AOT

## Módulo THZ-Agent
- Localizado em `JVM/thz-agent-jvm/`
- Entry point: `thz.lang.agent.ThzAgent`
- CLI: `thz agent --modelo model.gguf` ou `thz agent --api <url>`
- Ferramentas: read_file, write_file, apply_diff, execute_command, search_files, list_files
- Padrão: ReAct loop (Think → Act → Observe → Repeat)
