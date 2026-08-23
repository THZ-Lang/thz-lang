#!/usr/bin/env node
/**
 * Wrapper da CLI THZ-LANG (motor JVM).
 *
 * Uso a partir da raiz do workspace:
 *   npm run thz -- check thz-lang-jvm/thz-core/exemplos/faturamento.thz
 *   npm run thz -- run   thz-lang-jvm/thz-core/exemplos/colecao/01-ola-mundo.thz
 *   npm run repl
 *
 * Garante que o UberJAR exista (gera via Gradle shadowJar se necessário)
 * e encaminha os argumentos para `java -jar`.
 */
"use strict";

const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const repoJvm = path.join(__dirname, "..", "thz-lang-jvm");
const jar = path.join(repoJvm, "target", "thz-jvm-2.3.0.jar");

function gerarUberJar() {
  console.error("[thz] UberJAR não encontrado. Gerando via Gradle (shadowJar)...");
  const win32 = process.platform === "win32";
  const cmd = win32 ? "gradlew.bat" : "./gradlew";
  const r = spawnSync(cmd, ["shadowJar"], {
    cwd: repoJvm,
    stdio: "inherit",
    shell: win32,
  });
  if (r.status !== 0 || !fs.existsSync(jar)) {
    console.error("[thz] Falha ao gerar o UberJAR em thz-lang-jvm/target/.");
    process.exit(r.status ?? 1);
  }
}

if (!fs.existsSync(jar)) {
  gerarUberJar();
}

const args = process.argv.slice(2);
const r = spawnSync(
  "java",
  ["-Dfile.encoding=UTF-8", "-jar", jar, ...args],
  { stdio: "inherit" }
);
process.exit(r.status ?? 0);
