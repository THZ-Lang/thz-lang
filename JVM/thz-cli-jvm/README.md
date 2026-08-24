# thz-cli — CLI e REPL do THZ-LANG (Java 25)

Ponto de entrada de linha de comando e REPL interativo da linguagem THZ-LANG. Consome o núcleo [`thz-core`](../thz-core) e gera o UberJAR executável usado por empacotamento (`jpackage`) e compilação nativa (GraalVM).

## Comandos

```
thz check <arquivo.thz> [--estrito]   # léxico + sintaxe + semântica
thz run <arquivo.thz>                 # executa OPERACAO/PROCEDIMENTO Principal
thz ast <arquivo.thz>                 # dump da AST
thz fmt <arquivo.thz> --check         # formatação canônica idempotente
thz audit <arquivo.thz>               # matriz RASTREIO → Regra → Contrato
thz doc <arquivo.thz>                 # documentação viva (Markdown + Mermaid)
thz ir <arquivo.thz> [--llvm]         # THZ-IR/1 (+ LLVM IR)
thz repl                              # REPL multi-linha (.ajuda, .codigo, .limpar, .sair)
thz gui                               # lança a IDE Desktop se thz-gui estiver no classpath
```

Funções `TELA.*` em modo console: `alerta`, `confirmar` e `pedirTexto` operam via stdin/stdout; `renderizarFormulario` exige a IDE Desktop (módulo `thz-gui`). Com `-Dthz.nao_interativo=true` as interações assumem padrões não bloqueantes.

## Build

```bash
./gradlew test          # suíte JUnit 5
./gradlew shadowJar     # build/libs/thz-jvm-2.3.0.jar + target/thz-jvm-2.3.0.jar
./gradlew run --args="check exemplos/faturamento.thz"
./gradlew cli           # atalho para run
```

> Os exemplos canônicos `.thz` vivem no módulo `thz-core` (`exemplos/`).

## Dependência do core

`implementation("thz.lang:thz-core:2.3.3")` — resolvido via Composite Build a partir de `../thz-core-jvm` (mesma pasta `JVM/`), ou como artefato publicado fora do workspace.
