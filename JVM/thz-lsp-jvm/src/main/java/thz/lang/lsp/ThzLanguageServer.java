package thz.lang.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.launch.LSPLauncher;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * THZ-LANG LSP Server — stdio mode.
 * Substitui o servidor LSP Node.js (vscode-languageserver).
 *
 * Uso: java -jar thz-lsp.jar --stdio
 */
public class ThzLanguageServer {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        ThzLanguageServerImpl server = new ThzLanguageServerImpl();

        InputStream in = System.in;
        OutputStream out = System.out;

        // LSPLauncher.createServerLauncher retorna Launcher<LanguageClient>
        // O proxy remoto é o LanguageClient que usamos para enviar notificações
        var launcher = LSPLauncher.createServerLauncher(
                server, in, out, executor, consumer -> null
        );

        var remoteProxy = launcher.getRemoteProxy();
        server.connect(remoteProxy);

        launcher.startListening();
    }
}
