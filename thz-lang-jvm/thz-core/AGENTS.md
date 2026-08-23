# AGENTS.md — THZ-LANG Core

Diretrizes de Engenharia e Operação para Agentes de IA operando no repositório **thz-core** (núcleo da linguagem THZ-LANG em Java 25).

---

## 1. Identidade

* **Projeto:** thz-core — biblioteca central da linguagem THZ-LANG (`.thz`): lexer, parser, semântico, interpretador, runtime, SIMD, IR, governança, docgen e motor de documentos.
* **Autonomia:** este módulo **não conhece GUI nem CLI**. Os módulos `thz-cli` e `thz-gui` (irmãos neste build Gradle) consomem sua API pública e registram extensões via o ponto de extensão da stdlib.
* **Stack Canônica:** Java 25 (OpenJDK 25) + Gradle Wrapper (Kotlin DSL) + Apache POI + OpenPDF + JUnit 5.
* **Paridade:** paridade comportamental estrita e golden tests com o motor TypeScript de referência (`thz-lang-engine`).

## 2. Invariantes Técnicos e Normas Obrigatórias

1. **Aritmética Financeira e Decimais (ISO/IEC 10967):**
   * Proibido ponto flutuante binário IEEE 754 (`float`/`double`) para valores monetários/fiscais.
   * Toda aritmética decimal usa inteiros escalados com `BigInteger` (`DecimalFixo`, `Monetario`).
2. **Bloco de Memória Temporária (`USAR_BLOCO_MEMORIA` / `BlocoMemoria`):**
   * Alocação sequencial em bloco contíguo; liberação limpa via `liberarTudo()` ao final, sem pressão sobre GC.
3. **Contratos Formais (EXIGE/GARANTE/INVARIANTE):**
   * Validados estática e dinamicamente via `AnalisadorSemantico` e `InterpretadorThz`.
4. **Idempotência Inteligente:**
   * `IDEMPOTENTE`/`CHAVE_IDEMPOTENCIA` auditadas e cacheadas com descarte O(1) (`RegistroIdempotencia`).
5. **Sintaxe Canônica em Português:**
   * Palavras reservadas exclusivamente em `thz.lang.lexico.PalavrasReservadas`.
   * Diagnósticos no padrão `[Erro <Categoria>][Linha L:C]` com caret (`thz.lang.diagnosticos.Diagnosticos`).

## 3. Ponto de Extensão da Stdlib

* `BibliotecaPadrao.registrar(nome, FuncaoStdlib)` — mecanismo pelo qual módulos de apresentação registram funções nativas sem o core conhecê-las (ex.: as funções `TELA.*` do thz-gui/thz-cli).
* Assinaturas estáticas para análise semântica vivem em `semantico.AssinaturasStdlib` (metadados puros, sem dependências superiores).
* **Regra:** nada aqui pode importar de `thz.lang.gui` ou `thz.lang.cli`.

## 4. Mapa de Pacotes (`src/main/java/thz/lang/`)

* `ast/`, `lexico/`, `sintatico/`, `semantico/`, `runtime/`, `interpretador/`, `documento/`, `governanca/`, `docgen/`, `ir/`, `simd/`, `formato/`, `diagnosticos/` — ver README.md para detalhes.

## 5. Comandos de Build e Teste

```bash
./gradlew test                  # suíte completa JUnit 5
./gradlew publishToMavenLocal   # publica thz.lang:thz-core:2.3.3 (~/.m2)
```

Os exemplos canônicos `.thz` usados pelos testes ficam em `exemplos/` (lidos a partir da raiz do repositório).
