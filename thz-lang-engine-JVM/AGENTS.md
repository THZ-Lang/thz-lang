# AGENTS.md — THZ-LANG JVM

Diretrizes para agentes operando neste repositório (espelha `thz-lang-engine/AGENTS.md`).

1. **Aritmética:** proibido IEEE 754 para monetário/fiscal — usar `DecimalFixo` (BigInteger escalado).
2. **Memória:** `ArenaMemoria` (O(1)).
3. **Contratos:** `EXIGE`/`GARANTE`/`INVARIANTE` validados via `AnalisadorSemantico` + `InterpretadorThz`.
4. **Build:** Maven é o canônico; **Gradle está marcado como alternativa futura** (`build.gradle.kts` previsto). Código não deve acoplar a APIs específicas do Maven.
5. **Sintaxe canônica:** keywords em português (`PROGRAMA`, `METADADOS_ARQUITETURA`, ...), ошибок `[Erro ...][Linha L:C]`.
