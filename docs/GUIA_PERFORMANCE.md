# Guia de Performance — SoA, SIMD, Arenas e Tuning JMH

> **Throughput previsível.** Este guia ensina quando e como usar `LAYOUT_COLUNAR` (SoA), `VETORIZAR_PARA PASSO_SIMD` (SIMD) e `USAR_BLOCO_MEMORIA` (Arena O(1)) para extrair throughput de `PIPELINE_DADOS` e `PROGRAMA NEGOCIO`, como escolher `PASSO_SIMD` (2/4/8/16), dimensionar arenas, medir com JMH e evitar regressões.

Referências: [`ARQUITETURA_COMPILACAO_NATIVA.md`](ARQUITETURA_COMPILACAO_NATIVA.md) §8, `JVM/thz-bench-jvm/src/jmh/java/thz/lang/bench/*.java`, `JVM/thz-core-jvm/src/main/java/thz/lang/simd/ValidadorSimd.java:65`, `JVM/thz-core-jvm/src/main/java/thz/lang/runtime/BlocoMemoria.java:50`.

---

## 1. Mentalidade — Engenharia Orientada a Dados (DoD)

THZ-LANG é **DoD por sintaxe**: você declara layout e vetorização, o compilador garante. Três alavancas:

| Alavanca | Sintaxe | Efeito | Quando usar |
| :--- | :--- | :--- | :--- |
| **SoA** | `ESTRUTURA X LAYOUT_COLUNAR` | Campos viram vetores contíguos; cache line 100% útil | Sempre que iterar em lote (`tamanho(lote)>100`) |
| **SIMD** | `VETORIZAR_PARA x EM lote PASSO_SIMD 8` | 1 instrução opera em 8 itens (AVX2) / 16 (AVX-512) | Loop com aritmética homogênea, sem `LER`/`SE` divergente |
| **Arena** | `USAR_BLOCO_MEMORIA Bloco 1 FACA ... FIM_BLOCO_MEMORIA` | `alocar` = `offset+=bytes` (O1), `liberarTudo` = `offset=0` (O1) | Todo lote/streaming com alocações temporárias |

Sem as três, o mesmo algoritmo em AoS + `PARA` + `new` paga 3–20× mais em cache miss + GC + scalar.

---

## 2. SoA — `LAYOUT_COLUNAR` (Structure of Arrays)

### 2.1 AoS vs SoA

```thz
# AoS (padrão, sem modificador) — Array of Structures
ESTRUTURA ItemFatura
    quantidade  : NATURAL32
    preco       : DECIMAL(12, 4)
FIM_ESTRUTURA
# Memória: [{q:10,p:150.5}, {q:20,p:200.0}, ...] — stride = sizeof(Item)

# SoA (com modificador) — Structure of Arrays
ESTRUTURA ItemFatura LAYOUT_COLUNAR
    quantidade  : NATURAL32
    preco       : DECIMAL(12, 4)
FIM_ESTRUTURA
# Memória: { quantidade:[10,20,...], preco:[150.5,200.0,...] } — vetores contíguos
# THZ-IR: IrPrograma.IrEstrutura.layoutColunar=true (IrPrograma.java:18)
```

**Por que SoA é mais rápido:** `VETORIZAR_PARA` carrega `quantidade[0..7]` com **um** `vmovdqu` (256 bits = 8×i32) em vez de 8 `gather` com stride. `LayoutBench.java:13` mede `soaScan` vs `aosScan` em `N=10_000`: `soaScan` tem **1.5–3×** mais throughput (cache line 64B = 16×i32 contíguos).

### 2.2 Quando usar e quando não

| Use `LAYOUT_COLUNAR` | Não use (mantenha AoS) |
| :--- | :--- |
| `tamanho(lote) > 100` e itera em todos | `tamanho < 10` ou acesso randômico por `id` |
| `VETORIZAR_PARA` no pipeline | `PARA` scalar com `SE` divergente |
| Campos numéricos (`NATURAL32`, `DECIMAL`, `INTEIRO`) | Campos `TEXTO` variáveis + `UUID` (não vetoriza) |
| `PIPELINE_DADOS` batch/streaming | Entidade DDD com `INVARIANTE` por instância (ex.: `Cliente`) |

**Regra R1** (`ValidadorSimd.java:81`): se `VETORIZAR_PARA` em estrutura sem `LAYOUT_COLUNAR`, o validador emite **aviso** `R1: ... operará com carga Gather/Scatter` — funciona, mas mais lento. Corrija adicionando `LAYOUT_COLUNAR`.

---

## 3. SIMD — `VETORIZAR_PARA ... PASSO_SIMD`

### 3.1 Sintaxe e lowering

