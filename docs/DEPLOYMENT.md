# Deployment & Distribuição — jpackage, Docker, CI e Distribuíveis

> **Do `thz check` ao `dist/thz/thz.exe`.** Este guia cobre como empacotar, verificar e distribuir o THZ-LANG: `jpackage` (padrão), GraalVM Native Image (opcional), LLVM AOT legado, Docker `scratch`/`alpine`, artefatos `dist/`/`target/`, e CI com `audit --estrito` + `audit --git`.

Referências: [`CLI_E_TOOLING.md`](CLI_E_TOOLING.md) §5, [`ARQUITETURA_COMPILACAO_NATIVA.md`](ARQUITETURA_COMPILACAO_NATIVA.md) §6, `scripts/package-all.ps1:1`, `.github/workflows/ci.yml:1`.

---

## 1. Artefatos — O que cada build gera

| Build | Comando | Artefato | Tamanho típico | Requer JVM no cliente? |
| :--- | :--- | :--- | :--- | :--- |
| **UberJAR** | `./gradlew :thz-cli-jvm:shadowJar` / `scripts/build-jvm.ps1` | `target/thz-jvm-2.3.0.jar` + `JVM/*/build/libs/*.jar` | ~30MB | Sim (JDK 25) |
| **jpackage (padrão)** | `scripts/package-all.ps1` → `JVM/thz-cli-jvm/scripts/build-package.ps1` | `dist/thz/thz.exe` + `dist/thz/thz-gui.exe` + runtime JDK embutido | ~80MB | **Não** (JDK embutido) |
| **GraalVM Native** | `scripts/build-native.ps1` / `gradlew nativeCompile` | `dist/bin/thz.exe` | ~15MB | Não (`--no-fallback`) |
| **LLVM AOT** (legado) | `scripts/build-llvm.ps1` / `scripts/build-all.ps1` | `dist/bin/*.exe` (PE) + `*.elf` (ELF) | ~50KB + `thz_runtime.c` | Não |

`scripts/package-all.ps1:10-46` orquestra os três: `jpackage` sempre + `native-image` se `-WithNative` + `llvm` se `-WithLlvm`.

```bash
./gradlew build              # todos os JARs
./gradlew :thz-cli-jvm:shadowJar  # só CLI UberJAR → target/
./gradlew :thz-api-jvm:bootJar    # Spring Boot JAR
```

---

## 2. Padrão Recomendado — jpackage (`dist/thz/`)

`jpackage` (JDK 25) gera **app-image** com JDK mínimo embutido — sem `JAVA_HOME`, sem `msvcrt` externo, com ícone e atalho:

```powershell
# PowerShell (Windows)
.\scripts\package-all.ps1                    # jpackage padrão
.\scripts\package-all.ps1 -SkipTests        # sem testes
.\scripts\package-all.ps1 -WithNative       # + GraalVM dist/bin/thz.exe
.\scripts\package-all.ps1 -WithNative -WithLlvm  # + LLVM dist/bin/*.exe

# Saída
dist/thz/thz.exe        # CLI (jpackage)
dist/thz/thz-gui.exe    # IDE (jpackage)
# + runtime/  (JDK cortado via jlink)
# + app/thz-jvm-2.3.0.jar
```

Direto via Gradle (sem script):

```bash
./gradlew :thz-cli-jvm:jpackage  # se configurado em build-package.ps1
# ou
jpackage --type app-image --input JVM/thz-cli-jvm/build/libs \
  --main-jar thz-jvm-2.3.0.jar --main-class thz.lang.cli.ThzCli \
  --name thz --dest dist --java-options "-Xmx256m"
```

**Por que jpackage e não GraalVM por padrão?** `jpackage` funciona com Swing/FlatLaf sem `reflect-config.json`, sem `native-image-agent`, e distribui `thz_webview2.c` sem cross-compilação. GraalVM é 5× menor mas exige `guiColetarMetadadosAgente`.

---

## 3. Docker — Imagens mínimas

### 3.1 jpackage (recomendado)

