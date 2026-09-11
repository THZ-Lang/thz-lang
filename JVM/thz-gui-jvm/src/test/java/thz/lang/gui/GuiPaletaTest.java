package thz.lang.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;

import static org.junit.jupiter.api.Assertions.*;

public class GuiPaletaTest {

    @Test
    @DisplayName("Paletas devem cobrir todos os TokenType e atributos visuais")
    public void paletasCobremTodosTipos() {
        for (TokenType t : TokenType.values()) {
            if (t == TokenType.EOF) continue;
            Token tok = new Token(t, t == TokenType.IDENTIFICADOR ? "x" : t.name(), 1, 1);
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

    @Test
    @DisplayName("Tipos primitivos e novos tipos compostos devem ser reconhecidos")
    public void tiposPrimitivosReconhecidos() {
        assertTrue(PaletaThz.ehTipoPrimitivo("TEXTO"));
        assertTrue(PaletaThz.ehTipoPrimitivo("DECIMAL"));
        assertTrue(PaletaThz.ehTipoPrimitivo("DINHEIRO"));
        assertTrue(PaletaThz.ehTipoPrimitivo("REGISTRO"));
        assertTrue(PaletaThz.ehTipoPrimitivo("MAPA"));
        assertTrue(PaletaThz.ehTipoPrimitivo("BLOCO_MEMORIA"));
        assertTrue(PaletaThz.ehTipoPrimitivo("ARENA"));
        assertFalse(PaletaThz.ehTipoPrimitivo("INEXISTENTE"));
    }

    @Test
    @DisplayName("Módulos da biblioteca padrão devem ser reconhecidos")
    public void modulosStdlibReconhecidos() {
        assertTrue(PaletaThz.ehModuloStdlib("BRASIL"));
        assertTrue(PaletaThz.ehModuloStdlib("SNAPSHOT"));
        assertTrue(PaletaThz.ehModuloStdlib("ESTATISTICA"));
        assertTrue(PaletaThz.ehModuloStdlib("DAX"));
        assertTrue(PaletaThz.ehModuloStdlib("PLANILHA"));
        assertTrue(PaletaThz.ehModuloStdlib("DADOS"));
        assertTrue(PaletaThz.ehModuloStdlib("MENSAGERIA"));
        assertTrue(PaletaThz.ehModuloStdlib("BANCO"));
        assertTrue(PaletaThz.ehModuloStdlib("IA"));
        assertTrue(PaletaThz.ehModuloStdlib("ML"));
        assertTrue(PaletaThz.ehModuloStdlib("NATIVO"));
        assertFalse(PaletaThz.ehModuloStdlib("MODULO_INEXISTENTE"));
    }

    @Test
    @DisplayName("Highlighting contextual deve diferenciar módulos, funções e tipos")
    public void highlightingContextual() {
        Token tokBrasil = new Token(TokenType.IDENTIFICADOR, "BRASIL", 1, 1);
        Token tokPonto = new Token(TokenType.PONTO, ".", 1, 7);
        Token tokMetodo = new Token(TokenType.IDENTIFICADOR, "consultarCep", 1, 8);
        Token tokAbrePar = new Token(TokenType.ABRE_PARENTESE, "(", 1, 20);

        // Módulo Stdlib recebe attrModulo
        assertEquals(PaletaThz.ESCURO.attrModulo, PaletaThz.ESCURO.atributoPara(tokBrasil, null, tokPonto));

        // Chamada de método com abre parênteses recebe attrFuncao
        assertEquals(PaletaThz.ESCURO.attrFuncao, PaletaThz.ESCURO.atributoPara(tokMetodo, tokPonto, tokAbrePar));

        // Tipo primitivo recebe attrTipo
        Token tokTipo = new Token(TokenType.IDENTIFICADOR, "DECIMAL", 2, 1);
        assertEquals(PaletaThz.ESCURO.attrTipo, PaletaThz.ESCURO.atributoPara(tokTipo, null, null));
    }

    @Test
    @DisplayName("Sanity: Atributos das paletas não nulos")
    public void atributosPaletaNaoNulos() {
        assertNotNull(PaletaThz.ESCURO.attrComentario);
        assertNotNull(PaletaThz.CLARO.attrComentario);
        assertNotNull(PaletaThz.ESCURO.attrString);
        assertNotNull(PaletaThz.CLARO.attrString);
        assertNotNull(PaletaThz.ESCURO.attrModulo);
        assertNotNull(PaletaThz.CLARO.attrModulo);
        assertNotNull(PaletaThz.ESCURO.attrFuncao);
        assertNotNull(PaletaThz.CLARO.attrFuncao);
    }

    @Test
    public void inicializacaoGuiNaoQuebra() {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            thz.lang.gui.ThzGui gui = new thz.lang.gui.ThzGui();
            assertNotNull(gui);
            gui.dispose();
        }
    }
}