```thz
VETORIZAR_PARA item EM lote PASSO_SIMD 8
    item.valor_total_liquido <- item.quantidade * item.valor_unitario
    VARIAVEL imposto <- item.valor_total_liquido * 0.18
FIM_PARA
# THZ-IR: vector_loop item in lote step_simd 8 (GeradorIr.java:125)
# LLVM IR: loop com step 8 → SLP vectorizer → <8 x i32> mul
```

### 3.2 Regras R1–R5 (`ValidadorSimd.java:65`)

| Regra | Checagem | Falha |
| :--- | :--- | :--- |
| **R1** | Fonte é `LAYOUT_COLUNAR` | Aviso (gather) |
| **R2** | `PASSO_SIMD` é potência de 2 (2,4,8,16,32,64) | **Erro** `R2: Passo ... deve ser potência de 2` (`ValidadorSimd.java:73`) |
| **R3** | Ops homogêneas (sem divergência de tipo) | Atendida por padrão |
| **R4** | Sem dependência loop-carried (`a[i] <- a[i-1]`) | Aviso futuro |
| **R5** | Sem I/O impuro (`LER`, `EXIBA` com barreira) | **Erro** se `LER`, aviso se `EXIBA` (`ValidadorSimd.java:100`) |

`vetorizavel = violacoes.isEmpty()` (`ValidadorSimd.java:117`). `GeradorIr.java:92` só vetoriza se `vetorizavel==true`; `thz ir --saida` expõe `loopsSimd[].vetorizavel` + `violacoes`.

### 3.3 Como escolher `PASSO_SIMD`

| `PASSO_SIMD` | Vetor | Hardware | Quando |
| :--- | :--- | :--- | :--- |
| `2` | 64 bits | Qualquer x86_64 | Debug, lote pequeno, `DECIMAL` `i128` ainda não vetoriza bem |
| `4` | 128 bits | SSE2 | Lote médio, `NATURAL32`/`INTEIRO32` |
| `8` | 256 bits | **AVX2** (recomendado padrão) | **Padrão THZ** (`exemplos/faturamento.thz:45`, `pipeline_etl_telemetria.thz:30`) — 8× `i32` em 1 instrução |
| `16` | 512 bits | AVX-512 | Lote grande (>10k), `TODO.md:31` Fase 7 |
| `32`/`64` | 1024+/2048 bits | Futuro (2× AVX-512) | Reservado |

**Heurística:** comece com `8`; se `tamanho(lote) < 100`, use `4`; se `tamanho > 10_000` e `ValidadorSimd` aprovar, teste `16` e meça com JMH. `PASSO` deve dividir `tamanho` sem resto grande — resto vira scalar tail.

**Exemplo com tail:**

```thz
VETORIZAR_PARA i DE 0 ATE tamanho(lote)-1 PASSO_SIMD 8
    lote.subtotal[i] <- lote.quantidade[i] * lote.preco[i]
FIM_VETORIZAR
# lote=100 → 12 iterações vetorizadas (96 itens) + 4 scalar tail (backend insere)
```

---

## 4. Arenas — `USAR_BLOCO_MEMORIA`

### 4.1 API

```thz
USAR_BLOCO_MEMORIA BlocoTemp FACA
    VARIAVEL itens <- CarregarDadosBrutos()  # aloca dentro da arena
    VARIAVEL total <- CalcularEstatistica(itens)
FIM_BLOCO_MEMORIA  # offset=0, O(1) — sem GC
# GRAMATICA.md:115: BlocoMemoria ::= "USAR_BLOCO_MEMORIA" STRING_LITERAL ("," Expressao)? "FACA" Comando* "FIM_BLOCO_MEMORIA"
```

JVM: `BlocoMemoria.java:34` `new BlocoMemoria(tamanhoMb)` → `ByteBuffer.allocate(cap)`; `alocar(bytes)` (`BlocoMemoria.java:50`) = `offset+=bytes` + `if(>cap) throw "[Runtime THZ] Limite ..."`; `liberarTudo()` (`BlocoMemoria.java:69`) = `offset=0`.

Nativo: `thz_arena_alloc(bytes)` (`thz_runtime.c:39` `HeapAlloc`/`malloc`) + `thz_arena_free_all` (`thz_runtime.c:47`); futuro `thz_arena_push`.

LLVM IR: `call ptr @thz_arena_alloc(i64 1048576)` no `main` (`GeradorIr.java:395`) + `call void @thz_arena_free_all(ptr %arena)` (`GeradorIr.java:447`).

### 4.2 Dimensionamento

| Tamanho | Quando | Cálculo |
| :--- | :--- | :--- |
| `1` (1MB) | Lote até 10k `ItemFatura` SoA | `10_000 × (4 + 16) ≈ 200KB` + folga |
| `4` | Streaming 100k eventos/s | `100_000 × 32B ≈ 3.2MB` |
| `0` | Teste de bounds | `getPorcentagemUso()` (`BlocoMemoria.java:97`) deve dar 0% |

