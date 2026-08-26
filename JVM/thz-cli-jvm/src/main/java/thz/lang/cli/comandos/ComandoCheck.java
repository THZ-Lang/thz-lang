package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.ErrosCli;

public class ComandoCheck implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("check");
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

        List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
        if (!erros.isEmpty()) {
            List<DiagnosticoEntrada> diags = erros.stream()
                    .map(e -> new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
            for (String bloco : Diagnosticos.formatarDiagnosticos(fonte, diags, "Semântico"))
                System.err.println(bloco + "\n");
            ErrosCli.statusComandoCheck(erros.size());
            System.exit(1);
        }
        String versao = "";
        System.out.println("[THZ CHECK] Código validado com sucesso! AST íntegra para o programa: "
                + ast.nome() + versao + (estrito ? " [lint estrito aprovado]" : ""));
    }
}
