# Resolução de Problemas & Perguntas Frequentes (FAQ) — THZ-LANG

> **Primeiro lugar para procurar quando algo quebra.** Cobertura: build, `thz check`/`run`, `VETORIZAR_PARA`, arenas, `DECIMAL`, `PIPELINE_DADOS`, `TELA`, LSP, GraalVM, LLVM, Docker/CI.

---

## Build / Gradle

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `Unsupported class file major version 67` | Gradle/JDK <25 | `Settings → Gradle → Gradle JVM → 25` (`INTELLIJ_SETUP.md:3`) |
| `Could not find thz.lang:thz-core:2.3.3` | Composite não importado | `File → Invalidate Caches` + `settings.gradle.kts` tem `includeBuild("JVM/thz-core-jvm")` |
| `configuration-cache problems` | Cache stale | `./gradlew --no-configuration-cache test` ou `gradle.properties:12` `problems=warn` |
| Build lento | Sem parallel | `gradle.properties:19` `parallel=true`, `vfs.watch=true` |

---

## `thz check` / `thz run` / Semântico

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `FIM_PROGRAMA esperado` | `PROGRAMA` sem terminador | `GRAMATICA.md:24` — `PROGRAMA`→`FIM_PROGRAMA`, `PIPELINE_DADOS`→`FIM_PIPELINE`, `TELA`→`FIM_TELA` |
| `--estrito exige METADADOS_ARQUITETURA` | `METADADOS_ARQUITETURA` ausente | Adicione `METADADOS_ARQUITETURA SISTEMA:"X" DOMINIO:"Y" FIM_METADADOS` (`MANUAL_LINGUAGEM.md:39`) |
| `Moeda BRL não pode somar com USD` | `MONETARIO(BRL) + MONETARIO(USD)` | Conversão explícita; `CONFORMIDADE_E_NORMAS.md:44` |
| `Literal com mais casas que escala` | `DECIMAL(12,2) <- 0.0001` (4 casas >2) | `DecimalFixo.java:61` — ajuste literal ou `DECIMAL(12,4)` |
| `Divisão por zero` | `DECIMAL / 0` | `DecimalFixo.java:182` — `SE divisor==0 FALHAR_COM(...)` |
| `RESULTADO não tratado` | `RETORNAR RESULTADO` sem `CASO_RESULTADO` | `MANUAL_LINGUAGEM.md:257` — sempre `CASO_RESULTADO res SUCESSO=> ERRO=> FIM_CASO` |

---

## `VETORIZAR_PARA` / `LAYOUT_COLUNAR` (SIMD)

| Erro/Warning | Causa | Solução |
| :--- | :--- | :--- |
| `R2: Passo ... deve ser potência de 2` | `PASSO_SIMD 7` | `ValidadorSimd.java:73` — use 2/4/8/16/32/64 |
| `R1: ... operará com Gather/Scatter` | `VETORIZAR_PARA` sem `LAYOUT_COLUNAR` | `ESTRUTURA X LAYOUT_COLUNAR` (`GUIA_PERFORMANCE.md:2`) |
| `R5: LER não permitida em vetorizado` | `LER` dentro de `VETORIZAR_PARA` | `ValidadorSimd.java:101` — mova `LER` para fora |
| `vetorizavel:false` no `thz ir --saida` | Viola R2/R5 | `./gradlew :thz-cli-jvm:run --args="ir f.thz --saida /tmp/j.json"` + `jq .loopsSimd[].violacoes` |

---

## Arenas — `USAR_BLOCO_MEMORIA`

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `Limite do bloco excedido: solicitado 64, utilizado 1048000/1048576` | `tamanhoMb` pequeno | `BlocoMemoria.java:56` — dobre `tamanhoMb` ou chunk `VETORIZAR_PARA` |
| `thz_arena_alloc retornou 0` | `HeapAlloc`/`malloc` falhou (OOM) | `thz_runtime.c:41` — `if(!arena) return 0` — reduza `bytes` ou libere arenas |

Tuning: `GUIA_PERFORMANCE.md:4`, `getPorcentagemUso()` 30–70%.

---

