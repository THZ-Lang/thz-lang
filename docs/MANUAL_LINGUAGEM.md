# Manual Oficial da Linguagem THZ-LANG (v2.4.0)

> A sintaxe canônica atual é enxuta e baseada em indentação. Fontes históricas
> com `VARIAVEL`, `<-` e `FIM_*` continuam válidas durante a migração.

```thz
programa NEGOCIO Faturamento:
    estrutura Item:
        quantidade: INTEIRO
        valor: MONETARIO

    funcao subtotal(item: Item) -> MONETARIO:
        total := item.quantidade * item.valor
        retorne total

    regra EmitirFatura:
        exige cliente.ativo
        garante resultado.total >= 0
```

Use `thz fmt arquivo.thz -w` para migrar para a forma enxuta. Use
`thz fmt arquivo.thz --legado` quando precisar da representação antiga.

Bem-vindo ao **Manual Oficial do THZ-LANG**, a linguagem de programação de sistemas corporativa e orientada a domínio (DDD) estruturada inteiramente em Língua Portuguesa. O THZ-LANG foi projetado especificamente para unificar **Governança de Negócio**, **Design por Contrato (DbC)**, **Arquitetura Viva**, **Processamento Numérico e Financeiro de Alta Integridade** e **Engenharia de Dados de Alta Performance** (SIMD/SoA/Arenas).

---