```dockerfile
# Dockerfile.jpackage
FROM mcr.microsoft.com/windows/servercore:ltsc2022 AS base
COPY dist/thz /app/thz
ENTRYPOINT ["/app/thz/thz.exe", "run", "exemplos/faturamento.thz"]
```

```dockerfile
# Dockerfile.jpackage (Linux)
FROM eclipse-temurin:25-jre-alpine
COPY dist/thz /app/thz
ENTRYPOINT ["/app/thz/bin/thz", "run", "exemplos/faturamento.thz"]
```

### 3.2 LLVM AOT — `scratch` (Zero JVM)

```dockerfile
# Dockerfile.llvm — binário puro, sem JDK, sem shell
FROM scratch
COPY dist/bin/faturamento.exe /faturamento  # PE precisa wine; ELF é nativo Linux
# Para Linux:
COPY dist/bin/faturamento.elf /faturamento
ENTRYPOINT ["/faturamento"]
```

Construa `faturamento.elf` em WSL/CI (`ci.yml:95`):

```bash
mkdir -p dist/bin
./gradlew :thz-cli-jvm:run --args="ir exemplos/faturamento.thz --llvm --saida dist/bin/faturamento.ll"
clang -target x86_64-unknown-linux-gnu -c dist/bin/faturamento.ll -o dist/bin/faturamento.o
gcc -O3 dist/bin/faturamento.o src/runtime/thz_runtime.c -o dist/bin/faturamento.elf
```

### 3.3 GraalVM Native — `alpine`/`distroless`

```dockerfile
FROM gcr.io/distroless/base-debian12
COPY dist/bin/thz /thz
ENTRYPOINT ["/thz", "check", "exemplos/faturamento.thz"]
```

---

## 4. CI/CD — `.github/workflows/ci.yml:1-99`

Três jobs em `ubuntu-latest`:

| Job | Passos | Artefatos validados |
| :--- | :--- | :--- |
| `engine-jvm` | `setup-java 25` + `gradle setup` + `xvfb-run ./gradlew test --parallel` + `check` 8 arquivos (`faturamento.thz`, `pedidos.thz`, `compilador/*.thz` x6) + `shadowJar`/`bootJar` | JUnit 5 112 testes, 8 `thz check`, JARs |
| `vscode-extension` | `setup-node 20` + `npm ci && npm run compile` em `Extensions/thz-lsp-vscode` | `dist/extension.js` |
| `native-aot-clang` | `apt-get clang llvm gcc` + `thz ir driver.thz --llvm` + `clang -target ... -c` + `gcc -O3 ... -o driver.elf` + `./driver.elf` | `driver.elf` executa |

### 4.1 Gate `thz check --estrito` em PR

Adicione ao `engine-jvm` (após `check` simples):

```yaml
- name: Gate estrito (SLO + RASTREIO_REQUISITO obrigatórios)
  run: |
    ./gradlew :thz-cli-jvm:run --args="check exemplos/faturamento.thz --estrito"
    ./gradlew :thz-cli-jvm:run --args="check exemplos/pipeline_etl_telemetria.thz --estrito"
    ./gradlew :thz-cli-jvm:run --args="check exemplos/faturamento_dashboard.thzui --estrito"
```

### 4.2 Gate `thz audit --git` em PR

```yaml
- name: Audit diff do PR (só arquivos alterados)
  run: |
    ./gradlew :thz-cli-jvm:run --args="audit . --git --json --saida /tmp/audit.json"
    cat /tmp/audit.json | jq '.violacoes | length' # falha se >0
```

Local:

```bash
./gradlew :thz-cli-jvm:run --args="audit . --git"              # texto
./gradlew :thz-cli-jvm:run --args="audit . --git --json --saida audit.json"
thz audit exemplos/faturamento.thz --git  # via thz.exe nativo
```

`ThzGitAuditEngine.java` diff `git status`/`git diff HEAD` e só audita `*.thz` alterados — ideal para monorepo grande.

### 4.3 Gate `thz fmt --check`

