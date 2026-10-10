# ADR-007: Topologia Multi-Repositório e Governança do Ecossistema THZ-LANG

**Status:** Aprovado  
**Data:** 09/10/2026  
**Decisores:** Time THZ-LANG  
**Contexto Técnico:** Transição do monorepo original para repositórios federados sob a organização GitHub `THZ-Lang`.

---

## 1. Contexto e Problema

Inicialmente, o THZ-LANG foi concebido como um monorepo contendo todos os subsistemas: núcleo JVM, CLI, GUI Swing, extensões de editor, compilador self-hosted em THZ, runtime em Rust e suítes de benchmark. 

Com a evolução independente das tecnologias (Java 25 GraalVM, Rust 2024, TypeScript/Node para VS Code e código nativo THZ), o modelo monorepo único gerou sobrecarga de CI/CD, dependências cruzadas rígidas no controle de versão e ruído em histórico de commits e versionamento semântico.

## 2. Decisão Arquitetural

Decidiu-se particionar o ecossistema em **11 repositórios modulares** sob a organização [`THZ-Lang`](https://github.com/THZ-Lang), mantendo orquestração centralizada via *Gradle Composite Build*:

1. **`thz-lang`**: Repositório orquestrador, especificações, manuais, diretrizes de governança e composite build via `includeBuild` para os módulos JVM.
2. **`thz-core-jvm`**: AST, Parser, Interpretador, Analisador Semântico, Runtime JVM e Contratos.
3. **`thz-cli-jvm`**: Linha de comando `thz`, REPL e rotinas de packaging.
4. **`thz-gui-jvm`**: Desktop IDE / Studio em Java Swing / FlatLaf.
5. **`thz-api-jvm`**: Servidor de APIs HTTP/REST e WebSocket.
6. **`thz-agent-jvm`**: Agente autônomo de tarefas e integração de automação.
7. **`thz-lsp-jvm`**: Language Server Protocol (LSP4J).
8. **`thz-bench-jvm`**: Benchmarks JMH.
9. **`thz-runtime-rs`**: Runtime nativo Rust (Arenas SIMD, C-ABI, Criptografia).
10. **`thz-compiler`**: Compilador experimental self-hosted escrito em THZ.
11. **`thz-vscode`**: Extensão para VS Code / Antigravity IDE.

### Orquestração e Workspace Local
- A pasta raiz `thz-lang-repo` atua como agregador de desenvolvimento local.
- Um arquivo Multi-Root Workspace (`thz-ecosystem.code-workspace`) fornece visibilidade unificada nos editores.
- Scripts de conveniência (`thz-repo.ps1` e `thz-repo.sh`) padronizam operações em lote (`status`, `fetch`, `pull`, `build`, `test`, `health`).

## 3. Diretrizes de Governança Git

1. **Branches Atômicas:** Proibidos commits e pushes diretos na branch `main` em todos os repositórios.
2. **Pull Requests Obrigatórios:** Todas as mudanças estruturais e novas funcionalidades devem trafegar por branches `feat/...` ou `fix/...` com PRs auditados no GitHub.
3. **Rastreabilidade:** Decisões estruturais e técnicas devem ser registradas em ADRs mantidos sob `thz-lang/docs/ADRs/`.

## 4. Consequências

### Positivas
- Ciclo de vida e versionamento independentes por módulo.
- Pipelines de CI/CD rápidos e focados em cada stack.
- Construção composta no Gradle permite desenvolver os módulos JVM localmente com recarga automática como se fossem monorepo.

### Negativas / Mitigações
- Necessidade de coordenar atualizações conjuntas entre módulos (mitigado pelo script `thz-repo.ps1` e tarefa `sincronizarVersao` no Gradle).
