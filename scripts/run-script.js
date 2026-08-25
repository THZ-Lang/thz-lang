#!/usr/bin/env node
/**
 * Cross-platform script runner for npm scripts in THZ-LANG workspace.
 * Automatically delegates to .ps1 on Windows and .sh on Linux/macOS/Docker.
 */
const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const raiz = path.resolve(__dirname, "..");
const isWin = process.platform === "win32";

const target = process.argv[2];
const extraArgs = process.argv.slice(3);

if (!target) {
  console.error("Uso: node scripts/run-script.js <nome-do-script> [argumentos...]");
  process.exit(1);
}

// 1. Caso seja um comando Gradle direto (ex: gradle test :thz-core-jvm:test)
if (target.startsWith("gradle:") || target === "gradle") {
  const gradleTask = target.startsWith("gradle:") ? target.substring(7) : extraArgs.shift();
  const gradlewCmd = isWin ? path.join(raiz, "gradlew.bat") : path.join(raiz, "gradlew");
  const r = spawnSync(gradlewCmd, [gradleTask, ...extraArgs], {
    cwd: raiz,
    stdio: "inherit",
    shell: isWin,
  });
  process.exit(r.status ?? 0);
}

// 2. Procura o script na pasta scripts/ ou no caminho especificado
let scriptBase = target.replace(/\.(ps1|sh)$/, "");
if (!scriptBase.startsWith("scripts") && !scriptBase.includes(path.sep) && !scriptBase.includes("/")) {
  scriptBase = path.join("scripts", scriptBase);
}

const ps1Path = path.join(raiz, `${scriptBase}.ps1`);
const shPath = path.join(raiz, `${scriptBase}.sh`);

if (isWin) {
  if (fs.existsSync(ps1Path)) {
    const r = spawnSync("powershell", ["-ExecutionPolicy", "Bypass", "-File", ps1Path, ...extraArgs], {
      cwd: raiz,
      stdio: "inherit",
    });
    process.exit(r.status ?? 0);
  } else if (fs.existsSync(shPath)) {
    const r = spawnSync("bash", [shPath, ...extraArgs], {
      cwd: raiz,
      stdio: "inherit",
    });
    process.exit(r.status ?? 0);
  }
} else {
  if (fs.existsSync(shPath)) {
    const r = spawnSync("bash", [shPath, ...extraArgs], {
      cwd: raiz,
      stdio: "inherit",
    });
    process.exit(r.status ?? 0);
  } else if (fs.existsSync(ps1Path)) {
    // Fallback se pwsh estiver instalado no Linux
    const r = spawnSync("pwsh", ["-File", ps1Path, ...extraArgs], {
      cwd: raiz,
      stdio: "inherit",
    });
    process.exit(r.status ?? 0);
  }
}

console.error(`[run-script] Script não encontrado: ${target} (esperava ${ps1Path} ou ${shPath})`);
process.exit(1);
