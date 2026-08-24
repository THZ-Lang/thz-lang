---
name: thz-language-design
description: >-
  Use this skill when modifying the THZ-LANG grammar, adding new keywords, extending the AST, updating lexer/parser rules, or adding runtime operations.
---

# THZ-LANG — Guia de Evolução de Sintaxe e Design da Linguagem

Este guia descreve o pipeline obrigatório para evoluir a linguagem **THZ-LANG** (`.thz`), garantindo consistência léxica, sintática, semântica e paridade entre os motores TypeScript e Java 25.

---

## 1. Regra Fundamental: Fonte da Verdade Léxica

- **TypeScript:** Todas as palavras reservadas **devem** ser declaradas exclusivamente em `../../../thz-lang-base/src/keywords.ts`. É expressamente proibido o uso de strings literais para palavras-chave em `lexer.ts`, `parser.ts`, `interpretador.ts` ou `docgen.ts`.
- **Java 25:** Todas as palavras reservadas **devem** ser registradas em `thz.lang.lexico.PalavrasReservadas` e mapeadas no enum `thz.lang.lexico.TokenType`.

---

## 2. Pipeline de Modificação (Passo a Passo)

Sempre que introduzir uma nova palavra-chave, instrução ou construção sintática, siga rigorosamente esta sequência:

```
[1. Keywords] ──► [2. Token Type] ──► [3. AST Node] ──► [4. Lexer] ──► [5. Parser]
                                                                          │
[9. Snapshots/EBNF] ◄── [8. DocGen/CLI] ◄── [7. Runtime/Interp] ◄── [6. Semântico]
```

### 1. Palavras Reservadas
- Adicionar ao array apropriado em `src/keywords.ts` (ex: `PALAVRAS_ESTRUTURAIS`, `PALAVRAS_COMANDO`, etc.).
- Adicionar ao mapa em `thz-lang-engine-JVM/src/main/java/thz/lang/lexico/PalavrasReservadas.java`.

### 2. Tipos de Token (`TokenType`)
- Adicionar a variante ao enum/type em `src/types.ts` (`TokenType`).
- Adicionar a constante em `thz.lang.lexico.TokenType`.

### 3. Nós da AST
- Definir a interface TypeScript correspondente em `src/types.ts` (ou `src/tipos.ts`).
- Criar a classe/record Java correspondente em `thz-lang-engine-JVM/src/main/java/thz/lang/ast/`.

### 4. Reconhecimento Léxico (`ThzLexer`)
- Atualizar `src/lexer.ts` e `thz.lang.lexico.ThzLexer.java`.
- Garantir que linha e coluna sejam rastreadas com precisão determinística.

### 5. Regras Sintáticas (`ThzParser`)
- Adicionar o método de parsing no `src/parser.ts` respeitando a hierarquia de precedência de operadores.
- Implementar o método correspondente no `thz.lang.sintatico.ThzParser.java`.
- Implementar a função de formatação canônica no `src/fmt.ts` e `Formatador.java`.

### 6. Análise Semântica (`AnalisadorSemantico`)
- Adicionar verificação de tipos, resolução de escopo e checagem de contratos em `src/analisador.ts` e `thz.lang.semantico.AnalisadorSemantico.java`.
- Reportar diagnósticos formatados com caret no padrão `[Erro Semântico][Linha L:C]`.

### 7. Runtime e Interpretador (`InterpretadorThz`)
- Implementar a execução do novo nó em `src/interpretador.ts` e `thz.lang.interpretador.InterpretadorThz.java`.
- Manter suporte a `DecimalFixo` e descartabilidade em Arena.

### 8. Documentação Viva (`ThzDocGen`)
- Atualizar `src/docgen.ts` para refletir o novo nó na geração de Markdown e diagramas Mermaid.

### 9. Gramática EBNF e Snapshots
- Atualizar a especificação formal em `../../../thz-lang-base/docs/GRAMATICA.md`.
- Rodar os golden tests e atualizar `test/__snapshots__/` se o formato da AST mudar.

---

## 3. Checklist de Validação

Após qualquer modificação sintática:
1. `npm test` em `thz-lang-base` (159+ testes verdes).
2. `mvn test` em `thz-lang-engine-JVM` (29+ testes verdes).
3. `npm run thz:check -- --estrito` contra `exemplos/faturamento.thz` e `exemplos/pedidos.thz`.
4. `npm run fmt:check` para garantir que o formatador seja idempotente: `fmt(fmt(x)) == fmt(x)`.
