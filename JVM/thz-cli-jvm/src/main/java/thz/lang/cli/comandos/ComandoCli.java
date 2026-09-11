package thz.lang.cli.comandos;

import java.util.List;

/**
 * Interface para comandos CLI da THZ-LANG.
 * Cada comando (init, run, check, etc.) implementa esta interface.
 */
public interface ComandoCli {

    /** Nomes que acionam este comando (ex: "init", "inicializar"). */
    List<String> nomes();

    /** Executa o comando com os argumentos fornecidos. */
    void executar(List<String> argumentos, boolean estrito) throws Exception;
}
