package thz.lang.cli.comandos;

import thz.lang.agent.ThzAgent;

import java.util.List;

/**
 * Comando `thz agent` — lança o THZ-Agent (assistente de código autônomo).
 */
public final class ComandoAgent implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("agent", "agente");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        // Repassar todos os argumentos para o ThzAgent.main()
        String[] args = argumentos.toArray(new String[0]);
        ThzAgent.main(args);
    }
}
