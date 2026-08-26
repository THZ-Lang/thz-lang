package thz.lang.cli;

import java.util.List;

public class ComandoDev implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("dev", "serve", "web", "vaadin");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);

        int porta = 8080;
        int idxPorta = argumentos.indexOf("--porta");
        if (idxPorta < 0) idxPorta = argumentos.indexOf("-p");
        if (idxPorta < 0) idxPorta = argumentos.indexOf("--port");
        if (idxPorta >= 0 && idxPorta + 1 < argumentos.size()) {
            try {
                porta = Integer.parseInt(argumentos.get(idxPorta + 1));
            } catch (NumberFormatException ignored) {}
        }
        boolean abrir = argumentos.contains("--abrir") || argumentos.contains("--open");
        boolean vaadin = argumentos.contains("--vaadin") || argumentos.contains("vaadin");
        ThzDevServer.iniciar(arquivo, porta, abrir, vaadin);
        if (!Boolean.getBoolean("thz.test.mode")) {
            synchronized (ThzCli.class) {
                try {
                    ThzCli.class.wait();
                } catch (InterruptedException ignore) {}
            }
        }
    }
}
