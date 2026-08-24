package thz.lang.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThzConfig — Gerenciador de configurações com suporte a .env, variáveis de ambiente e properties.
 */
public final class ThzConfig {

    private static final Map<String, String> CONFIG_CACHE = new ConcurrentHashMap<>();

    static {
        // Carrega variáveis de ambiente padrão do SO
        System.getenv().forEach(CONFIG_CACHE::put);
        carregarEnvPadrao();
    }

    private ThzConfig() {}

    public static void carregarEnvPadrao() {
        carregarArquivoEnv(".env");
    }

    public static void carregarArquivoEnv(String caminho) {
        Path p = Path.of(caminho);
        if (Files.exists(p) && !Files.isDirectory(p)) {
            try {
                for (String linha : Files.readAllLines(p)) {
                    String trim = linha.trim();
                    if (trim.isEmpty() || trim.startsWith("#")) continue;
                    int eq = trim.indexOf('=');
                    if (eq > 0) {
                        String chave = trim.substring(0, eq).trim();
                        String valor = trim.substring(eq + 1).trim();
                        if ((valor.startsWith("\"") && valor.endsWith("\"")) || (valor.startsWith("'") && valor.endsWith("'"))) {
                            valor = valor.substring(1, valor.length() - 1);
                        }
                        CONFIG_CACHE.put(chave, valor);
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    public static void carregarProperties(String conteudo) {
        Properties prop = new Properties();
        try {
            prop.load(new StringReader(conteudo));
            for (String key : prop.stringPropertyNames()) {
                CONFIG_CACHE.put(key, prop.getProperty(key));
            }
        } catch (IOException ignored) {}
    }

    public static void definir(String chave, String valor) {
        if (chave != null && valor != null) {
            CONFIG_CACHE.put(chave, valor);
        }
    }

    public static String obter(String chave, String padrao) {
        String val = CONFIG_CACHE.get(chave);
        if (val != null) return val;
        val = System.getProperty(chave);
        return val != null ? val : padrao;
    }

    public static String obter(String chave) {
        return obter(chave, "");
    }

    public static long obterInteiro(String chave, long padrao) {
        String val = obter(chave, null);
        if (val == null || val.isBlank()) return padrao;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    public static boolean obterLogico(String chave, boolean padrao) {
        String val = obter(chave, null);
        if (val == null || val.isBlank()) return padrao;
        String limpo = val.trim().toLowerCase();
        return limpo.equals("true") || limpo.equals("verdadeiro") || limpo.equals("1") || limpo.equals("sim");
    }

    public static Map<String, String> obterTodos() {
        return Collections.unmodifiableMap(new HashMap<>(CONFIG_CACHE));
    }
}
