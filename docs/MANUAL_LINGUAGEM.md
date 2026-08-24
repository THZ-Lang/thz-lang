# Manual Oficial da Linguagem THZ-LANG (v2.4.0)

Bem-vindo ao **Manual Oficial do THZ-LANG**, a linguagem corporativa de sistemas projetada para unir **Governança de Negócio (DDD)**, **Design por Contrato**, **Arquitetura Viva**, **Processamento de Dados de Alta Performance (DoD / SIMD)** e **Big Data Streaming & Batch Pipelines**.

---

## 📚 Sumário
1. [Visão Geral e Filosofia](#1-visão-geral-e-filosofia)
2. [Linguagem Ubíqua e Glossário de Termos](#2-linguagem-ubíqua-e-glossário-de-termos)
3. [Tipos de Dados e Aritmética Exata](#3-tipos-de-dados-e-aritmética-exata)
4. [Arquétipos de Módulo](#4-arquétipos-de-módulo)
5. [Estruturas, Enums e Invariantes](#5-estruturas-enums-e-invariantes)
6. [Governança e Design por Contrato](#6-governança-e-design-por-contrato)
7. [Controle de Fluxo e Funções](#7-controle-de-fluxo-e-funções)
8. [Tratamento Idiomático de Resultados](#8-tratamento-idiomático-de-resultados)
9. [DSL de Interface Gráfica e Tela Declarativa (`.thzui`)](#9-dsl-de-interface-gráfica-e-tela-declarativa-thzui)
10. [Engenharia Orientada a Dados: Arenas e Vetorização SIMD](#10-engenharia-orientada-a-dados-arenas-e-vetorização-simd)
11. [Pipelines de Big Data: Ingestão Massiva (Streaming & Batch)](#11-pipelines-de-big-data-ingestão-massiva-streaming--batch)
12. [Biblioteca Padrão (Stdlib)](#12-biblioteca-padrão-stdlib)

---

## 1. Visão Geral e Filosofia

O **THZ-LANG** (`.thz`, `.thzui`) foi concebido para resolver o hiato entre as especificações de arquitetura corporativa e o código de produção de alto rendimento.

### Principais Pilares:
- **Expressividade em Português:** Palavras-chave claras que refletem o domínio do negócio sem ambiguidades.
- **Aritmética Financeira Rigorosa (ISO/IEC 10967 & ISO 4217):** Proibição total de ponto flutuante binário (`float`/`double`) para operações fiscais ou monetárias.
- **Design por Contrato Integrado:** As cláusulas `EXIGE`, `GARANTE` e `INVARIANTE` não são meros comentários, mas garantias executáveis.
- **Big Data Streaming & Batch Pipelines:** Arquitetura para ingestão e processamento em lote e tempo real em fontes heterogêneas (PostgreSQL, MySQL, MongoDB, JSONB, CSV, XLSX, LOG).
- **Vetorização SIMD Nativa:** Processamento colunar contíguo (*Structure of Arrays*) viabilizando operações vetorizadas via CPU (AVX2/AVX-512).
- **Compilação Nativa AOT:** Geração de binários de código de máquina nativo (.exe PE / .elf) sem sobrecarga de máquina virtual.

---

## 2. Linguagem Ubíqua e Glossário de Termos

No desenvolvimento orientado a domínio (DDD), a **Linguagem Ubíqua** é o conjunto de termos unificados que elimina a necessidade de tradução entre o analista de negócio e o desenvolvedor.

👉 Consulte o [**Glossário Oficial de Linguagem Ubíqua**](GLOSSARIO_LINGUAGEM_UBIQUA.md) para definições detalhadas de cada termo.

---

## 3. Tipos de Dados e Aritmética Exata

Em THZ-LANG, todos os tipos são estaticamente verificados pelo analisador semântico:

| Tipo | Descrição | Exemplo de Literal |
| :--- | :--- | :--- |
| `INTEIRO` | Inteiro de 64-bits assinado | `42`, `-100` |
| `DECIMAL(P, S)` | Decimal fixo de alta precisão (Precisão, Escala) | `150.50`, `0.0001` |
| `MONETARIO(Moeda)`| Valor monetário com tag ISO 4217 | `1450.00 BRL`, `99.99 USD` |
| `TEXTO` | Cadeia de caracteres Unicode | `"Faturamento 2026"` |
| `LOGICO` | Booleano (`VERDADEIRO` ou `FALSO`) | `VERDADEIRO`, `FALSO` |
| `UUID` | Identificador universal único de 128-bits | `"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"` |
| `FATIA[T]` | Vetor/Array tipado contíguo em memória | `[1, 2, 3]` |
| `RESULTADO[T, E]` | Canal de retorno tipado com SUCESSO ou ERRO | `RESULTADO("ok")` |

---

## 4. Arquétipos de Módulo

Todo arquivo THZ-LANG inicia com seu arquétipo correspondente e encerra com seu terminador pareado obrigatório:

```thz
PROGRAMA NEGOCIO FaturamentoVendas
    // Lógica corporativa principal
FIM_PROGRAMA

PIPELINE_DADOS IngestaoVendas
    // Pipeline de Big Data
FIM_PIPELINE

BIBLIOTECA UtilitariosCalculo
    // Funções reutilizáveis
FIM_BIBLIOTECA

TELA DashboardVendas
    // Interface gráfica declarativa (.thzui)
FIM_TELA
```

---

## 5. Estruturas, Enums e Invariantes

```thz
ENUMERACAO StatusPedido
    CRIADO,
    PROCESSANDO,
    APROVADO,
    CANCELADO
FIM_ENUMERACAO

ESTRUTURA Pedido LAYOUT_COLUNAR
    id: UUID
    cliente: TEXTO
    total: DECIMAL(12, 2)
    status: StatusPedido
    INVARIANTE total >= 0.00
FIM_ESTRUTURA
```

---

## 6. Governança e Design por Contrato

```thz
REGRA_NEGOCIO ValidarLimiteCredito
    RASTREIO_REQUISITO: "REQ-CRED-001"

    EXIGE: cliente.limite_credito > 0.00
    GARANTE: valor_compra <= cliente.limite_credito

    INICIO
        SE valor_compra > cliente.limite_credito ENTAO
            FALHAR_COM("Limite de crédito insuficiente")
        SENAO
            RETORNAR RESULTADO(VERDADEIRO)
        FIM_SE
    FIM
FIM_REGRA_NEGOCIO
```

---

## 7. Controle de Fluxo e Funções

```thz
PROCEDIMENTO CalcularBonus(salario: DECIMAL(10, 2), avaliacao: INTEIRO) : DECIMAL(10, 2)
INICIO
    SE avaliacao >= 9 ENTAO
        RETORNAR salario * 0.20
    SENAO SE avaliacao >= 7 ENTAO
        RETORNAR salario * 0.10
    SENAO
        RETORNAR 0.00
    FIM_SE
FIM
```

---

## 8. Tratamento Idiomático de Resultados

Em vez de exceções runtime descontroladas, utiliza-se `RESULTADO`, `FALHAR_COM` e `CASO_RESULTADO`:

```thz
PROCEDIMENTO ProcessarTransacao(id: INTEIRO)
INICIO
    VARIAVEL res <- ExecutarOperacao(id)

    CASO_RESULTADO res
        SUCESSO mensagem =>
            EXIBA("[OK] Transação aprovada: " + mensagem)
        ERRO erro_msg =>
            EXIBA("[ERRO] Transação rejeitada: " + erro_msg)
    FIM_CASO
FIM
```

---

## 9. DSL de Interface Gráfica e Tela Declarativa (`.thzui`)

```thz
TELA PainelFinanceiro

METADADOS_ARQUITETURA
    DOMINIO: "Financeiro"
    CAMADA: "Apresentacao"
FIM_METADADOS

PROCEDIMENTO MontarUI()
INICIO
    TELA.criarContainer("painel_central", "CONTAINER")
    TELA.criarCard("card_kpi", "Indicadores de Faturamento")
    TELA.adicionarMetrica("rec_hoje", "Receita Hoje", "R$ 450.000,00")
    TELA.adicionarBotao("btn_atualizar", "Atualizar Métricas", "RecarregarDados")
    TELA.exibir("PainelFinanceiro")
FIM

FIM_TELA
```

---

## 10. Engenharia Orientada a Dados: Arenas e Vetorização SIMD

```thz
USAR_BLOCO_MEMORIA "ARENA_EPHEMERAL", 1024 * 1024 FACA
    VETORIZAR_PARA i DE 0 ATE tamanho(itens) - 1 PASSO_SIMD 8
        itens.total[i] <- itens.quantidade[i] * itens.preco[i]
    FIM_VETORIZAR
FIM_BLOCO_MEMORIA
```

---

## 11. Pipelines de Big Data: Ingestão Massiva (Streaming & Batch)

```thz
PIPELINE_DADOS ProcessamentoTransacoesStreaming

METADADOS_ARQUITETURA
    SISTEMA: "DataPipelineCore"
    DOMINIO: "EngenhariaDeDados"
    SLO_LATENCIA_MS: 50
FIM_METADADOS

FONTE_ENTRADA OrigemTransacoes
    TIPO: "STREAMING"
    CONECTOR: "POSTGRESQL"
    FORMATO: "JSONB"
FIM_FONTE

DESTINO_SAIDA DestinoDataLake
    CONECTOR: "MONGODB"
    COLECAO: "faturamento_agregado"
FIM_DESTINO

TRANSFORMACAO ProcessarEFiltrar
    RASTREIO_REQUISITO: "REQ-DATA-001"
    EXIGE: tamanho(lote) > 0

    VETORIZAR_PARA i DE 0 ATE tamanho(lote) - 1 PASSO_SIMD 8
        lote.subtotal[i] <- lote.quantidade[i] * lote.preco_unitario[i]
    FIM_VETORIZAR
FIM_TRANSFORMACAO

FIM_PIPELINE
```

---

## 12. Biblioteca Padrão (Stdlib)

- **`Console`:** `EXIBA(msg)`, `LEIA_LINHA()`.
- **`Matematica`:** `ABS(v)`, `ARREDONDAR(v, casas)`, `MAX(a, b)`, `MIN(a, b)`, `POTENCIA(b, e)`.
- **`Texto`:** `TAMANHO(t)`, `SUBSTR(t, inicio, fim)`, `MAIUSCULA(t)`, `MINUSCULA(t)`, `DIVIDIR(t, sep)`.
- **`ThzIO` / `ThzConfig`:** Manipulação de arquivos e configurações JSON.
- **`ThzSecurity`:** Criptografia AES-256-GCM, PBKDF2 e hashes SHA-256.
- **`ThzLog`:** Emissão de logs estruturados em JSON.
- **`ThzHttpServer`:** Servidor Web REST com suporte a Virtual Threads.
- **`Documentos`:** Exportação nativa de relatórios em `PDF`, `XLSX` e `DOCX`.
