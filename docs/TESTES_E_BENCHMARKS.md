# Testes e Benchmarks — JUnit 5, Golden Snapshots, Paridade e JMH

> **Qualidade por construção.** Este guia cobre a estratégia de testes do THZ-LANG: JUnit 5 (112 testes, 100% PASSED), golden snapshots de AST, paridade TypeScript ↔ JVM, `CompiladorSelfHostTest`, e JMH (`DecimalBench`/`LayoutBench`/`BlocoMemoriaBench`), e como usar a skill `write-tests` para escrever testes novos.

Referências: `.agents/skills/write-tests/SKILL.md`, `JVM/*/src/test/java/thz/lang/**`, `.github/workflows/ci.yml:29`, `TODO.md:21`.

---

## 1. Pirâmide de Testes

```
E2E (1):  ci.yml native-aot-clang  (driver.elf executa)
Integração (7): CompiladorSelfHostTest (AUDITORIA/EXECUCAO_JVM/LLVM) + ThzPipelineDataEngineTest + ThzLspTest
Unidade (105+): Lexer, Parser, Semântico, IR, SIMD, Runtime, WebView, Gui, Api, CLI, Otimizador, Fmt, Governança, Decimal, BlocoMemoria
Benchmarks (3 famílias JMH): DecimalBench, LayoutBench, BlocoMemoriaBench (thz-bench-jvm)
Golden (N): snapshots AST JSON em src/test/resources + ParidadeTest (TS ↔ JVM)
```

**Onde vivem:**
- `JVM/thz-core-jvm/src/test/java/thz/lang/**` — núcleo (lexer, sintático, semântico, ir, simd, runtime, pipeline, webview, otimizador, driver, governança)
- `JVM/thz-cli-jvm/src/test/java/thz/lang/cli/ThzCliTest.java`
- `JVM/thz-gui-jvm/src/test/java/thz/lang/gui/**` — `ThzStudioIdeTest`, `FormularioGuiTest`, etc.
- `JVM/thz-lsp-jvm/src/test/java/thz/lang/lsp/ThzLspTest.java`
- `JVM/thz-api-jvm/src/test/java/thz/lang/api/ThzApiServiceTest.java`
- `JVM/thz-bench-jvm/src/jmh/java/thz/lang/bench/*.java` — JMH (não `src/test`)

---

## 2. JUnit 5 — Como rodar

```bash
./gradlew test                          # todos (thz-core, cli, gui, lsp, api) --parallel
./gradlew :thz-core-jvm:test            # só núcleo
./gradlew :thz-core-jvm:test --tests "thz.lang.driver.CompiladorSelfHostTest"
./gradlew :thz-core-jvm:test --tests "thz.lang.semantico.AnalisadorSemanticoEstritoTest"
./gradlew :thz-core-jvm:test --tests "thz.lang.ir.IrSimdTest"
xvfb-run ./gradlew test --no-daemon --parallel  # CI (ci.yml:30) precisa Xvfb para thz-gui
```

Config: `JVM/thz-*-jvm/build.gradle.kts:40` `test { useJUnitPlatform(); testLogging { events("passed","skipped","failed") } }`, `java.toolchain 25`, `org.junit.jupiter:junit-jupiter:5.11.3`.

**Saída esperada (`apresentacao_tecnica.md:128`):**

```
CompiladorSelfHostTest > testTokensSelfHost()  PASSED
CompiladorSelfHostTest > testDriverLlvmIr()    PASSED
...
BUILD SUCCESSFUL in 10s — 112 tests, 0 failures (100% PASSED)
```

---

## 3. Golden Snapshots de AST

**Padrão:** `ThzParser.parse()` → `ProgramaAst` → JSON canônico → compara com `src/test/resources/*.json` golden. Se o parser mudar, o teste quebra e o dev atualiza o golden com `--updateGoldens` (se o projeto expuser).

Exemplos:
- `JVM/thz-core-jvm/src/test/java/thz/lang/sintatico/ArquetiposModuloTest.java` — `PROGRAMA`/`PIPELINE_DADOS`/`TELA`/`BIBLIOTECA` com terminadores.
- `ParserErrorRecoveryTest.java` — `sincronizar()` em `FIM_PROGRAMA` etc.
- `ImportacaoModulosTest.java` — `IMPORTAR ... DE "..."`.

**Como escrever novo golden:**

```java
@Test void meuGolden() {
    var ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
    var json = AstJsonWriter.toJson(ast); // ou similar
    assertEquals(Files.readString(Path.of("src/test/resources/meu.json")), json);
}
```

---

## 4. Paridade TypeScript ↔ JVM (`ParidadeTest.java`)

`JVM/thz-core-jvm/src/test/java/thz/lang/ParidadeTest.java` compara saída do motor TS (`src/ir.ts`, `src/simd.ts`) com JVM (`GeradorIr`, `ValidadorSimd`) para `thz-ir/1` e R1-R5 — garante que `playground/` e `thz-core` divergem zero.

```bash
./gradlew :thz-core-jvm:test --tests "thz.lang.ParidadeTest"
```

---

