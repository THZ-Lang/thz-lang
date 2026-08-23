---
name: write-tests
description: >-
  Use this skill when creating, updating, running, or debugging tests for the THZ-LANG engine in both TypeScript (Node.js) and Java 25 (JUnit 5).
---

# THZ-LANG — Guia de Criação e Execução de Testes

Este guia define as instruções, padrões de asserção e procedimentos para adicionar e executar testes no ecossistema **THZ-LANG** (motores TypeScript e Java 25).

---

## 1. Visão Geral das Suítes de Teste

| Motor | Localização | Framework | Comando de Execução |
| :--- | :--- | :--- | :--- |
| **Node / TypeScript** | `thz-lang-engine/test/` | Node Test Runner (`tsx --test`) | `npm test` (dentro de `thz-lang-engine`) |
| **Java 25 / JVM** | `thz-lang-engine-JVM25/src/test/java/` | JUnit 5 (`org.junit.jupiter`) | `mvn test` (dentro de `thz-lang-engine-JVM25`) |

---

## 2. Invariantes Obrigatórios em Qualquer Teste

Ao escrever novos testes, garanta que:

1. **Aritmética Monetária e Decimais (ISO/IEC 10967):**
   - Nunca use números de ponto flutuante IEEE 754 (`number` ou `double`) para cálculos monetários.
   - Use `DecimalFixo` ou `Monetario` com `BigInt` (TS) / `BigInteger` (Java).
   - Teste arredondamento `MEIA_CIMA` (arredonda empates para cima) e `TRUNCAR`.

2. **Diagnósticos e Mensagens de Erro:**
   - Erros léxicos, sintáticos ou semânticos devem asserir o formato canônico: `[Erro Sintático][Linha L:C]`, `[Erro Léxico][Linha L:C]` ou `[Erro Semântico][Linha L:C]`.
   - No motor TypeScript, os diagnósticos contêm trecho e caret (`src/errors.ts`).

3. **Design by Contract:**
   - Teste tanto o caminho feliz quanto a violação de contratos `EXIGE`, `GARANTE` e `INVARIANTE`.
   - Asserte que violações de contrato disparam `ErroContrato` / `ErroExecucao` com linha exata e texto canônico da cláusula.

4. **Estabilidade de Golden Snapshots:**
   - Quando alterar a AST ou o parser, verifique se os snapshots em `thz-lang-engine/test/__snapshots__/` continuam válidos (`faturamento.ast.json` e `pedidos.ast.json`).

---

## 3. Escrevendo Testes no Motor TypeScript (`thz-lang-engine`)

### Localização dos Arquivos
Os testes ficam em `thz-lang-engine/test/`:
- `keywords.test.ts`: Tabela de palavras reservadas e integridade léxica.
- `lexer.test.ts`: Tokenização, linha/coluna, literais e comentários.
- `parser.test.ts`: Estruturas sintáticas, contratos e precedência de operadores.
- `analisador.test.ts`: Verificação de tipos, escopos e modo `--estrito`.
- `interpretador.test.ts`: Execução tree-walking, controle de fluxo e contratos.
- `decimal.test.ts`: Aritmética decimal escalada e operações monetárias.
- `golden.test.ts`: AST regression tests contra arquivos `.thz` canônicos.
- `language-service.test.ts`: Hover, símbolos, diagnósticos e completion.
- `governanca.test.ts`: Auditoria de requisitos `RASTREIO_REQUISITO`.
- `ir.test.ts` & `simd.test.ts`: Baixa para THZ-IR, regras R1-R5 e emissão LLVM.
- `fmt.test.ts`: Idempotência e preservação de AST do formatador.
- `generalista.test.ts`: Programas de domínio e testes integrados.

### Padrão de Código (Node Test Runner)

```typescript
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { AnalisadorSemantico } from '../src/analisador.js';
import { InterpretadorThz } from '../src/interpretador.js';

describe('Nova Funcionalidade THZ', () => {
  it('deve avaliar expressao com sucesso', () => {
    const fonte = `
PROGRAMA Teste
VERSAO_LINGUAGEM "2.2"
REGRA_NEGOCIO Executar
    x: INTEIRO <- 10 + 20
    GARANTE: x == 30
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
    `;
    const tokens = new ThzLexer(fonte).tokenizar();
    const ast = new ThzParser(tokens).parse();
    const semantico = new AnalisadorSemantico();
    const diagnosticos = semantico.analisar(ast);
    assert.equal(diagnosticos.length, 0, 'Não deve haver erros semânticos');

    const interpretador = new InterpretadorThz();
    assert.doesNotThrow(() => interpretador.executar(ast));
  });
});
```

---

## 4. Escrevendo Testes no Motor Java 25 (`thz-lang-engine-JVM25`)

### Localização dos Arquivos
Os testes ficam em `src/test/java/thz/lang/`:
- `DecimalMonetarioTest.java`: Aritmética `DecimalFixo` e `Monetario`.
- `ContratosInvariantesTest.java`: Validação de `EXIGE`, `GARANTE` e `INVARIANTE`.
- `InterpretadorTest.java`: Execução de controle de fluxo (`SE`, `ENQUANTO`, `PARA`).
- `ParidadeTest.java`: Paridade comportamental com o motor TypeScript e galeria de exemplos.
- `GuiPaletaTest.java`: Testes da paleta de cores e componentes da IDE gráfica.

### Padrão de Código (JUnit 5)

```java
package thz.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;
import thz.lang.interpretador.InterpretadorThz;

class NovaFuncionalidadeTest {

    @Test
    @DisplayName("Deve validar paridade e contratos com sucesso")
    void testNovaFuncionalidade() {
        String codigo = """
            PROGRAMA TesteJava
            VERSAO_LINGUAGEM "2.2"
            REGRA_NEGOCIO Calcular
                total: DECIMAL(10, 2) <- 15.50 + 4.50
                GARANTE: total == 20.00
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var tokens = new ThzLexer(codigo).tokenizar();
        var parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        var interpretador = new InterpretadorThz();
        assertDoesNotThrow(() -> interpretador.executar(ast));
    }
}
```

---

## 5. Como Executar e Validar

```bash
# 1. Executar testes do motor TypeScript:
cd thz-lang-engine
npm test

# 2. Executar testes do motor Java 25:
cd ../thz-lang-engine-JVM25
mvn clean test
```
