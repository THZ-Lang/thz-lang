#!/usr/bin/env node
/**
 * Wrapper da CLI THZ-LANG (motor JVM).
 *
 * Uso a partir da raiz do workspace:
 *   npm run thz -- check JVM/thz-core-jvm/exemplos/faturamento.thz
 *   npm run thz -- run   JVM/thz-core-jvm/exemplos/colecao/01-ola-mundo.thz
 *   npm run repl
 *
 * Garante que o UberJAR exista (gera via Gradle shadowJar se necessário)
 * e encaminha os argumentos para `java -jar`.
 */
"use strict";

const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const repoJvm = path.join(__dirname, "..", "JVM", "thz-cli-jvm");

function localizarJar() {
  const pastas = [
    path.join(__dirname, "..", "target"),
    path.join(repoJvm, "target"),
    path.join(repoJvm, "build", "libs"),
  ];
  for (const pasta of pastas) {
    const unversioned = path.join(pasta, "thz-jvm.jar");
    if (fs.existsSync(unversioned)) {
      return unversioned;
    }
  }
  const candidatos = [];
  for (const pasta of pastas) {
    if (fs.existsSync(pasta)) {
      const arquivos = fs.readdirSync(pasta);
      for (const f of arquivos) {
        if (f.startsWith("thz-jvm") && f.endsWith(".jar") && !f.includes("-sources")) {
          const fullPath = path.join(pasta, f);
          const stat = fs.statSync(fullPath);
          candidatos.push({ fullPath, mtime: stat.mtimeMs });
        }
      }
    }
  }
  if (candidatos.length > 0) {
    candidatos.sort((a, b) => b.mtime - a.mtime);
    return candidatos[0].fullPath;
  }
  return null;
}

let jar = localizarJar();

function gerarUberJar() {
  console.error("[thz] UberJAR não encontrado. Gerando via Gradle (shadowJar)...");
  const win32 = process.platform === "win32";
  const cmd = win32 ? "gradlew.bat" : "./gradlew";
  const r = spawnSync(cmd, [":thz-cli-jvm:shadowJar"], {
    cwd: path.join(__dirname, ".."),
    stdio: "inherit",
    shell: win32,
  });
  jar = localizarJar();
  if (r.status !== 0 || !jar || !fs.existsSync(jar)) {
    console.error("[thz] Falha ao gerar o UberJAR da CLI.");
    process.exit(r.status ?? 1);
  }
}

if (!jar || !fs.existsSync(jar)) {
  gerarUberJar();
}

const args = process.argv.slice(2);
const r = spawnSync(
  "java",
  ["-Dfile.encoding=UTF-8", "-jar", jar, ...args],
  { stdio: "inherit" }
);
process.exit(r.status ?? 0);
