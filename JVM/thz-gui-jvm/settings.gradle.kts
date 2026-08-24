rootProject.name = "thz-gui-jvm"

// Composite Build: em desenvolvimento, resolve thz.lang:thz-core direto do
// núcleo vizinho (../thz-core-jvm), sem precisar publicar no Maven Local.
// Em CI/consumidores externos, use `publishToMavenLocal` no motor e a
// dependência declarada será baixada como artefato.
includeBuild("../thz-core-jvm")
