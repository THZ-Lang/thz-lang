# IntelliJ IDEA — Setup THZ-LANG (JDK 25 + Gradle Composite + TextMate)

> **Setup em 5 minutos.** Este guia configura o monorepo `thz-lang` no IntelliJ IDEA (ou qualquer JetBrains IDE) com JDK 25, Gradle composite build (`JVM/*`), TextMate para `.thz`/`.thzui`, e run configurations para `thz-core`/`thz-cli`/`thz-gui`/`thz-lsp`/`thz-bench`/`thz-api`.

---

## 1. Pré-requisitos

| Ferramenta | Versão | Onde pegar |
| :--- | :--- | :--- |
| IntelliJ IDEA | 2024.3+ (Ultimate ou Community) | https://jetbrains.com/idea |
| JDK 25 | Oracle/OpenJDK 25 ou GraalVM JDK 25 | `scoop install openjdk25` (Win) ou https://jdk.java.net/25/ |
| Gradle | 8.x (wrapper incluso) | `./gradlew` — não instale global |
| Clang + MinGW | LLVM 22 + MinGW GCC | `scoop install llvm mingw` (só para `build-llvm.ps1`) |
| Node 20 | Para `Extensions/thz-lsp-vscode` | `scoop install nodejs` |

Verifique:

```powershell
java -version  # openjdk 25 2025-09-16
./gradlew --version  # Gradle 8.x, JVM 25
clang --version
```

`gradle.properties:36` aponta `org.gradle.java.installations.paths=C:/Users/lucas/scoop/apps/openjdk25/current` — ajuste para seu `JAVA_HOME` se diferente.

---

## 2. Abrir o Monorepo

**Não abra `thz-lang/` como projeto único.** O workspace usa **Gradle composite builds** (`includeBuild` em `settings.gradle.kts`):

```
thz-lang/                 ← abra ESTE como projeto (root com build.gradle.kts agregador)
├── build.gradle.kts      ← aggregate("test","cli","gui","jmh")
├── settings.gradle.kts   ← includeBuild("JVM/thz-core-jvm"), etc.
├── JVM/thz-core-jvm/     ← projeto Gradle autônomo (thz.lang:thz-core:2.3.3)
├── JVM/thz-cli-jvm/      ← idem (thz.lang:thz-cli)
├── JVM/thz-gui-jvm/
├── JVM/thz-lsp-jvm/
├── JVM/thz-bench-jvm/
└── JVM/thz-api-jvm/
```

1. `File → Open → selecione thz-lang/` → `Open as Project`.
2. IntelliJ detecta `gradle.properties:8` (`configuration-cache`, `parallel`, `vfs.watch`) e importa 6 builds.
3. Aguarde sync (1–2min na primeira vez).

Se `thz-core` não resolver, `File → Invalidate Caches → Invalidate and Restart`.

---

## 3. JDK 25 — Configurar Toolchain

1. `File → Project Structure → Project → SDK → Add → JDK → C:\Users\lucas\scoop\apps\openjdk25\current` (ou seu `JAVA_HOME`).
2. `Settings → Build, Execution, Deployment → Gradle → Gradle JVM → Project SDK (25)`.
3. `gradle.properties:32` `org.gradle.jvmargs=-Xmx2096m ... -XX:+UseParallelGC` já otimiza daemon.

Teste: `./gradlew :thz-core-jvm:compileJava` deve compilar sem erro.

---

## 4. TextMate — Realce `.thz`/`.thzui`

O IntelliJ suporta TextMate nativo via plugin **TextMate Bundles** (bundled, sem instalar):

1. `File → Settings → Editor → TextMate Bundles` (`Ctrl+Alt+S` → `TextMate Bundles`).
2. Clique `+` → selecione `Extensions/thz-lsp-vscode/` (o diretório, não um arquivo).
3. IntelliJ detecta:
   - `syntaxes/thz.tmLanguage.json` (`scopeName source.thz`, `*.thz`, `*.thzui`)
   - `language-configuration.json` (brackets `FIM_PROGRAMA`, comentários `#`/`//`)
4. `Apply → OK` → abra `exemplos/faturamento.thz` — keywords `PROGRAMA`, `VETORIZAR_PARA`, `LAYOUT_COLUNAR`, `DECIMAL(12,4)` devem colorir.

