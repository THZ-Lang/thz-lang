# ADR-004 — GraalVM Native Image vs jpackage para Distribuição

**Data:** 2025-08-25 · **Status:** Aceito · **Contexto:** Como distribuir `thz.exe`/`thz-gui.exe` — `jpackage` (JDK embutido) vs `native-image` (SubstrateVM).

## Contexto

`thz-cli-jvm`/`thz-gui-jvm` usam Swing+FlatLaf (`com.formdev:flatlaf:3.5.4`) com reflexão pesada. GraalVM `nativeCompile` (`thz-cli-jvm/build.gradle.kts:43`, `thz-gui-jvm/build.gradle.kts:44`) exige `native-image-agent` + `reflect-config.json` + `IncludeResources` + `-Djava.awt.headless=false` (`CLI_E_TOOLING.md:150`). `jpackage` (JDK 25) gera app-image com JDK cortado via `jlink`.

## Decisão

**Padrão: `jpackage` (`scripts/package-all.ps1:19` `dist/thz/thz.exe`); GraalVM opcional (`-WithNative` → `dist/bin/thz.exe`).**

## Consequências

- **jpackage Prós:** Sem `reflect-config.json`, sem `guiColetarMetadadosAgente`, `thz_webview2.c` funciona sem cross, build 30s. Contras: 80MB vs 15MB (GraalVM), precisa JRE cortada.
- **GraalVM Prós:** <15MB, <5ms startup (`ARQUITETURA_COMPILACAO_NATIVA.md:11`), `--no-fallback` seguro. Contras: build 1–2min, `closed-world` quebra se esquecer `IncludeResources`.
- **CI:** `ci.yml` só `jpackage`+`shadowJar`; `native-image` é `-WithNative` manual (`DEPLOYMENT.md:2`).

