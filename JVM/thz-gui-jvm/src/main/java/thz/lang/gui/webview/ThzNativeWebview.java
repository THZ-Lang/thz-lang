package thz.lang.gui.webview;

import thz.lang.webview.LancadorWebviewNativo;
import thz.lang.webview.ThzWebviewBridge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ThzNativeWebview — Host de janela gráfica nativa para WebView em Windows, Linux e macOS.
 * Compatível com GraalVM Native Image e execução HotSpot.
 */
public final class ThzNativeWebview {

    public record JanelaConfig(
            String titulo,
            String urlOuHtml,
            int largura,
            int altura,
            boolean frameless,
            boolean telaCheia
    ) {
        public static JanelaConfig padrao(String titulo, String urlOuHtml) {
            return new JanelaConfig(titulo, urlOuHtml, 1024, 768, false, false);
        }
    }

    private static Process processoNativo = null;
    private static JanelaConfig configAtiva = null;

    private ThzNativeWebview() {}

    public static synchronized void abrir(JanelaConfig config) {
        configAtiva = config;
        String url;

        if (config.urlOuHtml().startsWith("http://") || config.urlOuHtml().startsWith("https://")) {
            url = config.urlOuHtml();
        } else {
            url = ThzWebviewBridge.iniciar(config.urlOuHtml()) >= 0 ? ThzWebviewBridge.getUrl() : config.urlOuHtml();
        }

        // Delega para launcher do core (sem AWT); fallback interno já usa rundll32/xdg-open
        LancadorWebviewNativo.JanelaConfig coreCfg = new LancadorWebviewNativo.JanelaConfig(config.titulo(), url, config.largura(), config.altura());
        // Tenta app-mode; se falhar, LancadorWebviewNativo já faz fallback
        if (!lancarNativo(url, config)) {
            LancadorWebviewNativo.abrir(coreCfg);
        }
    }

    public static synchronized void abrirHtml(String titulo, String html, int largura, int altura) {
        abrir(new JanelaConfig(titulo, html, largura, altura, false, false));
    }

    public static synchronized void abrirUrl(String titulo, String url, int largura, int altura) {
        abrir(new JanelaConfig(titulo, url, largura, altura, false, false));
    }

    public static synchronized void fechar() {
        if (processoNativo != null && processoNativo.isAlive()) {
            processoNativo.destroyForcibly();
            processoNativo = null;
        }
        ThzWebviewBridge.parar();
        configAtiva = null;
    }

    public static boolean estaAberta() {
        return processoNativo != null && processoNativo.isAlive();
    }

    private static boolean lancarNativo(String url, JanelaConfig config) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> cmd = new ArrayList<>();

        if (os.contains("win")) {
            // Windows: Microsoft Edge ou Google Chrome em App Mode (janela isolada sem abas/URL bar)
            String[] caminhosWin = {
                    System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getenv("ProgramFiles") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getenv("LocalAppData") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe"
            };

            for (String caminho : caminhosWin) {
                if (caminho != null && new File(caminho).exists()) {
                    cmd.add(caminho);
                    cmd.add("--app=" + url);
                    cmd.add("--window-size=" + config.largura() + "," + config.altura());
                    cmd.add("--window-name=" + config.titulo());
                    cmd.add("--user-data-dir=" + System.getProperty("java.io.tmpdir") + "\\thz_webview_profile");
                    break;
                }
            }
        } else if (os.contains("mac")) {
            // macOS: Safari ou Chrome App Mode
            String chromeMac = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            if (new File(chromeMac).exists()) {
                cmd.add(chromeMac);
                cmd.add("--app=" + url);
                cmd.add("--window-size=" + config.largura() + "," + config.altura());
            }
        } else {
            // Linux: Chromium, Google Chrome ou WebKit
            String[] binsLinux = {"chromium", "chromium-browser", "google-chrome", "google-chrome-stable"};
            for (String b : binsLinux) {
                cmd.add(b);
                cmd.add("--app=" + url);
                cmd.add("--window-size=" + config.largura() + "," + config.altura());
                break;
            }
        }

        if (cmd.isEmpty()) return false;

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            processoNativo = pb.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private static void abrirNavegadorPadrao(String url) {
        // Delegado para LancadorWebviewNativo (sem AWT). Mantido para compatibilidade.
        try {
            thz.lang.webview.LancadorWebviewNativo.abrir(
                    new thz.lang.webview.LancadorWebviewNativo.JanelaConfig("THZ", url, 1024, 768));
        } catch (Exception ignored) {}
    }
}
