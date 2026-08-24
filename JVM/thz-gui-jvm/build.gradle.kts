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

// Task para iniciar a IDE Desktop
tasks.register<JavaExec>("gui") {
    group = "application"
    description = "Inicia a IDE Desktop Swing do THZ-LANG"
    mainClass.set("thz.lang.gui.ThzGui")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    jvmArgs("-Dfile.encoding=UTF-8")
}
