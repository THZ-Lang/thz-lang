package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.CliLogger;
import thz.lang.cli.CliErros;

public class ComandoCompile implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("compile", "compilar", "build");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank()) {
            CliErros.erroNenhumArquivoEspecificado("thz compile <arquivo.thz>");
        }

        var resolved = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(arquivo, Path.of("."), List.of(".thz", ".thzui"));
        if (resolved.isPresent()) arquivo = resolved.get().toString();

        if (!Files.exists(Path.of(arquivo))) {
            CliErros.erroArquivoNaoEncontradoAposBusca(arquivo);
        }

        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        var analise = thz.lang.fachada.ThzCompilerFacade.analisar(fonte, estrito);
        if (analise.temErros()) {
            for (String bloco : analise.textoDiagnosticos()) {
                CliLogger.erro(bloco + "\n");
            }
            CliErros.statusComandoCheck(analise.diagnosticos().size());
            if (Boolean.getBoolean("thz.test.mode")) {
                throw new IllegalStateException("Compilação abortada: " + analise.diagnosticos().size() + " erro(s) encontrado(s).");
            }
            System.exit(1);
        }

        ProgramaAst ast = analise.ast();
        String nomeBase = Path.of(arquivo).getFileName().toString().replace(".thz", "");

        String alvo = "todos";
        int idxAlvo = argumentos.indexOf("--alvo");
        if (idxAlvo >= 0 && idxAlvo + 1 < argumentos.size()) {
            alvo = argumentos.get(idxAlvo + 1).toLowerCase();
        }

        if ("wasm".equals(alvo) || "webassembly".equals(alvo)) {
            String msgWasm = "[Backend WebAssembly] O compilador JVM não gera bytecode binário WebAssembly (.wasm) diretamente. "
                    + "A compilação WASM requer o pipeline nativo Rust/AOT ou o emissor JavaScript (Alvo.JAVASCRIPT).";
            CliLogger.erro(msgWasm);
            if (Boolean.getBoolean("thz.test.mode")) {
                throw new IllegalStateException(msgWasm);
            }
            System.exit(1);
        }

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

        boolean compilarIr = alvo.equals("todos") || alvo.equals("ir");
        boolean compilarLlvm = alvo.equals("todos") || alvo.equals("llvm");
        boolean compilarJs = alvo.equals("todos") || alvo.equals("js") || alvo.equals("javascript");

        if (compilarIr) {
            Files.writeString(dirIr.resolve(nomeBase + "_ir.json"), thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast)), StandardCharsets.UTF_8);
        }

        if (compilarLlvm) {
            try {
                Files.writeString(dirLlvm.resolve(nomeBase + ".ll"), thz.lang.ir.GeradorIr.emitirLlvm(ast, estrito), StandardCharsets.UTF_8);
            } catch (thz.lang.ir.ErroEmissaoBackendException e) {
                CliLogger.erro("[THZ COMPILE] " + e.getMessage());
                if (Boolean.getBoolean("thz.test.mode")) {
                    throw e;
                }
                System.exit(1);
            }
        }

        if (compilarJs) {
            // Emite a ponte JavaScript (compatibilidade legada de arquivo wasm.js)
            Files.writeString(dirWasm.resolve(nomeBase + ".wasm.js"), "// THZ-LANG v3.0.0 JS Bridge (WASM requer pipeline nativo Rust/AOT)\n" + thz.lang.js.ThzJsEmitter.emitir(ast), StandardCharsets.UTF_8);
        }

        CliLogger.info("[THZ COMPILE] " + arquivo + " compilado com sucesso para os alvos solicitados em: " + raizSaida.toAbsolutePath());
    }
}
