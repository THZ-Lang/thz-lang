# ADR-009 — Suporte à SCREEN SECTION e Atributos de Cores em COBOL

**Status:** Aceito  
**Data:** 16/10/2026  
**Autores:** Arquitetura THZ-LANG

---

## 🏛️ Contexto

Programas legados em COBOL e sistemas de missão crítica frequentemente utilizam interfaces de console ricas baseadas em caracteres (CUI/TUI). O padrão ANSI/ISO COBOL e compiladores de referência como GnuCOBOL e Micro Focus especificam a seção `SCREEN SECTION` dentro da `DATA DIVISION`.

Essa seção define telas completas compostas por campos posicionados (`LINE`, `COLUMN`), atributos visuais (`FOREGROUND-COLOR`, `BACKGROUND-COLOR`, `HIGHLIGHT`, `REVERSE-VIDEO`, `UNDERLINE`, `BLINK`, `BLANK SCREEN`) e amarração bidirecional com variáveis de memória (`FROM`, `TO`, `USING`).

Até o momento, o módulo `thz-lang-cobol` executava instruções `DISPLAY` direcionando literais e variáveis para `stdout` sem interpretar propriedades de tela e cores da `SCREEN SECTION`.

---

## 🎯 Decisão

1. **Modelo de Dados da SCREEN SECTION**:
   - Introduzir a abstração `CobolScreenItem` no modelo do `thz-lang-cobol`, contendo:
     - Nome do grupo ou item de tela;
     - Nível hierárquico (`01`, `05`);
     - Coordenadas (`line`, `column`);
     - Cores (`foregroundColor`, `backgroundColor` mapeadas de 0 a 7 conforme o padrão COBOL);
     - Modificadores visuais (`highlight`, `reverseVideo`, `blink`, `blankScreen`);
     - Texto fixo (`value`) e amarração de dados (`fromField`, `toField`, `usingField`).
   - Adicionar `List<CobolScreenItem> screenSection` ao contrato de `CobolProgram`.

2. **Parser Dedicado**:
   - Estender o `CobolProcedureParser` para identificar blocos `SCREEN SECTION.` dentro de `DATA DIVISION.` e extrair a árvore de itens de tela com seus atributos de cor e posicionamento.

3. **Renderização e Execução ANSI**:
   - No `CobolStandaloneRunner`, instruções como `DISPLAY nome-da-tela` devem converter a árvore de `CobolScreenItem` em sequências de escape ANSI VT100/xterm (códigos de posicionamento de cursor `\033[line;colH` e estilo `\033[3Xm;4Ym`).
   - Mapear a paleta COBOL canônica:
     - 0: Preto (`30m`/`40m`)
     - 1: Azul (`34m`/`44m`)
     - 2: Verde (`32m`/`42m`)
     - 3: Ciano (`36m`/`46m`)
     - 4: Vermelho (`31m`/`41m`)
     - 5: Magenta (`35m`/`45m`)
     - 6: Marrom/Amarelo (`33m`/`43m`)
     - 7: Branco (`37m`/`47m`)
   - Compatibilidade com emissores nativos `cobc` do GnuCOBOL, assegurando que o mesmo código fonte funcione idêntico sob a JVM e como binário compilado.

---

## ⚡ Consequências

- **Compatibilidade:** Programas COBOL com interfaces de tela legadas passam a ser interpretados e executados fidedignamente pelo motor do THZ-LANG.
- **Modernização:** Campos ligados com `USING` ou `TO` refletem o estado das variáveis de `WORKING-STORAGE` automaticamente ao serem exibidos na tela.
- **Zero Dependências Pesadas:** Renderização por sequências ANSI diretas preserva o princípio de execução leve e autônoma sem exigir bibliotecas nativas de terminal adicionais para casos de uso padrão.
