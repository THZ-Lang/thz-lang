# PIPELINE_DADOS — Guia Completo de Engenharia de Dados (Batch & Streaming)

> **Arquétipo nativo de Big Data.** `PIPELINE_DADOS` é o quarto arquétipo da linguagem (ao lado de `PROGRAMA`, `BIBLIOTECA`, `TELA`), com topologia validada pelo analisador semântico: `FONTE_ENTRADA → TRANSFORMACAO → DESTINO_SAIDA`, com `METADADOS_ARQUITETURA`, contratos `EXIGE/GARANTE`, `LAYOUT_COLUNAR` e `VETORIZAR_PARA PASSO_SIMD` nativos. Suporta lote e streaming, conectores heterogêneos e Virtual Threads.

Referências: [`GRAMATICA.md`](GRAMATICA.md) §4, [`MANUAL_LINGUAGEM.md`](MANUAL_LINGUAGEM.md) §10, `exemplos/pipeline_etl_telemetria.thz:1`, `JVM/thz-core-jvm/src/main/java/thz/lang/pipeline/ThzPipelineDataEngine.java:1`.

---

## 1. Sintaxe — EBNF e Terminadores

```ebnf
ArquetipoModulo   ::= "PROGRAMA" ("NEGOCIO"|"VISUAL"|"ARQUITETURA")? | "PIPELINE_DADOS" | "BIBLIOTECA" | "EXTENSAO" | "FERRAMENTA" | "TESTE" | "TELA"
TerminadorModulo  ::= "FIM_PROGRAMA" | "FIM_PIPELINE" | "FIM_BIBLIOTECA" | "FIM_EXTENSAO" | "FIM_FERRAMENTA" | "FIM_TESTE" | "FIM_TELA"

DeclaracaoPipelineBloco ::= FonteEntradaBloco | DestinoSaidaBloco | TransformacaoBloco
FonteEntradaBloco   ::= "FONTE_ENTRADA" IDENTIFICADOR PropriedadeItem* "FIM_FONTE"
DestinoSaidaBloco   ::= "DESTINO_SAIDA" IDENTIFICADOR PropriedadeItem* "FIM_DESTINO"
TransformacaoBloco  ::= "TRANSFORMACAO" IDENTIFICADOR ClausulaGovernanca* BlocoCodigo "FIM_TRANSFORMACAO"
PropriedadeItem     ::= IDENTIFICADOR ":" (STRING_LITERAL | NUMERO | IDENTIFICADOR)
ClausulaGovernanca  ::= "RASTREIO_REQUISITO" ":" STRING_LITERAL | "EXIGE" ":" Expressao | "GARANTE" ":" Expressao | "INVARIANTE" ":" Expressao
BlocoCodigo         ::= "INICIO" Comando*   // VARIAVEL, SE, PARA, VETORIZAR_PARA, USAR_BLOCO_MEMORIA, RETORNE, etc. (GRAMATICA.md:88)
```

**Regras do analisador semântico:**
- `METADADOS_ARQUITETURA` é obrigatório em `--estrito` (SLO, `CONFORMIDADE`, `DOMINIO`).
- `TRANSFORMACAO` exige `RASTREIO_REQUISITO` em `--estrito`.
- `PropriedadeItem` valida `TIPO`, `CONECTOR`, `FORMATO`, `COLECAO`, etc. — valores livres mas auditados via `thz audit`.
- Um `PIPELINE_DADOS` pode ter **N fontes**, **M destinos** e **K transformações** — topologia em DAG, sem ciclos.

---

## 2. Anatomia Mínima — O Pipeline mais simples que compila

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

```bash
./gradlew :thz-cli-jvm:run --args="check exemplos/pipeline_etl_telemetria.thz --estrito" # valida topologia
./gradlew :thz-cli-jvm:run --args="ir exemplos/pipeline_etl_telemetria.thz --saida /tmp/pipe.json" # THZ-IR com loopsSimd
```

---

## 3. Exemplo Real — Telemetria com SoA + SIMD (`exemplos/pipeline_etl_telemetria.thz:1`)

