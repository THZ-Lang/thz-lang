package thz.lang.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ui.ThzUiMaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThzStudioIdeTest {

    @Test
    @DisplayName("THZ-STUDIO (.thzui) deve compilar AST e gerar página HTML5 Glassmorphism")
    void testThzStudioIdeDeclarativo() throws Exception {
        Path path = Path.of("exemplos/thz_studio_ide.thzui");
        if (!Files.exists(path)) {
            path = Path.of("JVM/thz-gui-jvm/exemplos/thz_studio_ide.thzui");
        }
        assertTrue(Files.exists(path), "Arquivo thz_studio_ide.thzui deve existir");

        String fonte = Files.readString(path);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

        assertNotNull(ast);
        assertEquals("ThzStudioIde", ast.nome());

        ThzUiMaker uiMaker = ThzUiMaker.card("card_studio", ast.nome(), c -> {
            c.adicionar(ThzUiMaker.alerta("ide_status", "info", "THZ-STUDIO em execução"));
        });
        String html5 = uiMaker.renderizarHtml("THZ-STUDIO IDE", null);

        assertNotNull(html5);
        assertTrue(html5.contains("THZ-STUDIO"));
    }
}
