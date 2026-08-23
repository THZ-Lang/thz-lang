package thz.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.gui.config.ConfiguracaoDesktop;
import thz.lang.gui.config.GerenciadorConfiguracao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfiguracaoDesktopTest {

    @Test
    public void testConfiguracaoPadrao() {
        ConfiguracaoDesktop padrao = ConfiguracaoDesktop.padrao();
        assertNotNull(padrao);
        assertEquals("ESCURO", padrao.tema());
        assertFalse(padrao.modoEstrito());
        assertEquals(1100, padrao.larguraJanela());
        assertEquals(720, padrao.alturaJanela());
        assertEquals(480, padrao.posicaoDivisor());
        assertEquals(13, padrao.tamanhoFonte());
        assertNotNull(padrao.arquivosRecentes());
        assertTrue(padrao.arquivosRecentes().isEmpty());
    }

    @Test
    public void testSerializacaoEDesserializacaoJson() {
        ConfiguracaoDesktop original = new ConfiguracaoDesktop(
                "CLARO",
                true,
                "C:\\workspace\\programa.thz",
                1280,
                800,
                100,
                50,
                true,
                550,
                14,
                "C:\\Java\\jdk-25",
                List.of("C:\\workspace\\arq1.thz", "C:\\workspace\\arq2.thz")
        );


        String json = GerenciadorConfiguracao.paraJson(original);
        assertNotNull(json);
        assertTrue(json.contains("\"tema\": \"CLARO\""));
        assertTrue(json.contains("\"modoEstrito\": true"));
        assertTrue(json.contains("\"larguraJanela\": 1280"));
        assertTrue(json.contains("\"posicaoDivisor\": 550"));
        assertTrue(json.contains("\"maximizada\": true"));

        ConfiguracaoDesktop restaurada = GerenciadorConfiguracao.deJson(json);
        assertNotNull(restaurada);
        assertEquals("CLARO", restaurada.tema());
        assertTrue(restaurada.modoEstrito());
        assertEquals("C:\\workspace\\programa.thz", restaurada.ultimoArquivo());
        assertEquals(1280, restaurada.larguraJanela());
        assertEquals(800, restaurada.alturaJanela());
        assertEquals(100, restaurada.posicaoX());
        assertEquals(50, restaurada.posicaoY());
        assertTrue(restaurada.maximizada());
        assertEquals(550, restaurada.posicaoDivisor());
        assertEquals(14, restaurada.tamanhoFonte());
        assertEquals(2, restaurada.arquivosRecentes().size());
        assertEquals("C:\\workspace\\arq1.thz", restaurada.arquivosRecentes().get(0));
        assertEquals("C:\\workspace\\arq2.thz", restaurada.arquivosRecentes().get(1));
    }

    @Test
    public void testHistoricoArquivosRecentes() {
        ConfiguracaoDesktop c = ConfiguracaoDesktop.padrao();
        c = c.comArquivoRecente("arq1.thz");
        c = c.comArquivoRecente("arq2.thz");
        c = c.comArquivoRecente("arq3.thz");

        assertEquals(3, c.arquivosRecentes().size());
        assertEquals("arq3.thz", c.arquivosRecentes().get(0));
        assertEquals("arq2.thz", c.arquivosRecentes().get(1));
        assertEquals("arq1.thz", c.arquivosRecentes().get(2));

        // Reabrir arq1 deve movê-lo para o topo sem duplicar
        c = c.comArquivoRecente("arq1.thz");
        assertEquals(3, c.arquivosRecentes().size());
        assertEquals("arq1.thz", c.arquivosRecentes().get(0));
        assertEquals("arq3.thz", c.arquivosRecentes().get(1));
        assertEquals("arq2.thz", c.arquivosRecentes().get(2));
    }

    @Test
    public void testJsonVazioOuInvalidoUsaPadrao() {
        ConfiguracaoDesktop deNulo = GerenciadorConfiguracao.deJson(null);
        assertNotNull(deNulo);
        assertEquals("ESCURO", deNulo.tema());

        ConfiguracaoDesktop deVazio = GerenciadorConfiguracao.deJson("   ");
        assertNotNull(deVazio);
        assertEquals("ESCURO", deVazio.tema());

        ConfiguracaoDesktop deInvalido = GerenciadorConfiguracao.deJson("{ \"tema\": \"CLARO\" }");
        assertNotNull(deInvalido);
        assertEquals("CLARO", deInvalido.tema());
        assertEquals(1100, deInvalido.larguraJanela()); // manteve padrão para campos omitidos
    }
}
