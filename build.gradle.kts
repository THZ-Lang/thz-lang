plugins {
    base
}

fun aggregate(name: String, group: String) {
    val includedTasks = gradle.includedBuilds.map { it.task(":$name") }
    tasks.findByName(name)?.dependsOn(includedTasks)
        ?: tasks.register(name) {
            this.group = group
            dependsOn(includedTasks)
        }
}

val sincronizarVersaoTask = tasks.register("sincronizarVersao") {
    group = "versioning"
    description = "Sincroniza a versão de version.txt em todos os manifestos e arquivos do ecossistema"
    val projectDir = layout.projectDirectory.asFile
    val vFile = projectDir.resolve("version.txt")
    inputs.file(vFile)

    doLast {
        if (!vFile.exists()) return@doLast
        val versao = vFile.readText().trim()
        if (versao.isEmpty()) return@doLast

        // 1. thz.config.json
        val cfg = projectDir.resolve("thz.config.json")
        if (cfg.exists()) {
            val t = cfg.readText().replace(Regex("\"versao\":\\s*\"[^\"]+\""), "\"versao\": \"$versao\"")
            cfg.writeText(t)
        }

        // 2. Cargo.toml
        val cargo = listOf(projectDir.resolve("src/runtime_rs/Cargo.toml"), projectDir.resolve("../thz-runtime-rs/Cargo.toml")).firstOrNull { it.exists() }
        if (cargo != null && cargo.exists()) {
            val t = cargo.readText().replace(Regex("(?m)^version\\s*=\\s*\"[^\"]+\""), "version = \"$versao\"")
            cargo.writeText(t)
        }

        // 3. wasm.rs
        val wasm = listOf(projectDir.resolve("src/runtime_rs/src/wasm.rs"), projectDir.resolve("../thz-runtime-rs/src/wasm.rs")).firstOrNull { it.exists() }
        if (wasm != null && wasm.exists()) {
            val t = wasm.readText().replace(Regex("CString::new\\(\"[^\"]+-WASM\"\\)"), "CString::new(\"$versao-WASM\")")
            wasm.writeText(t)
        }

        // 4. package.json
        val pkg = listOf(projectDir.resolve("Extensions/thz-lsp-vscode/package.json"), projectDir.resolve("../thz-vscode/package.json")).firstOrNull { it.exists() }
        if (pkg != null && pkg.exists()) {
            val t = pkg.readText().replace(Regex("\"version\":\\s*\"[^\"]+\""), "\"version\": \"$versao\"")
            pkg.writeText(t)
        }

        // 5. extension.ts
        val extTs = listOf(projectDir.resolve("Extensions/thz-lsp-vscode/src/extension.ts"), projectDir.resolve("../thz-vscode/src/extension.ts")).firstOrNull { it.exists() }
        if (extTs != null && extTs.exists()) {
            val t = extTs.readText().replace(Regex("'THZ-LANG \\d+\\.\\d+\\.\\d+'"), "'THZ-LANG $versao'")
            extTs.writeText(t)
        }

        // 6. README.md
        val readme = projectDir.resolve("README.md")
        if (readme.exists()) {
            val t = readme.readText().replace(Regex("badge/version-\\d+\\.\\d+\\.\\d+-blue\\.svg"), "badge/version-$versao-blue.svg")
            readme.writeText(t)
        }

        // 7. version.txt em módulos irmãos
        projectDir.parentFile?.listFiles()?.filter { it.isDirectory && it.name.startsWith("thz-") }?.forEach { sub ->
            val vf = sub.resolve("version.txt")
            if (vf.exists()) {
                vf.writeText("$versao\n")
            }
        }

        logger.lifecycle(">> Sincronização automática global para v$versao concluída.")
    }
}

aggregate("assemble", "build")
aggregate("check", "verification")
aggregate("test", "verification")
aggregate("clean", "build")

tasks.named("assemble") {
    dependsOn(sincronizarVersaoTask)
}
tasks.named("check") {
    dependsOn(sincronizarVersaoTask)
}

// Tarefa CLI
tasks.register("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    dependsOn(gradle.includedBuild("thz-cli-jvm").task(":cli"))
}

// Tarefa GUI
tasks.register("gui") {
    group = "application"
    description = "Executa a Desktop IDE do THZ-LANG"
    dependsOn(gradle.includedBuild("thz-gui-jvm").task(":gui"))
}

// Tarefa Benchmarks
tasks.register("jmh") {
    group = "benchmarks"
    description = "Executa benchmarks JMH do thz-bench-jvm"
    dependsOn(gradle.includedBuilds.map { it.task(":jmh") })
}

// Tarefa Livro-Manual PDF
tasks.register<JavaExec>("livro") {
    group = "documentation"
    description = "Compila todos os arquivos Markdown (.md) em Livro-Manual PDF"
    mainClass.set("thz.lang.cli.ThzCli")
    classpath = gradle.includedBuild("thz-cli-jvm").projectDir.resolve("build/classes/java/main").let {
        files(
            it,
            gradle.includedBuild("thz-core-jvm").projectDir.resolve("build/classes/java/main"),
            gradle.includedBuild("thz-gui-jvm").projectDir.resolve("build/classes/java/main")
        )
    }
    args = listOf("livro", "--saida", "dist/MANUAL_THZ_LANG.pdf")
    workingDir = rootDir
    dependsOn(
        gradle.includedBuild("thz-cli-jvm").task(":classes"),
        gradle.includedBuild("thz-core-jvm").task(":classes"),
        gradle.includedBuild("thz-gui-jvm").task(":classes")
    )
}

tasks.register("manual") {
    group = "documentation"
    description = "Alias para tarefa livro"
    dependsOn(tasks.named("livro"))
}