## 📚 Sumário
1. [Filosofia de Design e Arquitetura de Dialeto Duplo (PT-BR & EN-US)](#1-filosofia-de-design-e-arquitetura-de-dialeto-duplo-pt-br--en-us)
2. [Diretiva de Dialeto e Tabela de Equivalência de Sintaxe](#2-diretiva-de-dialeto-e-tabela-de-equivalência-de-sintaxe)
3. [Arquitetura Viva e Metadados (`METADADOS_ARQUITETURA`)](#3-arquitetura-viva-e-metadados-metadados_arquitetura)
4. [Tipos de Dados e Aritmética Exata (ISO/IEC 10967 & ISO 4217)](#4-tipos-de-dados-e-aritmética-exata-isoiec-10967--iso-4217)
5. [Sintaxe Global de Módulos e Arquétipos](#5-sintaxe-global-de-módulos-e-arquétipos)
6. [Estruturas, Enumerações e Modelagem Colunar (SoA)](#6-estruturas-enumerações-e-modelagem-colunar-soa)
7. [Design por Contrato (DbC) e Governança](#7-design-por-contrato-dbc-e-governança)
8. [Variáveis, Funções e Estruturas de Controle](#8-variáveis-funções-e-estruturas-de-controle)
9. [Tratamento Idiomático de Erros (`RESULTADO`)](#9-tratamento-idiomático-de-erros-resultado)
10. [Engenharia de Dados: Arenas de Memória e SIMD](#10-engenharia-de-dados-arenas-de-memória-e-simd)
11. [Pipelines de Big Data (`PIPELINE_DADOS`)](#11-pipelines-de-big-data-pipeline_dados)
12. [DSL de Interface Gráfica e Tela Declarativa (`.thzui`)](#12-dsl-de-interface-gráfica-e-tela-declarativa-thzui)
13. [Segurança Criptográfica & Conformidade BACEN/LGPD (PBKDF2/AES-GCM)](#13-segurança-criptográfica--conformidade-bacenlgpd-pbkdf2aes-gcm)
14. [Referência Exaustiva da Biblioteca Padrão (Stdlib API)](#14-referência-exaustiva-da-biblioteca-padrão-stdlib-api)

---

## 1. Filosofia de Design e Arquitetura de Dialeto Duplo (PT-BR & EN-US)

O **THZ-LANG** resolve o hiato entre a modelagem arquitetural (Domain-Driven Design), a governança de negócio e a entrega de software de alto desempenho.

A partir da versão **v2.4.0**, o THZ-LANG introduz **Arquitetura de Dialetos Duplos (PT-BR / EN-US)**:
* **Dialeto Canônico PT-BR:** Sintaxe nativa em português estruturado para clareza corporativa.
* **Dialeto Internacional EN-US:** Sintaxe equivalente em inglês para interoperabilidade com equipes globais.
* **AST & IR Unificados:** Ambos os dialetos geram a exata mesma Árvore Sintática e Representação Intermediária (`thz-ir/1`), garantindo compatibilidade total com o compilador nativo LLVM, o interpretador e a engine SIMD.
* **Regra de Pureza Estrita (*Single Dialect*):** É proibido misturar palavras-chave em inglês e português no mesmo arquivo.

---

## 2. Diretiva de Dialeto e Tabela de Equivalência de Sintaxe

### 2.1 Diretiva de Cabeçalho (Linha 1 ou 2)
Para selecionar o dialeto, declare no cabeçalho do arquivo fonte:
* `LINGUAGEM: pt-BR` (ou ausência de diretiva, assumindo PT-BR por padrão)
* `LANGUAGE: en-US` (ativa dialeto em inglês)

### 2.2 Tabela de Equivalência Canônica

| Categoria | PT-BR (`LINGUAGEM: pt-BR`) | EN-US (`LANGUAGE: en-US`) |
| :--- | :--- | :--- |
| **Arquétipo** | `PROGRAMA`, `BIBLIOTECA`, `MODULO`, `EXTENSAO`, `TESTE`, `FERRAMENTA`, `TELA` | `PROGRAM`, `LIBRARY`, `MODULE`, `EXTENSION`, `TEST`, `TOOL`, `SCREEN` |
| **Metadados** | `METADADOS_ARQUITETURA`, `DOMINIO`, `AUTOR`, `VERSAO`, `CAMADA`, `CRITICIDADE` | `ARCHITECTURE_METADATA`, `DOMAIN`, `AUTHOR`, `VERSION`, `LAYER`, `CRITICALITY` |
| **Contratos** | `REGRA_NEGOCIO`, `PROCESSO`, `EXIGE`, `GARANTE`, `INVARIANTE`, `IDEMPOTENTE` | `BUSINESS_RULE`, `PROCESS`, `REQUIRES`, `ENSURES`, `INVARIANT`, `IDEMPOTENT` |
| **Estruturas**| `ESTRUTURA`, `CRIAR`, `VALIDAR` | `STRUCTURE`, `CREATE`, `VALIDATE` |
| **Memória**   | `USAR_BLOCO_MEMORIA`, `LAYOUT_COLUNAR`, `VETORIZAR_PARA` | `USE_MEMORY_BLOCK`, `COLUMNAR_LAYOUT`, `VECTORIZE_FOR` |
| **Controle**  | `SE`, `ENTAO`, `SENAO`, `ENQUANTO`, `FACA`, `PARA`, `DE`, `ATE`, `PASSO` | `IF`, `THEN`, `ELSE`, `WHILE`, `DO`, `FOR`, `FROM`, `TO`, `STEP` |
| **Saída & I/O** | `EXIBA`, `RETORNE`, `LER`, `FALHAR_COM` | `PRINT`, `RETURN`, `READ`, `FAIL_WITH` |
| **Terminadores** | `FIM_PROGRAMA`, `FIM_ESTRUTURA`, `FIM_REGRA`, `FIM_SE`, `FIM_PARA` | `END_PROGRAM`, `END_STRUCTURE`, `END_RULE`, `END_IF`, `END_FOR` |

---

## 2. Arquitetura Viva e Metadados (`METADADOS_ARQUITETURA`)

Toda unidade lógica pode ter sua governança e limites funcionais acoplados sintaticamente por meio de um cabeçalho estruturado. Essa especificação é validada pelo [Analisador Semântico](file:///c:/Users/lucas/Projetos/thz-lang/JVM/thz-core-jvm/src/main/java/thz/lang/semantico/AnalisadorSemantico.java) e exportada automaticamente como documentação estruturada.

```thz
METADADOS_ARQUITETURA
    SISTEMA: "PlataformaVendas"
    SUBDOMINIO: "MotorFaturamento"
    CAMADA: "Dominio"
    VERSAO: "2.4.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "10ms"
    CONFORMIDADE: "ISO-10967", "LGPD-Art7", "BACEN-Res4893"
FIM_METADADOS
```

### Validações Estritas:
* O cabeçalho é obrigatório quando a compilação é executada em modo estrito (`--estrito`).
* Permite definir conformidades normativas específicas que influenciam na auditoria de conformidade regulatória via `thz audit --git`.

---

## 3. Tipos de Dados e Aritmética Exata (ISO/IEC 10967 & ISO 4217)

O sistema de tipos do THZ-LANG é estático, impedindo a ocorrência de erros implícitos de conversão de dados.

### 3.1 Tipos Suportados

| Tipo | Sintaxe de Declaração | Descrição / Restrição |
| :--- | :--- | :--- |
| **`INTEIRO`** | `INTEIRO` | Inteiro sinalizado padrão de 64 bits. |
| **`INTEIRO32`** | `INTEIRO32` | Inteiro sinalizado de 32 bits (comum para índices/SIMD). |
| **`INTEIRO64`** | `INTEIRO64` | Sinônimo explícito de 64 bits para conformidade de dados. |
| **`NATURAL32`** | `NATURAL32` | Inteiro não-sinalizado de 32 bits para valores contáveis positivos. |
| **`DECIMAL(P,S)`** | `DECIMAL(12, 4)` | Decimal fixo com precisão $P$ (dígitos totais) e escala $S$ (dígitos fracionários). |
| **`MONETARIO(M)`**| `MONETARIO(BRL)` | Tipo de valor monetário vinculado a uma tag ISO 4217 alfa-3 de moeda. |
| **`TEXTO`** | `TEXTO` | Cadeia de caracteres codificada em UTF-8 nativo. |
| **`LOGICO`** | `LOGICO` | Booleano, assume apenas `VERDADEIRO` ou `FALSO`. |
| **`UUID`** | `UUID` | Identificador universal único de 128 bits (RFC 4122). |
| **`DATA`** | `DATA` | Data civil (Ano, Mês, Dia). |
| **`DATA_HORA`** | `DATA_HORA` | Data e Hora (Ano, Mês, Dia, Hora, Minuto, Segundo). |
| **`FATIA[T]`** | `FATIA[INTEIRO]` | Vetor indexável contíguo de tipo homogêneo $T$. |
| **`RESULTADO[T,E]`**| `RESULTADO[Pedido, TEXTO]`| Canal de controle monádico que retorna `SUCESSO(T)` ou `ERRO(E)`. |

### 3.2 Aritmética Monetária Rigorosa (ISO 4217)
Operações financeiras não utilizam ponto flutuante binário. Aritmética decimal de precisão e escala é garantida através do arredondamento contábil meio-par (*Half-Even / Banker's Rounding*).
* **Bloqueio de Conversão Implícita:** É terminantemente proibido somar ou subtrair `MONETARIO` de moedas distintas (ex. `BRL` com `USD`) sem conversão explícita.
* **Declaração de Literal:** Valores monetários utilizam o sufixo da moeda (`150.50 BRL`).

---

## 4. Sintaxe Global de Módulos e Arquétipos

Toda unidade de código-fonte é iniciada por um cabeçalho estrutural de arquétipo e encerrada pelo seu terminador correspondente, definindo seu ciclo de vida e empacotamento:

```thz
# 1. Módulo de Execução Principal de Domínio
PROGRAMA NEGOCIO FaturamentoLote
    // Código principal aqui
FIM_PROGRAMA

# 2. Módulo de Engenharia de Dados
PIPELINE_DADOS TransacoesStreaming
    // Código do pipeline aqui
FIM_PIPELINE

# 3. Módulo de Funções Reutilizáveis (Stdlib / Utilities)
BIBLIOTECA MatematicaAplicada
    // Métodos exportáveis
FIM_BIBLIOTECA

# 4. Módulo de Interface Visual declarativa (.thzui)
TELA PainelLancamentos
    // Componentes gráficos reativos
FIM_TELA

# 5. Módulos de Verificação e Testes
TESTE TesteUnitarioCalculo
    // Suíte de testes de invariantes
FIM_TESTE
```

---

## 5. Estruturas, Enumerações e Modelagem Colunar (SoA)

As estruturas são os blocos construtores de domínio no THZ-LANG. Elas suportam propriedades estruturadas, invariantes de consistência e alocação orientada a dados.

### 5.1 Estrutura e Enumeração Comum
```thz
ENUMERACAO CanalAtendimento
    LOJA_FISICA,
    E_COMMERCE,
    PARCEIRO_API
FIM_ENUMERACAO

ESTRUTURA Cliente
    id: UUID
    nome: TEXTO
    canal: CanalAtendimento
    limite_compra: DECIMAL(12, 2)

    INVARIANTE limite_compra >= 0.00
FIM_ESTRUTURA
```

### 5.2 Modificador `LAYOUT_COLUNAR` (Structure of Arrays - SoA)
Quando aplicado o modificador `LAYOUT_COLUNAR`, o compilador rearranja os elementos internos em memória na forma de vetores contíguos de propriedades individuais em vez de um vetor de objetos. Isso viabiliza o processamento vetorial através da CPU usando instruções SIMD (AVX2/AVX-512) no laço `VETORIZAR_PARA`.

```thz
ESTRUTURA ItemFatura LAYOUT_COLUNAR
    codigo_barras: TEXTO
    quantidade: NATURAL32
    preco_unitario: DECIMAL(12, 4)
    valor_tributo: DECIMAL(12, 4)
FIM_ESTRUTURA
```

---

## 6. Design por Contrato (DbC) e Governança

O THZ-LANG implementa contratos formais em tempo de execução e compilação como cidadãos de primeira classe no bloco `REGRA_NEGOCIO`.

```thz
REGRA_NEGOCIO CalculoIcmsLote
    IDENTIFICADOR_REGRA: "BR-FISCAL-ICMS-001"
    RASTREIO_REQUISITO: "REQ-TRIBUTOS-2026"
    DESCRICAO: "Processa cálculo do ICMS estadual sob o lote de transações."
    IDEMPOTENTE
    CHAVE_IDEMPOTENCIA: "UUID-RECONCILIACAO-TRIBUTARIA"

    CONTRATO_ENTRADA
        EXIGE itens.quantidade > 0
        EXIGE itens.valor_unitario >= 0.0000
    FIM_CONTRATO_ENTRADA

    CONTRATO_SAIDA
        GARANTE itens.valor_tributo >= 0.0000
    FIM_CONTRATO_SAIDA

    OPERACAO CalcularTributo(itens: FATIA[ItemFatura]): DECIMAL(18, 4)
    INICIO
        VARIAVEL acumulador : DECIMAL(18, 4) <- 0.0000
        VETORIZAR_PARA item EM itens PASSO_SIMD 8
            VARIAVEL valor_item : DECIMAL(18, 4) <- item.quantidade * item.preco_unitario
            item.valor_tributo <- valor_item * 0.1800
            acumulador <- acumulador + item.valor_tributo
        FIM_PARA
        RETORNE acumulador
    FIM
FIM_REGRA_NEGOCIO
```

### Elementos do Contrato:
* **`EXIGE`:** Define as pré-condições da operação. Se violado, causa uma falha imediata antes da execução do corpo do bloco.
* **`GARANTE`:** Especifica as pós-condições da operação. Garante que os estados e retornos ao fim da execução satisfazem a regra.
* **`IDEMPOTENTE` / `CHAVE_IDEMPOTENCIA`:** Garante conformidade de repetição segura na execução financeira e lógica.

---

## 7. Variáveis, Funções e Estruturas de Controle

### 7.1 Declaração e Atribuição
Variáveis são declaradas usando a palavra-chave `VARIAVEL`. A tipagem é estática e pode ser inferida dinamicamente na atribuição inicial usando o operador de seta esquerda `<-`.

```thz
# Tipagem declarada
VARIAVEL saldo : DECIMAL(12, 2) <- 1500.00

# Tipagem inferida (UUID)
VARIAVEL id_sessao <- SEGURANCA.uuid()
```

### 7.2 Funções reutilizáveis

`FUNCAO` representa cálculo reutilizável com retorno tipado obrigatório. Ela
não substitui `OPERACAO`, que pertence a `REGRA_NEGOCIO` e pode carregar
contratos formais, nem `PROCEDIMENTO`, destinado à orquestração e efeitos
externos.

```thz
FUNCAO somar(a: INTEIRO32, b: INTEIRO32): INTEIRO32
    RETORNE a + b
FIM_FUNCAO
```

Para cálculos de uma única expressão, o bloco pode ser reduzido:

```thz
FUNCAO dobrar(valor: INTEIRO32): INTEIRO32 = valor * 2
```

### 7.3 Estruturas de Controle de Fluxo


#### Condicional (`SE`)
```thz
SE total_fatura > 1000.00 BRL ENTAO
    AplicaDesconto(0.10)
SENAO SE total_fatura > 500.00 BRL ENTAO
    AplicaDesconto(0.05)
SENAO
    AplicaDesconto(0.00)
FIM_SE
```

#### Laço Enquanto (`ENQUANTO`)
```thz
ENQUANTO total_processado < tamanho(itens) FACA
    ProcessarItem(itens[total_processado])
    total_processado <- total_processado + 1
FIM_ENQUANTO
```

#### Laço Para (`PARA`)
```thz
PARA i DE 0 ATE 9 PASSO 1 FACA
    EXIBA "Processando etapa " + i
FIM_PARA
```

---

## 8. Tratamento Idiomático de Erros (`RESULTADO`)

O THZ-LANG proíbe o lançamento de exceções em tempo de execução (*exceptions*) na lógica de negócio. Em seu lugar, adota o tipo monádico `RESULTADO[T, E]`.

```thz
PROCEDIMENTO ValidarEstoque(id: UUID, qtd: INTEIRO) : RESULTADO[TEXTO, TEXTO]
INICIO
    SE ConsultarDisponibilidade(id) < qtd ENTAO
        FALHAR_COM("Estoque insuficiente para a transação")
    SENAO
        RETORNE "Estoque disponível"
    FIM_SE
FIM
```

### Instrução de Pattern Matching (`CASO_RESULTADO`)
Para consumir uma variável do tipo `RESULTADO`, utiliza-se o bloco `CASO_RESULTADO` com os manipuladores estruturados `SUCESSO` e `ERRO`.

```thz
VARIAVEL res_estoque <- ValidarEstoque(id_produto, 5)

CASO_RESULTADO res_estoque
    SUCESSO(mensagem) -> INICIO
        EXIBA "[PROCESSO] " + mensagem
        RealizarVenda()
    FIM
    ERRO(msg_erro) -> INICIO
        EXIBA "[ALERTA] Venda abortada: " + msg_erro
        RegistrarAnomalia()
    FIM
FIM_CASO
```

---

## 9. Engenharia de Dados: Arenas de Memória e SIMD

Para alta taxa de transferência e baixa latência de processamento numérico, o THZ-LANG introduz primitivas nativas de alocação de memória e vetorização no processador.

### 9.1 Alocação em Arena (`USAR_BLOCO_MEMORIA`)
Permite alocar memória contígua em bloco na memória Heap da aplicação. Ao fim do escopo, toda a memória contida no bloco é descartada em tempo constante $O(1)$ sem invocar coletores de lixo tradicionais.

```thz
USAR_BLOCO_MEMORIA BlocoTemporarioCalculo FACA
    VARIAVEL itens_carregados <- CarregarDadosBrutos()
    CalcularEstatistica(itens_carregados)
FIM_BLOCO_MEMORIA # Descarte O(1) de tudo alocado dentro da arena
```

### 9.2 Laço Vetorizado (`VETORIZAR_PARA` / `PASSO_SIMD`)
Instrui o gerador de código (TypeScript/LLVM) a realizar o desenrolamento do laço (*loop unrolling*) e a agrupar a aritmética de campos de estruturas `LAYOUT_COLUNAR` em registradores SIMD (ex.: carregando blocos de 8 itens decimais de uma só vez).

```thz
VETORIZAR_PARA item EM lote_itens PASSO_SIMD 8
    item.valor_total_liquido <- item.quantidade * item.preco_unitario
FIM_PARA
```

---

## 10. Pipelines de Big Data (`PIPELINE_DADOS`)

Arquétipo nativo de fluxo e tratamento de dados em lote ou em tempo real (Streaming). O analisador semântico valida a topologia do fluxo garantindo conexões seguras.

```thz
PIPELINE_DADOS IngestaoVendasRealTime

METADADOS_ARQUITETURA
    SISTEMA: "DataLakeIngest"
    DOMINIO: "EngenhariaDeDados"
    SLO_LATENCIA_MS: 2
FIM_METADADOS

FONTE_ENTRADA OrigemFaturamento
    TIPO: "STREAMING"
    CONECTOR: "POSTGRESQL"
    FORMATO: "JSONB"
FIM_FONTE

DESTINO_SAIDA DestinoAnalyticLake
    CONECTOR: "MONGODB"
    COLECAO: "historico_faturamento"
FIM_DESTINO

TRANSFORMACAO AgregarImpostos
    RASTREIO_REQUISITO: "REQ-DATA-8812"
    EXIGE: tamanho(lote) > 0

    VETORIZAR_PARA f EM lote PASSO_SIMD 8
        f.tributo <- f.valor_bruto * 0.0500
    FIM_VETORIZAR
FIM_TRANSFORMACAO

FIM_PIPELINE
```

---

## 11. DSL de Interface Gráfica e Tela Declarativa (`.thzui`)

O módulo `TELA` disponibiliza uma DSL para renderização declarativa e reativa de formulários a partir de estruturas de domínio e operações registradas em stdlib.

```thz
TELA CadastroProduto

METADADOS_ARQUITETURA
    DOMINIO: "Administracao"
    CAMADA: "Apresentacao"
FIM_METADADOS

PROCEDIMENTO MontarFormulario()
INICIO
    TELA.criarContainer("painel_principal", "CONTAINER")
    TELA.criarCard("card_dados", "Cadastrar Novo Produto")
    TELA.adicionarMetrica("vendas_dia", "Total Vendas Hoje", "R$ 0,00")
    TELA.adicionarBotao("btn_salvar", "Salvar Registro", "CalculoTributarioLote.ProcessarVetorizado")
    TELA.exibir("CadastroProduto")
FIM

FIM_TELA
```

### Renderizadores:
1. **Swing & FlatLaf (Desktop):** O módulo [`thz-gui-jvm`](file:///c:/Users/lucas/Projetos/thz-lang/JVM/thz-gui-jvm/) intercepta as chamadas `TELA.*` e constrói uma UI Desktop de alta fidelidade com tema Glassmorphism adaptado.
2. **HTML5 / WebView (Nuvem/Local):** O núcleo gera representações web semânticas compatíveis com Monaco Editor e pontes bidirecionais de eventos.

---

## 12. Referência Exaustiva da Biblioteca Padrão (Stdlib API)

Abaixo constam todas as APIs nativas expostas pela classe core [`BibliotecaPadrao`](file:///c:/Users/lucas/Projetos/thz-lang/JVM/thz-core-jvm/src/main/java/thz/lang/interpretador/BibliotecaPadrao.java) e módulos de execução (`thz-gui`, `thz-cli`).

### 12.1 Namespace `TEXTO`
Funções utilitárias de manipulação de strings Unicode.

* **`TEXTO.comprimento(t: TEXTO): INTEIRO`**
  Retorna o número total de caracteres de uma string.
* **`TEXTO.maiusculas(t: TEXTO): TEXTO`**
  Retorna a string convertida para caracteres maiúsculos.
* **`TEXTO.minusculas(t: TEXTO): TEXTO`**
  Retorna a string convertida para caracteres minúsculos.
* **`TEXTO.aparar(t: TEXTO): TEXTO`**
  Remove espaços em branco no início e fim da string.
* **`TEXTO.contem(t: TEXTO, busca: TEXTO): LOGICO`**
  Retorna `VERDADEIRO` se a string `busca` estiver contida no texto.
* **`TEXTO.subtexto(t: TEXTO, inicio: INTEIRO, [fim: INTEIRO]): TEXTO`**
  Retorna uma subcadeia de caracteres a partir do índice `inicio` (inclusivo) até `fim` (exclusivo). Se `fim` for omitido, extrai até o encerramento da string. Suporta índices negativos.
* **`TEXTO.substituir(t: TEXTO, alvo: TEXTO, substituicao: TEXTO): TEXTO`**
  Substitui todas as ocorrências textuais de `alvo` por `substituicao` na string base.
* **`TEXTO.dividir(t: TEXTO, delimitador: TEXTO): FATIA[TEXTO]`**
  Divide o texto nos locais do `delimitador` e retorna uma fatia contendo as partes.
* **`TEXTO.juntar(f: FATIA[TEXTO], delimitador: TEXTO): TEXTO`**
  Une as strings de uma fatia interpondo o `delimitador` correspondente.

---

### 12.2 Namespace `MATEMATICA`
Funções matemáticas e numéricas sobre `INTEIRO` e `DECIMAL`.

* **`MATEMATICA.abs(n: NUMERICO): NUMERICO`**
  Retorna o valor absoluto de um número.
* **`MATEMATICA.min(a: INTEIRO, b: INTEIRO): INTEIRO`**
  Retorna o menor valor de dois números inteiros.
* **`MATEMATICA.max(a: INTEIRO, b: INTEIRO): INTEIRO`**
  Retorna o maior valor de dois números inteiros.
* **`MATEMATICA.potencia(base: INTEIRO, expoente: INTEIRO): INTEIRO`**
  Retorna o valor numérico da base elevada ao expoente.
* **`MATEMATICA.raiz(n: INTEIRO): INTEIRO`**
  Retorna a raiz quadrada inteira (truncada) de um número positivo.
* **`MATEMATICA.arredondar(d: DECIMAL, casas: INTEIRO): DECIMAL`**
  Arredonda o decimal na escala informada utilizando arredondamento bancário meio-par (*Half-Even*).
* **`MATEMATICA.aleatorio(limite: INTEIRO): INTEIRO`**
  Retorna um número pseudo-aleatório no intervalo $[0, limite - 1]$.

---

### 12.3 Namespace `DATA`
Gerenciamento de tempo, calendários e fusos.

* **`DATA.hoje(): DATA`**
  Retorna o dia atual do calendário.
* **`DATA.agora(): DATA_HORA`**
  Retorna a data e hora corrente do sistema operacional.
* **`DATA.criar(ano: INTEIRO, mes: INTEIRO, dia: INTEIRO): DATA`**
  Instancia uma nova data a partir das frações.
* **`DATA.criarDataHora(ano: INTEIRO, mes: INTEIRO, dia: INTEIRO, hora: INTEIRO, minuto: INTEIRO, [segundo: INTEIRO]): DATA_HORA`**
  Instancia um novo carimbo data-hora.
* **`DATA.adicionarDias(d: DATA, dias: INTEIRO): DATA`**
  Retorna uma nova data somando a quantidade de dias.
* **`DATA.adicionarHoras(dh: DATA_HORA, horas: INTEIRO): DATA_HORA`**
  Retorna uma nova data/hora somando a quantidade de horas.
* **`DATA.diferencaDias(a: DATA, b: DATA): INTEIRO`**
  Calcula os dias de diferença entre duas datas.
* **`DATA.ano(d: DATA|DATA_HORA): INTEIRO`**
  Retorna o ano da data.
* **`DATA.mes(d: DATA|DATA_HORA): INTEIRO`**
  Retorna o mês (1-12) da data.
* **`DATA.dia(d: DATA|DATA_HORA): INTEIRO`**
  Retorna o dia do mês (1-31) da data.
* **`DATA.diaDaSemana(d: DATA): INTEIRO`**
  Retorna o dia da semana (1 = Segunda, 7 = Domingo).
* **`DATA.texto(d: DATA|DATA_HORA): TEXTO`**
  Retorna a representação formatada padrão ISO da data.

---

### 12.4 Namespace `TELA`
Componentes visuais e formulários de interação.

* **`TELA.renderizarFormulario(r: REGISTRO, operacao: TEXTO): TEXTO`**
  Renderiza o formulário visual associado a uma estrutura `ESTRUTURA`, mapeando o retorno final do submit para a regra/operação cadastrada.
* **`TELA.alerta(titulo: TEXTO, mensagem: TEXTO): TEXTO`**
  Apresenta um pop-up de alerta ou notificação.
* **`TELA.confirmar(titulo: TEXTO, mensagem: TEXTO): LOGICO`**
  Apresenta diálogo interativo de sim/não para confirmação.
* **`TELA.pedirTexto(titulo: TEXTO, prompt: TEXTO): TEXTO`**
  Apresenta uma caixa de solicitação de texto ao usuário.

---

### 12.5 Namespace `DOCUMENTO`
Exportação nativa de relatórios de dados corporativos (PDF/Planilhas/Word).

* **`DOCUMENTO.exportar(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO|FATIA): TEXTO`**
  Exporta o conjunto de dados em formato padrão estruturado.
* **`DOCUMENTO.exportarPdf(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO|FATIA): TEXTO`**
  Gera um documento PDF estilizado no local de destino.
* **`DOCUMENTO.exportarXlsx(caminho: TEXTO, nomeAba: TEXTO, dados: REGISTRO|FATIA): TEXTO`**
  Gera uma planilha de dados Microsoft Excel `.xlsx` contendo o mapeamento de propriedades.
* **`DOCUMENTO.exportarDocx(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO|FATIA): TEXTO`**
  Gera um documento Word `.docx` no caminho especificado.

---

### 12.6 Namespace `VERSAO` (SemVer 2.0.0)
* **`VERSAO.obter(): TEXTO`**
  Retorna a versão atual do motor THZ-LANG executando o script.
* **`VERSAO.satisfaz(versao: TEXTO, restricoes: TEXTO): LOGICO`**
  Retorna `VERDADEIRO` se a string de versão atende às condições passadas (ex.: `VERSAO.satisfaz("2.4.0", ">=2.3.0")`).

---

### 12.7 Namespace `ARQUIVO` & `DIRETORIO`
Gerenciamento de entrada/saída de arquivos nativos do sistema de arquivos.

* **`ARQUIVO.lerTexto(caminho: TEXTO): TEXTO`**
  Lê o conteúdo integral de um arquivo texto em codificação UTF-8.
* **`ARQUIVO.escreverTexto(caminho: TEXTO, conteudo: TEXTO): LOGICO`**
  Grava o conteúdo de texto no caminho (sobrescreve se o arquivo já existir).
* **`ARQUIVO.anexarTexto(caminho: TEXTO, conteudo: TEXTO): LOGICO`**
  Anexa a string no final do arquivo de texto.
* **`ARQUIVO.existe(caminho: TEXTO): LOGICO`**
  Retorna `VERDADEIRO` se o arquivo ou diretório existir no local.
* **`ARQUIVO.remover(caminho: TEXTO): LOGICO`**
  Exclui o arquivo físico informado.
* **`DIRETORIO.listar(caminho: TEXTO): FATIA[TEXTO]`**
  Retorna a listagem de arquivos e pastas no diretório informado.
* **`DIRETORIO.criar(caminho: TEXTO): LOGICO`**
  Cria o diretório correspondente no sistema operacional.

---

### 12.8 Namespace `CONFIG`
Configurações, variáveis de ambiente e arquivos `.env`.

* **`CONFIG.obter(chave: TEXTO, [padrao: TEXTO]): TEXTO`**
  Busca o valor da variável de ambiente correspondente à chave ou retorna o valor opcional `padrao`.
* **`CONFIG.carregarEnv([caminho: TEXTO]): LOGICO`**
  Processa e expõe as variáveis descritas no arquivo `.env` (procura no diretório atual se omitido).

---

### 12.9 Namespace `SEGURANCA`
APIs criptográficas para conformidade de dados e proteção de segredos.

* **`SEGURANCA.sha256(t: TEXTO): TEXTO`**
  Retorna o hash SHA-256 em representação hexadecimal.
* **`SEGURANCA.sha512(t: TEXTO): TEXTO`**
  Retorna o hash SHA-512 em representação hexadecimal.
* **`SEGURANCA.hmacSha256(t: TEXTO, chave: TEXTO): TEXTO`**
  Assina a string usando chave secreta com algoritmo HMAC-SHA256.
* **`SEGURANCA.criptografarAes(t: TEXTO, chave: TEXTO): TEXTO`**
  Criptografa o texto em formato seguro usando chave sob padrão AES-256-GCM.
* **`SEGURANCA.descriptografarAes(t: TEXTO, chave: TEXTO): TEXTO`**
  Descriptografa o texto AES cifrado usando a chave.
* **`SEGURANCA.hashSenha(senha: TEXTO): TEXTO`**
  Cria um hash seguro de senha utilizando PBKDF2 com sal.
* **`SEGURANCA.verificarSenha(senha: TEXTO, hash: TEXTO): LOGICO`**
  Valida se a senha corresponde ao hash gerado.
* **`SEGURANCA.gerarToken([tamanho: INTEIRO]): TEXTO`**
  Gera um token criptográfico seguro de string (padrão de 32 bytes).
* **`SEGURANCA.uuid(): TEXTO`**
  Gera um novo UUID v4 em conformidade com o RFC 4122.

---

### 12.10 Namespace `LOG`
Registro estruturado de informações e conformidade regulatória.

* **`LOG.info(msg: TEXTO): LOGICO`**
  Grava log nível INFO em formato estruturado.
* **`LOG.aviso(msg: TEXTO): LOGICO`**
  Grava log nível WARNING em formato estruturado.
* **`LOG.erro(msg: TEXTO): LOGICO`**
  Grava log nível ERROR em formato estruturado.
* **`LOG.auditoria(sujeito: TEXTO, acao: TEXTO, recurso: TEXTO): LOGICO`**
  Emite um log específico de auditoria de governança contendo os metadados de rastreio.

---

### 12.11 Namespace `BANCO`
* **`BANCO.conectar(url: TEXTO): LOGICO`**
  Estabelece uma pool de conexões com o banco de dados especificado na URL (PostgreSQL, MySQL, SQLite).

---

### 12.12 Namespace `WEBVIEW`
Controle de interfaces visuais dinâmicas usando motores locais Webview2/Chromium.

* **`WEBVIEW.iniciar(html: TEXTO): TEXTO`**
  Abre uma instância de Webview contendo a página e retorna o endereço local configurado.
* **`WEBVIEW.emitir(evento: TEXTO, dadosJson: TEXTO): LOGICO`**
  Envia dados no formato JSON à ponte JavaScript da página ativa.
* **`WEBVIEW.parar(): LOGICO`**
  Destrói e fecha a instância ativa da janela Webview.

---

### 12.13 Namespace `UI` (ThzUiMaker)
* **`UI.temaPadrao(): TEXTO`**
  Retorna o tema padrão ("THZ Dark Glass").
* **`UI.renderizarHtml(titulo: TEXTO, botaoAcao: TEXTO): TEXTO`**
  Renderiza o HTML5 básico correspondente à visualização da tela sob design de Glassmorphism.
* **`UI.gerarCodigo(nomeApp: TEXTO): TEXTO`**
  Gera a DSL estruturada `.thzui` de representação visual do componente.
