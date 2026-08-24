package thz.lang.gui.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dados imutável da configuração persistente do THZ-LANG Desktop.
 */
public record ConfiguracaoDesktop(
        String tema,
        boolean modoEstrito,
        String ultimoArquivo,
        int larguraJanela,
        int alturaJanela,
        int posicaoX,
        int posicaoY,
        boolean maximizada,
        int posicaoDivisor,
        int tamanhoFonte,
        String caminhoJvm,
        List<String> arquivosRecentes
) {
    public static ConfiguracaoDesktop padrao() {
        return new ConfiguracaoDesktop(
                "ESCURO",
                false,
                "",
                1100,
                720,
                -1,
                -1,
                false,
                480,
                13,
                "",
                List.of()
        );
    }

    public ConfiguracaoDesktop comTema(String novoTema) {
        return new ConfiguracaoDesktop(novoTema, modoEstrito, ultimoArquivo, larguraJanela, alturaJanela, posicaoX, posicaoY, maximizada, posicaoDivisor, tamanhoFonte, caminhoJvm, arquivosRecentes);
    }

    public ConfiguracaoDesktop comModoEstrito(boolean novoEstrito) {
        return new ConfiguracaoDesktop(tema, novoEstrito, ultimoArquivo, larguraJanela, alturaJanela, posicaoX, posicaoY, maximizada, posicaoDivisor, tamanhoFonte, caminhoJvm, arquivosRecentes);
    }

    public ConfiguracaoDesktop comJvm(String novoCaminhoJvm) {
        return new ConfiguracaoDesktop(tema, modoEstrito, ultimoArquivo, larguraJanela, alturaJanela, posicaoX, posicaoY, maximizada, posicaoDivisor, tamanhoFonte, novoCaminhoJvm != null ? novoCaminhoJvm : "", arquivosRecentes);
    }

    public ConfiguracaoDesktop comArquivoRecente(String caminho) {
        if (caminho == null || caminho.isBlank()) return this;
        List<String> novaLista = new ArrayList<>();
        novaLista.add(caminho);
        if (arquivosRecentes != null) {
            for (String arq : arquivosRecentes) {
                if (!arq.equalsIgnoreCase(caminho) && novaLista.size() < 10) {
                    novaLista.add(arq);
                }
            }
        }
        return new ConfiguracaoDesktop(tema, modoEstrito, caminho, larguraJanela, alturaJanela, posicaoX, posicaoY, maximizada, posicaoDivisor, tamanhoFonte, caminhoJvm, List.copyOf(novaLista));
    }

    public ConfiguracaoDesktop comDimensoes(int l, int a, int x, int y, boolean max, int div) {
        return new ConfiguracaoDesktop(tema, modoEstrito, ultimoArquivo, l, a, x, y, max, div, tamanhoFonte, caminhoJvm, arquivosRecentes);
    }
}
