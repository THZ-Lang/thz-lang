# Análise — Migração Java 25 → Kotlin no THZ-LANG

**Data:** 2026-08-31
**Baseline:** `main` @ `3.0.0` (`version.txt:3.0.0`) — `JVM/thz-core-jvm:147/18.8k loc`, total `317 .java / 33.9k loc`, `0 .kt` (exceto `*.kts` de build)
**Status:** Recomendação **NÃO fazer rewrite total agora** — piloto incremental opcional

---

## 1. Baseline Técnico

| Área | Arquivo-chave | Estado |
|---|---|---|
| Core JVM | `JVM/thz-core-jvm/src/main/java/thz/lang/lexico/TokenType.java:1`, `ThzParser.java:960`, `semantico/AnalisadorSemantico.java:813`, `runtime/DecimalFixo.java:290` | Java 25 com `sealed interface` + `record` (ComandoAst 13 permits, ValorThz 12 permits), `switch` pattern matching, `instanceof` pattern |
| Build | `JVM/thz-core-jvm/build.gradle.kts:1`, `thz-cli-jvm/build.gradle.kts`, `thz-gui-jvm/build.gradle.kts`, `settings.gradle.kts:7`, `gradle.properties:8` | `java { toolchain 25 }`, composite 7 includeBuild, `configuration-cache/parallel/vfs.watch`, `hardcoded java.installations.paths` |
| Rust | `src/runtime_rs/Cargo.toml` v3.0 `staticlib/cdylib`, `lib.rs`, `llm.rs` 12k | Único runtime oficial, `thz_runtime.c` ausente no `main` vs `scripts/build-llvm.ps1:50` |
| GraalVM | `thz-cli-jvm/.../native-image/reflect-config.json:11`, `thz-core:4`, `thz-gui:1000+` | `--no-fallback --initialize-at-build-time`, `FlatLaf 3.5.4` Swing |
| API/Bench | `thz-api-jvm/build.gradle.kts:Spring Boot 4.1.1 + Jackson`, `thz-bench-jvm/build.gradle.kts:24 jmhAnnotationProcessor 1.37` | Único AP do repo |
| Testes | `73 *Test.java` (core 49) | `DecimalMonetarioTest`, `BlocoMemoriaTest`, `GovernancaTest` — sem teste dedicado `ThzLexer` |
| Docs | `docs/GRAMATICA.md:1` EBNF v2.4, `TODO.md:9-23` G1-G6 concluídos, `plans/plano-sintaxe-moderna-31082026-0953.md:1` | EBNF defasada vs parser real |

Não usa Virtual Threads / Vector API / Panama (`grep jdk.incubator.vector / java.lang.foreign =0`).

---

## 2. O que Kotlin traria

- `data class` vs `record` (copy/destructuring) — ganho marginal, `record` já cobre AST e `DecimalFixo`.
- Null-safety (`String?`) — útil em parser/semântico, mas validação semântica já é estrita.
- `when` exhaustive + extension functions — ajudaria dispatch `ValorThz`/`ExprAst`, hoje com `switch`.

Java 25 já entrega `sealed` + `record` + pattern matching; salto semântico é pequeno.

---

## 3. Custos e Riscos de Migração Total (240 arq / 28k loc main)

- **God-classes:** `ThzParser 982`, `AnalisadorSemantico 859`, `InterpretadorThz 784` — conversão auto IntelliJ ~70%, resto manual.
- **GraalVM closed-world:** `kotlin-stdlib` + `kotlin-reflect` exigem regenerar `reflect-config.json`/`resource-config.json` com `native-image-agent`; `thz-gui-jvm` (1000+ entradas) é alto risco com `--no-fallback`.
- **Spring Boot 4.1:** exige `kotlin("plugin.spring")` (open classes), `jackson-module-kotlin`, ajustes `jakarta.validation` nullable.
- **JMH:** troca `jmhAnnotationProcessor` por `kapt`/`ksp`.
- **Toolchain:** Kotlin 2.1 `jvmTarget` oficial até 21/23; **target 25 experimental** — conflita com `JavaLanguageVersion.of(25)` ou força downgrade.

Estimativa grosseira: 3–6 sprints para 240 arquivos com revisão completa; `thz-bench` (128 loc) e `thz-api` (396 loc) são baixo risco, `thz-core` (18.8k) e `thz-gui` (4.5k) alto risco.

---

## 4. Recomendação

**Não fazer rewrite total agora.** ROI sintático não compensa revalidar GraalVM + Spring + JMH em momento de fix AOT (`scripts/build-llvm.ps1:50` quebrado), EBNF desatualizada e sintaxe moderna THZ (`FUNCAO` vs `REGRA_NEGOCIO`).

### Se quiser Kotlin, faça incremental opt-in

1. **Piloto isolado** `thz-bench-jvm` → `thz-api-jvm` — valida `kotlin.jvm:jvmTarget`, `shadow`, `graalvm`.
2. **Novos módulos** (`thz-agent-jvm` expansões, tooling) em Kotlin interoperando com Java.
3. **Testes/DSL** em Kotlin (`kotest`) mantendo produção Java.
4. Só então avaliar `thz-core-jvm` em fases (AST → semântico → interpretador), quando Kotlin suportar target 25 estável.

### Alternativa de maior ROI imediato

Aprofundar Java 25 idiomático, ativar Virtual Threads onde faz sentido (`ThzBarramentoEventos.java`), corrigir AOT/EBNF/version skew (`Extensions/thz-lsp-vscode/package.json:0.3.0` vs `3.0.0`), e executar `plans/plano-sintaxe-moderna-31082026-0953.md` Fase 0 (baseline AST + ADR-006).

---

## 5. Referências

- `JVM/thz-core-jvm/src/main/java/thz/lang/lexico/TokenType.java`
- `JVM/thz-core-jvm/src/main/java/thz/lang/sintatico/ThzParser.java:247,459,951`
- `JVM/thz-core-jvm/src/main/java/thz/lang/semantico/AnalisadorSemantico.java:280,496`
- `JVM/thz-core-jvm/src/main/java/thz/lang/runtime/DecimalFixo.java`
- `JVM/thz-cli-jvm/build.gradle.kts` (GraalVM native-image)
- `JVM/thz-gui-jvm/build.gradle.kts` (FlatLaf)
- `JVM/thz-api-jvm/build.gradle.kts` (Spring Boot 4.1.1)
- `JVM/thz-bench-jvm/build.gradle.kts:24`
- `gradle.properties:8` (`java.installations.paths`)
- `scripts/build-llvm.ps1:50`
- `CHANGELOG.md:7` (3.0.0), `TODO.md:29` (backlog Fase 7)