## `PIPELINE_DADOS`

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `FONTE_ENTRADA sem FIM_FONTE` | Terminador `FIM_FONTE` ausente | `GRAMATICA.md:79` |
| `CONECTOR: "KAFKA" desconhecido` | Roadmap Fase 7 | `TODO.md:32` — hoje só `POSTGRESQL`/`MYSQL`/`MONGODB`/`CSV`/`JSONB` |
| `thz audit` não vê `TRANSFORMACAO` | Sem `RASTREIO_REQUISITO` | Adicione `RASTREIO_REQUISITO: "REQ-..."` (`PIPELINE_DADOS.md:4.3`) |

---

## `TELA` / `.thzui`

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `thz run *.thzui` não abre | `WebView2Loader.dll` ausente | Instale Edge WebView2 Runtime; `thz_webview2.c:74` `thz_webview_loader_status()` |
| `thz gui` branco | `reflect-config.json` desatualizado | `./gradlew :thz-gui-jvm:guiColetarMetadadosAgente` (`RUNTIME_NATIVO.md:7`) |
| `build-llvm.ps1 bloqueou _gui` | `*_gui.thz` sem `-ForceLegado` | `TELA_THZUI.md:7` — use `thz run` (WebView), não LLVM para GUI |
| `TELA.renderizarFormulario` não valida | Sem `INVARIANTE`/`EXIGE` | `TELA_THZUI.md:5` — adicione `INVARIANTE preco>=0` + `EXIGE` |

---

## LSP / VS Code

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `thz-lsp-jvm-shadow.jar not found` | Não buildado | `./gradlew :thz-lsp-jvm:shadowJar` (`LSP_VSCODE.md:5`) |
| Sem cor `.thz` | TextMate não adicionado | `LSP_VSCODE.md:4` — `+` `Extensions/thz-lsp-vscode/` em `TextMate Bundles` |
| `lintEstrito` não pega | Flag `false` | `settings.json: "thz-lang.lintEstrito": true` ou `thz check --estrito` |
| Extension não ativa | `activationEvents` | `package.json` `onLanguage:thz` — abra um `.thz` |

---

## GraalVM / Native Image

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `ClassNotFoundException FlatLaf` | Sem `reflect-config.json` | `guiColetarMetadadosAgente` (`DEPLOYMENT.md:5`) |
| `Resource not found *.thz` | Sem `IncludeResources` | `thz-cli-jvm/build.gradle.kts:54` `-H:IncludeResources=.*\.thz.*` |
| Build OOM | `Xmx` baixo | `gradle.properties:32` `-Xmx2096m` |

---

## LLVM / `thz_runtime.c`

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `clang: command not found` | LLVM não instalado | `scoop install llvm` (Win) / `apt-get clang llvm gcc` (Linux, `ci.yml:88`) |
| `undefined reference to thz_arena_alloc` | `thz_runtime.c` não linkado | `build-llvm.ps1:78` — `gcc ... thz_runtime.c` |
| `thz_exiba_i128` só imprime low | Stub | `thz_runtime.c:137` TODO — formatar `high`+`scale` |

---

## Docker / CI

| Erro | Causa | Solução |
| :--- | :--- | :--- |
| `dist/thz/thz.exe: not found` | `package-all.ps1` não rodado | `DEPLOYMENT.md:2` — `.\scripts\package-all.ps1` |
| `xvfb-run: not found` (CI) | Sem Xvfb para `thz-gui` testes | `ci.yml:30` `xvfb-run ./gradlew test` |
| `audit --git` não filtra | Fora de repo Git | Rode dentro de `thz-lang/` com `git status` limpo |

---

## FAQ

**Q: LLVM AOT é obrigatório?** Não — `thz run` via JVM é padrão; LLVM é `dist/bin/*.elf` Zero-JVM para `PIPELINE_DADOS` crítico (`DEPLOYMENT.md:3.2`).

**Q: `DECIMAL` sempre lento?** Não — `GUIA_PERFORMANCE.md:5` — vetorize `NATURAL32`/`INTEIRO` e use `DECIMAL` só para dinheiro; `DecimalBench` mostra 10–50× gap vs `double`, mas correto.

**Q: Onde fica `dist` vs `target`?** `dist/thz/` (jpackage), `dist/bin/` (GraalVM/LLVM), `target/thz-jvm-2.3.0.jar` (shadowJar) — `DEPLOYMENT.md:1`.

**Q: Como atualizar goldens AST?** `TESTES_E_BENCHMARKS.md:3` — re-run com flag se o projeto expuser `--updateGoldens`, caso contrário edite `src/test/resources/*.json` manualmente.