```thz
PIPELINE_DADOS TelemetriaTransacional

METADADOS_ARQUITETURA
    DOMINIO: "EngenhariaDeDados"
    SUBDOMINIO: "TelemetriaEStreaming"
    CAMADA: "Infraestrutura"
    VERSAO: "2.4.0"
    SLO_LATENCIA_MAXIMA: "5ms"
    CONFORMIDADE: "ISO-27001", "LGPD-Art7", "BACEN-Res4893"
FIM_METADADOS

ESTRUTURA EventoTelemetria LAYOUT_COLUNAR
    id_evento     : UUID
    sensor_id     : TEXTO
    latencia_ms   : INTEIRO32
    taxa_erros_pct: DECIMAL(5, 2)
    severidade    : NivelSeveridade
FIM_ESTRUTURA

ENUMERACAO NivelSeveridade
    NORMAL,
    ATENCAO,
    CRITICO
FIM_ENUMERACAO

REGRA_NEGOCIO ProcessamentoTelemetriaStreaming
    IDENTIFICADOR_REGRA: "BR-DATA-PIPELINE-001"
    RASTREIO_REQUISITO: "REQ-STREAM-4412"

    OPERACAO AgregarMetricas(eventos: FATIA[EventoTelemetria]): SumarioPipeline
    INICIO
        VETORIZAR_PARA ev EM eventos PASSO_SIMD 8
            SE ev.taxa_erros_pct > 5.00 ENTAO
                ev.severidade <- CRITICO
            FIM_SE
        FIM_PARA
        RETORNE CRIAR SumarioPipeline(total: tamanho(eventos))
    FIM
FIM_REGRA_NEGOCIO

FIM_PIPELINE
```

**Padrões em uso:** `LAYOUT_COLUNAR` (SoA para 8-wide), `VETORIZAR_PARA PASSO_SIMD 8` (R1-R5 de `ValidadorSimd.java:65`), `EXIGE`/`GARANTE` em `TRANSFORMACAO`, `METADADOS_ARQUITETURA` com SLO 5ms e conformidade tripla.

---

## 4. Blocos em Detalhe

### 4.1 `FONTE_ENTRADA` — De onde vêm os dados

```thz
FONTE_ENTRADA OrigemFaturamento
    TIPO: "STREAMING"        // ou "LOTE" (batch)
    CONECTOR: "POSTGRESQL"   // POSTGRESQL | MYSQL | MONGODB | CSV | JSONB | XLSX | LOG | KAFKA* | SPARK* | DELTA*
    FORMATO: "JSONB"         // JSONB | CSV | TEXTO | BINARIO | PARQUET*
    URI: "postgres://..."    // opcional — validado pelo engine
    TABELA: "faturamento"    // opcional
    CONSULTA: "SELECT ..."   // opcional
FIM_FONTE
```

| Propriedade | Valores comuns | Engine (`ThzPipelineDataEngine.java:18`) |
| :--- | :--- | :--- |
| `TIPO` | `LOTE`, `STREAMING` | `ModoExecucao {STREAMING, LOTE}` (`ThzPipelineDataEngine.java:13`) |
| `CONECTOR` | `POSTGRESQL`, `MYSQL`, `MONGODB`, `CSV`, `JSONB`, `XLSX` | `FonteConfig(conector, modo, uriOuCaminho, formato, opcoes)` |
| `FORMATO` | `JSONB`, `CSV`, `TEXTO`, `BINARIO` | `formato` em `FonteConfig` |
| `URI`/`TABELA`/`COLECAO`/`CONSULTA` | livre | `opcoes: Map<String,String>` |

`*` Roadmap Fase 7 (`TODO.md:29`): `KAFKA`, `SPARK`, `DELTA LAKE`, `PARQUET`, `ARROW IPC`.

### 4.2 `DESTINO_SAIDA` — Para onde vão

```thz
DESTINO_SAIDA DestinoAnalyticLake
    CONECTOR: "MONGODB"
    COLECAO: "historico_faturamento"
    MODO: "APPEND"           // APPEND | OVERWRITE | UPSERT
FIM_DESTINO
```

Mapeia para `DestinoConfig(conector, alvo, opcoes)` (`ThzPipelineDataEngine.java:18`).

### 4.3 `TRANSFORMACAO` — O que acontece no meio

