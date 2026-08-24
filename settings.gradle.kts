plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "thz-lang"

includeBuild("JVM/thz-core-jvm")
includeBuild("JVM/thz-cli-jvm")
includeBuild("JVM/thz-gui-jvm")
includeBuild("JVM/thz-api-jvm")
includeBuild("JVM/thz-lsp-jvm")
includeBuild("JVM/thz-bench-jvm")
