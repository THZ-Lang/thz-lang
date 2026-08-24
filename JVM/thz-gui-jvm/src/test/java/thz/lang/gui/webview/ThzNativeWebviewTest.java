package thz.lang.gui.webview;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ThzNativeWebviewTest {

    @AfterEach
    void tearDown() {
        ThzNativeWebview.fechar();
    }

    @Test
    @DisplayName("ThzNativeWebview deve aceitar configurações de janela e gerenciar ciclo de vida")
    void testConfiguracaoJanela() {
        ThzNativeWebview.JanelaConfig config = new ThzNativeWebview.JanelaConfig(
                "Dashboard Financeiro THZ",
                "<html><body><h1>Dashboard</h1></body></html>",
                800,
                600,
                false,
                false
        );

        assertEquals("Dashboard Financeiro THZ", config.titulo());
        assertEquals(800, config.largura());
        assertEquals(600, config.altura());

        // Fecha o webview e a ponte
        ThzNativeWebview.fechar();
        assertFalse(ThzNativeWebview.estaAberta());
    }
}
