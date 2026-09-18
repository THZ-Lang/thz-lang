# Deployment & Containerization Guide — THZ-LANG

This guide covers deployment pipelines, Docker containerization, and native binary distribution for THZ-LANG.

---

## 1. Docker Multi-Stage Build

```dockerfile
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :thz-cli-jvm:shadowJar
RUN native-image -jar JVM/thz-cli-jvm/build/libs/thz-jvm.jar -o thz

FROM debian:bookworm-slim
COPY --from=builder /app/thz /usr/local/bin/thz
ENTRYPOINT ["thz"]
```
