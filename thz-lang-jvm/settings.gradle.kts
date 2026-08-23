rootProject.name = "thz-lang-jvm"

// Motor JVM da THZ-LANG — três módulos autônomos que comunicam entre si:
//  - thz-core: Core/Stdlib da linguagem (léxico, parser, semântico, interpretador,
//              runtime, SIMD, IR, governança, documentos) + maven-publish
//  - thz-cli : Ponto de entrada de linha de comando e REPL (consome thz-core)
//  - thz-gui : IDE Desktop Swing (consome thz-core e registra as funções TELA.*)
include("thz-core", "thz-cli", "thz-gui")
