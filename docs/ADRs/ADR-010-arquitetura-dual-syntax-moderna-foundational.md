# ADR-010 — Arquitetura de Sintaxe Dual: Foundational (Corporativa) e Moderna (.NET 10 / Kotlin / Rust)

**Status:** Aceito  
**Data:** 16/10/2026  
**Autores:** Arquitetura THZ-LANG

---

## 🏛️ Contexto

A linguagem **THZ-LANG** nasceu com fortes raízes em sistemas bancários, contratos de precisão monetária (ISO 10967), governança arquitetural explícita (`METADADOS_ARQUITETURA`), verificação formal (`EXIGE`, `GARANTE`, `INVARIANTE`) e interoperabilidade com COBOL. Essa sintaxe, denominada **Sintaxe Foundational**, possui grande valor corporativo, sendo autoexplicativa e auditável.

Contudo, para cenários ágeis, microsserviços modernos, scripts utilitários, automações e prototipagem rápida, a prolixidade de cabeçalhos de programa e encerramentos repetitivos (`PROGRAMA ... FIM_PROGRAMA`, `PROCEDIMENTO ... INICIO ... FIM`) impõe fricção excessiva. Linguagens contemporâneas como C# (.NET 10 Minimal APIs e Top-Level Statements), Kotlin, Rust, TypeScript e Python demonstraram o poder de "escrever menos código e executar mais".

---

## 🎯 Decisão

Adotar formalmente uma **Arquitetura de Sintaxe Dual** onde dois estilos sintáticos convivem em perfeita harmonia, desaguando na **mesma Árvore Sintática Abstrata (AST)**:

### 1. Sintaxe Foundational (Corporativa / COBOL-grade)
- Mantida integralmente, sem qualquer quebra de compatibilidade.
- Estruturada, explícita, verbose: `PROGRAMA`, `METADADOS_ARQUITETURA`, `ESTRUTURA ... FIM_ESTRUTURA`, `PROCEDIMENTO ... INICIO ... FIM`, `BLOCO_COBOL`.
- Recomendada para: Contratos financeiros, transações bancárias de missão crítica, auditorias BACEN/ISO e especificações corporativas formais.

### 2. Sintaxe Moderna (Concisa / Script / Cloud-Native)
- **Top-Level Statements (Estilo .NET 10 / Python):** Qualquer arquivo `.thz` sem cabeçalhos de módulo executa diretamente da primeira à última linha. O compilador/interpretador encapsula o fluxo principal automaticamente.
- **Declaração e Inferência Enxutas (Kotlin / TypeScript / Rust):**
  - `val x = expr` ou `let x = expr` para constantes imutáveis.
  - `var x = expr` ou `let mut x = expr` para variáveis mutáveis.
  - Declaração simplificada de records: `record Cliente(id: Int, nome: String, saldo: Decimal)`.
- **Expressões de Controle de Fluxo:** `if` / `else` e `match` como expressões que produzem valores de retorno.
- **Interpolação de Strings:** Literais com interpolação inline `f"Olá {nome}, seu saldo é {saldo}"` ou `$"Olá {nome}"`.
- **Minimal APIs:** Criação de endpoints HTTP e rotas de microsserviço em 3 linhas:
  ```thz
  val app = Http.app(porta = 8080)
  app.get("/clientes/{id}", (id: Int) => Banco.buscarCliente(id))
  app.iniciar()
  ```

### 3. Convergência e Transpilação Bidirecional
- Toda a sintaxe moderna é dessugarizada diretamente para a AST canônica (`thz.lang.ast.*`).
- A ferramenta `thz fmt` do ecossistema proverá conversão bidirecional sem perda semântica:
  - `thz fmt --to-foundational`: Converte um script moderno conciso em um contrato corporativo formal com metadados e blocos explícitos para auditoria.
  - `thz fmt --to-modern`: Moderniza código corporativo legado para a sintaxe enxuta contemporânea.

---

## ⚡ Consequências

- **Unificação:** Não existem "dois THZ-LANGs". É a mesma linguagem, os mesmos tipos e o mesmo compilador, apenas com duas opções de ergonomia para o desenvolvedor.
- **Produtividade Aumentada:** Redução de até 80% nas linhas de código necessárias para scripts e serviços REST.
- **Preservação de Legado:** Nenhum programa antigo deixará de funcionar.