```bash
./gradlew :thz-cli-jvm:run --args="fmt . --check"  # falha se não formatado
./gradlew :thz-cli-jvm:run --args="fmt . --escrever" # fixa
```

---

## 5. Scripts — Mapa `scripts/*.ps1` (14 arquivos)

| Script | Uso | Quando |
| :--- | :--- | :--- |
| `setup.ps1` | Instala JDK 25, Gradle, Clang, MinGW via `scoop` | Primeira vez |
| `build-jvm.ps1` | `./gradlew build [--parallel] [-x test]` + lista `JVM/*/build/libs/*.jar` | Dev |
| `test-all.ps1` | `./gradlew test --parallel` | CI local |
| `fmt.ps1` | `thz fmt --escrever` | Pre-commit |
| `dev.ps1` | `thz dev <arquivo> --porta` | Live reload |
| `run.ps1` | `thz run <arquivo>` | Execução |
| `gui.ps1` | `thz gui` / `gradlew gui` | IDE |
| `health-check.ps1` | `thz check` em todos `exemplos/*.thz` + `compilador/*.thz` | Smoke test |
| `build-llvm.ps1` | `thz ir --llvm` + `clang -target` + `gcc -O3 thz_runtime.c` → `dist/bin/*.exe/*.elf` | AOT legado |
| `build-all.ps1` | Loop `build-llvm.ps1` em `compilador/*.thz` + `exemplos/*.thz` (exclui `_gui`) | AOT em massa |
| `build-native.ps1` | `gradlew nativeCompile` (GraalVM) | Native |
| `build-vsix.ps1` | `npm ci && npm run compile` + `vsce package` em `Extensions/thz-lsp-vscode` | VS Code |
| `package-all.ps1` | `jpackage` + opcional `native` + `llvm` | **Distribuição** (`DEPLOYMENT.md:1`) |
| `clean.ps1` | `gradlew clean` + `rm dist/ target/` | Limpeza |
| `bootstrap-selfhost.ps1` | Valida `CompiladorSelfHostTest` + `driver.ll` | Self-hosting |

---

## 6. Versionamento e Distribuição

- **Versão** em `JVM/thz-core-jvm/build.gradle.kts:15` (`version = "2.3.3"`) + `gradle.properties` — single source; `thz check` valida `VERSAO: "2.4.0"` em `METADADOS_ARQUITETURA`.
- **UberJAR** (`shadowJar`) → `target/thz-jvm-2.3.0.jar` (copiado por `instalarUberJar:80` em `thz-cli-jvm/build.gradle.kts`).
- **VSIX** (`Extensions/thz-lsp-vscode/dist/extension.js` + `thz-lang.vsix`) → Marketplace via `vsce publish`.
- **Release GitHub**: tag `v2.4.0` → Actions builda `dist/thz/` + `dist/bin/thz.exe` + `.vsix` → anexa ao Release.

---

## 7. Checklist de Release

- [ ] `./gradlew test` 100% PASSED (112 JUnit 5)
- [ ] `health-check.ps1` — 8 `thz check` + 18 `exemplos/*.thz` OK
- [ ] `thz check --estrito` nos 3 arquétipos (`PROGRAMA`, `PIPELINE_DADOS`, `TELA`)
- [ ] `thz audit --git --json` sem violações
- [ ] `thz fmt --check` limpo
- [ ] `./gradlew jmh` (sem regressão >5% vs baseline)
- [ ] `package-all.ps1 -WithNative -WithLlvm` gera `dist/thz/` + `dist/bin/thz.exe` + `driver.elf` que executa (`ci.yml:99`)
- [ ] `build-vsix.ps1` compila extensão
- [ ] Tag `vX.Y.Z` + Release Notes (`CHANGELOG.md`)

---

> **Próximo:** [`GUIA_PERFORMANCE.md`](GUIA_PERFORMANCE.md) (como medir que o deploy é rápido), [`TESTES_E_BENCHMARKS.md`](TESTES_E_BENCHMARKS.md) (como garantir que continua correto).

