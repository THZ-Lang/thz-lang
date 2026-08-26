package thz.lang.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ComandoInit implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("init", "inicializar");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        Path arquivoConfig = Path.of("thz.config.json");
        if (Files.exists(arquivoConfig)) {
            System.out.println("[THZ INIT] O arquivo de manifesto 'thz.config.json' já existe neste projeto.");
            return;
        }
        var padrao = thz.lang.config.ThzProjectConfig.criarPadrao(arquivoConfig);
        String jsonModelo = thz.lang.config.ThzProjectConfig.gerarJsonModelo(padrao);
        Files.writeString(arquivoConfig, jsonModelo, StandardCharsets.UTF_8);
        System.out.println("================================================================================");
        System.out.println("   PROJETO THZ-LANG INICIALIZADO COM SUCESSO!");
        System.out.println("   Manifesto criado: " + arquivoConfig.toAbsolutePath());
        System.out.println("================================================================================\n");
        System.out.println("Configurações padrão ativas:");
        System.out.println("  • Dialeto: pt-BR");
        System.out.println("  • Banco de Dados: Auto (SQLite / PostgreSQL / MySQL / JDBC)");
        System.out.println("  • Mensageria: Auto (RabbitMQ / Kafka / AWS SQS / Embutido)");
        System.out.println("  • IA & Embeddings: Local FNV-1a L2");
        System.out.println("\nEdite 'thz.config.json' para personalizar suas preferências de banco e mensageria.");
    }
}
