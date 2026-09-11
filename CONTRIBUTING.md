# Guia de Contribuição — THZ-LANG

Obrigado pelo interesse em contribuir com o **THZ-LANG**!

Este projeto implementa uma linguagem de programação corporativa estruturada em língua portuguesa, com tipagem estática forte, aritmética decimal exata, contratos formais, compilador self-hosted e alto desempenho com compilação nativa AOT.

---

## 1. Estrutura do Repositório

- `JVM/`: Motor canônico em Java 25 (Gradle Multi-Módulo), composto por:
  - `thz-core-jvm/`: Núcleo da linguagem (Lexer, Parser, AST, Semântico, Runtime, DecimalFixo, IR, DocGen).
  - `thz-cli-jvm/`: CLI executável, REPL interativo e Dev Server.
  - `thz-gui-jvm/`: Desktop IDE Swing + FlatLaf (Editor, Gutter, Formulários Dinâmicos).
  - `thz-lsp-jvm/`: Language Server Protocol (LSP4J).
  - `thz-bench-jvm/`: Suíte de Benchmarks JMH.
  - `thz-api-jvm/`: API REST Spring Boot.
- `compilador/`: Compilador self-hosted escrito em `.thz` (`tokens.thz`, `ast.thz`, `lexer.thz`, `parser.thz`, `codegen.thz`, `driver.thz`).
- `src/runtime/thz_runtime.c`: Runtime nativo C Dual-OS (Win32/POSIX) para executáveis gerados via LLVM Clang.
- `scripts/`: Automações de compilação AOT (`build-llvm.ps1`, `build-native.ps1`).
- `Extensions/thz-lsp-vscode/`: Extensão oficial para o VS Code.
- `docs/`: Manuais, especificação EBNF, conformidade normativa e diretrizes de qualidade.

---

## 2. Invariantes Técnicos Obrigatórios

Ao submeter código ou propor mudanças, certifique-se de respeitar os seguintes princípios:

1. **Aritmética Financeira e Decimais (ISO/IEC 10967 & ISO 4217):**
   - É expressamente proibido o uso de ponto flutuante binário IEEE 754 (`number` float / `double`) para valores fiscais e monetários.
   - Toda aritmética decimal utiliza inteiros escalados com `DecimalFixo` no Java e inteiros de 128-bits no codegen LLVM.

2. **Fonte da Verdade Léxica:**
   - Palavras-chave reservadas vivem unicamente em `thz.lang.lexico.PalavrasReservadas` (Java) e `compilador/tokens.thz`. É proibido criar literais dispersos no parser ou runtime.

3. **Design by Contract & Governança:**
   - Suporte e respeito integral às cláusulas `EXIGE`, `GARANTE` e `INVARIANTE`.

4. **Diagnósticos com Posição Exata:**
   - Erros léxicos, sintáticos e semânticos devem sempre reportar linha e coluna no formato `[Erro Sintático][Linha L:C]`.

5. **Branch para Self-Hosting e Autonomia LLVM:**
   - Trabalhos no compilador self-hosted (`compilador/`), runtime C (`thz_runtime.c`) ou codegen LLVM devem ser desenvolvidos na branch `feat/self-hosting-llvm-autonomy`.

---

## 3. Comandos Úteis de Desenvolvimento

```bash
# Executar toda a suíte de testes JUnit 5:
./gradlew test

# Executar a CLI em desenvolvimento:
./gradlew cli --args="check exemplos/faturamento.thz"

# Iniciar a Desktop IDE:
./gradlew gui

# Executar benchmarks:
./gradlew jmh

# Compilar binários nativos AOT via LLVM Clang:
powershell.exe -ExecutionPolicy Bypass -File scripts/build-llvm.ps1 -ArquivoThz compilador/driver.thz
```

---

## 4. Submissão de Pull Requests

1. Crie uma branch para a sua alteração: `git checkout -b feature/nome-da-feature`
2. Certifique-se de que todos os testes passem com sucesso (`./gradlew test`).
3. Mantenha os arquivos de documentação sincronizados caso altere a gramática ou adicione comandos.
4. Abra um Pull Request detalhando as alterações e motivação técnica.
