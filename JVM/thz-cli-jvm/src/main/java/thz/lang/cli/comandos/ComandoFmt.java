package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import thz.lang.formato.Formatador;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.CliLogger;
import thz.lang.cli.CliErros;

public class ComandoFmt implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("fmt");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank() || !Files.exists(Path.of(arquivo))) {
            CliErros.erroArquivoNaoEncontrado(arquivo);
        }
        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

        boolean check = argumentos.contains("--check");
        boolean escrever = argumentos.contains("--escrever") || argumentos.contains("-w");
        String idxSaida = null;
        int idx = argumentos.indexOf("--saida");
        if (idx >= 0 && idx + 1 < argumentos.size())
            idxSaida = argumentos.get(idx + 1);
        String formatado = Formatador.formatar(ast);
        if (check) {
            if (!fonte.equals(formatado)) {
                CliErros.statusFmtNaoFormatado();
                String[] a = fonte.split("\n", -1);
                String[] b = formatado.split("\n", -1);
                for (int i = 0; i < Math.max(a.length, b.length); i++)
                    if (!Objects.equals(i < a.length ? a[i] : null, i < b.length ? b[i] : null)) {
                        CliErros.linhaDiferenca(i + 1,
                                b.length > i ? b[i] : "",
                                a.length > i ? a[i] : "");
                        break;
                    }
                System.exit(1);
            }
            CliLogger.info("[THZ FMT] OK — arquivo já está canônico.");
            return;
        }
        if (idxSaida != null) {
            Path alvo = idxSaida.contains(".thz") ? Path.of(idxSaida)
                    : Path.of(idxSaida, Path.of(arquivo).getFileName().toString());
            Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
            Files.writeString(alvo, formatado, StandardCharsets.UTF_8);
            CliLogger.info("[THZ FMT] Arquivo formatado gravado em: " + alvo);
            return;
        }
        if (escrever) {
            Files.writeString(Path.of(arquivo), formatado, StandardCharsets.UTF_8);
            CliLogger.info("[THZ FMT] " + arquivo + " formatado.");
            return;
        }
        CliLogger.saida(formatado);
    }
}
