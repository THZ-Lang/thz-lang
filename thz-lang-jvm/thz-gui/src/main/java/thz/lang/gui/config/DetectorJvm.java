package thz.lang.gui.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilitário de detecção, validação e gerenciamento de JVMs instaladas no ambiente.
 */
public final class DetectorJvm {

    public record InfoJvm(
            String rotulo,
            String caminho,
            String versao,
            String fornecedor,
            boolean ehAtual
    ) {
        @Override
        public String toString() {
            return rotulo + (versao != null && !versao.isBlank() ? " (" + versao + ")" : "") + (ehAtual ? " [Atual]" : "");
        }
    }

    private DetectorJvm() {}

    /**
     * Retorna informações sobre a JVM que está executando o processo atual.
     */
    public static InfoJvm obterJvmAtual() {
        String home = System.getProperty("java.home", "");
        String ver = System.getProperty("java.version", "desconhecida");
        String vendor = System.getProperty("java.vendor", "desconhecido");
        return new InfoJvm("JVM Atual do Sistema / Embutida", home, ver, vendor, true);
    }

    /**
     * Varre diretórios e variáveis de ambiente comuns no Windows e Linux/macOS para listar JVMs disponíveis.
     */
    public static List<InfoJvm> detectarJvmsDisponiveis() {
        Set<String> caminhosTestados = new LinkedHashSet<>();
        List<InfoJvm> resultado = new ArrayList<>();

        // 1. JVM atual
        InfoJvm atual = obterJvmAtual();
        resultado.add(atual);
        if (!atual.caminho().isBlank()) {
            caminhosTestados.add(normalizarCaminho(atual.caminho()));
        }

        // 2. JAVA_HOME do ambiente
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null && !envJavaHome.isBlank()) {
            adicionarSeValido("JAVA_HOME (" + envJavaHome + ")", envJavaHome, caminhosTestados, resultado);
        }

        // 3. Scoop JDKs (Windows)
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File scoopApps = new File(userHome, "scoop/apps");
            if (scoopApps.exists() && scoopApps.isDirectory()) {
                File[] dirs = scoopApps.listFiles();
                if (dirs != null) {
                    for (File d : dirs) {
                        if (d.isDirectory() && (d.getName().startsWith("openjdk") || d.getName().startsWith("graalvm") || d.getName().startsWith("temurin") || d.getName().startsWith("corretto") || d.getName().startsWith("zulu"))) {
                            File current = new File(d, "current");
                            if (current.exists()) {
                                adicionarSeValido("Scoop " + d.getName(), current.getAbsolutePath(), caminhosTestados, resultado);
                            } else {
                                adicionarSeValido("Scoop " + d.getName(), d.getAbsolutePath(), caminhosTestados, resultado);
                            }
                        }
                    }
                }
            }

            // 4. SDKMAN (Linux/macOS/WSL)
            File sdkmanCandidates = new File(userHome, ".sdkman/candidates/java");
            if (sdkmanCandidates.exists() && sdkmanCandidates.isDirectory()) {
                File[] dirs = sdkmanCandidates.listFiles();
                if (dirs != null) {
                    for (File d : dirs) {
                        if (d.isDirectory()) {
                            adicionarSeValido("SDKMAN " + d.getName(), d.getAbsolutePath(), caminhosTestados, resultado);
                        }
                    }
                }
            }
        }

        // 5. Pastas padrões do sistema Windows (Program Files)
        String[] dirsWindows = {
                "C:\\Program Files\\Java",
                "C:\\Program Files\\Eclipse Adoptium",
                "C:\\Program Files\\BellSoft",
                "C:\\Program Files\\Amazon Corretto",
                "C:\\Program Files\\Zulu",
                "C:\\Program Files\\Microsoft"
        };
        for (String base : dirsWindows) {
            File fBase = new File(base);
            if (fBase.exists() && fBase.isDirectory()) {
                File[] subs = fBase.listFiles();
                if (subs != null) {
                    for (File sub : subs) {
                        if (sub.isDirectory()) {
                            adicionarSeValido(sub.getName(), sub.getAbsolutePath(), caminhosTestados, resultado);
                        }
                    }
                }
            }
        }

        // 6. Linux /usr/lib/jvm
        File linuxJvm = new File("/usr/lib/jvm");
        if (linuxJvm.exists() && linuxJvm.isDirectory()) {
            File[] subs = linuxJvm.listFiles();
            if (subs != null) {
                for (File sub : subs) {
                    if (sub.isDirectory()) {
                        adicionarSeValido("Linux " + sub.getName(), sub.getAbsolutePath(), caminhosTestados, resultado);
                    }
                }
            }
        }

        return resultado;
    }

    /**
     * Valida se um diretório é um JDK/JRE válido contendo o executável java.
     */
    public static boolean ehDiretorioJvmValido(String caminho) {
        if (caminho == null || caminho.isBlank()) return false;
        File f = new File(caminho);
        if (!f.exists() || !f.isDirectory()) return false;

        File binJavaExe = new File(f, "bin/java.exe");
        File binJava = new File(f, "bin/java");
        return binJavaExe.exists() || binJava.exists();
    }

    /**
     * Inspeciona um diretório e extrai a versão e fornecedor da JVM (via arquivo release).
     */
    public static InfoJvm inspecionarJvm(String rotulo, String caminho) {
        if (!ehDiretorioJvmValido(caminho)) {
            return new InfoJvm(rotulo, caminho, "Inválido (java não encontrado em bin/)", "", false);
        }

        String versao = "";
        String vendor = "";

        File releaseFile = new File(caminho, "release");
        if (releaseFile.exists()) {
            try {
                List<String> linhas = Files.readAllLines(releaseFile.toPath(), StandardCharsets.UTF_8);
                for (String l : linhas) {
                    if (l.startsWith("JAVA_VERSION=")) {
                        versao = l.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                    } else if (l.startsWith("IMPLEMENTOR=")) {
                        vendor = l.substring("IMPLEMENTOR=".length()).replace("\"", "").trim();
                    }
                }
            } catch (Exception ignore) {}
        }

        if (versao.isBlank()) {
            versao = "Detectada";
        }

        String caminhoAtual = System.getProperty("java.home", "");
        boolean ehAtual = normalizarCaminho(caminho).equalsIgnoreCase(normalizarCaminho(caminhoAtual));

        return new InfoJvm(rotulo, caminho, versao, vendor, ehAtual);
    }

    private static void adicionarSeValido(String rotulo, String caminho, Set<String> testados, List<InfoJvm> lista) {
        if (caminho == null || caminho.isBlank()) return;
        String norm = normalizarCaminho(caminho);
        if (testados.contains(norm)) return;

        if (ehDiretorioJvmValido(caminho)) {
            testados.add(norm);
            lista.add(inspecionarJvm(rotulo, caminho));
        }
    }

    private static String normalizarCaminho(String c) {
        try {
            return new File(c).getCanonicalPath();
        } catch (Exception e) {
            return new File(c).getAbsolutePath();
        }
    }
}