## 5. Self-Hosting — `CompiladorSelfHostTest.java:29-85`

7 testes que provam que `compilador/*.thz` é válido, executável e gera LLVM:

| Teste | Alvo | Args | Assert |
| :--- | :--- | :--- | :--- |
| `testTokensSelfHost` | `AUDITORIA` | `tokens.thz` | `sucesso==true` |
| `testAstSelfHost` | `AUDITORIA` | `ast.thz` | `sucesso==true` |
| `testLexerSelfHost` | `EXECUCAO_JVM` | `lexer.thz` + `tamanho_fonte=100` | `EstadoLexer.tokens=28` |
| `testParserSelfHost` | `EXECUCAO_JVM` | `parser.thz` + `total_caracteres=100` | `EstadoParser.nos=8` |
| `testCodegenSelfHost` | `EXECUCAO_JVM` | `codegen.thz` + `total_nos=8` | `EstadoCodegen.instr=16` |
| `testDriverSelfHost` | `EXECUCAO_JVM` | `driver.thz` + `tamanho=100` | `ResultadoCompilacao(32/12/16)` |
| `testDriverLlvmIr` | `LLVM` | `driver.thz` | `saida.contains("ModuleID")` |

Lê `compilador/*.thz` com fallback `../../compilador` e `JVM/thz-core-jvm/exemplos/compilador` (`CompiladorSelfHostTest.java:18`).

---

## 6. JMH — `thz-bench-jvm`

Três benches (`@BenchmarkMode(Throughput) @OutputTimeUnit(MICROSECONDS) @Warmup(3×1s) @Measurement(5×1s) @Fork(1)`):

| Bench | O que mede | Arquivo |
| :--- | :--- | :--- |
| `DecimalBench` | `DecimalFixo.somar/multiplicar/dividir` vs `double` | `JVM/thz-bench-jvm/src/jmh/java/thz/lang/bench/DecimalBench.java:26` |
| `LayoutBench` | `soaScan` (SoA) vs `aosScan` (AoS) `N=10_000` | `LayoutBench.java:34` |
| `BlocoMemoriaBench` | `alocarLiberar`/`multiplasAlocacoes(1000×64B)`/`alocacaoGrande(1MB)` vs `new Object()` | `BlocoMemoriaBench.java:17` |

```bash
./gradlew jmh
./gradlew :thz-bench-jvm:jmh --args=".*DecimalBench.*"
# Saída: Benchmark  Mode  Cnt  Score  Error  Units — Score maior = melhor (thrpt)
```

Ver `GUIA_PERFORMANCE.md:6` para quando `soaScan` deve vencer `aosScan` (1.5–3×) e `alocarLiberar` vs `new` (5–20×).

---

## 7. Skill `write-tests` — Como escrever testes novos

Carregue a skill antes de codar testes:

```
skill: write-tests  →  .agents/skills/write-tests/SKILL.md
```

Regras da skill:

- **TypeScript (Node):** `src/*.test.ts` com `node --test`, golden em `src/__snapshots__/`.
- **Java 25 JUnit 5:** `JVM/*/src/test/java/thz/lang/**`, `@DisplayName`, `useJUnitPlatform()`, sem `@author/@version`.
- **Paridade:** todo `src/simd.ts` ↔ `ValidadorSimd.java` precisa `ParidadeTest`.
- **Self-hosting:** todo `compilador/*.thz` novo precisa `CompiladorSelfHostTest` + `Alvo.AUDITORIA`.

**Template Java:**

```java
@DisplayName("MeuFeature — descrição")
class MeuFeatureTest {
    @Test void casoFeliz() {
        var driver = new ThzCompilerDriver();
        var res = driver.compilarOuExecutar(fonte, ThzCompilerDriver.Alvo.AUDITORIA, false, Map.of());
        assertTrue(res.sucesso());
        assertEquals(0, res.erros().size());
    }
}
```

**Template JMH:**

```java
@BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=1) @Measurement(iterations=5,time=1) @Fork(1) @State(Scope.Thread)
public class MeuBench {
    @Benchmark public void meuCaso(Blackhole bh){ bh.consume(meuCodigo()); }
}
```

---

## 8. CI Gates — O que quebra o PR

`ci.yml:29-99` — três jobs, todos devem passar:

1. `engine-jvm`: `test` + 8× `thz check` + `shadowJar`/`bootJar`
2. `vscode-extension`: `npm ci && npm run compile`
3. `native-aot-clang`: `clang -target ... -c` + `gcc -O3` + `./driver.elf`

Adicione localmente antes de push:

```bash
./gradlew test && ./gradlew jmh -PjmhCompare=/tmp/baseline.json  # sem regressão >5%
./gradlew :thz-cli-jvm:run --args="fmt . --check"                 # formatação
./gradlew :thz-cli-jvm:run --args="audit . --git --json --saida /tmp/audit.json"  # governança
```

---

> **Próximo:** [`GUIA_PERFORMANCE.md`](GUIA_PERFORMANCE.md) (como medir), [`DEPLOYMENT.md`](DEPLOYMENT.md) (como distribuir sem quebrar testes).

