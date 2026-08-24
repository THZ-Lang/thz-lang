# Guia de Contribuição — THZ-LANG

Obrigado pelo interesse em contribuir com o **THZ-LANG**!

Este projeto implementa uma linguagem de programação corporativa estruturada em língua portuguesa, com tipagem estática forte, aritmética decimal exata, contratos formais e alto desempenho.

---

## 1. Estrutura do Repositório

- `JVM/`: Motor canônico em Java 25 (JDK 25 com Gradle), composto por módulos autônomos integrados (`thz-core-jvm`, `thz-cli-jvm`, `thz-gui-jvm`, `thz-api-jvm`, `thz-lsp-jvm` e `thz-bench-jvm`).
- `Extensions/thz-lsp-vscode/`: Extensão oficial para VS Code conectada ao `thz-lsp-jvm` via stdio (LSP4J).
- `docs/`: EBNF da gramática (`docs/GRAMATICA.md`), manual da linguagem e documentações arquiteturais.
- `docs/PROJECT.md` & `AGENTS.md`: Diretrizes formais de arquitetura, invariantes e mapa do ecossistema.

---

## 2. Invariantes Técnicos Obrigatórios

Ao submeter código ou propor mudanças, certifique-se de respeitar os seguintes princípios:

1. **Aritmética Financeira e Decimais (ISO/IEC 10967):**
   - É expressamente proibido o uso de ponto flutuante binário IEEE 754 (`number` float / `double`) para valores fiscais e monetários.
   - Toda aritmética decimal utiliza inteiros escalados com `BigInt` / `DecimalFixo` no Java.

2. **Fonte da Verdade Léxica:**
   - Palavras-chave reservadas vivem unicamente em `thz.lang.lexico.PalavrasReservadas` (Java). É proibido criar literais dispersos no parser ou runtime.

3. **Design by Contract:**
   - Suporte e respeito integral a cláusulas `EXIGE`, `GARANTE` e `INVARIANTE`.

4. **Diagnósticos com Posição Exata:**
   - Erros léxicos, sintáticos e semânticos devem sempre reportar linha e coluna no formato `[Erro Sintático][Linha L:C]`.

---

## 3. Fluxo de Desenvolvimento

### Motor JVM 25 / Java

```bash
npm test                        # Executa toda a suíte de testes (Core, GUI, API, LSP)
npm run ide                     # Executa a IDE Desktop Swing
npm run lsp:jar                 # Compila o servidor LSP (shadowJar)
npm run ext:compile             # Compila a extensão VS Code
```

---

## 4. Submissão de Pull Requests

1. Crie uma branch para a sua feature ou correção: `git checkout -b feature/nome-da-feature`
2. Certifique-se de que todos os testes passem (`npm test` e `mvn test`).
3. Formate o código e adicione testes correspondentes para novas funcionalidades.
4. Abra um Pull Request detalhando o que foi alterado e a motivação.
