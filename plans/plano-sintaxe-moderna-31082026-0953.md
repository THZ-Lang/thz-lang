# Plano de Evolução da Sintaxe Moderna da THZ-LANG

**Criado em:** 31082026 09:53
**Status:** Em andamento
**Direção:** Sintaxe moderna inspirada em TypeScript, segurança semântica inspirada em Rust e controle explícito de recursos inspirado em Zig.

## 1. Objetivo

Evoluir a sintaxe da THZ-LANG para torná-la mais compacta, coerente e familiar, sem transformar a linguagem em um dialeto de outra linguagem e sem perder seus diferenciais corporativos:

- `METADADOS_ARQUITETURA`;
- `REGRA_NEGOCIO`;
- `EXIGE`, `GARANTE` e `INVARIANTE`;
- `MONETARIO` e `DECIMAL` exatos;
- `LAYOUT_COLUNAR`, SIMD e Arena;
- rastreabilidade e governança.

## 2. Princípios de design

1. A sintaxe canônica continua sendo THZ-LANG e mantém palavras-chave em português.
2. A inspiração externa altera a forma de escrever, não a semântica da linguagem.
3. Construções equivalentes devem gerar os mesmos nós da AST.
4. A governança deve ser proporcional ao contexto: programas corporativos exigem metadados e regras de negócio; bibliotecas, ferramentas e scripts pequenos podem usar uma forma reduzida.
5. Tipagem estática, erros explícitos e segurança financeira não podem ser enfraquecidos por açúcar sintático.
6. O comportamento deve permanecer compatível entre JVM, runtime nativo e compilador self-hosted.

## 3. Modelo sintático proposto

### 3.1 Declarações

Usar tipos após dois-pontos, com inferência opcional:

```thz
VARIAVEL total: MONETARIO <- MONETARIO("0,00")
VARIAVEL quantidade <- 10
CONSTANTE LIMITE: INTEIRO32 <- 100
```

### 3.2 Funções e operações

Separar cálculo puro de comportamento corporativo:

```thz
FUNCAO calcularTotal(itens: LISTA<Item>): MONETARIO
    ...
FIM_FUNCAO

REGRA_NEGOCIO EmissaoFatura
    OPERACAO emitir(fatura: Fatura): RESULTADO<Fatura, ErroFaturamento>
        ...
    FIM_OPERACAO
FIM_REGRA_NEGOCIO
```

`FUNCAO` representa comportamento reutilizável e preferencialmente puro. `OPERACAO` pertence a uma `REGRA_NEGOCIO` e pode carregar contratos, rastreabilidade e efeitos controlados. `PROCEDIMENTO` permanece para entrada, orquestração e efeitos externos.

### 3.3 Fluxo de controle

Manter blocos explícitos e terminadores coerentes:

```thz
SE cliente.ativo = VERDADEIRO
    ...
SENAO
    ...
FIM_SE

PARA item EM itens
    ...
FIM_PARA

ESCOLHA resultado
    CASO SUCESSO(valor)
        ...
    CASO FALHA(erro)
        ...
FIM_ESCOLHA
```

### 3.4 Erros e recursos

Erros devem ser valores tipados ou falhas explícitas. Não adotar propagação silenciosa equivalente a `On Error Resume Next`.

```thz
TENTE
    resultado <- emitir(fatura)
CAPTURE ErroFaturamento.CLIENTE_INATIVO
    ...
FIM_TENTE
```

O controle de memória deve permanecer explícito nos pontos de performance:

```thz
USAR_BLOCO_MEMORIA arena
    itens <- carregarItens(arena)
FIM_BLOCO_MEMORIA
```

## 4. Etapas de implementação

### Fase 0 — Decisão e baseline

- [ ] Congelar exemplos canônicos atuais e sua AST esperada.
- [x] Definir a EBNF da sintaxe proposta.
- Definir quais recursos são obrigatórios para programas corporativos e quais são opcionais em código reduzido.
- [x] Registrar decisões em ADR, incluindo a decisão de não criar um dialeto separado.

### Fase 1 — Tokens e palavras reservadas

- Catalogar tokens existentes no núcleo Java 25.
- Adicionar apenas palavras novas realmente necessárias, evitando aliases de VB6.
- Atualizar `PalavrasReservadas` e `TokenType`.
- Garantir rastreamento determinístico de linha e coluna.

**Andamento:** `FUNCAO`/`FIM_FUNCAO` registrados no lexer JVM e nos dialetos PT-BR/EN-US.

### Fase 2 — AST unificada

