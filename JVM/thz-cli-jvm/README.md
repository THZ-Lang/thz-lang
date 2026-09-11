# thz-cli — CLI, Dev Server e REPL do THZ-LANG (Java 25)

Ponto de entrada de linha de comando, servidor de desenvolvimento com live reload e REPL interativo da linguagem THZ-LANG. Consome o núcleo [`thz-core`](../thz-core-jvm) e gera o UberJAR executável usado por empacotamento (`jpackage`) e compilação nativa (GraalVM).

## Comandos Suportados

```bash
thz check <arquivo.thz> [--estrito]   # léxico + sintaxe + semântica + lint
thz run <arquivo.thz>                 # executa OPERACAO/PROCEDIMENTO Principal
thz dev <arquivo.thz>                 # servidor dev com Live Reload automático
thz audit <arquivo.thz> [--git]       # auditoria de requisitos (suporte a Git diff)
thz fmt <arquivo.thz> --escrever      # formatação canônica idempotente
thz doc <arquivo.thz>                 # documentação viva (Markdown + Mermaid)
thz ui <arquivo.thzui> --html         # renderização de interface gráfica em HTML5
thz ir <arquivo.thz> [--llvm]         # THZ-IR/1 (+ LLVM IR)
thz ast <arquivo.thz>                 # dump da AST em JSON
thz repl                              # REPL multi-linha (.ajuda, .codigo, .limpar, .sair)
thz gui                               # lança a Desktop IDE Swing FlatLaf
```

Funções `TELA.*` em modo console: `alerta`, `confirmar` e `pedirTexto` operam via stdin/stdout; `renderizarFormulario` aciona a Desktop IDE (módulo `thz-gui`). Com `-Dthz.nao_interativo=true` as interações assumem padrões não bloqueantes.

## Build

```bash
./gradlew test          # suíte JUnit 5
./gradlew shadowJar     # gera UberJAR executável
./gradlew run --args="check exemplos/faturamento.thz"
```

## Compilação Nativa (GraalVM)

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/build-native.ps1 -PularTestes
```

Gera o executável nativo autônomo `dist/bin/thz.exe`.

## Dependência do Core

`implementation("thz.lang:thz-core:2.3.3")` — resolvido via Composite Build a partir de `../thz-core-jvm` (mesma pasta `JVM/`), ou como artefato publicado via Maven local.
