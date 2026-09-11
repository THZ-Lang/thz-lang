package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.CliLogger;
import thz.lang.cli.CliErros;

public class ComandoIr implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("ir");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank() || !Files.exists(Path.of(arquivo))) {
            CliErros.erroArquivoNaoEncontrado(arquivo);
        }
        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        var analise = thz.lang.fachada.ThzCompilerFacade.analisar(fonte, estrito);
        if (analise.temErros()) {
            for (String bloco : analise.textoDiagnosticos()) {
                CliLogger.erro(bloco + "\n");
            }
            CliErros.statusComandoCheck(analise.diagnosticos().size());
            if (Boolean.getBoolean("thz.test.mode")) {
                throw new IllegalStateException("Geração de IR abortada: " + analise.diagnosticos().size() + " erro(s) encontrado(s).");
            }
            System.exit(1);
        }

        ProgramaAst ast = analise.ast();

        boolean llvm = argumentos.contains("--llvm");
        String idxSaida = null;
        int idx = argumentos.indexOf("--saida");
        if (idx >= 0 && idx + 1 < argumentos.size())
            idxSaida = argumentos.get(idx + 1);

        String resultado = llvm
                ? thz.lang.ir.GeradorIr.emitirLlvm(ast)
                : thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast));

        if (idxSaida != null) {
            Path alvo = idxSaida.contains(".") ? Path.of(idxSaida)
                    : Path.of(idxSaida, ast.nome() + (llvm ? ".ll" : "_ir.json"));
            Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
            Files.writeString(alvo, resultado, StandardCharsets.UTF_8);
            CliLogger.info("[THZ IR] Saída (" + (llvm ? "LLVM IR" : "THZ-IR/1") + ") gravada em: " + alvo);
        } else {
            CliLogger.saida(resultado);
        }
    }
}