```thz
TRANSFORMACAO AgregarImpostos
    RASTREIO_REQUISITO: "REQ-DATA-8812"
    EXIGE: tamanho(lote) > 0
    GARANTE: tamanho(lote) > 0

    USAR_BLOCO_MEMORIA BlocoTemp FACA
        VETORIZAR_PARA f EM lote PASSO_SIMD 8
            f.tributo <- f.valor_bruto * 0.0500
        FIM_VETORIZAR
    FIM_BLOCO_MEMORIA
FIM_TRANSFORMACAO
```

- **Contratos** são cidadãos de primeira classe — `EXIGE`/`GARANTE` validados antes/depois da transformação; `thz audit` rastreia `RASTREIO_REQUISITO` até o requisito funcional.
- **Corpo** é `BlocoCodigo` (`INICIO` + comandos `VARIAVEL`, `SE`, `PARA`, `VETORIZAR_PARA`, `ENQUANTO`, `USAR_BLOCO_MEMORIA`, `EXIBA`, `RETORNE`, `FALHAR_COM`, `CASO_RESULTADO`).
- **Performance** — `VETORIZAR_PARA PASSO_SIMD 8` + `LAYOUT_COLUNAR` + `USAR_BLOCO_MEMORIA` são a tríade de throughput (ver `GUIA_PERFORMANCE.md`).

### 4.4 `REGRA_NEGOCIO` + `OPERACAO` dentro de Pipeline

Pipelines podem ter `REGRA_NEGOCIO` com `OPERACAO` (ex.: `telemetria.thz:30` `AgregarMetricas`) — útil para agregar métricas, validar invariantes e expor via `thz audit`/`thz doc`. Semântica idêntica a `PROGRAMA NEGOCIO`.

---

## 5. Modos de Execução — Lote vs Streaming

### 5.1 JVM Engine (`ThzPipelineDataEngine.java:1-68`)

```java
// ThzPipelineDataEngine.java:13
enum ModoExecucao { STREAMING, LOTE }
record FonteConfig(conector, modo, uriOuCaminho, formato, opcoes)
record DestinoConfig(conector, alvo, opcoes)

// ThzPipelineDataEngine.java:25 — lote síncrono
List<RegistroDado> executarLote(FonteConfig, DestinoConfig, List<RegistroDado> entrada)

// ThzPipelineDataEngine.java:48 — streaming com Virtual Threads
void simularStreaming(FonteConfig, DestinoConfig, int eventos) {
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    CompletableFuture.allOf(futures).join();
}
```

- **Lote**: carrega tudo (`ARQUIVO.lerTexto`/`BANCO.conectar`), transforma em `VETORIZAR_PARA`, escreve em destino — ideal para ETL noturno, fechamento fiscal.
- **Streaming**: cada evento em Virtual Thread (JVM 25, `Executors.newVirtualThreadPerTaskExecutor()`), `CompletableFuture.allOf().join()` — ideal para telemetria, `SLO_LATENCIA_MAXIMA: "5ms"`.

### 5.2 Nativo (futuro)

`PIPELINE_DADOS` baixa para THZ-IR (`GeradorIr.java:92` `loopsSimd`) e LLVM IR (`GeradorIr.java:221`); o runtime nativo (`thz_runtime.c:87` `thz_ler_arquivo`) já faz I/O. Streaming nativo usará `epoll`/`IOCP` + arenas por batch.

---

## 6. Conectores — Estado Atual e Roadmap

| Conector | Fonte | Destino | Formato | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POSTGRESQL` | ✅ | ✅ | `JSONB`, `TEXTO` | Produção (`BANCO.conectar` + `ThzDb.java`) |
| `MYSQL` | ✅ | ✅ | `TEXTO` | Produção |
| `MONGODB` | ✅ | ✅ | `JSONB` | Produção |
| `CSV` | ✅ | ✅ | `CSV` | Produção (`ARQUIVO.lerTexto` + `TEXTO.dividir`) |
| `XLSX`/`PDF`/`DOCX` | ✅ | ✅ | `XLSX` | Produção (`DOCUMENTO.exportar*` via POI/OpenPDF) |
| `JSONB` | ✅ | ✅ | `JSONB` | Produção |
| `LOG` | ✅ | ✅ | `TEXTO` | Produção (`LOG.*`) |
| `KAFKA` | — | — | — | **Roadmap Fase 7** (`TODO.md:32`) |
| `SPARK` | — | — | `PARQUET` | Roadmap |
| `DELTA LAKE` | — | — | `PARQUET` | Roadmap |
| `ARROW IPC / Flight` | — | — | `ARROW` | Roadmap Zero-Copy (`TODO.md:29`) |

**Exemplo CSV → MongoDB (lote):**

```thz
PIPELINE_DADOS CargaCsvParaLake

