package thz.lang.gui.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ui.ThzUiComponente;
import thz.lang.ui.ThzUiMaker;
import thz.lang.ui.ThzUiTema;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ThzUiSwingRendererTest {

    @Test
    @DisplayName("ThzUiSwingRenderer deve instanciar árvore de componentes Swing a partir do schema ThzUiMaker")
    void testRenderizacaoSwing() {
        AtomicBoolean acaoDisparada = new AtomicBoolean(false);

        ThzUiComponente schema = ThzUiMaker.container("root", c -> {
            c.adicionar(ThzUiMaker.card("card1", "Card Swing", card -> {
                card.adicionar(ThzUiMaker.campoTexto("txt1", "Nome", "Digite...", "nome"));
                card.adicionar(ThzUiMaker.metrica("kpi1", "Vendas", "100", "+5%", "sucesso"));
                card.adicionar(ThzUiMaker.botao("btn1", "Clique Aqui", "MinhaAcao"));
            }));
        }).construir();

        JComponent raizSwing = ThzUiSwingRenderer.renderizar(schema, ThzUiTema.escuroGlass(), (acao, id) -> {
            if ("MinhaAcao".equals(acao) && "btn1".equals(id)) {
                acaoDisparada.set(true);
            }
        });

        assertNotNull(raizSwing);
        assertTrue(raizSwing instanceof JPanel);
        assertTrue(raizSwing.getComponentCount() >= 1);

        // Encontra o botão e simula o clique
        JButton btn = encontrarBotao(raizSwing);
        assertNotNull(btn, "Botão deve ser encontrado na hierarquia Swing");
        btn.doClick();
        assertTrue(acaoDisparada.get(), "Ação do botão deve ser despachada");
    }

    private JButton encontrarBotao(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton b) return b;
            if (c instanceof Container sub) {
                JButton b = encontrarBotao(sub);
                if (b != null) return b;
            }
        }
        return null;
    }
}
