# Plano de Refatoração do Parser Self-Hosted

**Criado em:** 31/08/2026  
**Status:** Cancelado — fora do caminho crítico em 02/09/2026  
**Escopo original:** substituir o pipeline demonstrativo por um parser geral inicialmente concentrado em `compilador/selfhost.thz`, mantendo a sintaxe canônica, diagnósticos determinísticos e paridade com o núcleo JVM.

> **Decisão de produto (02/09/2026):** este plano foi cancelado para evitar que a autonomia self-hosted consuma esforço desproporcional. Os arquivos em `compilador/` permanecem como experimento técnico, sem gate de produto e sem alegação de paridade. O caminho oficial passa a ser: (1) JVM como compilador e interpretador, (2) Rust para runtime/AOT/performance, (3) testes, exemplos e IDE/LSP, e (4) sintaxe enxuta e estável. Qualquer retomada exige um novo plano com escopo, orçamento e critérios de aceitação próprios.

## 1. Objetivo e não objetivos

O parser deverá consumir uma lista real de tokens, produzir uma AST estruturada e acumular erros sintáticos com linha e coluna. Não será permitido manter contagens fixas de tokens/nós, mensagens narrativas como resultado do parsing ou nomes de módulos codificados.

Não faz parte desta etapa alterar a semântica da linguagem, remover compatibilidade legada, trocar as palavras-chave em português ou implementar resolução de módulos. Durante a primeira etapa, `lexer.thz`, `parser.thz`, `ast.thz` e `driver.thz` permanecem como referências de migração; a única unidade executável passa a ser `selfhost.thz`.

### Decisão arquitetural — módulo único inicial (02/09/2026)

O compilador self-hosted será consolidado em um único módulo `compilador/selfhost.thz`. Ele conterá as estruturas de token/AST, lexer, cursor, parser e uma única operação pública de compilação. Isso elimina cópias temporárias, dependência implícita entre arquivos e a necessidade prematura de um resolvedor de `IMPORTAR`. A extração em módulos menores só ocorrerá depois que o compilador real tiver testes de AST e diagnósticos verdes.

## 2. Contratos de entrada e saída

- Entrada: `LISTA<Token>` produzida por `lexer.thz`.
- Saída: `ModuloAst` + `LISTA<DiagnosticoSintatico>`.
- Diagnóstico: `[Erro Sintático][Linha L:C] mensagem`.
- EOF inesperado e terminadores incompatíveis devem ser erros recuperáveis.
- A AST deve ser serializável em forma canônica para comparação com a JVM.
- Andamento: `TokenizarTexto` já contém o percurso caractere a caractere, comentários, operadores compostos e classificação numérica; o cursor alcança EOF de forma idempotente. A suíte self-hosted foi reexecutada sem cache em 02/09/2026 e revelou o gate pendente: o analisador ainda não contextualiza `[]` como `FATIA[TokenSelfHost]`, e a branch self-hosted ainda não registra `FATIA.adicionar`. Literais de texto foram mantidos fora desse caminho até haver uma representação canônica de aspa dupla no próprio THZ.

## 3. Fases de implementação

### Fase A — Infraestrutura

- [x] Criar `compilador/selfhost.thz` como única unidade executável do compilador real.
- [x] Migrar para ele somente as estruturas e operações necessárias, removendo números/mensagens demonstrativos do caminho novo.
- [x] Adicionar uma operação pública `CompilarTexto(fonte: TEXTO)` que retorne o resultado estrutural da análise. Evidência: `CompiladorSelfHostTest` reexecutado sem cache em 02/09/2026; a entrada ignora espaço/quebras/comentários e retorna 21 caracteres significativos para a fonte de teste. A composição de lexemas e o parser geral seguem pendentes.
- [x] Definir estruturas `Token`, `DiagnosticoSintatico`, `CursorTokens` e nós-base da AST em `ast.thz`.
- [x] Implementar `FATIA.adicionar` imutável no runtime JVM e `AdicionarToken` no lexer self-hosted.
- [x] Implementar `aceitar`, `exigir` e `estaEm` sobre o token atual; `avancar`, limite/EOF, registro de erro e sincronização já possuem primitivas iniciais. `CarregarTokens` usa `FATIA[TokenSelfHost]` e `Avancar` atualiza `tipo_atual`/`lexema_atual` por indexação.
- [x] Garantir que avanço após EOF seja seguro e idempotente no cursor self-hosted.
- [ ] Permitir inferência contextual para `[]` tipado e disponibilizar `FATIA.adicionar` no runtime usado pelo self-hosted.

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
