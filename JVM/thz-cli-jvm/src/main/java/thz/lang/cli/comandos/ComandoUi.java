package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.interpretador.InterpretadorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.ErrosCli;
import thz.lang.cli.ThzCli;

/**
 * Comando para exibir a interface gráfica de um programa Thz.
 * @implNote Este comando tenta exibir a interface gráfica usando Swing, se disponível. Caso contrário, ele gera uma versão HTML da interface e a abre em um navegador ou WebView.
 */
public class ComandoUi implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("ui");
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

        boolean html = argumentos.contains("--html") || argumentos.contains("--web") || argumentos.contains("--webview");
        boolean swing = argumentos.contains("--swing") || argumentos.contains("--gui");

        if (swing || (!html && !java.awt.GraphicsEnvironment.isHeadless())) {
            try {
                boolean precisaEntrada = CliHelper.precisaEntrada(ast);
                java.util.function.Supplier<String> entrada = precisaEntrada ? CliHelper.criarLeitorEntrada() : null;
                InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);
                Object frame = thz.lang.gui.ui.ThzUiSwingRenderer.renderizarOuExibir(ast, interp);
                if (frame instanceof javax.swing.JFrame jf) {
                    System.out.println("[THZ-UI SWING] Interface gráfica declarativa '" + ast.nome() + "' exibida com sucesso.");
                    if (!Boolean.getBoolean("thz.test.mode")) {
                        synchronized (ThzCli.class) {
                            while (jf.isDisplayable()) {
                                try {
                                    ThzCli.class.wait(1000);
                                } catch (InterruptedException ignore) {
                                    break;
                                }
                            }
                        }
                    }
                    return;
                }
            } catch (Throwable t) {
                ErrosCli.displayIndisponivel(t.getMessage(), "HTML/Web");
            }
        }

        var maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
            c.adicionar(thz.lang.ui.ThzUiMaker.card("card_" + ast.nome(), ast.nome(), card -> {
                card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_modulo", "info",
                        "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "]"));
                if (ast.procedimentos() != null) {
                    for (var p : ast.procedimentos()) {
                        card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_" + p.nome(), p.nome(), p.nome()));
                    }
                }
            }));
        });
        if (html) {
            String codigoHtml = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
            String url = thz.lang.webview.LancadorWebviewNativo.abrirHtml("THZ-UI: " + ast.nome(), codigoHtml, 1024, 768);
            System.out.println("[THZ-UI WEB] Interface '" + ast.nome() + "' aberta via Web / WebView em: " + url);
        } else {
            System.out.println(maker.gerarCodigoThz(ast.nome()));
        }
    }
}
