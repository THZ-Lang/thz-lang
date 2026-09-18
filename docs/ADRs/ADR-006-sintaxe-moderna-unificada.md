# ADR-006 — Sintaxe Moderna Unificada sem Dialeto Paralelo

**Status:** Aceito  
**Data:** 31/08/2026

## Contexto

A THZ-LANG possui construções corporativas próprias — `METADADOS_ARQUITETURA`,
`REGRA_NEGOCIO`, contratos formais, decimais exatos, Arena e layout colunar —
que não podem ser diluídas por uma modernização superficial da escrita.

O parser já aceita declarações tipadas com `:` e inicialização com `<-`, mas a
forma canônica anterior inseria espaço antes de `:`. Também não existe ainda
uma representação de `FUNCAO` no AST, e introduzi-la somente no parser criaria
divergência entre análise semântica, interpretador e backends.

## Decisão

1. A THZ-LANG continuará sendo uma linguagem única, sem um dialeto moderno
   separado.
2. A forma canônica de declaração tipada é `VARIAVEL nome: TIPO <- expressão`.
   A leitura da forma legada `nome : TIPO` permanece compatível durante a
   migração.
3. `FUNCAO` será um nó próprio de AST, com retorno declarado obrigatório e
   terminador `FIM_FUNCAO`. Ela representa cálculo reutilizável; não substitui
   `OPERACAO` dentro de `REGRA_NEGOCIO` nem `PROCEDIMENTO` de orquestração.
4. Nenhuma palavra-chave ou construção nova será considerada entregue enquanto
   lexer, parser, AST, semântica, interpretador, formatador, DocGen e backends
   aplicáveis não preservarem a mesma semântica.
5. A compatibilidade de leitura será mantida até existir aviso formal de
   depreciação e migração automatizável.

## Consequências

- O formatador conduz a migração sem reescrever fontes automaticamente.
- Posições de linha e coluna continuam descrevendo a fonte original; testes de
  equivalência entre grafias devem comparar ASTs após normalização canônica.
- A implementação de `FUNCAO` exige atualização coordenada dos consumidores de
  `ProgramaAst`; não será introduzida como alias de `PROCEDIMENTO`.
- `MONETARIO` e `DECIMAL` continuam representados por `DecimalFixo`/inteiros
  escalados, sem `float` ou `double` nas operações financeiras.