**Scopes suportados:** `keyword.control.thz` (`PROGRAMA`, `REGRA_NEGOCIO`, `PIPELINE_DADOS`, `EXIGE`), `entity.name.type.thz` (`DECIMAL`, `MONETARIO`, `FATIA`, `UUID`), `constant.numeric.thz`, `comment.line.number-sign.thz`, `string.quoted.double.thz` (`INTELLIJ_SETUP.md:35`).

**Tema:** FlatLaf não afeta editor; para Dark, `Settings → Appearance → Theme: Darcula`.

---

## 5. Run Configurations

Crie em `Run → Edit Configurations → + → Gradle`:

| Nome | Gradle project | Task | Args |
| :--- | :--- | :--- | :--- |
| `thz-core:test` | `thz-lang` | `:thz-core-jvm:test` | — |
| `thz-cli:run check` | `thz-lang` | `:thz-cli-jvm:run` | `--args="check exemplos/faturamento.thz --estrito"` |
| `thz-cli:run` | `thz-lang` | `:thz-cli-jvm:run` | `--args="run exemplos/faturamento.thz"` |
| `thz-gui` | `thz-lang` | `:thz-gui-jvm:gui` | — |
| `thz-lsp` | `thz-lang` | `:thz-lsp-jvm:run` | — |
| `thz-bench:jmh` | `thz-lang` | `jmh` | — |

Ou use **Gradle tool window** (`View → Tool Windows → Gradle`) → `thz-lang → Tasks → verification → test`.

**CLI agregada (root):** `thz-lang: cli`, `gui`, `jmh`, `test` (definidas em `build.gradle.kts:19` `aggregate(...)`).

---

## 6. Debugging e Testes

- **Debugar `ThzCli`:** `Run → Debug thz-cli:run check` com breakpoint em `ThzLexer.java:16` ou `ThzParser.java:52`.
- **Testes:** clique no gutter verde ao lado de `CompiladorSelfHostTest.java:29` → `Run testTokensSelfHost`. Ou `Run → thz-core:test`.
- **Self-hosting:** debugue `ThzCompilerDriver.java:44` `compilarOuExecutar(..., Alvo.LLVM)` para ver `GeradorIr.emitirLlvm`.

---

## 7. Resolução de Problemas (Troubleshooting)

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `Unsupported class file major version 67` | Gradle com JDK <25 | `Gradle JVM → 25` (`Settings → Gradle`) |
| `Could not find thz.lang:thz-core:2.3.3` | Composite não importado | `File → Invalidate Caches` + reimport; verifique `settings.gradle.kts` tem `includeBuild("JVM/thz-core-jvm")` |
| `.thz` sem cor | TextMate não adicionado | Refaça §4; verifique `Extensions/thz-lsp-vscode/syntaxes/thz.tmLanguage.json` existe |
| `thz-gui` branco | Sem `FlatLaf` | `./gradlew :thz-gui-jvm:run` baixa `com.formdev:flatlaf:3.5.4` (`thz-gui-jvm/build.gradle.kts:34`) |
| Build lento | Sem parallel | `gradle.properties:19` `org.gradle.parallel=true` + `configuration-cache=true` |
| `clang not found` | Só para LLVM AOT | `scoop install llvm mingw` ou ignore — `build-llvm.ps1` é opcional |

---

## 8. VS Code vs IntelliJ

| | VS Code | IntelliJ |
| :--- | :--- | :--- |
| LSP | `Extensions/thz-lsp-vscode` (hover, completion, audit, IR) | TextMate só (sem LSP ainda) — use VS Code para LSP ou aguarde plugin JetBrains |
| Debug THZ | `F5` (`thz run ${file}`) | Debug Java (`ThzCompilerDriver`) |
| Formatar | `thz fmt` no save (LSP) | `thz fmt --escrever` manual (`scripts/fmt.ps1`) |

Para LSP no IntelliJ, use **LSP4IJ** plugin (experimental) apontando para `thz-lsp-jvm-shadow.jar`.

---

> **Próximo:** [`TESTES_E_BENCHMARKS.md`](TESTES_E_BENCHMARKS.md) (como escrever testes no IntelliJ), [`LSP_VSCODE.md`](LSP_VSCODE.md) (LSP real).

