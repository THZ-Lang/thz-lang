# ==============================================================================
# THZ-LANG — Devcontainer Dockerfile
# ==============================================================================
FROM eclipse-temurin:25-jdk-noble AS devcontainer

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_HOME=/opt/java/openjdk \
    THZ_HOME=/opt/thz \
    PATH="/opt/thz/bin:${PATH}"

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
    zsh \
    gdb \
    valgrind \
    htop \
    nano \
    vim \
    tree \
    bash-completion \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

RUN if id "ubuntu" >/dev/null 2>&1; then \
        usermod -l vscode -d /home/vscode -m ubuntu && \
        groupmod -n vscode ubuntu; \
    else \
        groupadd --gid 1000 vscode && \
        useradd --uid 1000 --gid 1000 -m -s /bin/bash vscode; \
    fi && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/vscode && \
    chmod 0440 /etc/sudoers.d/vscode && \
    mkdir -p /opt/thz/bin /opt/thz/lib /opt/thz/target /workspace /home/vscode/.gradle /home/vscode/.npm && \
    chown -R vscode:vscode /opt/thz /workspace /home/vscode

RUN printf '#!/usr/bin/env bash\nif [ -f /workspace/target/thz-jvm.jar ]; then\n  exec java -Dfile.encoding=UTF-8 -jar /workspace/target/thz-jvm.jar "$@"\nelif [ -f /opt/thz/target/thz-jvm.jar ]; then\n  exec java -Dfile.encoding=UTF-8 -jar /opt/thz/target/thz-jvm.jar "$@"\nelse\n  cd /workspace && ./gradlew :thz-cli-jvm:shadowJar --no-daemon -q && exec java -Dfile.encoding=UTF-8 -jar /workspace/target/thz-jvm.jar "$@"\nfi\n' > /opt/thz/bin/thz \
    && chmod +x /opt/thz/bin/thz \
    && ln -s /opt/thz/bin/thz /usr/local/bin/thz

USER vscode
WORKDIR /workspace

EXPOSE 8080 5005 5007

CMD ["sleep", "infinity"]
