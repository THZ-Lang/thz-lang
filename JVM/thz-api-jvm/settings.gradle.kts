plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "thz-api-jvm"

includeBuild("../thz-core-jvm")
