plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "thz-lang"

includeBuild("../thz-core-jvm")
includeBuild("../thz-cli-jvm")
includeBuild("../thz-gui-jvm")
includeBuild("../thz-api-jvm")
includeBuild("../thz-lsp-jvm")
includeBuild("../thz-bench-jvm")
includeBuild("../thz-agent-jvm")
