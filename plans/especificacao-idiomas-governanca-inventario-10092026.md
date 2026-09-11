# Especificação inicial — idiomas, governança e inventário

**Data:** 10/09/2026  
**Status:** decisões de produto aprovadas; sintaxe ilustrativa e implementação pendentes.  
**Origem:** decisões confirmadas pelo mantenedor na conversa de definição da linguagem.

## 1. Escopo e relação com os planos anteriores

A THZ deve permitir processamento orientado a dados, regras compreensíveis por
desenvolvedores e analistas, baixa latência e consumo controlado de recursos.
O comportamento será consolidado na JVM e terá equivalência nativa como prioridade.
A aplicação de referência importa planilhas, concilia com o banco e mantém um
inventário auditável. Conciliação, limpeza e validação, cálculos contratuais e
consolidação de indicadores compõem a direção mais ampla da linguagem.

Esta especificação registra a preferência aprovada por blocos com chaves e escrita
compacta. Ela sucede, como direção de produto, a preferência por indentação da
[reorientação de 02/09](plano-sintaxe-moderna-31082026-0953.md).
Não constitui evidência de mudança no compilador ou de paridade entre alvos.

As grafias compactas abaixo ainda precisam de um mapeamento formal para as
construções canônicas, incluindo `REGRA_NEGOCIO`, `EXIGE`, `GARANTE` e
`METADADOS_ARQUITETURA`. A exigência de metadados nos programas corporativos
permanece. Os exemplos são fragmentos de desenho, não programas completos.
A migração e a compatibilidade com arquivos existentes exigem especificação própria.
O [ADR-006](../docs/ADRs/ADR-006-sintaxe-moderna-unificada.md) permanece como registro
histórico; uma futura decisão de migração deverá explicitar sua substituição parcial.

## 2. Idioma declarado por arquivo

Decisões aprovadas:

- Todo arquivo de código começa com `lang ptbr` ou `lang enus`.
- A declaração seleciona palavras-chave, tipos nativos e idioma dos diagnósticos.
- Ambos os idiomas representam a mesma linguagem, com a mesma semântica,
  contratos, precisão numérica e garantias de execução.
- Arquivos em idiomas diferentes podem importar uns aos outros. Cada arquivo
  mantém seu próprio idioma; importar não traduz símbolos exportados.
- Identificadores do programador e textos literais são preservados. Podem estar
  em qualquer idioma.
- Ausência da declaração, idioma desconhecido e uso de palavra-chave exclusiva
  do outro idioma produzem diagnóstico com linha e coluna na fonte original.
- Formatos de datas, números e moedas nas entradas são explícitos e independem
  do idioma do código. A seleção não autoriza conversão silenciosa de dados.

O reconhecimento de palavras-chave deve respeitar o contexto léxico: uma palavra
inglesa em um comentário ou literal não representa mistura de idiomas.
A política de colisão entre identificadores e palavras reservadas ainda precisa
ser formalizada.

### Exemplos equivalentes de desenho

As duas versões preservam os mesmos identificadores e a mesma mensagem literal
intencionalmente. Apenas as construções da linguagem e o tipo nativo mudam.
As grafias `regra`/`rule`, `Texto`/`Text` e `exige`/`requires` são propostas de
mapeamento, ainda não um vocabulário completo aprovado.

```text
lang ptbr

regra IdentificacaoObrigatoria(codigo: Texto) {
    exige codigo != ""
        senao "Código de inventário ausente."
}
```

```text
lang enus

rule IdentificacaoObrigatoria(codigo: Text) {
    requires codigo != ""
        else "Código de inventário ausente."
}
```

Esses fragmentos ilustram apenas a equivalência linguística de uma condição;
não esgotam validação de ausências, espaços em branco, revisão, vigência ou
metadados corporativos. Mensagens literais de negócio não são traduzidas pelo
compilador; sua localização pertence à aplicação.

## 3. Dados, regras e operações

- Entidades declaram dados e garantias permanentes. Governança é obrigatória
  quando declarada; não é imposta a todo dado temporário ou programa utilitário.
