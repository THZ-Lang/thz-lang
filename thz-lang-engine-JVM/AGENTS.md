# AGENTS.md — THZ-LANG JVM

Diretrizes para agentes operando neste repositório (espelha `thz-lang-engine/AGENTS.md`).

1. **Aritmética:** proibido IEEE 754 para monetário/fiscal — usar `DecimalFixo` (BigInteger escalado).
2. **Gerenciamento por Bloco de Memória Temporária (`USAR_BLOCO_MEMORIA` / `BlocoMemoria`):**
   * Processamento em lote e escopos temporários utilizam alocação sequencial em bloco de memória (`BlocoMemoria`).
   * Toda a memória utilizada no bloco é liberada de forma limpa e automática ao final da execução (`liberarTudo()`), sem sobrecarga de Garbage Collection nem fragmentação.

3. **Contratos:** `EXIGE`/`GARANTE`/`INVARIANTE` validados via `AnalisadorSemantico` + `InterpretadorThz`.
4. **Build:** Gradle (Kotlin DSL com Gradle Wrapper) é o sistema canônico oficial (`build.gradle.kts` / `gradlew`).
5. **Sintaxe canônica:** keywords em português (`PROGRAMA`, `METADADOS_ARQUITETURA`, ...), erros `[Erro ...][Linha L:C]`.
