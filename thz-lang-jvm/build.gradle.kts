// ==============================================================================
// THZ-LANG ENGINE JVM — Build raiz (multi-módulo)
//
// Módulos autônomos que comunicam entre si através da API pública do thz-core:
//   thz-core -> biblioteca central (sem GUI, sem CLI)
//   thz-cli  -> aplicação de linha de comando + REPL
//   thz-gui  -> IDE Desktop Swing
// ==============================================================================

group = "thz.lang"
version = "2.3.3"

// Gera o UberJAR executável da CLI (mantém compatibilidade com scripts/ e jpackage)
tasks.register("shadowJar") {
    group = "build"
    description = "Gera o JAR shaded da CLI (target/thz-jvm-2.3.0.jar)"
    dependsOn(":thz-cli:shadowJar")
}

// Inicia a IDE Desktop Swing
tasks.register("gui") {
    group = "application"
    description = "Inicia a IDE Desktop Swing do THZ-LANG"
    dependsOn(":thz-gui:gui")
}

// Executa a CLI (use --args="check exemplos/faturamento.thz")
tasks.register("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    dependsOn(":thz-cli:run")
}
