# thz-bench-jvm — Benchmarks JMH

Benchmarks de performance do THZ-LANG usando JMH (Java Microbenchmark Harness).

## Benchmarks

| Benchmark | Descrição |
|-----------|-----------|
| `DecimalBench` | Operações de ponto fixo (somar, multiplicar, dividir) vs Number (baseline) |
| `BlocoMemoriaBench` | Alocação de memória contígua vs Object/ArrayList (GC) |
| `LayoutBench` | Structure-of-Arrays vs Array-of-Structures |

## Execução

```bash
cd JVM/thz-bench-jvm
./gradlew jmh          # todos os benchmarks
./gradlew jmh -Pbenchmark=DecimalBench  # apenas decimal
```

## Resultados

Resultados salvos em `build/results/jmh/results.json`.

## Compilação

```bash
./gradlew compileJmhJava
```
