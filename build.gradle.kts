plugins {
    base
}

fun aggregate(name: String, group: String) {
    val includedTasks = gradle.includedBuilds.map { it.task(":$name") }
    tasks.findByName(name)?.dependsOn(includedTasks)
        ?: tasks.register(name) {
            this.group = group
            dependsOn(includedTasks)
        }
}

aggregate("assemble", "build")
aggregate("check", "verification")
aggregate("test", "verification")
aggregate("clean", "build")

// Tarefa CLI
tasks.register("cli") {
    group = "application"
    description = "Executa a CLI do THZ-LANG"
    dependsOn(gradle.includedBuild("thz-cli-jvm").task(":cli"))
}

// Tarefa GUI
tasks.register("gui") {
    group = "application"
    description = "Executa a Desktop IDE do THZ-LANG"
    dependsOn(gradle.includedBuild("thz-gui-jvm").task(":gui"))
}

// Tarefa Benchmarks
tasks.register("jmh") {
    group = "benchmarks"
    description = "Executa benchmarks JMH do thz-bench-jvm"
    dependsOn(gradle.includedBuilds.map { it.task(":jmh") })
}

