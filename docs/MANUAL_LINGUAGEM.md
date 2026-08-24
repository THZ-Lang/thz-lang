# Manual Oficial da Linguagem THZ-LANG (v2.4.0)

Bem-vindo ao **Manual Oficial do THZ-LANG**, a linguagem corporativa de sistemas projetada para unir **Governança de Negócio (DDD)**, **Design por Contrato**, **Arquitetura Viva** e **Processamento de Dados de Alta Performance (DoD / SIMD)**.

---

## 📚 Sumário
1. [Visão Geral e Filosofia](#1-visão-geral-e-filosofia)
2. [Linguagem Ubíqua e Glossário de Termos](#2-linguagem-ubíqua-e-glossário-de-termos)
3. [Tipos de Dados e Aritmética Exata](#3-tipos-de-dados-e-aritmética-exata)
4. [Arquétipos de Módulo](#4-arquétipos-de-módulo)
5. [Estruturas, Enums e Módulos](#5-estruturas-enums-e-módulos)
6. [Governança e Design por Contrato](#6-governança-e-design-por-contrato)
7. [Controle de Fluxo e Funções](#7-controle-de-fluxo-e-funções)
8. [Tratamento Idiomático de Resultados](#8-tratamento-idiomático-de-resultados)
9. [DSL de Interface Gráfica e Tela Declarativa (`.thzui`)](#9-dsl-de-interface-gráfica-e-tela-declarativa-thzui)
10. [Engenharia Orientada a Dados: Arenas e Vetorização SIMD](#10-engenharia-orientada-a-dados-arenas-e-vetorização-simd)
11. [Biblioteca Padrão (Stdlib)](#11-biblioteca-padrão-stdlib)

---

## 1. Visão Geral e Filosofia

O **THZ-LANG** (`.thz`, `.thzui`) foi concebido para resolver o hiato entre especificações de arquitetura de software corporativo e o código de produção de alto desempenho.

### Principais Pilares:
- **Expressividade em Português:** Palavras-chave claras que refletem o domínio do negócio sem ambiguidades.
- **Aritmética Financeira Rigorosa (ISO/IEC 10967 & ISO 4217):** Proibição total de ponto flutuante binário (`float`/`double`) para operações fiscais ou monetárias.
- **Design por Contrato Integrado:** As cláusulas `EXIGE`, `GARANTE` e `INVARIANTE` não são meros comentários, mas garantias executáveis.
- **Arquitetura Viva:** O bloco `METADADOS_ARQUITETURA` permite auditoria automática de SLOs, criticidade e governança.
- **Vetorização SIMD Nativa:** Processamento colunar contíguo (*Structure of Arrays*) viabilizando operações vetorizadas via CPU.

---

## 2. Linguagem Ubíqua e Glossário de Termos

No desenvolvimento orientado a domínio (DDD), a **Linguagem Ubíqua** é o conjunto de termos unificados que elimina a necessidade de "tradução" entre o que o especialista de negócio pede e o que o desenvolvedor codifica.

No THZ-LANG, essa linguagem é parte nativa do código compilável.

👉 Consulte o [**Glossário Oficial de Linguagem Ubíqua**](file:///c:/Users/lucas/Projetos/thz-lang/docs/GLOSSARIO_LINGUAGEM_UBIQUA.md) para a definição completa de termos como `REGRA_NEGOCIO`, `EXIGE`, `GARANTE`, `INVARIANTE`, `RASTREIO_REQUISITO`, `METADADOS_ARQUITETURA`, `DECIMAL`, `MONETARIO`, `LAYOUT_COLUNAR`, `PASSO_SIMD`, `RESULTADO` e `ThzUiMaker`.

---

## 3. Tipos de Dados e Aritmética Exata

Em THZ-LANG, todos os tipos são estaticamente verificados pelo compilador/analisador semântico.

| Tipo | Descrição | Exemplo de Literal |
| :--- | :--- | :--- |
| `INTEIRO` | Inteiro de 64-bits assinado | `42`, `-100` |
| `DECIMAL(P, S)` | Decimal fixo de alta precisão (Precisão, Escala) | `150.50`, `0.0001` |
| `MONETARIO(Moeda)`| Valor monetário com tag ISO 4217 | `1450.00 BRL`, `99.99 USD` |
| `TEXTO` | Cadeia de caracteres Unicode | `"Faturamento 2026"` |
| `LOGICO` | Booleano (`VERDADEIRO` ou `FALSO`) | `VERDADEIRO`, `FALSO` |

### Exemplo de Declaração e Aritmética:

```thz
VARIAVEL valor_item: DECIMAL(12, 2) <- 199.90
VARIAVEL quantidade: INTEIRO <- 5
VARIAVEL total: DECIMAL(12, 2) <- valor_item * quantidade
VARIAVEL saldo_carteira: MONETARIO(BRL) <- 1500.00 BRL
```

> ⚠️ **Aviso de Invariante de Domínio:** Tentativas de misturar moedas diferentes (ex: `100.00 BRL + 50.00 USD`) geram erros explícitos em tempo de compilação ou execução.

---

## 4. Arquétipos de Módulo

Todo programa em THZ-LANG pertence a um **Arquétipo de Módulo**, garantindo semântica clara e terminador pareado obrigatório.

```thz
PROGRAMA NEGOCIO ProcessamentoContas
    // Código do programa principal de negócio
FIM_PROGRAMA

BIBLIOTECA UtilitariosFinanceiros
    // Funções e procedimentos reutilizáveis
FIM_BIBLIOTECA

EXTENSAO IntegracaoBancaria
    // Módulo de extensão de sistema
FIM_EXTENSAO

FERRAMENTA AuditoriaCusto
    // Utilitário CLI ou script de ferramentas
FIM_FERRAMENTA

TESTE SuiteCalculoImpostos
    // Casos de testes automatizados
FIM_TESTE

TELA DashboardVendas
    // Interface gráfica declarativa (.thzui)
FIM_TELA
```

---

## 5. Estruturas, Enums e Módulos

### 5.1 Estruturas (`ESTRUTURA`)
Representam entidades de domínio com suporte opcional ao modificador `LAYOUT_COLUNAR` (Structure of Arrays).

```thz
ESTRUTURA Cliente
    id: INTEIRO
    nome: TEXTO
    documento: TEXTO
    ativo: LOGICO
FIM_ESTRUTURA
```

### 5.2 Enumerações (`ENUMERACAO`)
Conjuntos finitos de constantes nomeadas.

```thz
ENUMERACAO StatusPedido
    RASCUNHO,
    APROVADO,
    FATURADO,
    CANCELADO
FIM_ENUMERACAO
```

### 5.3 Módulos e Importação (`IMPORTAR`)
Reutilização de código entre arquivos:

```thz
IMPORTAR Cliente, StatusPedido DE "modelos/cliente.thz"
```

---

## 6. Governança e Design por Contrato

O THZ-LANG integra governança corporativa no coração do código fonte.

```thz
PROGRAMA NEGOCIO FaturamentoLote
VERSAO_LINGUAGEM "2.4"

METADADOS_ARQUITETURA
    SISTEMA: "FaturamentoCore"
    MODULO: "CalculoImpostos"
    DOMINIO: "Financeiro"
    SLO_LATENCIA_MS: 50
    CRITICIDADE: "ALTA"
FIM_METADADOS

REGRA_NEGOCIO ProcessarFatura
    RASTREIO_REQUISITO: "REQ-FIN-2026-001"
    EXIGE: valor_total > 0.00
    GARANTE: imposto_calculado >= 0.00

    INICIO
        VARIAVEL imposto_calculado: DECIMAL(12, 2) <- valor_total * 0.15
        RETORNAR imposto_calculado
    FIM
FIM_REGRA_NEGOCIO
```

---

## 7. Controle de Fluxo e Funções

### 7.1 Condicionais (`SE ... SENAO`)
```thz
SE valor > 1000.00 ENTAO
    EXIBA("Desconto de grande porte aplicado")
SENAO
    EXIBA("Valor padrão")
FIM_SE
```

### 7.2 Laços (`ENQUANTO` e `PARA`)
```thz
PARA i DE 0 ATE 10 PASSO 1 FACA
    EXIBA("Índice: " + i)
FIM_PARA
```

---

## 8. Tratamento Idiomático de Resultados

O THZ-LANG evita exceções descontroladas utilizando o padrão explícito `RESULTADO`.

### 8.1 Retornando Resultados
```thz
PROCEDIMENTO ValidarCredito(cliente_id: INTEIRO, valor: DECIMAL(12, 2))
INICIO
    SE valor > 50000.00 ENTAO
        FALHAR_COM("Limite de crédito excedido para a conta")
    SENAO
        RETORNAR RESULTADO(VERDADEIRO)
    FIM_SE
FIM
```

### 8.2 Pattern Matching com `CASO_RESULTADO`
```thz
VARIAVEL res <- ValidarCredito(101, 60000.00)

CASO_RESULTADO res
    SUCESSO valor_aprovado =>
        EXIBA("Crédito liberado com sucesso: " + valor_aprovado)
    ERRO mensagem_erro =>
        EXIBA("Solicitação negada: " + mensagem_erro)
FIM_CASO
```

---

## 9. DSL de Interface Gráfica e Tela Declarativa (`.thzui`)

Com o arquétipo `TELA`, você constrói interfaces visuais declarativas em arquivos `.thzui` ou `.thz`.

```thz
TELA PainelFaturamento

METADADOS_ARQUITETURA
    DOMINIO: "Financeiro"
    CAMADA: "Apresentacao"
FIM_METADADOS

PROCEDIMENTO MontarInterface()
INICIO
    TELA.criarContainer("raiz", "CONTAINER")
    TELA.criarCard("card_kpi", "Resumo Diário")
    TELA.adicionarMetrica("kpi_vendas", "Vendas Hoje", "R$ 450.000,00")
    TELA.adicionarBotao("btn_atualizar", "Atualizar Dados", "CarregarDados")
    TELA.exibir("PainelFaturamento")
FIM

FIM_TELA
```

Renderize e exporte UIs diretamente pelo CLI:
```bash
thz ui painel.thzui --html
```

---

## 10. Engenharia Orientada a Dados: Arenas e Vetorização SIMD

### 10.1 Blocos de Memória Contígua em Arena
Alocação em arena para descarte $O(1)$ em lote:

```thz
USAR_BLOCO_MEMORIA "ArenaFaturamento", 1024 * 1024 FACA
    // Operações em lote na memória contígua
FIM_BLOCO_MEMORIA
```

### 10.2 Vetorização SIMD (`LAYOUT_COLUNAR`)
```thz
ESTRUTURA ItemLote LAYOUT_COLUNAR
    quantidade: INTEIRO
    preco: DECIMAL(12, 2)
    subtotal: DECIMAL(12, 2)
FIM_ESTRUTURA

VETORIZAR_PARA i DE 0 ATE tamanho(itens) - 1 PASSO_SIMD 8
    itens.subtotal[i] <- itens.quantidade[i] * itens.preco[i]
FIM_VETORIZAR
```

---

## 11. Biblioteca Padrão (Stdlib)

Módulos utilitários embutidos acessíveis em runtime:

- **`Console`:** `EXIBA(msg)`, `LEIA_LINHA()`.
- **`Matematica`:** `ABS(v)`, `ARREDONDAR(v, casas)`, `MAX(a, b)`, `MIN(a, b)`.
- **`Texto`:** `TAMANHO(t)`, `SUBSTR(t, inicio, fim)`, `MAIUSCULA(t)`, `MINUSCULA(t)`.
- **`ThzIO` / `ThzConfig`:** Manipulação de arquivos e configurações JSON.
- **`ThzSecurity`:** Criptografia AES-256-GCM, PBKDF2 e hashes SHA-256.
- **`ThzLog`:** Emissão de logs estruturados em JSON.
- **`ThzHttpServer`:** Servidor Web REST com suporte a Virtual Threads.
