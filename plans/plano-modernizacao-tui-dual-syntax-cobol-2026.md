# Plano de Execução: Cores em COBOL, TUIs Dinâmicas e Dualidade de Sintaxe Moderna

**Data:** 16/10/2026  
**Status:** Em Execução  
**Módulos Impactados:** `thz-lang`, `thz-lang-cobol`, `thz-core-jvm`, `thz-runtime-rs`

---

## 🏛️ Objetivos do Plano

1. **Cores e Interface em COBOL (`SCREEN SECTION`):**
   - Implementar parser da cláusula `SCREEN SECTION` (`FOREGROUND-COLOR`, `BACKGROUND-COLOR`, `HIGHLIGHT`, `LINE`, `COL`) no `thz-lang-cobol`.
   - Adicionar renderização ANSI automática ao executar instruções `DISPLAY` de itens de tela no `CobolStandaloneRunner`.
   - Criar suíte de testes unitários validando a extração dos nós de tela e saída ANSI.

2. **Motor de TUIs Dinâmicas no Ecossistema:**
   - Criar o motor `ThzTerminalTui` no `thz-core-jvm` com buffer virtual, desenho de caixas UTF-8, barras de progresso, tabelas e gradientes de cores ANSI.
   - Integrar na `BibliotecaPadrao` via funções `TUI.*`.
   - Prover bindings C-ABI equivalentes no `thz-runtime-rs`.

3. **Sintaxe Moderna & Dualidade Arquitetural (.NET 10 / Kotlin / Rust / TS):**
   - Oficializar Top-Level Statements nativos (execução sem boilerplate `PROGRAMA`/`MAIN`).
   - Implementar inferência e declaração concisa (`val`, `var`, `let`, `let mut`).
   - Implementar records compactos em uma única linha (`record Nome(campos)`).
   - Implementar interpolação de strings (`f"..."` / `$"..."`).
   - Garantir preservação estrita da sintaxe foundational e unificação de AST.

---

## 📋 Checklist de Entrega

- [x] Criação das ADRs formais (`ADR-009`, `ADR-010`, `ADR-011`).
- [ ] Implementação de `CobolScreenItem`, `SCREEN SECTION` e renderizador ANSI em `thz-lang-cobol`.
- [ ] Implementação de `ThzTerminalTui` e `BibliotecaTui` em `thz-core-jvm`.
- [ ] Implementação da sintaxe moderna, top-level statements e interpolação em `thz-core-jvm`.
- [ ] Adição de bindings C-ABI de TUI no `thz-runtime-rs`.
- [ ] Execução e validação de testes unitários em todos os módulos.
- [ ] Publicação via feature branches e criação dos Pull Requests no GitHub.
