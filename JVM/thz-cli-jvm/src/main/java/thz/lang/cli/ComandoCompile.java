package thz.lang.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;

public class ComandoCompile implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("compile", "compilar", "build");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank()) {
            System.err.println("[ERRO] Nenhum arquivo .thz ou .thzui especificado. Use: thz compile <arquivo.thz>");
            System.exit(1);
        }

        var resolved = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(arquivo, Path.of("."), List.of(".thz", ".thzui"));
        if (resolved.isPresent()) arquivo = resolved.get().toString();

        if (!Files.exists(Path.of(arquivo))) {
            System.err.println("[ERRO] Arquivo não encontrado após pesquisa recursiva: " + arquivo);
            System.exit(1);
        }

        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();
        String nomeBase = Path.of(arquivo).getFileName().toString().replace(".thz", "");

        String dirSaida = "dist/exemplos_compilados";
        int idxSaida = argumentos.indexOf("--saida");
        if (idxSaida >= 0 && idxSaida + 1 < argumentos.size()) dirSaida = argumentos.get(idxSaida + 1);
        Path raizSaida = Path.of(dirSaida);

        Path dirIr = raizSaida.resolve("ir");
        Path dirLlvm = raizSaida.resolve("llvm");
        Path dirWasm = raizSaida.resolve("wasm");
        Files.createDirectories(dirIr);
        Files.createDirectories(dirLlvm);
        Files.createDirectories(dirWasm);

        Files.writeString(dirIr.resolve(nomeBase + "_ir.json"), thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast)), StandardCharsets.UTF_8);
        Files.writeString(dirLlvm.resolve(nomeBase + ".ll"), thz.lang.ir.GeradorIr.emitirLlvm(ast), StandardCharsets.UTF_8);
        Files.writeString(dirWasm.resolve(nomeBase + ".wasm.js"), "// THZ-LANG v3.0.0 WASM\n" + thz.lang.js.ThzJsEmitter.emitir(ast), StandardCharsets.UTF_8);

        System.out.println("[THZ COMPILE] " + arquivo + " compilado com sucesso para IR, LLVM e WASM em: " + raizSaida.toAbsolutePath());
    }
}
