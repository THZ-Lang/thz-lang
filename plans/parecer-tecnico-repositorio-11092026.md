# Parecer técnico sobre o repositório THZ-LANG

**Data:** 11/09/2026  
**Base local:** branch `main`, commit `23f149b`, incluindo a especificação documental ainda não commitada de 10/09.  
**Natureza:** revisão transversal de arquitetura e implementação; não certificação de produção.

## 1. Posicionamento

A THZ tem uma base JVM substancial e reaproveitável: frontend, AST, análise
semântica, interpretador, contratos, biblioteca de dados e ferramentas. Entretanto,
as garantias anunciadas não são uniformes entre caminhos de execução e alvos.
O próximo marco deve priorizar correção e um fluxo de inventário verificável.

Recomendo manter a direção aprovada de chaves e idiomas por arquivo, mas iniciar
pela estabilização das garantias existentes e pela rejeição explícita de recursos
não suportados nos backends. Internacionalização já existe parcialmente; histórico
automático e vigência exigem mudanças semânticas mais profundas que novos tokens.

## 2. Alcance e limites da revisão

Foram inventariadas as áreas versionadas: sete módulos JVM, runtime Rust,
compilador experimental, extensão VS Code, exemplos, scripts, workflows,
documentação e planos. Foram lidos os caminhos centrais de análise, execução,
emissão, governança e dados, integrações das ferramentas e testes associados.
Foram encontrados 79 arquivos de testes Java; a quantidade não comprova cobertura.

Não foi feita leitura linha a linha de todos os arquivos, revisão dos binários,
execução da interface gráfica ou auditoria de serviços externos. As conclusões
abaixo distinguem inspeção estática de verificações executadas. Nenhuma consulta
ao estado remoto de CI ou releases foi usada para afirmar sucesso atual.

## 3. Defeitos reproduzidos com classes reais

As classes de runtime e do lexer foram compiladas isoladamente com o JDK 21
disponível, em `/tmp/thz-review-probes`, sem alterar suas fontes. Isso não substitui
a suíte oficial com Java 25.

| Prioridade | Reprodução | Resultado observado | Resultado necessário |
| --- | --- | --- | --- |
| Alta | `DecimalFixo.deTexto("4", 0).dividir(DecimalFixo.deTexto("3", 0))` | `2` | `1` no arredondamento bancário |
| Alta | Inserir `1.0` e `1.00` em `HashSet<DecimalFixo>` | `equals` verdadeiro, hashes 311 e 3102; conjunto com dois elementos | Hash compatível com igualdade; um elemento |
| Alta | Bloco de 1 MB; alocar 1 byte e depois `Integer.MAX_VALUE` | Aceita a solicitação e registra utilização `-2147483648` | Rejeitar sem alterar o estado |

No [decimal](../JVM/thz-core-jvm/src/main/java/thz/lang/runtime/DecimalFixo.java),
a divisão compara o resto com a metade inteira do denominador, confundindo certos
valores abaixo de meio com empates quando o denominador é ímpar. A comparação
deve considerar o dobro do resto contra o denominador. O hash usa valor escalado
e escala, enquanto a igualdade compara o valor matemático normalizado.

No [bloco de memória](../JVM/thz-core-jvm/src/main/java/thz/lang/runtime/BlocoMemoria.java),
a soma de inteiros transborda antes da comparação com a capacidade. A reprodução
reserva apenas 1 MB; não tenta reservar efetivamente bilhões de bytes. O defeito
demonstrado é na checagem e contabilização de limites.

## 4. Idiomas e diagnóstico: capacidade parcial existente

O [lexer](../JVM/thz-core-jvm/src/main/java/thz/lang/lexico/ThzLexer.java) já detecta
`LINGUAGEM: pt-BR` e `LANGUAGE: en-US`. Há tabela de palavras por idioma,
rejeição de mistura e tradução via formatador, com
[testes próprios](../JVM/thz-core-jvm/src/test/java/thz/lang/lexico/DialetoLinguagemTest.java).

Verificações isoladas confirmaram:

- `LANGUAGE: en-US` reconhece `PROGRAM` e `END_PROGRAM`.
- `lang enus` não seleciona inglês; `PROGRAM` é rejeitado como mistura com PT-BR.
- `LINGUAGEM: desconhecido` cai silenciosamente em PT-BR.
- A transformação de `x := 2`, com `x` na coluna 5, informa o token `x` na
  coluna 14 da linha transformada. Preservar o número da linha não preserva a coluna.

As mensagens de mistura continuam em português mesmo no modo inglês. A nova
especificação precisa evoluir o mecanismo existente, incluindo diagnóstico
localizado, posições originais e preservação do idioma em formatador e ferramentas.
Minha formulação anterior de internacionalização apenas como novidade foi incompleta.

## 5. Compilação e alvos: lacunas que impedem afirmar equivalência

### LLVM

Por inspeção, o [emissor de corpos](../JVM/thz-core-jvm/src/main/java/thz/lang/ir/GeradorIr.java)
trata `EXIBA` e ignora outros comandos no `default`. A exibição usa o texto canônico
da expressão, não sua avaliação geral. Operações de negócio são emitidas como
funções `void` sem parâmetros; funções puras têm suporte restrito a retornos e
expressões simples. Logo, emitir e linkar IR não demonstra execução da semântica JVM.

O [teste de LLVM/SIMD](../JVM/thz-core-jvm/src/test/java/thz/lang/ir/IrSimdTest.java)
verifica marcadores textuais como `main` e chamadas de arena. Esses testes são úteis
para estrutura, mas insuficientes para contratos, resultados e efeitos de negócio.