- Regras recebem dados e produzem decisões ou cálculos, sem acesso direto a
  banco, arquivos, rede ou relógio implícito. Informações variáveis, como a data
  de avaliação, são entradas explícitas.
- Operações coordenam entrada e saída, identidade autenticada e persistência.
- Violações demonstráveis estaticamente são rejeitadas na compilação. Entradas
  externas devem ser validadas durante a execução antes de efeitos protegidos.
- Violações independentes são acumuladas por item. Cálculos dependentes de um
  campo inválido não são executados como se o campo fosse válido.
- Revisão por analista é recomendada, sem aprovação humana obrigatória para
  compilar ou executar.

Decimais e moedas preservam aritmética exata, arredondamento meio-par e restrições
de moeda já estabelecidas no projeto. Nenhum idioma altera essas garantias.

## 4. Contrato da aplicação de inventário

### Identidade, importação e conciliação

O código de inventário é obrigatório, único e imutável após o cadastro.
Uma tentativa explícita de alterá-lo deve ser rejeitada na compilação quando
demonstrável; nenhum caminho de atualização pode contornar a imutabilidade.

| Situação | Resultado obrigatório |
| --- | --- |
| Código novo e item válido | Cadastrar automaticamente e registrar a origem |
| Item existente e valores iguais | Preservar o registro sem consumir histórico de valores |
| Item existente e valores diferentes | Relatar divergências; banco e histórico prevalecem |
| Item inválido | Bloquear sua ação e continuar com os demais itens válidos |
| Mesmo código em linhas com valores diferentes | Bloquear todas as linhas conflitantes e continuar com os demais códigos |

A atualização de item existente exige uma operação explícita. Importação comum
não autoriza sobrescrita, mesmo que a planilha pareça mais recente.

**Consequência da duplicidade:** a implementação não pode confirmar o cadastro de
uma linha e só depois descobrir outra conflitante no mesmo arquivo. Deve detectar
os conflitos antes da confirmação daquele código, por exemplo com pré-validação
ou armazenamento intermediário. A estratégia deve respeitar os limites de memória;
carregar a planilha inteira em RAM não é requisito desta especificação.

### Atualização, autoria e concorrência

Toda atualização exige autor proveniente da identidade autenticada e justificativa
preenchida. Um nome informado livremente não substitui a identidade autenticada.
Uma operação pode alterar vários campos do mesmo item: todos são validados e
gravados de forma indivisível com o histórico. Falha em qualquer parte desfaz
toda a alteração desse item; outros itens válidos continuam independentes.

Uma atualização baseada em versão desatualizada é bloqueada. O solicitante deve
reler o item e revisar as diferenças. A verificação da versão e a gravação precisam
ser indivisíveis para evitar sobrescrita entre a leitura e a confirmação.
Não há tentativa automática de reaplicar a alteração sobre dados novos.

### Histórico automático

Quando declarado, o histórico é imposto pela implementação, sem depender de
chamadas manuais do desenvolvedor a cada alteração:

- Valor atual mais até dez valores anteriores por campo.
- Apenas mudanças efetivas de valor ocupam posições nesse histórico.
- Valores que excedem a janela recente seguem para auditoria histórica,
  preservando sequência e origem; não são descartados.
- Alteração, histórico recente e transferência para auditoria são indivisíveis.
- A rastreabilidade inclui valor anterior e novo, autor autenticado, justificativa,
  data, origem e revisão da regra aplicada.

## 5. Revisão, vigência e diagnósticos

Cada regra possui identidade, revisão e vigência. A referência temporal é explícita
e o processamento registra a revisão aplicada. Deve existir exatamente uma revisão
aplicável: nenhuma revisão vigente ou múltiplas revisões aplicáveis bloqueiam a ação
com diagnóstico específico. Não se escolhe implicitamente a revisão mais recente.

| Categoria | Significado |
| --- | --- |
| Ausente | Valor não fornecido; bloqueia se obrigatório |
| Ilegível ou inválido | Conteúdo fornecido que não pode ser interpretado ou aceito; preservar conteúdo e causa |
| Divergência | Valores válidos diferentes na conciliação; não autoriza atualização |
| Falha operacional | Problema de acesso ou persistência, distinto de conteúdo inválido |
| Conflito de versão | Atualização baseada em estado desatualizado |
| Conflito de entrada | Linhas do mesmo código com valores diferentes |
| Vigência indefinida ou ambígua | Zero ou múltiplas revisões aplicáveis |

