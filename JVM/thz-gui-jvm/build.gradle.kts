// ==============================================================================
// thz-gui-jvm — IDE Desktop Swing do THZ-LANG
//
// Projeto Gradle autônomo na pasta JVM/ do workspace. Consome o núcleo thz-core
// (../thz-core-jvm) via Composite Build no dev local ou
// artefato publicado (thz.lang:thz-core) em CI.
// Registra as funções TELA.* (Swing) na stdlib via BibliotecaTela.registrar().
// ==============================================================================

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.1"
    id("org.graalvm.buildtools.native") version "0.10.2"
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

    // UI Desktop & Tema
    implementation("com.formdev:flatlaf:3.5.4")

    // Testes Automatizados
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("thz.lang.gui.ThzGui")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("thz-gui")
            mainClass.set("thz.lang.gui.ThzGui")
            buildArgs.addAll(
                "--no-fallback",
                "-Djava.awt.headless=false",
                "-H:+ReportExceptionStackTraces",
                "--enable-http",
                "--enable-https",
                "-H:IncludeResources=.*\\.thz.*|.*\\.properties|.*\\.png|.*\\.svg",
                "-H:Log=registerResource:"
            )
        }
    }
    agent {
        defaultMode.set("standard")
        // So ativa o agente de metadados automatico se a propriedade do projeto 'graalvmAgent' ou a variavel de ambiente 'GRAALVM_AGENT' estiver definida como 'true'
        enabled.set(
            project.hasProperty("graalvmAgent") && project.property("graalvmAgent").toString().toBoolean() ||
            System.getenv("GRAALVM_AGENT") == "true"
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Task para iniciar a IDE Desktop — workingDir = raiz do workspace
tasks.register<JavaExec>("gui") {
    group = "application"
    description = "Inicia a IDE Desktop Swing do THZ-LANG"
    mainClass.set("thz.lang.gui.ThzGui")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir.resolve("../../")
    jvmArgs("-Dfile.encoding=UTF-8", "-Djava.awt.headless=false")
}

// Task para executar a IDE com o GraalVM Tracing Agent e coletar metadados de AWT/FlatLaf
tasks.register<JavaExec>("guiColetarMetadadosAgente") {
    group = "native image"
    description = "Executa a IDE Swing com o GraalVM Tracing Agent para coletar metadados de AWT/Swing"
    mainClass.set("thz.lang.gui.ThzGui")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir.resolve("../../")
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Djava.awt.headless=false",
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/thz.lang/thz-gui"
    )
}