FONTE_ENTRADA CsvBruto
    TIPO: "LOTE"
    CONECTOR: "CSV"
    URI: "./data/faturamento.csv"
    FORMATO: "CSV"
FIM_FONTE

DESTINO_SAIDA LakeMongo
    CONECTOR: "MONGODB"
    COLECAO: "faturamento_raw"
FIM_DESTINO

TRANSFORMACAO NormalizarLinhas
    RASTREIO_REQUISITO: "REQ-ETL-101"
    EXIGE: tamanho(linhas) > 0
    VETORIZAR_PARA l EM linhas PASSO_SIMD 4
        l.valor_total_liquido <- l.quantidade * l.valor_unitario
    FIM_VETORIZAR
FIM_TRANSFORMACAO

FIM_PIPELINE
```

---

## 7. Governança e Auditoria

Cada `TRANSFORMACAO`/`REGRA_NEGOCIO` com `RASTREIO_REQUISITO` aparece em:

```bash
./gradlew :thz-cli-jvm:run --args="audit exemplos/pipeline_etl_telemetria.thz"
# → Markdown com matriz Requisito → Regra → EXIGE/GARANTE → SLO

./gradlew :thz-cli-jvm:run --args="audit exemplos/pipeline_etl_telemetria.thz --git"
# → audita só o diff do Git (ThzGitAuditEngine.java)

./gradlew :thz-cli-jvm:run --args="audit exemplos/pipeline_etl_telemetria.thz --json --saida /tmp/audit.json"
# → JSON para CI (CONFORMIDADE_E_NORMAS.md:76)
```

`METADADOS_ARQUITETURA` com `SLO_LATENCIA_MAXIMA` + `CONFORMIDADE` alimenta `thz doc` (C4) e `RELATORIO-EVOLUCAO.md`.

---

## 8. Como Reproduzir

```bash
# Validar
./gradlew :thz-cli-jvm:run --args="check exemplos/pipeline_etl_telemetria.thz"
./gradlew :thz-cli-jvm:run --args="check exemplos/pipeline_etl_telemetria.thz --estrito"

# Ver IR (THZ-IR + loopsSimd)
./gradlew :thz-cli-jvm:run --args="ir exemplos/pipeline_etl_telemetria.thz --saida /tmp/pipe.json"
cat /tmp/pipe.json | grep -A2 loopsSimd

# LLVM
./gradlew :thz-cli-jvm:run --args="ir exemplos/pipeline_etl_telemetria.thz --llvm --saida /tmp/pipe.ll"

# Executar (JVM, lote)
./gradlew :thz-cli-jvm:run --args="run exemplos/pipeline_etl_telemetria.thz"

# Nativo (quando TRANSFORMACAO for totalmente nativa)
powershell -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz exemplos/pipeline_etl_telemetria.thz -Alvo windows
```

---

## 9. Roadmap Fase 7 — O que vem

- **Arrow IPC / Flight / Plasma** (`TODO.md:29`): `FATIA[T]` Zero-Copy entre THZ, Python, Spark — sem serialização JSON/CSV.
- **AVX-512 / ARM Neon** (`TODO.md:31`): `PASSO_SIMD 16/32/64` com `ValidadorSimd.java:72` (potência de 2 já valida).
- **Kafka / Spark / Delta Lake** (`TODO.md:32`): `CONECTOR: "KAFKA"` com `TIPO: "STREAMING"` real (hoje `simularStreaming` é Virtual Threads).

---

> **Próximo:** [`TELA_THZUI.md`](TELA_THZUI.md) (o outro arquétipo visual), [`GUIA_PERFORMANCE.md`](GUIA_PERFORMANCE.md) (como extrair throughput de `PIPELINE_DADOS`).