### WASM

O alvo `WEBASSEMBLY` no [driver](../JVM/thz-core-jvm/src/main/java/thz/lang/driver/ThzCompilerDriver.java)
retorna JavaScript com comentários de identificação. O
[teste correspondente](../JVM/thz-core-jvm/src/test/java/thz/lang/ThzWasmBuildTest.java)
procura texto e uma classe JavaScript, sem instanciar módulo WASM. Esse caminho não
comprova compilação THZ para WebAssembly, independentemente dos exports Rust existentes.

### CLI e empacotamento

O [comando compile](../JVM/thz-cli-jvm/src/main/java/thz/lang/cli/comandos/ComandoCompile.java)
faz lexer/parser e chama os emissores diretamente, sem executar o analisador
semântico utilizado pelo driver. É necessário unificar a barreira de validação.

O [script AOT](../scripts/build-llvm.ps1) compila objeto Linux com `clang -c` e
copia esse objeto para um arquivo `.elf`, que depois apresenta como executável.
A rotina Linux do workflow de CI tem uma etapa distinta de linkagem; os dois
caminhos não devem ser tratados como equivalentes.

## 6. Governança e inventário

Há contratos de entrada/saída, invariantes, metadados, rastreabilidade e
idempotência implementados na JVM. Há também primitivas de transação em `ThzDb`.
São bases úteis, mas não equivalem ao contrato de inventário aprovado.

Por inspeção, [campos da AST](../JVM/thz-core-jvm/src/main/java/thz/lang/ast/CampoEstruturaAst.java)
contêm nome e tipo; a [regra](../JVM/thz-core-jvm/src/main/java/thz/lang/ast/RegraNegocioAst.java)
não representa revisão e vigência. Não foi identificado nesses caminhos o mecanismo
de histórico automático por campo, autoria autenticada obrigatória e imutabilidade
persistente que a nova especificação exige.

O [auditor](../JVM/thz-core-jvm/src/main/java/thz/lang/governanca/AuditorGovernanca.java)
emite “APROVADO PARA PRODUÇÃO” a partir de inspeção da AST e métricas de governança.
Isso não corresponde à liberação baseada em homologação e dados reais que definimos.

Também existe um conflito concreto com a política de dados: o
[conversor decimal de qualidade de dados](../JVM/thz-core-jvm/src/main/java/thz/lang/analytics/ThzDataQuality.java)
retorna zero para ausência e para certas falhas de conversão. O importador de
referência precisa preservar a distinção entre ausência, entrada inválida e zero.

## 7. Recursos, ferramentas e amplitude

O runtime Rust contém arena, operações vetoriais e criptografia. A presença dessas
primitivas não prova que programas THZ completos respeitam limites de recursos.
O [pipeline](../JVM/thz-core-jvm/src/main/java/thz/lang/pipeline/ThzPipelineDataEngine.java)
cria uma tarefa e um futuro por registro; a rotina de streaming é explicitamente
simulada. Não há nesse fluxo a política aprovada de memória e concorrência limitada.

CLI, GUI, API e LSP possuem integrações concretas com o núcleo e podem sustentar
a aplicação de referência. A fachada compartilhada é um ponto de consolidação,
mas ainda há caminhos que a contornam. A expansão paralela para agente de IA,
UI, documentos, bancos, mensageria e vários backends aumenta a superfície a manter.
O módulo LLM Rust declara respostas simuladas; isso deve permanecer visível na
classificação de maturidade.

O README, TODO, scripts e planos têm orientações conflitantes: runtime C versus
Rust, formas legada/indentada/com chaves e capacidades anunciadas como completas
apesar de testes restritos. Recomendo uma matriz de suporte por recurso e alvo,
com evidência executável, como referência única de maturidade.

## 8. Verificação do ambiente

- `./gradlew test --offline --no-daemon`: wrapper sem permissão de execução.
- `bash gradlew ...`: cache padrão do Gradle sem permissão de escrita.
- Com `GRADLE_USER_HOME` em `/tmp`: wrapper precisou baixar a distribuição e a
  rede restrita impediu a inicialização. A suíte não começou.
- `cargo test --offline`, com saída em `/tmp`: dependência `aes-gcm` ausente do
  cache. A suíte Rust não começou.
- Verificações isoladas de runtime e lexer: compiladas e executadas; resultados
  registrados nas seções 3 e 4.

Esses bloqueios de ambiente não são resultados de falha dos testes do produto.
Não há base nesta revisão para declarar toda a suíte aprovada ou reprovada.

## 9. Sequência recomendada

1. Corrigir divisão decimal, igualdade/hash e limites de memória, com regressões
   que cubram valores negativos, escalas diferentes e fronteiras de capacidade.
2. Unificar análise e emissão: nenhum comando de compilação pode contornar erros;
   backends devem rejeitar construções ainda não suportadas.
3. Formalizar `lang` e chaves sobre tokens/AST comuns, com migração explícita,
   mapa de posições e preservação de identificadores e literais.
4. Entregar inventário JVM de ponta a ponta: entrada tipada, conciliação, atualização
   explícita, concorrência, histórico atômico e seleção de revisão vigente.
5. Implementar a liberação baseada em evidências, separada da auditoria estática.
6. Demonstrar o mesmo recorte em execução nativa, comparando resultados, rejeições,
   efeitos e limites de recursos. Self-hosting permanece fora do caminho crítico.

Não recomendo reescrever todo o projeto nem expandir agora os recursos periféricos.
O investimento prioritário deve transformar as garantias centrais em comportamentos
observáveis, preservados em todas as entradas e alvos suportados.
