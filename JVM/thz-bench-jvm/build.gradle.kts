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

dependencies {
    jmhImplementation("thz.lang:thz-core:2.3.3")
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
