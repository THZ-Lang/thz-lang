# Plano de Refatoração do Parser Self-Hosted

**Criado em:** 31/08/2026  
**Status:** Proposto  
**Escopo:** substituir o pipeline demonstrativo de `compilador/parser.thz` por um parser geral, mantendo a sintaxe canônica, diagnósticos determinísticos e paridade com o núcleo JVM.

## 1. Objetivo e não objetivos

O parser deverá consumir uma lista real de tokens, produzir uma AST estruturada e acumular erros sintáticos com linha e coluna. Não será permitido manter contagens fixas de tokens/nós, mensagens narrativas como resultado do parsing ou nomes de módulos codificados.

Não faz parte desta etapa alterar a semântica da linguagem, remover compatibilidade legada ou trocar as palavras-chave em português.

## 2. Contratos de entrada e saída

- Entrada: `LISTA<Token>` produzida por `lexer.thz`.
- Saída: `ModuloAst` + `LISTA<DiagnosticoSintatico>`.
- Diagnóstico: `[Erro Sintático][Linha L:C] mensagem`.
- EOF inesperado e terminadores incompatíveis devem ser erros recuperáveis.
- A AST deve ser serializável em forma canônica para comparação com a JVM.

## 3. Fases de implementação

### Fase A — Infraestrutura

- [x] Definir estruturas `Token`, `DiagnosticoSintatico`, `CursorTokens` e nós-base da AST em `ast.thz`.
- [x] Implementar `aceitar`, `exigir` e `estaEm` sobre o token atual; `avancar`, limite/EOF, registro de erro e sincronização já possuem primitivas iniciais. `CarregarTokens` usa `FATIA[TokenSelfHost]` e `Avancar` atualiza `tipo_atual`/`lexema_atual` por indexação.
- [ ] Garantir que avanço após EOF seja seguro e idempotente.

### Fase B — Declarações de módulo

- [ ] Implementar `parseModulo` e terminadores simétricos.
- [ ] Implementar `METADADOS_ARQUITETURA` com campos opcionais e obrigatórios por modo.
- [ ] Implementar `ESTRUTURA`, campos, `LAYOUT_COLUNAR` e declarações modernas `nome: TIPO`.
- [ ] Preservar leitura da forma legada durante a transição.

### Fase C — Funções, procedimentos e regras

- [ ] Implementar parâmetros, tipos de retorno e corpo de `FUNCAO`.
- [ ] Implementar corpo compacto `FUNCAO ... = expressão`.
- [ ] Implementar `PROCEDIMENTO`, `REGRA_NEGOCIO` e `OPERACAO`.
- [ ] Implementar contratos `EXIGE`, `GARANTE` e `INVARIANTE` como nós estruturados.

### Fase D — Expressões e comandos

- [ ] Implementar parser de precedência: unário, multiplicação, soma, comparação, igualdade e lógica.
- [ ] Implementar literais exatos (`INTEIRO`, `DECIMAL`, `MONETARIO`, texto e lógico).
- [ ] Implementar acesso de campo, indexação, chamadas e criação de registros.
- [ ] Implementar `SE`, `PARA`, `ENQUANTO`, `ESCOLHA/CASO` e `TENTE/CAPTURE`.
- [ ] Implementar `RETORNE`, `FALHAR_COM`, atribuição e `EXIBA`.

### Fase E — Recuperação e diagnósticos

- [ ] Sincronizar em quebras de linha e terminadores `FIM_*`.
- [ ] Emitir múltiplos diagnósticos em uma única execução.
- [ ] Cobrir EOF, bloco aberto, bloco fechado incorreto e expressão incompleta.

### Fase F — Codegen e integração

- [ ] Fazer `codegen.thz` consumir a AST, sem textos ou valores fixos.
- [ ] Gerar THZ-IR para funções, operações, procedimentos e fluxos.
- [ ] Integrar o parser real ao `driver.thz` atrás de uma opção de migração.
- [ ] Tornar o parser real padrão somente após os gates de paridade.

## 4. Testes de aceitação

- [ ] Testes unitários de cada método de parsing.
- [ ] Golden tests de AST canônica para exemplos pequenos e corporativos.
- [ ] Paridade JVM/self-hosted para AST, diagnósticos e THZ-IR.
- [ ] `FUNCAO`, `ESCOLHA` e `TENTE` cobertos em sintaxe moderna e forma legada.
- [ ] Galeria completa validada pelo parser real.
- [ ] Nenhum uso de `float`/`double` em valores monetários.
- [ ] Suíte JVM, self-hosting e backend nativo verdes.

## 5. Critério de conclusão

O pipeline demonstrativo será removido apenas quando o parser real produzir AST e diagnósticos equivalentes aos do núcleo JVM para todos os casos da galeria e para a suíte de erros. Até lá, a opção demonstrativa permanecerá somente como fallback de desenvolvimento.
