package thz.lang.semantico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.formato.Formatador;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InferenciaVariavelTest {

    private ProgramaAst parse(String codigo) {
        return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
    }

    @Test
    @DisplayName("Deve inferir tipos de variáveis sem anotação explícita de tipo")
    void testInferenciaVariaveisPrimitivas() {
        String src = """
                PROGRAMA InferenciaBasica
                PROCEDIMENTO Teste()
                INICIO
                    VARIAVEL nome <- "Lucas"
                    VARIAVEL ativo <- VERDADEIRO
                    VARIAVEL total <- 1500.75
                    VARIAVEL contador <- 42
                    EXIBA nome
                FIM
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        AnalisadorSemantico analisador = new AnalisadorSemantico(ast);
        List<ErroSemantico> erros = analisador.analisar();

        assertTrue(erros.isEmpty(), "Não deve haver erros com variáveis de tipo inferido: " + erros);

        String formatado = Formatador.formatar(ast);
        assertTrue(formatado.contains("VARIAVEL nome <- \"Lucas\""));
        assertTrue(formatado.contains("VARIAVEL total <- 1500.75"));
    }

    @Test
    @DisplayName("Deve acusar erro caso atribua tipo incompatível em variável com tipo inferido")
    void testIncompatibilidadeVariavelInferida() {
        String src = """
                PROGRAMA IncompatibilidadeInferida
                PROCEDIMENTO Teste()
                INICIO
                    VARIAVEL total <- 100.50
                    total <- "Texto Invalido"
                FIM
                FIM_PROGRAMA
                """;

        ProgramaAst ast = parse(src);
        AnalisadorSemantico analisador = new AnalisadorSemantico(ast);
        List<ErroSemantico> erros = analisador.analisar();

        assertFalse(erros.isEmpty(), "Deve acusar erro semântico na reatribuição de TEXTO em DECIMAL");
        assertTrue(erros.stream().anyMatch(e -> e.mensagem().contains("Atribuição incompatível")));
    }
}
