package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.ErrosCli;

public class ComandoDoc implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("doc");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank() || !Files.exists(Path.of(arquivo))) {
            ErrosCli.erroArquivoNaoEncontrado(arquivo);
        }
        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

        String idxSaida = null;
        int idx = argumentos.indexOf("--saida");
        if (idx >= 0 && idx + 1 < argumentos.size())
            idxSaida = argumentos.get(idx + 1);

        String doc = thz.lang.docgen.ThzDocGen.gerarDocumentacao(ast);
        if (idxSaida != null) {
            Path alvo = idxSaida.contains(".") ? Path.of(idxSaida)
                    : Path.of(idxSaida, ast.nome() + "_documentacao.md");
            Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
            Files.writeString(alvo, doc, StandardCharsets.UTF_8);
            System.out.println("[THZ DOC] Documentação gerada em: " + alvo);
        } else {
            System.out.println(doc);
        }
    }
}