**Métrica:** `getUtilizacaoBytes()`/`getPorcentagemUso()` (`BlocoMemoria.java:76-97`). Se `>80%`, dobre `tamanhoMb`. Se `<10%`, reduza — arena grande demais polui cache.

**Erro comum:** `BlocoMemoria.java:56` `Limite do bloco excedido: solicitado %d, utilizado %d/%d` — aumente `tamanhoMb` ou divida lote em chunks `VETORIZAR_PARA` com `PASSO` menor.

### 4.3 Arena vs `new` — números JMH

`BlocoMemoriaBench.java:15-49`: `alocarLiberar`/`multiplasAlocacoes(1000×64B)` vs `javaObjectAllocation`/`javaArrayListAllocation`. `alocar` (bump) tem **5–20×** mais throughput que `new Object()` (TLAB + header + GC tracking). `liberarTudo` é `offset=0` vs `mark/sweep`.

---

## 5. DECIMAL — `i128` vs `double`

`DecimalBench.java:15-54`: `DecimalFixo` (`BigInteger` + `half-even`, `DecimalFixo.java:18`) vs `double`.

- `DecimalFixo.somar/multiplicar/dividir` normaliza escala (`escalaComum`, `DecimalFixo.java:107`), reescala produto exato (`DecimalFixo.java:166`), amplia numerador na divisão (`DecimalFixo.java:188`), arredonda `BANCARIO` (`DecimalFixo.java:118`).
- `double` é **10–50× mais rápido** (FPU), mas **errado**: `0.1+0.2=0.30000000000000004` (`CONFORMIDADE_E_NORMAS.md:21` ISO 10967 proíbe `float`/`double` para fiscal).
- THZ compensa com **vetorização `i128`** (futuro) e **arenas** — exatidão sem pagar GC.

**Regra:** `DECIMAL(P,S)` para dinheiro/imposto, `INTEIRO`/`NATURAL32` para contadores/índices (vetoriza melhor).

---

## 6. JMH — Como medir sem mentir

### 6.1 Rodar

```bash
./gradlew jmh                          # todos (DecimalBench, LayoutBench, BlocoMemoriaBench)
./gradlew :thz-bench-jvm:jmh --args=".*LayoutBench.*"
./gradlew :thz-bench-jvm:jmh --args=".*DecimalBench.*"
```

Config em cada bench (`*.java:9-13`): `@BenchmarkMode(Throughput) @OutputTimeUnit(MICROSECONDS) @Warmup(3×1s) @Measurement(5×1s) @Fork(1)`.

### 6.2 Ler saída

```
Benchmark                          Mode  Cnt   Score   Error  Units
DecimalBench.somar                 thrpt    5   1.234 ± 0.05  ops/us  # maior = melhor
LayoutBench.soaScan                thrpt    5   0.890 ± 0.03  ops/us  # soa > aos = SoA venceu
BlocoMemoriaBench.alocarLiberar    thrpt    5  12.345 ± 0.4   ops/us  # arena > new
```

- `Score ± Error` 99.9% CI; se `Error` sobrepõe, diferença não é significativa.
- `Blackhole.consume` (`Bench.java:28`) evita DCE — sem ele o JIT elimina o bench.
- Rode em máquina isolada, sem turbo variável.

### 6.3 Gate de regressão

```bash
./gradlew jmh -PjmhBaseline=/tmp/baseline.json  # salvar
./gradlew jmh -PjmhCompare=/tmp/baseline.json   # falhar se >5% regressão
```

Adicione ao `ci.yml` se `Score` cair >5% vs `main`.

---

## 7. Checklist de Tuning

- [ ] `ESTRUTURA` de lote tem `LAYOUT_COLUNAR`?
- [ ] `VETORIZAR_PARA` tem `PASSO_SIMD` potência de 2 (8 padrão)?
- [ ] `ValidadorSimd` sem `violacoes` (`thz ir --saida` → `loopsSimd[].vetorizavel==true`)?
- [ ] Sem `LER`/`EXIBA` dentro de `VETORIZAR_PARA` (R5)?
- [ ] `USAR_BLOCO_MEMORIA` com `tamanhoMb` dimensionado (`getPorcentagemUso` 30–70%)?
- [ ] `DECIMAL` só para dinheiro, `NATURAL32`/`INTEIRO32` para índices?
- [ ] JMH `soaScan > aosScan` e `alocarLiberar > new`?
- [ ] `SLO_LATENCIA_MAXIMA` em `METADADOS_ARQUITETURA` medido em produção = JMH?

---

> **Próximo:** [`TESTES_E_BENCHMARKS.md`](TESTES_E_BENCHMARKS.md) (como garantir que tuning não quebra correção), [`PIPELINE_DADOS.md`](PIPELINE_DADOS.md) (onde aplicar).

