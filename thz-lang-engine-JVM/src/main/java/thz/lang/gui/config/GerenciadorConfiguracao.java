package thz.lang.gui.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerenciador de Leitura e Escrita da Configuração Persistente do THZ-LANG Desktop em JSON.
 */
public final class GerenciadorConfiguracao {

    private static final String NOME_PASTA = ".thz";
    private static final String NOME_ARQUIVO = "desktop-config.json";

    private GerenciadorConfiguracao() {}

    /**
     * Retorna o caminho do arquivo de configuração no diretório do usuário (~/.thz/desktop-config.json).
     */
    public static Path obterCaminhoConfig() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, NOME_PASTA, NOME_ARQUIVO);
    }

    /**
     * Carrega a configuração persistida do disco. Se o arquivo não existir ou for inválido, retorna os valores padrão.
     */
    public static ConfiguracaoDesktop carregar() {
        try {
            Path arquivo = obterCaminhoConfig();
            if (Files.exists(arquivo)) {
                String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
                return deJson(conteudo);
            }
        } catch (Exception ignore) {
            // Em caso de falha de I/O ou JSON corrompido, utiliza configuração padrão
        }
        return ConfiguracaoDesktop.padrao();
    }

    /**
     * Salva a configuração fornecida no arquivo JSON.
     */
    public static void salvar(ConfiguracaoDesktop config) {
        if (config == null) return;
        try {
            Path arquivo = obterCaminhoConfig();
            if (arquivo.getParent() != null) {
                Files.createDirectories(arquivo.getParent());
            }
            String json = paraJson(config);
            Files.writeString(arquivo, json, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            // Não deve quebrar a execução se o disco estiver protegido contra escrita
        }
    }

    /**
     * Serializa o objeto ConfiguracaoDesktop para JSON formatado.
     */
    public static String paraJson(ConfiguracaoDesktop c) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"tema\": \"").append(escapar(c.tema())).append("\",\n");
        sb.append("  \"modoEstrito\": ").append(c.modoEstrito()).append(",\n");
        sb.append("  \"ultimoArquivo\": \"").append(escapar(c.ultimoArquivo())).append("\",\n");
        sb.append("  \"larguraJanela\": ").append(c.larguraJanela()).append(",\n");
        sb.append("  \"alturaJanela\": ").append(c.alturaJanela()).append(",\n");
        sb.append("  \"posicaoX\": ").append(c.posicaoX()).append(",\n");
        sb.append("  \"posicaoY\": ").append(c.posicaoY()).append(",\n");
        sb.append("  \"maximizada\": ").append(c.maximizada()).append(",\n");
        sb.append("  \"posicaoDivisor\": ").append(c.posicaoDivisor()).append(",\n");
        sb.append("  \"tamanhoFonte\": ").append(c.tamanhoFonte()).append(",\n");
        sb.append("  \"caminhoJvm\": \"").append(escapar(c.caminhoJvm())).append("\",\n");
        sb.append("  \"arquivosRecentes\": [\n");
        if (c.arquivosRecentes() != null) {
            for (int i = 0; i < c.arquivosRecentes().size(); i++) {
                sb.append("    \"").append(escapar(c.arquivosRecentes().get(i))).append("\"");
                if (i + 1 < c.arquivosRecentes().size()) sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Desserializa o JSON para um objeto ConfiguracaoDesktop de forma resiliente.
     */
    public static ConfiguracaoDesktop deJson(String json) {
        if (json == null || json.isBlank()) return ConfiguracaoDesktop.padrao();

        String tema = extrairString(json, "tema", "ESCURO");
        boolean estrito = extrairBoolean(json, "modoEstrito", false);
        String ultimoArquivo = extrairString(json, "ultimoArquivo", "");
        int largura = extrairInt(json, "larguraJanela", 1100);
        int altura = extrairInt(json, "alturaJanela", 720);
        int x = extrairInt(json, "posicaoX", -1);
        int y = extrairInt(json, "posicaoY", -1);
        boolean max = extrairBoolean(json, "maximizada", false);
        int div = extrairInt(json, "posicaoDivisor", 480);
        int fonte = extrairInt(json, "tamanhoFonte", 13);
        String jvm = extrairString(json, "caminhoJvm", "");
        List<String> recentes = extrairListaString(json, "arquivosRecentes");

        return new ConfiguracaoDesktop(
                tema, estrito, ultimoArquivo, largura, altura, x, y, max, div, fonte, jvm, recentes
        );
    }


    private static String extrairString(String json, String chave, String padrao) {
        Pattern p = Pattern.compile("\"" + chave + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\\\\", "\\").replace("\\\"", "\"");
        return padrao;
    }

    private static boolean extrairBoolean(String json, String chave, boolean padrao) {
        Pattern p = Pattern.compile("\"" + chave + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return padrao;
    }

    private static int extrairInt(String json, String chave, int padrao) {
        Pattern p = Pattern.compile("\"" + chave + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignore) {}
        }
        return padrao;
    }

    private static List<String> extrairListaString(String json, String chave) {
        List<String> lista = new ArrayList<>();
        Pattern pBloco = Pattern.compile("\"" + chave + "\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
        Matcher mBloco = pBloco.matcher(json);
        if (mBloco.find()) {
            String conteudo = mBloco.group(1);
            Pattern pItem = Pattern.compile("\"([^\"]*)\"");
            Matcher mItem = pItem.matcher(conteudo);
            while (mItem.find()) {
                String val = mItem.group(1).replace("\\\\", "\\").replace("\\\"", "\"");
                if (!val.isBlank()) lista.add(val);
            }
        }
        return List.copyOf(lista);
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
