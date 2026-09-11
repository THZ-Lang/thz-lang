package thz.lang.gui.webview;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ThzNativeWebViewTest {

    @AfterEach
    void tearDown() {
        ThzNativeWebView.fechar();
    }

    @Test
    @DisplayName("ThzNativeWebView deve aceitar configurações de janela e gerenciar ciclo de vida")
    void testConfiguracaoJanela() {
        ThzNativeWebView.JanelaConfig config = new ThzNativeWebView.JanelaConfig(
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

        ThzNativeWebView.fechar();
        assertFalse(ThzNativeWebView.estaAberta());
    }
}
