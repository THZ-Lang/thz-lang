package thz.lang.cli;

import thz.lang.net.ThzEmbeddedWebServer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Servidor de Desenvolvimento em Tempo Real (Live Reload & Native Dev Server).
 * Utiliza ThzEmbeddedWebServer com Virtual Threads (JVM 25) para servir interfaces .thzui
 * com auto-reload, RPC bidirecional e reatividade granular.
 */
public final class ThzDevServer {

    private static ThzEmbeddedWebServer servidorAtivo;

    public static synchronized void iniciar(String caminhoArquivo, int porta) throws Exception {
        iniciar(caminhoArquivo, porta, false, false);
    }

    public static synchronized void iniciar(String caminhoArquivo, int porta, boolean abrirNavegador) throws Exception {
        iniciar(caminhoArquivo, porta, abrirNavegador, false);
    }

    public static synchronized void iniciar(String caminhoArquivo, int porta, boolean abrirNavegador, boolean usarVaadin) throws Exception {
        Path path = Path.of(caminhoArquivo);
        if (!Files.exists(path)) {
            System.err.println("[THZ DEV] Arquivo não encontrado: " + caminhoArquivo);
            return;
        }

        if (servidorAtivo != null && servidorAtivo.estaRodando()) {
            servidorAtivo.parar();
        }

        System.out.println("================================================================================");
        System.out.println("   ⚡ SERVIDOR WEB EMBUTIDO THZ-LANG (JAVA 25 VIRTUAL THREADS)" + (usarVaadin ? " [VAADIN FLOW]" : ""));
        System.out.println("================================================================================\n");

        servidorAtivo = new ThzEmbeddedWebServer();
        var config = new ThzEmbeddedWebServer.ConfiguracaoServidor(
                porta > 0 ? porta : 8080,
                "0.0.0.0",
                true,
                abrirNavegador,
                thz.lang.ui.ThzUiTema.escuroGlass(),
                usarVaadin
        );

        String url = servidorAtivo.iniciar(path, config);

        System.out.println("🚀 [THZ EMBEDDED] Servidor pronto e escutando!");
        System.out.println("🔗 [THZ EMBEDDED] Acesso direto: " + url);
        System.out.println("📡 [THZ EMBEDDED] Health Check:  " + url + "api/health");
        System.out.println("📦 [THZ EMBEDDED] RPC Endpoint:  " + url + "api/rpc/invocar");
        System.out.println("\nPressione Ctrl+C para encerrar o servidor.");
    }

    public static synchronized void parar() {
        if (servidorAtivo != null) {
            servidorAtivo.parar();
            servidorAtivo = null;
        }
    }

    public static ThzEmbeddedWebServer getServidorAtivo() {
        return servidorAtivo;
    }
}