Diagnósticos sintáticos e semânticos devem preservar linha e coluna exatas.
O formato PT-BR segue `[Erro Sintático][Linha L:C]`; a representação localizada
EN-US ainda será especificada. Falhas de negócio devem identificar a regra,
condição e item afetado, além da revisão quando houver uma revisão selecionada.

## 6. Liberação do binário final para produção

A liberação exige tanto homologação aprovada quanto ausência de pendências
impeditivas nos dados reais avaliados. Um caso de homologação propositalmente
inválido passa se for rejeitado conforme o resultado esperado.

O modo de validação para liberação não grava no acervo. As evidências devem estar
vinculadas à versão do código, revisões das regras, referência temporal e conjunto
de dados avaliado. Evidências de outra versão não comprovam a versão atual.
O mecanismo de identificação das versões dos dados ainda será especificado.

Essa verificação não prova a validade de eventos futuros. Em produção, novos dados
continuam sujeitos às regras e ao bloqueio por item. Revisão humana recomendada
não deve ser confundida com a exigência técnica de evidências de validação.

## 7. Casos de aceitação planejados

Estes casos são requisitos para testes futuros, não resultados de testes executados.

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| I01 | Fontes equivalentes em `ptbr` e `enus` | Mesma semântica e resultados após normalização das grafias |
| I02 | Declaração de idioma ausente | Diagnóstico com posição na fonte |
| I03 | `lang` com idioma desconhecido | Diagnóstico na declaração |
| I04 | Palavra-chave exclusiva do idioma errado | Diagnóstico na palavra, com linha e coluna |
| I05 | Identificador, comentário ou literal em outro idioma | Nenhuma tradução ou rejeição apenas pelo idioma |
| I06 | Importação entre módulos de idiomas diferentes | Símbolos preservados e contratos aplicados igualmente |
| I07 | Mesmos dados e formatos explícitos nos dois idiomas | Mesma interpretação, sem mudança implícita de datas ou decimais |
| I08 | Erro em módulo importado com outro idioma | Diagnóstico no idioma e na posição do arquivo de origem |
| D01 | Item novo válido | Cadastro e origem registrados |
| D02 | Dois campos obrigatórios ausentes | Duas violações independentes; nenhuma ação no item |
| D03 | Campo ilegível e falha de acesso | Categorias distintas, sem substituição por valor padrão |
| D04 | Divergência com item existente | Relatório com os valores; banco preservado |
| D05 | Atualização sem autenticação ou sem justificativa | Bloqueio sem alteração |
| D06 | Alteração de dois campos, com falha em um | Nenhum campo alterado nem histórico parcial |
| D07 | Entrada do 11º valor anterior no histórico | Dez anteriores recentes; excedente preservado na auditoria |
| D08 | Falha ao gravar histórico ou auditoria | Valor atual e históricos anteriores preservados |
| D09 | Reaplicação do mesmo valor | Nenhuma nova posição no histórico de valores |
| D10 | Tentativa de mudar o código | Rejeição; identidade preservada |
| D11 | Duas atualizações da mesma versão | Uma confirmação; a outra bloqueada para releitura e revisão |
| D12 | Linhas conflitantes distantes, inclusive a última linha | Todas bloqueadas para aquele código; nenhuma gravação antecipada |
| D13 | Um item falha e outro é válido | O válido pode ser confirmado independentemente |
| R01 | Nenhuma revisão vigente | Ação bloqueada com causa específica |
| R02 | Duas revisões aplicáveis | Ação bloqueada por ambiguidade |
| R03 | Exatamente uma revisão aplicável | Avaliação registra a revisão e referência usadas |
| R04 | Regra tenta acessar banco, arquivo, rede ou relógio implícito | Rejeição do efeito proibido |
| P01 | Teste negativo é rejeitado conforme esperado | Homologação desse caso aprovada |
| P02 | Homologação falha ou dados reais têm pendência impeditiva | Liberação do binário final bloqueada |
| P03 | Validação para liberação | Relatório e evidências produzidos sem gravação no acervo |
| P04 | Código, regras ou dados diferem da evidência apresentada | Exigir validação correspondente à versão pretendida |
| P05 | Evento inválido após liberação | Ação do item bloqueada durante a execução |
| E01 | Casos equivalentes JVM e nativo | Resultados, erros, precisão e efeitos equivalentes |
| E02 | Execução Windows e Linux | Mesmas garantias com caminhos e comandos válidos em cada sistema |

