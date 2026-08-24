// ==============================================================================
// thz-lsp-jvm — LSP Server do THZ-LANG (LSP4J)
//
// Servidor LSP via stdio, alimentado diretamente pelo thz-core-jvm.
// Substitui o servidor LSP Node.js (vscode-languageserver).
// ==============================================================================

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.1"
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

    // LSP4J — implementação Java do Language Server Protocol
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.21.1")

    // Gson (usado pelo LSP4J para serialização JSON)
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("thz.lang.lsp.ThzLanguageServer")
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

tasks.shadowJar {
    archiveBaseName.set("thz-lsp")
    archiveClassifier.set("")
    archiveVersion.set("2.3.0")
}
