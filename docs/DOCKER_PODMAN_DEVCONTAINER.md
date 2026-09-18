# Guia Completo: Docker, Podman e Devcontainers no THZ-LANG

Este guia descreve como utilizar contêineres OCI (**Docker** e **Podman**) e **Devcontainers** (VS Code / DevPod / Codespaces) para executar, desenvolver, depurar e testar qualquer componente do ecossistema **THZ-LANG** sem necessidade de instalar JDK, Clang ou Node.js diretamente na máquina host.

---

## 1. Visão Geral da Arquitetura de Contêineres

O repositório disponibiliza um build multi-stage unificado (`Dockerfile` e `Containerfile`) e orquestração via Compose (`compose.yaml` / `docker-compose.yml`), suportando os seguintes alvos:

| Target / Imagem | Finalidade | Portas Expostas |
| :--- | :--- | :--- |
| **`api`** (`thz-lang/api`) | Microserviço Spring Boot REST & WebSocket para execução remota e cockpit | `8080` (HTTP) |
| **`cli`** (`thz-lang/cli`) | Compilador e Interpretador CLI do THZ-LANG + REPL interativo | - |
| **`dev`** (`thz-lang/dev`) | Ambiente completo de desenvolvimento (Java 25, Clang, Node.js, Gradle, GDB) | `8080`, `5005` (Debug JVM), `5007` (LSP) |
| **`lsp`** (`thz-lang/lsp`) | Language Server Protocol daemon em Java | stdio / `5007` |

---

## 2. Início Rápido (Plug & Play)

### A. Utilizando scripts de automação ou npm

O THZ-LANG detecta automaticamente se você possui **Podman** ou **Docker** instalado:

```bash
# Ajuda e lista de comandos
npm run docker:build      # Compila as imagens via Docker/Podman
npm run docker:up         # Sobe a API REST em background (http://localhost:8080)
npm run docker:api        # Sobe a API em foreground com logs
npm run docker:repl       # Abre o REPL interativo no contêiner
npm run docker:test       # Roda todos os testes unitários dentro do contêiner
npm run docker:down       # Encerra os serviços

# Forçando o uso do Podman explicitamente:
npm run podman:build
npm run podman:up
npm run podman:repl
```

Você também pode chamar diretamente os scripts:
- **Windows (PowerShell):** `.\scripts\docker.ps1 [comando]`
- **Linux / macOS / WSL:** `./scripts/docker.sh [comando]`

---

## 3. Uso do Devcontainer no VS Code / DevPod / Codespaces

O repositório já inclui configuração pronta em `.devcontainer/devcontainer.json`.

### Passo a Passo:
1. Abra a pasta do projeto no **VS Code**.
2. Quando solicitado pelo pop-up, clique em **"Reopen in Container"** (ou pressione `F1` e digite `Dev Containers: Reopen in Container`).
3. O VS Code construirá o contêiner com:
   - **Java 25 (Eclipse Temurin)** pré-configurado com toolchains.
   - **LLVM / Clang** e GCC para compilação nativa AOT.
   - **Node.js 20** e npm para o tooling de editor.
   - **Extensões essenciais:** Java Extension Pack, Gradle Support, C/C++, ESLint, Prettier, GitLens e suporte ao THZ-LANG.
   - **Caches persistentes** de Gradle e NPM em volumes nomeados para inicialização instantânea em aberturas subsequentes.
4. Ao abrir o terminal integrado dentro do contêiner, o comando `thz` estará disponível no `PATH`:
   ```bash
   thz run exemplos/faturamento.thz
   thz repl
   ```

---

## 4. Uso Direto via Podman (Rootless)

O THZ-LANG é 100% compatível com a execução *rootless* do Podman (usuário não-root `vscode` UID/GID 1000):

```bash
# 1. Compilar a imagem dev ou api
podman build -t thz-lang:latest --target dev .

# 2. Executar um arquivo THZ local montando o diretório
podman run --rm -it -v "$PWD:/workspace:z" -w /workspace thz-lang:latest thz run exemplos/faturamento.thz

# 3. Abrir o REPL interativo
podman run --rm -it -v "$PWD:/workspace:z" -w /workspace thz-lang:latest thz repl

# 4. Subir a API Spring Boot com Podman Compose
podman compose up -d thz-api
```

---

## 5. Uso Direto via Docker Compose

```bash
# Subir a API REST na porta 8080
docker compose up -d thz-api

# Verificar status e healthcheck
curl http://localhost:8080/api/health

# Executar programa via serviço CLI descartável
docker compose run --rm thz-cli run exemplos/faturamento.thz

# Abrir terminal no ambiente Dev completo
docker compose run --rm -it thz-dev /bin/bash
```

---

## 6. Portas e Depuração Remota

Quando o contêiner `dev` ou os serviços do Compose estão ativos:
- **Porta `8080`:** Endpoint da API REST / WebSocket do THZ-LANG.
- **Porta `5005`:** Porta de Remote Debugging da JVM (permite anexar debuggers do IntelliJ / VS Code com `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`).
- **Porta `5007`:** Depuração do LSP Server.
