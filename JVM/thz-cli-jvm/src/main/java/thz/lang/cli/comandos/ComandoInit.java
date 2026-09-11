package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.cli.CliLogger;

public class ComandoInit implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("init", "inicializar");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        Path arquivoConfig = Path.of("thz.config.json");
        if (Files.exists(arquivoConfig)) {
            CliLogger.info("[THZ INIT] O arquivo de manifesto 'thz.config.json' já existe neste projeto.");
            return;
        }
        var padrao = thz.lang.config.ThzProjectConfig.criarPadrao(arquivoConfig);
        String jsonModelo = thz.lang.config.ThzProjectConfig.gerarJsonModelo(padrao);
        Files.writeString(arquivoConfig, jsonModelo, StandardCharsets.UTF_8);
        CliLogger.info("================================================================================");
        CliLogger.info("   PROJETO THZ-LANG INICIALIZADO COM SUCESSO!");
        CliLogger.info("   Manifesto criado: " + arquivoConfig.toAbsolutePath());
        CliLogger.info("================================================================================\n");
        CliLogger.info("Configurações padrão ativas:");
        CliLogger.info("  • Dialeto: pt-BR");
        CliLogger.info("  • Banco de Dados: Auto (SQLite / PostgreSQL / MySQL / JDBC)");
        CliLogger.info("  • Mensageria: Auto (RabbitMQ / Kafka / AWS SQS / Embutido)");
        CliLogger.info("  • IA & Embeddings: Local FNV-1a L2");
        CliLogger.info("\nEdite 'thz.config.json' para personalizar suas preferências de banco e mensageria.");
    }
}
