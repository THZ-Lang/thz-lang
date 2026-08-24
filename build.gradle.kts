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

tasks.register("jmh") {
    group = "benchmarks"
    description = "Executa benchmarks JMH do thz-bench-jvm"
    dependsOn(gradle.includedBuilds.map { it.task(":jmh") })
}
