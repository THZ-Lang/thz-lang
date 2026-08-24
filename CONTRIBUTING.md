# Guia de Contribuição — THZ-LANG

Obrigado pelo interesse em contribuir com o **THZ-LANG**!

Este projeto implementa uma linguagem de programação corporativa estruturada em língua portuguesa, com tipagem estática forte, aritmética decimal exata, contratos formais e alto desempenho.

---

## 1. Estrutura do Repositório

- `Node/thz-lang-base/`: Motor canônico em Node.js (v20+) + TypeScript (v5+), com Language Service, Playground Web (Monaco), Servidor LSP, Extensão VS Code, IR e vetorização SIMD.
- `JVM/thz-core-jvm/`, `JVM/thz-cli-jvm/`, `JVM/thz-gui-jvm/`: Porto do motor em Java 25 como três projetos Gradle autônomos (núcleo/stdlib, CLI+REPL e IDE Desktop Swing), comunicando pela API pública do core via Composite Build.
- `docs/`: EBNF da gramática (`docs/GRAMATICA.md`) e documentações arquiteturais.
- `docs/PROJECT.md` & `AGENTS.md`: Diretrizes formais de arquitetura, invariantes e mapa do ecossistema.

---

## 2. Invariantes Técnicos Obrigatórios

Ao submeter código ou propor mudanças, certifique-se de respeitar os seguintes princípios:

1. **Aritmética Financeira e Decimais (ISO/IEC 10967):**
   - É expressamente proibido o uso de ponto flutuante binário IEEE 754 (`number` float / `double`) para valores fiscais e monetários.
   - Toda aritmética decimal utiliza inteiros escalados com `BigInt` (`DecimalFixo` no TypeScript / Java).

2. **Fonte da Verdade Léxica:**
   - Palavras-chave reservadas vivem unicamente em `src/keywords.ts` (TypeScript) e `thz.lang.lexico.PalavrasReservadas` (Java). É proibido criar literais dispersos no parser ou runtime.

3. **Design by Contract:**
   - Suporte e respeito integral a cláusulas `EXIGE`, `GARANTE` e `INVARIANTE`.

4. **Diagnósticos com Posição Exata:**
   - Erros léxicos, sintáticos e semânticos devem sempre reportar linha e coluna no formato `[Erro Sintático][Linha L:C]`.

---

## 3. Fluxo de Desenvolvimento

### Motor Node / TypeScript

```bash
cd thz-lang-engine
npm install
npm test                     # Executa toda a suíte de testes (159 testes)
npm run thz:check -- --estrito # Verificação semântica estrita
npm run playground           # Inicia o Playground Web localmente
```

### Motor JVM 25 / Java (três projetos autônomos)

```bash
cd JVM/thz-core-jvm && ./gradlew test          # Núcleo: suíte JUnit 5 com JDK 25
cd JVM/thz-gui-jvm  && ./gradlew test          # IDE Desktop Swing
cd JVM/thz-cli-jvm  && ./gradlew shadowJar     # UberJAR executável
```

---

## 4. Submissão de Pull Requests

1. Crie uma branch para a sua feature ou correção: `git checkout -b feature/nome-da-feature`
2. Certifique-se de que todos os testes passem (`npm test` e `mvn test`).
3. Formate o código e adicione testes correspondentes para novas funcionalidades.
4. Abra um Pull Request detalhando o que foi alterado e a motivação.
