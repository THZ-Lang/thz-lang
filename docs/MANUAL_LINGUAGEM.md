# Manual Oficial da Linguagem THZ-LANG (v2.4.0)

Bem-vindo ao **Manual Oficial do THZ-LANG**, a linguagem corporativa de sistemas projetada para unir **Governança de Negócio (DDD)**, **Design por Contrato**, **Arquitetura Viva**, **Processamento de Dados de Alta Performance (DoD / SIMD)** e **Big Data Streaming & Batch Pipelines**.

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
11. [Pipelines de Big Data: Ingestão Massiva (Streaming & Batch)](#11-pipelines-de-big-data-ingestão-massiva-streaming--batch)
12. [Biblioteca Padrão (Stdlib)](#12-biblioteca-padrão-stdlib)

---

## 1. Visão Geral e Filosofia

O **THZ-LANG** (`.thz`, `.thzui`) foi concebido para resolver o hiato entre especificações de arquitetura de software corporativo e o código de produção de alto desempenho.

### Principais Pilares:
- **Expressividade em Português:** Palavras-chave claras que refletem o domínio do negócio sem ambiguidades.
- **Aritmética Financeira Rigorosa (ISO/IEC 10967 & ISO 4217):** Proibição total de ponto flutuante binário (`float`/`double`) para operações fiscais ou monetárias.
- **Design por Contrato Integrado:** As cláusulas `EXIGE`, `GARANTE` e `INVARIANTE` não são meros comentários, mas garantias executáveis.
- **Big Data Streaming & Batch Pipelines:** Arquitetura para ingestão e processamento em lote e tempo real em fontes heterogêneas (PostgreSQL, MySQL, MongoDB, JSONB, CSV, XLSX, LOG).
- **Vetorização SIMD Nativa:** Processamento colunar contíguo (*Structure of Arrays*) viabilizando operações vetorizadas via CPU.

---

## 2. Linguagem Ubíqua e Glossário de Termos

No desenvolvimento orientado a domínio (DDD), a **Linguagem Ubíqua** é o conjunto de termos unificados que elimina a necessidade de "tradução" entre o que o especialista de negócio pede e o que o desenvolvedor codifica.

👉 Consulte o [**Glossário Oficial de Linguagem Ubíqua**](file:///c:/Users/lucas/Projetos/thz-lang/docs/GLOSSARIO_LINGUAGEM_UBIQUA.md) para a definição de termos como `REGRA_NEGOCIO`, `EXIGE`, `GARANTE`, `PIPELINE_DADOS`, `FONTE_ENTRADA`, `DESTINO_SAIDA`, `STREAMING`, `LOTE` e `LAYOUT_COLUNAR`.

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

---

## 4. Arquétipos de Módulo

Todo programa em THZ-LANG pertence a um **Arquétipo de Módulo**, garantindo semântica clara e terminador pareado obrigatório.

```thz
PROGRAMA NEGOCIO ProcessamentoContas
    // Código do programa principal de negócio
FIM_PROGRAMA

PIPELINE_DADOS IngestaoVendas
    // Pipeline de Big Data (Streaming / Batch)
FIM_PIPELINE

TELA DashboardVendas
    // Interface gráfica declarativa (.thzui)
FIM_TELA
```

---

## 11. Pipelines de Big Data: Ingestão Massiva (Streaming & Batch)

O arquétipo `PIPELINE_DADOS` viabiliza a ingestão e transformação massiva de dados em lote (*Batch*) ou em tempo real (*Streaming*) a partir de fontes heterogêneas:

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

Módulos utilitários embutidos acessíveis em runtime:

- **`Console`:** `EXIBA(msg)`, `LEIA_LINHA()`.
- **`Matematica`:** `ABS(v)`, `ARREDONDAR(v, casas)`, `MAX(a, b)`, `MIN(a, b)`.
- **`Texto`:** `TAMANHO(t)`, `SUBSTR(t, inicio, fim)`, `MAIUSCULA(t)`, `MINUSCULA(t)`.
- **`ThzIO` / `ThzConfig`:** Manipulação de arquivos e configurações JSON.
- **`ThzSecurity`:** Criptografia AES-256-GCM, PBKDF2 e hashes SHA-256.
- **`ThzLog`:** Emissão de logs estruturados em JSON.
- **`ThzHttpServer`:** Servidor Web REST com suporte a Virtual Threads.
