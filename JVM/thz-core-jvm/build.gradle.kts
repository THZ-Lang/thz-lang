// ==============================================================================
// thz-core — Núcleo da linguagem THZ-LANG (Core/Stdlib)
//
// Repositório independente: https://github.com/<org>/thz-core
// Consumido por thz-cli e thz-gui via Gradle Composite Build (dev) ou
// artefato publicado (CI): thz.lang:thz-core:<versão>
// ==============================================================================

plugins {
    `java-library`
    `maven-publish`
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
    withSourcesJar()
}

dependencies {
    // Motor de Documentos (PDF, XLSX, DOCX) — capacidade de stdlib DOCUMENTO.*
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

    // Testes Automatizados
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("THZ-LANG Core")
                description.set("Núcleo da linguagem THZ-LANG: lexer, parser, semântico, interpretador, runtime decimal exato, SIMD, IR e governança.")
            }
        }
    }
}
