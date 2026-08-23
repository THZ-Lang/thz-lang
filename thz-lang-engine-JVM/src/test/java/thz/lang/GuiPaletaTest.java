package thz.lang;

import thz.lang.gui.PaletaThz;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GuiPaletaTest {

    @Test public void paletasCobremTodosTipos() {
        for (TokenType t : TokenType.values()) {
            if (t == TokenType.EOF) continue;
            Token tok = new Token(t, t == TokenType.IDENTIFICADOR ? "x" : t.name(), 1, 1);
            // tipos primitivos para IDENTIFICADOR
            if (t == TokenType.IDENTIFICADOR) {
                assertNotNull(PaletaThz.ESCURO.atributoPara(new Token(TokenType.IDENTIFICADOR, "TEXTO", 1, 1)));
                assertNotNull(PaletaThz.CLARO.atributoPara(new Token(TokenType.IDENTIFICADOR, "TEXTO", 1, 1)));
                assertNotNull(PaletaThz.ESCURO.atributoPara(tok));
                assertNotNull(PaletaThz.CLARO.atributoPara(tok));
                continue;
            }
            assertNotNull(PaletaThz.ESCURO.atributoPara(tok), "ESCURO não cobre " + t);
            assertNotNull(PaletaThz.CLARO.atributoPara(tok), "CLARO não cobre " + t);
        }
    }

    @Test public void tiposPrimitivosReconhecidos() {
        assertTrue(PaletaThz.ehTipoPrimitivo("TEXTO"));
        assertTrue(PaletaThz.ehTipoPrimitivo("DECIMAL"));
        assertFalse(PaletaThz.ehTipoPrimitivo("INEXISTENTE"));
    }

    @Test public void comentarioForaDeString() {
        // Sanity: Paleta não null
        assertNotNull(PaletaThz.ESCURO.attrComentario);
        assertNotNull(PaletaThz.CLARO.attrComentario);
        assertNotNull(PaletaThz.ESCURO.attrString);
        assertNotNull(PaletaThz.CLARO.attrString);
    }

    @Test public void inicializacaoGuiNaoQuebra() {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            thz.lang.gui.ThzGui gui = new thz.lang.gui.ThzGui();
            assertNotNull(gui);
            gui.dispose();
        }
    }
}

