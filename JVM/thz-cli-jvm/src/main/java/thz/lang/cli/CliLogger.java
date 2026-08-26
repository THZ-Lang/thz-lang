package thz.lang.cli;

import java.util.Map;
import java.util.function.Consumer;

import thz.lang.log.ThzLog;

/**
 * Logger adaptado para CLI — mantém formato legível [TAG] e usa ThzLog como backend.
 * Configura ThzLog com escritor human-readable e redireciona accordingly.
 */
public final class CliLogger {

    private static boolean inicializado = false;

    private CliLogger() {}

    /** Inicializa o ThzLog com formato legível para CLI. Chamar uma vez no início do main(). */
    public static void inicializar() {
        if (inicializado) return;
        inicializado = true;

        ThzLog.setEscritor(new Consumer<>() {
            @Override
            public void accept(String json) {
                String nivel = extrairNivel(json);
                String mensagem = extrairMensagem(json);
                if ("ERRO".equals(nivel) || "AVISO".equals(nivel)) {
                    System.err.println(mensagem);
                } else {
                    System.out.println(mensagem);
                }
            }
        });
        ThzLog.setNivelMinimo(ThzLog.Nivel.INFO);
        ThzLog.definirContexto("modulo", "cli");
    }

    // ── Métodos de conveniência (formato [TAG] legível) ──────────────────────

    public static void info(String mensagem) {
        inicializar();
        ThzLog.info(mensagem);
    }

    public static void aviso(String mensagem) {
        inicializar();
        ThzLog.aviso(mensagem);
    }

    public static void erro(String mensagem) {
        inicializar();
        ThzLog.erro(mensagem);
    }

    public static void info(String tag, String mensagem) {
        inicializar();
        ThzLog.info("[" + tag + "] " + mensagem);
    }

    public static void aviso(String tag, String mensagem) {
        inicializar();
        ThzLog.aviso("[" + tag + "] " + mensagem);
    }

    public static void erro(String tag, String mensagem) {
        inicializar();
        ThzLog.erro("[" + tag + "] " + mensagem);
    }

    public static void info(String tag, String mensagem, Map<String, Object> dados) {
        inicializar();
        ThzLog.info("[" + tag + "] " + mensagem, dados);
    }

    public static void aviso(String tag, String mensagem, Map<String, Object> dados) {
        inicializar();
        ThzLog.aviso("[" + tag + "] " + mensagem, dados);
    }

    public static void erro(String tag, String mensagem, Map<String, Object> dados) {
        inicializar();
        ThzLog.erro("[" + tag + "] " + mensagem, dados);
    }

    // ── Saída direta (para dados puros que devem ir para stdout) ──────────────

    public static void saida(String mensagem) {
        System.out.println(mensagem);
    }

    public static void saidaRaw(String mensagem) {
        System.out.print(mensagem);
    }

    // ── Helpers para parsing do JSON do ThzLog ───────────────────────────────

    private static String extrairNivel(String json) {
        int idx = json.indexOf("\"nivel\":\"");
        if (idx < 0) return "INFO";
        int start = idx + 9;
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "INFO";
    }

    private static String extrairMensagem(String json) {
        int idx = json.indexOf("\"mensagem\":\"");
        if (idx < 0) return json;
        int start = idx + 12;
        int end = json.indexOf("\"", start);
        if (end <= start) return json.substring(start);
        String msg = json.substring(start, end);
        msg = msg.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
        return msg;
    }
}
