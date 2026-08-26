package thz.lang.gui.webview;

import thz.lang.webview.ThzWebViewLauncher;
import thz.lang.webview.ThzWebViewBridge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ThzNativeWebView — Host de janela gráfica nativa para WebView em Windows, Linux e macOS.
 * Compatível com GraalVM Native Image e execução HotSpot.
 */
public final class ThzNativeWebView {

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

    private ThzNativeWebView() {}

    public static JanelaConfig getConfigAtiva() {
        return configAtiva;
    }

    public static synchronized void abrir(JanelaConfig config) {
        configAtiva = config;
        String url;

        if (config.urlOuHtml().startsWith("http://") || config.urlOuHtml().startsWith("https://")) {
            url = config.urlOuHtml();
        } else {
            url = ThzWebViewBridge.iniciar(config.urlOuHtml()) >= 0 ? ThzWebViewBridge.getUrl() : config.urlOuHtml();
        }

        ThzWebViewLauncher.JanelaConfig coreCfg = new ThzWebViewLauncher.JanelaConfig(config.titulo(), url, config.largura(), config.altura());
        if (!lancarNativo(url, config)) {
            ThzWebViewLauncher.abrir(coreCfg);
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
        ThzWebViewBridge.parar();
        configAtiva = null;
    }

    public static boolean estaAberta() {
        return processoNativo != null && processoNativo.isAlive();
    }

    private static boolean lancarNativo(String url, JanelaConfig config) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> cmd = new ArrayList<>();

        if (os.contains("win")) {
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
            String chromeMac = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            if (new File(chromeMac).exists()) {
                cmd.add(chromeMac);
                cmd.add("--app=" + url);
                cmd.add("--window-size=" + config.largura() + "," + config.altura());
            }
        } else {
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
        try {
            thz.lang.webview.ThzWebViewLauncher.abrir(
                    new thz.lang.webview.ThzWebViewLauncher.JanelaConfig("THZ", url, 1024, 768));
        } catch (Exception ignored) {}
    }
}
