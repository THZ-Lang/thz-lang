# ADR-008 — Interoperabilidade, Transpilação de COBOL e Compilação Nativa AOT via LLVM IR (Zero JVM)

- **Status:** Aceito
- **Data:** 11/10/2026
- **Decisores:** Lucas Thomaz & THZ Architecture Guild
- **Contexto:** Ecossistema THZ-LANG, Compilação Nativa AOT, Modernização de Sistemas Legados de Missão Crítica

---

## 1. Contexto e Motivação

O setor corporativo e financeiro global executa trilhões de dólares em sistemas legados escritos em **COBOL**. Embora robusto, o COBOL impõe desafios severos de escassez de profissionais, integração moderna e custos de mainframe.

O **THZ-LANG** nasceu com semântica de negócios rica, auditoria nativa, contratos formais (`EXIGE`/`GARANTE`) e tipos numéricos de alta precisão (`DECIMAL`/`MONETARIO` baseados em `i128` ISO 10967 e ISO 4217). Para ser a linguagem definitiva do setor financeiro, o THZ-LANG precisa:
1. **Compreender e dialogar diretamente com COBOL**: ingerir *Copybooks* (`.cpy`), entender blocos embutidos (`BLOCO_COBOL`) e traduzir regras de negócio da *Procedure Division*.
2. **Eliminar a dependência da JVM fora de desenvolvimento**: a máquina virtual Java serviu como plataforma de bootstrap (Stage 0), mas a execução em produção precisa ser **nativa AOT compilada via LLVM IR**, sem JVM, gerando binários autônomos (`.exe` e `.elf`) de alta velocidade e pegada de memória mínima (<15MB RSS).
3. **Autocompilação (Self-Hosting)**: permitir que, no Stage 2, o compilador `thz-compiler` compile a si mesmo e o ferramental de COBOL de forma autossuficiente.

---

## 2. Decisão Arquitetural

Decidimos criar o 12º módulo oficial do ecossistema, **`thz-lang-cobol`**, com arquitetura em quatro pilares fundamentais:

```
┌────────────────────────────────────────────────────────┐
│                   THZ-LANG ECOSYSTEM                   │
├──────────────────────────┬─────────────────────────────┤
│   Ambiente Dev (Stage 0) │   Produção Nativa (Stage 2) │
│   - Gradle Composite     │   - LLVM IR AOT (.ll)       │
│   - Java 25 (GraalVM)    │   - C-ABI Nativa (Clang/C)  │
│   - Execução Rápida      │   - Zero JVM / Scratch OCI  │
│   - Tooling e Debug      │   - Arena O(1) Memory       │
└──────────────────────────┴─────────────────────────────┘
                             │
     ┌───────────────────────┴────────────────────────┐
     ▼                                                ▼
[Copybook Ingestion]                           [Procedure Transpiler]
.CPY -> ESTRUTURA THZ                          COBOL Logic -> REGRA_NEGOCIO
COMP-3 (BCD) -> DECIMAL (i128)                 COMPUTE/MOVE/PERFORM -> THZ Ops
```

### 2.1 Ingestão de Contratos de Dados (Copybooks)
- Leitura de arquivos `.cpy` e seções `DATA DIVISION`.
- Mapeamento direto de layouts legados:
  - `PIC X(n)` → `TEXTO`
  - `PIC 9(n)` → `INTEIRO32` ou `INTEIRO`
  - `PIC S9(m)V9(n) COMP-3` (Packed Decimal BCD) → `DECIMAL` / `MONETARIO`
  - Nível `88` → `LOGICO` / anotações de condição.
- Geração automática de `ESTRUTURA` THZ-LANG idêntica ao layout binário.

### 2.2 Transpilação e Execução Híbrida
- Reconhecimento de blocos embutidos: `BLOCO_COBOL ... FIM_BLOCO_COBOL`.
- Transpilação da *Procedure Division* para `REGRA_NEGOCIO` e `OPERACAO` com contratos formais (`CONTRATO_ENTRADA` / `CONTRATO_SAIDA`).
- Tradução de comandos: `MOVE` → atribuição `<-`, `COMPUTE` → expressões nativas, `DISPLAY` → `EXIBA`, `PERFORM` → invocações de sub-rotinas.

### 2.3 Compilação Nativa AOT via LLVM IR (Zero JVM)
- O módulo emite código **LLVM IR textual puro** (`CobolLlvmEmitter`), com suporte a `target triple`:
  - Windows: `x86_64-pc-windows-msvc`
  - Linux: `x86_64-pc-linux-gnu`
- Ligação direta via **C-ABI** com rotinas COBOL compiladas nativamente (GnuCOBOL / LLVM COBOL) usando ponteiros (`BY REFERENCE`) e alocações na arena do THZ (`thz_arena_alloc`).
- Rotinas de conversão vetorial de alta performance:
  - `thz_cobol_comp3_to_i128` (Packed Decimal BCD para inteiro de 128 bits)
  - `thz_cobol_i128_to_comp3` (inteiro de 128 bits para Packed Decimal BCD)
- Nenhum runtime Java, Node ou dependência externa é necessário no binário final.

---

## 3. Consequências e Benefícios

### Positivas:
- **Zero Lock-In**: O ecossistema avança firmemente para a independência da JVM.
- **Modernização Segura**: Permite migração gradual de sistemas bancários e governamentais sem risco de reescrita *big bang*.
- **Desempenho Extremo**: Latência de milissegundos e inicialização sub-5ms em binários compilados nativamente.
- **Exatidão Aritmética**: Preservação de 100% da precisão financeira ISO 10967 através do tipo `DECIMAL` (i128).
- **Paridade com Self-Hosting**: Define as bases para o `thz-compiler` compilar rotinas de modernização COBOL sem intervenção de outros compiladores.

### Compensações (Trade-offs):
- Requer manutenção de testes em modo dual (verificação na JVM para desenvolvimento rápido + validação de IR LLVM para AOT).
