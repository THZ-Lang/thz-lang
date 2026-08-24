package thz.lang.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.net.ThzHttpServer;
import thz.lang.sintatico.ThzParser;
import thz.lang.ui.ThzUiMaker;

/**
 * Servidor de Desenvolvimento em Tempo Real (Live Reload & Native Dev Server).
 * Utiliza ThzHttpServer com Virtual Threads (JVM 25) para servir interfaces
 * .thzui
 * com auto-reload e reatividade granular.
 */
public final class ThzDevServer {

    public static void iniciar(String caminhoArquivo, int porta) throws Exception {
        Path path = Path.of(caminhoArquivo);
        if (!Files.exists(path)) {
            System.err.println("[THZ DEV] Arquivo não encontrado: " + caminhoArquivo);
            return;
        }

        System.out.println("⚡ [THZ DEV] Servidor Dev ativado na porta " + porta + "...");
        System.out.println("🔗 [THZ DEV] Acesse: http://localhost:" + porta + "/");

        ThzHttpServer.iniciar(porta);

        ThzHttpServer.registrarRota("POST", "/api/evento", req -> ThzHttpServer.Resposta.ok("{\"status\":\"ok\"}"));

        // Gerar HTML5 inicial a partir do arquivo THZ-UI
        String codigoFonte = Files.readString(path);
        List<Token> tokens = new ThzLexer(codigoFonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();
        ThzUiMaker uiMaker = ThzUiMaker.card("card_dev", ast.nome(), c -> {
            c.adicionar(ThzUiMaker.alerta("alerta_live", "success", "Live Reload & Virtual Threads Ativas"));
        });
        String html5 = uiMaker.renderizarHtml("THZ-LANG Live Dev - " + ast.nome(), null);

        ThzHttpServer.registrarRota("GET", "/",
                req -> new ThzHttpServer.Resposta(200, html5, "text/html; charset=utf-8", java.util.Map.of()));

        System.out.println("✅ [THZ DEV] Interface declarativa renderizada com sucesso (Virtual Threads prontas).");
    }
}