- Criar ou ajustar nós para `FUNCAO`, `OPERACAO`, `PROCEDIMENTO`, `RESULTADO` e blocos de controle.
- Reutilizar os nós existentes quando a semântica for a mesma.
- Representar contratos como árvores estruturadas, não como texto.
- Adicionar testes de igualdade entre ASTs equivalentes.

**Andamento:** `FuncaoAst` e a coleção `funcoes` foram adicionados a `ProgramaAst` no núcleo JVM.

### Fase 3 — Lexer e parser

- [x] Implementar declarações com `nome: TIPO`.
- [x] Implementar inicialização com `<-`.
- [x] Implementar `FUNCAO` e terminadores simétricos.
- [x] Implementar `SE`, `PARA`, `ENQUANTO`, `ESCOLHA` e `TENTE`.
- Permitir forma reduzida de arquivo sem exigir metadados quando o modo não for corporativo.
- Manter mensagens no formato `[Erro Sintático][Linha L:C]`.

**Andamento:** `FUNCAO`, `ESCOLHA` e `TENTE` implementadas no parser JVM; as formas legadas de resultado permanecem compatíveis.

### Fase 4 — Semântica e contratos

- Validar tipos, escopos, retornos e mutabilidade.
- Impedir operações monetárias com ponto flutuante.
- Verificar `EXIGE`, `GARANTE` e `INVARIANTE` em compilação e execução.
- Validar que `REGRA_NEGOCIO` seja usada quando houver rastreabilidade ou comportamento corporativo.
- Definir diagnóstico claro para metadados ausentes em modo corporativo.

**Andamento:** a análise JVM valida assinatura, argumentos e compatibilidade do `RETORNE` de `FUNCAO`; `ESCOLHA` e `TENTE` reutilizam a semântica tipada de `RESULTADO`/`FALHAR_COM`.

### Fase 5 — Interpretador e backends

- Executar os novos nós no interpretador JVM.
- Atualizar emissão de IR/LLVM.
- Garantir representação decimal com `DecimalFixo`/`i128` conforme o backend.
- Preservar Arena, SoA, SIMD e ABI do runtime Rust/C.
- Verificar paridade entre JVM e AOT.

**Andamento:** interpretação JVM, THZ-IR e JavaScript de `FUNCAO`, `ESCOLHA` e `TENTE` concluídos. Funções tipadas têm assinatura, retorno direto e operações binárias simples emitidos no LLVM AOT, com testes de soma e parâmetros. O lexer do compilador self-hosted foi atualizado e sua suíte passou na branch `feat/self-hosting-llvm-autonomy`; lowering geral de expressões/fluxos e a paridade final com o runtime Rust permanecem pendentes.

### Fase 6 — Formatador, LSP e documentação

- Implementar formatação canônica e idempotente.
- Atualizar realce, completion, hover e diagnósticos no LSP.
- Atualizar gramática formal, manual, DocGen e exemplos.
- Criar snippets para a sintaxe moderna.

**Andamento:** formatação canônica, gramática, manual, DocGen, completion/hover LSP, realce VS Code e snippet de `FUNCAO` atualizados.

### Fase 7 — Migração gradual

- [x] Migrar primeiro exemplos pequenos e utilitários.
- [x] Migrar depois regras corporativas e pipelines.
- Manter compatibilidade de leitura com arquivos existentes.
- Usar avisos de depreciação antes de remover qualquer construção.

**Andamento:** a galeria `exemplos/` foi migrada em lote para declarações `nome: TIPO` (56 arquivos alterados), mantendo leitura legada no parser. O parser self-hosted valida explicitamente os blocos modernos `FUNCAO`, `ESCOLHA` e `TENTE`; avisos de depreciação e parser self-hosted geral ainda estão pendentes.

## 5. Testes de aceitação

- Cada construção nova deve ter teste léxico, sintático, semântico e de execução.
- Código canônico e código equivalente devem gerar a mesma AST.
- `fmt(fmt(codigo))` deve ser igual a `fmt(codigo)`.
- Exemplos corporativos devem falhar quando metadados obrigatórios estiverem ausentes.
- Scripts pequenos devem compilar sem burocracia arquitetural desnecessária.
- Valores `MONETARIO` devem permanecer exatos e sem uso de `float`/`double`.
- A suíte JVM, os exemplos, o self-hosting e o backend nativo devem ser validados antes de cada marco.

## 6. Resultado esperado

A THZ-LANG deve ter leitura moderna e compacta, próxima de TypeScript, com contratos e resultados explícitos como Rust e controle de memória/performance como Zig, preservando a identidade corporativa e a linguagem ubíqua própria da THZ-LANG.
