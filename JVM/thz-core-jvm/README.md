# thz-core — Núcleo da Linguagem THZ-LANG (Java 25)

Biblioteca central do motor JVM da THZ-LANG: linguagem corporativa orientada a domínio (DDD) com contratos formais de governança, aritmética decimal exata e alta taxa de transferência de dados.

**Autônoma por design:** este módulo não conhece GUI nem CLI. Os módulos de apresentação (`thz-cli`, `thz-gui` — neste mesmo build Gradle) consomem sua API pública e registram extensões via `BibliotecaPadrao.registrar(nome, fn)`.

## Módulos internos (`src/main/java/thz/lang/`)

| Pacote | Responsabilidade |
|---|---|
| `ast` | Árvore de Sintaxe Abstrata (sealed records imutáveis) |
| `lexico` | Scanner determinístico com linha/coluna e tolerância a BOM UTF-8 |
| `sintatico` | Parser recursivo descendente com precedência canônica e recuperação |
| `semantico` | Verificação de tipos, contratos `EXIGE`/`GARANTE` ($\forall$ sobre fatias), lint estrito |
| `interpretador` | Tree-walking interpreter + stdlib extensível (`BibliotecaPadrao`) |
| `runtime` | Decimal exato (`DecimalFixo`, ISO/IEC 10967), `Monetario` (ISO 4217), datas, `BlocoMemoria`, idempotência LRU/TTL |
| `documento` | Exportação corporativa PDF/XLSX/DOCX (`DOCUMENTO.*`) |
| `governanca` | Auditoria G4: matriz `RASTREIO_REQUISITO → Regra → Contrato` |
| `docgen` | Documentação viva Markdown + Mermaid a partir da AST |
| `ir` | Representação intermediária `thz-ir/1` + emissão LLVM IR |
| `simd` | Validação formal de vetorização (regras R1–R5) |
| `formato` | Formatador canônico idempotente + serializador JSON |
| `diagnosticos` | Erros `[Categoria][Linha L:C]` com caret |

## Build

```bash
./gradlew test                  # suíte JUnit 5
./gradlew publishToMavenLocal   # publica thz.lang:thz-core no ~/.m2
```

## Consumo

Os consumidores JVM (`JVM/thz-cli-jvm`, `JVM/thz-gui-jvm`, etc.) declaram `implementation("thz.lang:thz-core:2.3.3")` e resolvem via Composite Build (`includeBuild("../thz-core-jvm")`). Fora do workspace, publique com `./gradlew publishToMavenLocal` e a dependência é baixada como artefato.

## Stack

Java 25 (toolchain) · Apache POI 5.3 · OpenPDF 2.0 · Log4j 2.23 · JUnit 5.11 · Gradle (Kotlin DSL)
