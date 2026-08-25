plugins {
    java
    id("me.champeau.jmh") version "0.7.3"
}

repositories { mavenCentral() }

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val repoRoot = rootProject.projectDir.resolve("../../")
val versionFile = if (file("version.txt").exists()) file("version.txt") else repoRoot.resolve("version.txt")
val thzVersion = if (versionFile.exists()) versionFile.readText().trim() else "2.4.0"

group = "thz.lang"
version = thzVersion

dependencies {
    jmhImplementation("thz.lang:thz-core:$thzVersion")
    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    jmhVersion.set("1.37")
    fork.set(1)
    warmupIterations.set(3)
    iterations.set(5)
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
}
