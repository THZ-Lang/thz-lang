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
    id("com.gradleup.shadow") version "9.6.1"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

val repoRoot = rootProject.projectDir.resolve("../../")
val versionFile = if (file("version.txt").exists()) file("version.txt") else repoRoot.resolve("version.txt")
val thzVersion = if (versionFile.exists()) versionFile.readText().trim() else "2.4.0"

group = "thz.lang"
version = thzVersion

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation("thz.lang:thz-core:$thzVersion")
    implementation("thz.lang:thz-gui-jvm:$thzVersion")

    // Testes Automatizados
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("thz.lang.cli.ThzCli")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("thz")
            mainClass.set("thz.lang.cli.ThzCli")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
                "--enable-http",
                "--enable-https",
                "--initialize-at-build-time=thz.lang.ui, thz.lang.webview, thz.lang.interpretador, thz.lang.lexico, thz.lang.sintatico, thz.lang.semantico",
                "-H:IncludeResources=.*\\.thz.*",
                "-H:Log=registerResource:"
            )
            // Recursos para HttpServer + ThzUiHtmlEmitter + LancadorWebviewNativo
        }
    }
    metadataRepository {
        enabled.set(true)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
    // Testes leem exemplos/ relativo à raiz do workspace (thz-lang)
    workingDir = rootProject.projectDir.resolve("../../")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Copia o UberJAR para target/ e dist/bin na raiz do workspace (compatibilidade universal)
tasks.register<Copy>("instalarUberJar") {
    group = "build"
    description = "Copia o UberJAR para target/ na raiz do workspace"
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(rootProject.projectDir.resolve("../../target"))
    rename { "thz-jvm.jar" }
}

tasks.register<Copy>("instalarUberJarVersao") {
    group = "build"
    description = "Copia o UberJAR versionado para target/"
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(rootProject.projectDir.resolve("../../target"))
}

tasks.shadowJar {
    archiveBaseName.set("thz-jvm")
    archiveClassifier.set("")
    archiveVersion.set(thzVersion)

    finalizedBy("instalarUberJar", "instalarUberJarVersao")
}

// Task para executar a CLI — workingDir = raiz do workspace para resolver exemplos/*.thz
tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    mainClass.set("thz.lang.cli.ThzCli")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir.resolve("../../")
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-Dnative.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir.resolve("../../")
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-Dnative.encoding=UTF-8")
}
