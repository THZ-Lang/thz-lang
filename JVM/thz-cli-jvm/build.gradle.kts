// ==============================================================================
// thz-cli — CLI e REPL do THZ-LANG
//
// Projeto Gradle autônomo na raiz do workspace. Consome o núcleo thz-core
// (../thz-core-jvm) via Composite Build ou artefato publicado. Registra as funções
// TELA.* em modo console (não interativo). Gera o UberJAR executável usado
// pelos scripts de empacotamento e pelo GraalVM native-image.
// ==============================================================================

plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "thz.lang"
version = "2.3.3"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation("thz.lang:thz-core:2.3.3")

    // Testes Automatizados
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("thz.lang.cli.ThzCli")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
    // Testes leem exemplos/ relativo à raiz do motor JVM
    workingDir = rootProject.projectDir
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Copia o UberJAR para target/ na raiz (compatibilidade com scripts e jpackage).
// Task Copy dedicada em vez de doLast: roda mesmo quando shadowJar está UP-TO-DATE.
tasks.register<Copy>("instalarUberJar") {
    group = "build"
    description = "Copia o UberJAR para target/ na raiz do motor JVM"
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(rootProject.file("target"))
    rename { "thz-jvm-2.3.0.jar" }
}

tasks.shadowJar {
    archiveBaseName.set("thz-jvm")
    archiveClassifier.set("")
    archiveVersion.set("2.3.0")

    finalizedBy("instalarUberJar")
}

// Task para executar a CLI
tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    mainClass.set("thz.lang.cli.ThzCli")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    jvmArgs("-Dfile.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
