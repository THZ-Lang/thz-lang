# ==============================================================================
# THZ-LANG — OCI Containerfile (Podman Native)
# Ver Dockerfile para a especificação completa multi-stage.
# ==============================================================================
# ------------------------------------------------------------------------------
# 1. BASE: Ambiente unificado com JDK 25, Clang, GCC e Node.js
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS base

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_HOME=/opt/java/openjdk \
    THZ_HOME=/opt/thz \
    PATH="/opt/thz/bin:${PATH}"

# Instalação de dependências do sistema, toolchain C/C++, LLVM/Clang e utilitários
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    clang \
    lld \
    llvm \
    gcc \
    g++ \
    make \
    curl \
    wget \
    git \
    unzip \
    tar \
    xz-utils \
    procps \
    ca-certificates \
    fontconfig \
    libfreetype6 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    sudo \
    jq \
    cargo \
    rustc \
    && rm -rf /var/lib/apt/lists/*

# Instalação do Node.js 20 LTS e npm
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Configuração do usuário não-root 'vscode' (compatível com Devcontainer, Podman e Ubuntu 24.04)
RUN if id "ubuntu" >/dev/null 2>&1; then \
        usermod -l vscode -d /home/vscode -m ubuntu && \
        groupmod -n vscode ubuntu; \
    else \
        groupadd --gid 1000 vscode && \
        useradd --uid 1000 --gid 1000 -m -s /bin/bash vscode; \
    fi && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/vscode && \
    chmod 0440 /etc/sudoers.d/vscode && \
    mkdir -p /opt/thz/bin /opt/thz/lib /opt/thz/target /workspace && \
    chown -R vscode:vscode /opt/thz /workspace

WORKDIR /workspace

# ------------------------------------------------------------------------------
# 2. BUILDER: Compilação de todos os módulos JVM e assets do ecossistema
# ------------------------------------------------------------------------------
FROM base AS builder

COPY --chown=vscode:vscode . /workspace/

USER vscode

# Dá permissão de execução aos scripts e ao gradlew
RUN chmod +x /workspace/gradlew /workspace/scripts/*.sh /workspace/thz.sh || true

# 1) Publica thz-core no mavenLocal
# 2) Compila shadowJars (cli, lsp), classes da GUI e bootJar da API Spring Boot
RUN ./gradlew :thz-core-jvm:publishToMavenLocal --no-daemon \
    && ./gradlew :thz-cli-jvm:shadowJar :thz-api-jvm:bootJar :thz-lsp-jvm:shadowJar :thz-gui-jvm:classes --no-daemon -x test

# Compila a extensão VS Code se presente
RUN if [ -f "Extensions/thz-lsp-vscode/package.json" ]; then \
        cd Extensions/thz-lsp-vscode && npm install && npm run compile && cd /workspace; \
    fi

# ------------------------------------------------------------------------------
# 3. CLI: Imagem de produção da CLI / Compilador Nativo THZ-LANG
# ------------------------------------------------------------------------------
FROM base AS cli

# Copia os artefatos compilados
COPY --from=builder /workspace/target/thz-jvm.jar /opt/thz/target/thz-jvm.jar
COPY --from=builder /workspace/target/thz-lsp.jar /opt/thz/lib/thz-lsp.jar
COPY --from=builder /workspace/src/runtime/thz_runtime.c /opt/thz/runtime/thz_runtime.c
COPY --from=builder /workspace/exemplos /opt/thz/exemplos

# Cria script executável 'thz' no PATH global
RUN printf '#!/usr/bin/env bash\nexec java -Dfile.encoding=UTF-8 -jar /opt/thz/target/thz-jvm.jar "$@"\n' > /opt/thz/bin/thz \
    && chmod +x /opt/thz/bin/thz \
    && ln -s /opt/thz/bin/thz /usr/local/bin/thz

USER vscode
WORKDIR /workspace

ENTRYPOINT ["thz"]
CMD ["--ajuda"]

# ------------------------------------------------------------------------------
# 4. API: Microserviço Spring Boot REST & WebSocket (Porta 8080)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS api

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    PORT=8080

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && if id "ubuntu" >/dev/null 2>&1; then \
        usermod -l thz -d /home/thz -m ubuntu && \
        groupmod -n thz ubuntu; \
    else \
        groupadd --gid 1000 thz && \
        useradd --uid 1000 --gid 1000 -m -s /bin/bash thz; \
    fi \
    && mkdir -p /opt/thz && chown -R thz:thz /opt/thz

COPY --from=builder --chown=thz:thz /workspace/JVM/thz-api-jvm/build/libs/*.jar /opt/thz/thz-api.jar

USER thz
WORKDIR /opt/thz

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-jar", "/opt/thz/thz-api.jar"]

# ------------------------------------------------------------------------------
# 5. LSP: Language Server Protocol Daemon (stdio / socket)
# ------------------------------------------------------------------------------
FROM base AS lsp

COPY --from=builder /workspace/target/thz-lsp.jar /opt/thz/lib/thz-lsp.jar

USER vscode
WORKDIR /workspace

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/opt/thz/lib/thz-lsp.jar"]
CMD ["--stdio"]

# ------------------------------------------------------------------------------
# 6. DEV: Ambiente de Desenvolvimento Completo (Devcontainers / VS Code / Podman)
# ------------------------------------------------------------------------------
FROM base AS dev

# Copia os artefatos compilados para disponibilidade imediata
COPY --from=builder /workspace/target/thz-jvm.jar /opt/thz/target/thz-jvm.jar
COPY --from=builder /workspace/target/thz-lsp.jar /opt/thz/lib/thz-lsp.jar
COPY --from=builder /workspace/src/runtime/thz_runtime.c /opt/thz/runtime/thz_runtime.c

# Instala ferramentas adicionais para produtividade em desenvolvimento
RUN apt-get update && apt-get install -y --no-install-recommends \
    zsh \
    gdb \
    valgrind \
    htop \
    nano \
    vim \
    tree \
    bash-completion \
    && rm -rf /var/lib/apt/lists/*

# Configura script 'thz' no PATH
RUN printf '#!/usr/bin/env bash\nif [ -f /workspace/target/thz-jvm.jar ]; then\n  exec java -Dfile.encoding=UTF-8 -jar /workspace/target/thz-jvm.jar "$@"\nelif [ -f /opt/thz/target/thz-jvm.jar ]; then\n  exec java -Dfile.encoding=UTF-8 -jar /opt/thz/target/thz-jvm.jar "$@"\nelse\n  cd /workspace && ./gradlew :thz-cli-jvm:shadowJar --no-daemon -q && exec java -Dfile.encoding=UTF-8 -jar /workspace/target/thz-jvm.jar "$@"\nfi\n' > /opt/thz/bin/thz \
    && chmod +x /opt/thz/bin/thz \
    && ln -s /opt/thz/bin/thz /usr/local/bin/thz

# Prepara diretório de cache do Gradle para o usuário vscode
RUN mkdir -p /home/vscode/.gradle /home/vscode/.npm \
    && chown -R vscode:vscode /home/vscode

USER vscode
WORKDIR /workspace

# Portas padrão: 8080 (API), 5005 (JVM Remote Debugger), 5007 (LSP Debugger)
EXPOSE 8080 5005 5007

CMD ["sleep", "infinity"]
