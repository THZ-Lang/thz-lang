plugins {
    java
    application
    eclipse
    idea
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
    // UI Desktop & Tema
    implementation("com.formdev:flatlaf:3.5.4")

    // Motor de Documentos (PDF, XLSX, DOCX)
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

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
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.shadowJar {
    archiveBaseName.set("thz-jvm")
    archiveClassifier.set("")
    archiveVersion.set("2.3.0")
    
    // Copia o JAR gerado também para target/ para manter compatibilidade com scripts existentes
    doLast {
        val targetDir = file("target")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        archiveFile.get().asFile.copyTo(file("target/thz-jvm-2.3.0.jar"), overwrite = true)
    }
}

// Task para executar a IDE Desktop
tasks.register<JavaExec>("gui") {
    group = "application"
    description = "Inicia a IDE Desktop Swing do THZ-LANG"
    mainClass.set("thz.lang.gui.ThzGui")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs("-Dfile.encoding=UTF-8")
}

// Task para executar a CLI
tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    mainClass.set("thz.lang.cli.ThzCli")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs("-Dfile.encoding=UTF-8")
}
