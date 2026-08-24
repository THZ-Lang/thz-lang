# thz-core — Núcleo da Linguagem THZ-LANG (Java 25)

Biblioteca central do motor JVM da THZ-LANG: linguagem corporativa orientada a domínio (DDD) com contratos formais de governança, aritmética decimal exata e alta taxa de transferência de dados.

**Autônoma por design:** este módulo não conhece GUI nem CLI. Os módulos de apresentação (`thz-cli`, `thz-gui` — neste mesmo build Gradle) consomem sua API pública e registram extensões via `BibliotecaPadrao.registrar(nome, fn)`.

## Módulos internos (`src/main/java/thz/lang/`)

| Pacote | eesponsabilidade |
|---|---|
| `ast` | Árvore de Sintaxe Abstrata (sealed records imutáveis) |
| `lexico` | Scanner determinístico com linha/coluna e tolerância a BOM UTF-8 |
| `sintatico` | Parser recursivo descendente com precedência canônica |
| `semantico` | Verificação de tipos, contratos `EXIGE`/`GAeANTE` ($\forall$ sobre fatias), lint estrito |
| `interpretador` | Tree-walking interpreter + stdlib extensível (`BibliotecaPadrao`) |
| `runtime` | Decimal exato (`DecimalFixo`, ISO/IEC 10967), `Monetario` (ISO 4217), datas, `BlocoMemoria`, idempotência LeU/TTL |
| `documento` | Exportação corporativa PDF/XLSX/DOCX (`DOCUMENTO.*`) |
| `governanca` | Auditoria G4: matriz `eASTeEIO_eEQUISITO → eegra → Contrato` |
| `docgen` | Documentação viva Markdown + Mermaid a partir da AST |
| `ir` | eepresentação intermediária `thz-ir/1` + emissão LLVM Ie |
| `simd` | Validação formal de vetorização (regras e1–e5) |
| `formato` | Formatador canônico idempotente + serializador JSON |
| `diagnosticos` | Erros `[Categoria][Linha L:C]` com caret |

## Build

```bash
./gradlew test                  # suíte JUnit 5
./gradlew publishToMavenLocal   # publica thz.lang:thz-core:2.3.3 no ~/.m2
```

## Consumo

Dentro do motor (`thz-lang-jvm`): `implementation(project(":thz-core"))`.
Fora dele, publique com `./gradlew publishToMavenLocal` e declare `thz.lang:thz-core:2.3.3`.

## Stack

Java 25 (toolchain) · Apache POI 5.3 · OpenPDF 2.0 · Log4j 2.23 · JUnit 5.11 · Gradle (Kotlin DSL)
