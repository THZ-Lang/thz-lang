package thz.lang;

import org.junit.jupiter.api.Test;
import thz.lang.gui.config.ConfiguracaoDesktop;
import thz.lang.gui.config.DetectorJvm;
import thz.lang.gui.config.GerenciadorConfiguracao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DetectorJvmTest {

    @Test
    public void testObterJvmAtual() {
        DetectorJvm.InfoJvm atual = DetectorJvm.obterJvmAtual();
        assertNotNull(atual);
        assertNotNull(atual.caminho());
        assertFalse(atual.caminho().isBlank());
        assertNotNull(atual.versao());
        assertTrue(atual.ehAtual());
    }

    @Test
    public void testDetectarJvmsDisponiveis() {
        List<DetectorJvm.InfoJvm> jvms = DetectorJvm.detectarJvmsDisponiveis();
        assertNotNull(jvms);
        assertFalse(jvms.isEmpty(), "Deve detectar pelo menos a JVM atual");
        assertTrue(jvms.stream().anyMatch(DetectorJvm.InfoJvm::ehAtual));
    }

    @Test
    public void testValidacaoDiretorioJvm() {
        String javaHome = System.getProperty("java.home");
        assertTrue(DetectorJvm.ehDiretorioJvmValido(javaHome), "java.home deve ser um diretório de JVM válido");
        assertFalse(DetectorJvm.ehDiretorioJvmValido(null));
        assertFalse(DetectorJvm.ehDiretorioJvmValido(""));
        assertFalse(DetectorJvm.ehDiretorioJvmValido("C:\\pasta_que_nao_existe_12345"));
    }

    @Test
    public void testInspecionarJvmAtual() {
        String javaHome = System.getProperty("java.home");
        DetectorJvm.InfoJvm info = DetectorJvm.inspecionarJvm("Teste", javaHome);
        assertNotNull(info);
        assertEquals(javaHome, info.caminho());
        assertTrue(info.ehAtual());
        assertFalse(info.versao().isBlank());
    }

    @Test
    public void testPersistenciaJvmPersonalizada() {
        ConfiguracaoDesktop config = ConfiguracaoDesktop.padrao();
        assertEquals("", config.caminhoJvm());

        String fakeJvm = "C:\\Java\\jdk-25";
        config = config.comJvm(fakeJvm);
        assertEquals(fakeJvm, config.caminhoJvm());

        String json = GerenciadorConfiguracao.paraJson(config);
        assertTrue(json.contains("\"caminhoJvm\": \"C:\\\\Java\\\\jdk-25\""));

        ConfiguracaoDesktop carregada = GerenciadorConfiguracao.deJson(json);
        assertEquals(fakeJvm, carregada.caminhoJvm());
    }
}