## 8. Pendências e sequência de implementação

1. Especificar vocabulário completo PT-BR/EN-US, gramática com chaves, distinção
   entre palavras reservadas e identificadores, sensibilidade a maiúsculas e
   migração das construções atuais.
2. Definir tratamento de BOM, comentários e linhas vazias antes de `lang`, declaração
   repetida e idioma do diagnóstico quando não houver declaração válida.
3. Definir limites dos intervalos de vigência e preservação das revisões para
   reprodução histórica; não presumir inclusão das duas extremidades.
4. Definir duplicidades com valores iguais, normalização dos códigos e critérios
   de igualdade por campo, sem alterar silenciosamente a identidade.
5. Especificar origem de atualizações manuais, auditoria de tentativas sem mudança
   e representação conjunta das revisões quando múltiplas regras protegem a ação.
6. Definir identificação dos dados validados, validade das evidências e protocolo
   de liberação para produção.
7. Fixar limites de memória, concorrência, filas e política de sobrecarga, com
   metas mensuráveis de latência. Nenhuma meta numérica foi aprovada até aqui.
8. Implementar um recorte vertical na JVM, depois validar equivalência nativa.
   A integração com o compilador self-hosted exige plano próprio; seu
   [plano anterior](plano-refatoracao-parser-self-hosted-31082026.md) está cancelado.

## 9. Evidências deste marco documental

- [x] Decisões aprovadas consolidadas, com sintaxe ilustrativa separada de implementação.
- [x] Fragmentos equivalentes PT-BR/EN-US e casos de aceitação documentados.
- [x] Relação com o plano anterior e pendências de migração explicitadas.
- [ ] Gramática, compilador e ferramentas implementados para esta especificação.
- [ ] Casos de aceitação automatizados e executados.
- [ ] Paridade JVM/nativo e, quando aplicável, JVM/self-hosted demonstrada.

### Marco JVM inicial — 11/09/2026

- [x] CSV UTF-8 limitado, identificado por SHA-256 e validado antes da gravação.
- [x] Cadastro, conciliação sem sobrescrita e falha independente por item.
- [x] Atualização explícita com versão esperada, autor fornecido pelo host e justificativa.
- [x] Histórico indivisível, preservado integralmente, com janela recente calculada por campo.
- [x] Revisão vigente única e referência temporal explícita.
- [x] Modo de validação sem criação ou alteração do banco do acervo.
- [x] Programa THZ de referência ligado à capacidade autenticada da JVM.
- [ ] Gramática `lang ptbr`/`lang enus` e chaves implementada.
- [ ] Importação de XLSX e banco externo além do SQLite de referência.
- [ ] Liberação de produção baseada em evidências implementada.
- [ ] Equivalência nativa demonstrada.

Evidências executadas em 11/09/2026 com Java 25:

- suíte completa de `thz-core-jvm`: aprovada, incluindo os quatro testes iniciais
  do serviço e as regressões de decimal e memória;
- teste de integração do programa `inventario_auditavel.thz`: aprovado;
- suíte de `thz-cli-jvm` pelo build composto: aprovada;
- execução real do exemplo pela CLI contra SQLite temporário: concluída com sucesso.

A compilação Linux exigiu corrigir apenas a capitalização do arquivo
`ThzWebViewBridge.java`, cujo nome anterior não correspondia à classe pública.

Verificação deste marco: revisão documental, destinos dos links relativos e
`git diff --check`. Não foram executadas suítes do compilador: esta entrega
altera apenas documentação e não declara capacidade executável nem paridade.
