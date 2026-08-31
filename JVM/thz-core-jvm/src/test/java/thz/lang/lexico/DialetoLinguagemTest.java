package thz.lang.lexico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.formato.Formatador;
import thz.lang.sintatico.ThzParser;

import static org.junit.jupiter.api.Assertions.*;

public class DialetoLinguagemTest {

    @Test
    @DisplayName("Deve fazer parsing e execução de programa escrito 100% em dialeto EN-US")
    void deveFazerParsingProgramaEnUs() {
        String codigoEn = """
            LANGUAGE: en-US
            PROGRAM OrderProcessing

            ARCHITECTURE_METADATA
                DOMAIN: "ECommerce"
                VERSION: "1.0.0"
                AUTHOR: "Global Team"
            END_METADATA

            STRUCTURE Order
                id : TEXT
                amount : DECIMAL(18, 2)
            END_STRUCTURE

            BUSINESS_RULE ProcessOrder
                INPUT_CONTRACT
                    REQUIRES amount > 0
                END_INPUT_CONTRACT

                OUTPUT_CONTRACT
                    ENSURES total >= amount
                END_OUTPUT_CONTRACT

                OPERATION CalculateTotal(order: Order, taxRate: DECIMAL(5, 4)) : DECIMAL(18, 2)
                BEGIN
                    VARIABLE tax : DECIMAL(18, 2) <- order.amount * taxRate
                    VARIABLE total : DECIMAL(18, 2) <- order.amount + tax
                    IF tax > 100 THEN
                        PRINT "High tax order"
                    END_IF
                    RETURN total
                END
            END_BUSINESS_RULE

            END_PROGRAM
            """;

        ThzLexer lexer = new ThzLexer(codigoEn);
        assertEquals(DialetoLinguagem.EN_US, lexer.getDialeto());

        var tokens = lexer.tokenize();
        assertFalse(tokens.isEmpty());

        ThzParser parser = new ThzParser(tokens);
        ProgramaAst ast = parser.parse();

        assertNotNull(ast);
        assertEquals("OrderProcessing", ast.nome());
        assertEquals(1, ast.estruturas().size());
        assertEquals(1, ast.regras().size());
    }

    @Test
    @DisplayName("Deve proibir mistura de palavras-chave PT-BR em arquivo configurado para EN-US")
    void deveProibirMisturaPtEmArquivoEn() {
        String codigoMisto = """
            LANGUAGE: en-US
            PROGRAM OrderProcessing

            REGRA_NEGOCIO ProcessOrder
            END_PROGRAM
            """;

        ThzLexer lexer = new ThzLexer(codigoMisto);
        assertEquals(DialetoLinguagem.EN_US, lexer.getDialeto());

        ErroLexico erro = assertThrows(ErroLexico.class, lexer::tokenize);
        assertTrue(erro.getMessage().contains("REGRA_NEGOCIO") && erro.getMessage().contains("BUSINESS_RULE"),
                "Mensagem deve orientar o uso do equivalente em inglês: " + erro.getMessage());
    }

    @Test
    @DisplayName("Deve proibir mistura de palavras-chave EN-US em arquivo configurado para PT-BR")
    void deveProibirMisturaEnEmArquivoPt() {
        String codigoMisto = """
            LINGUAGEM: pt-BR
            PROGRAMA ProcessamentoPedidos

            BUSINESS_RULE ProcessarPedido
            FIM_PROGRAMA
            """;

        ThzLexer lexer = new ThzLexer(codigoMisto);
        assertEquals(DialetoLinguagem.PT_BR, lexer.getDialeto());

        ErroLexico erro = assertThrows(ErroLexico.class, lexer::tokenize);
        assertTrue(erro.getMessage().contains("BUSINESS_RULE") && erro.getMessage().contains("REGRA_NEGOCIO"),
                "Mensagem deve orientar o uso do equivalente em português: " + erro.getMessage());
    }

    @Test
    @DisplayName("Deve traduzir programa PT-BR para EN-US e vice-versa de forma canônica")
    void deveTraduzirProgramaEntreDialetos() {
        String codigoPt = """
            PROGRAMA Faturamento

            METADADOS_ARQUITETURA
                DOMINIO: "Fiscal"
                VERSAO: "2.4.0"
            FIM_METADADOS

            ESTRUTURA ItemFatura
                descricao : TEXTO
                valor : DECIMAL(18, 2)
            FIM_ESTRUTURA

            REGRA_NEGOCIO CalcularTotal
                CONTRATO_ENTRADA
                    EXIGE valor > 0
                FIM_CONTRATO_ENTRADA

                OPERACAO Executar(item: ItemFatura) : DECIMAL(18, 2)
                INICIO
                    VARIAVEL total : DECIMAL(18, 2) <- item.valor
                    SE total > 1000 ENTAO
                        EXIBA "Fatura Alta"
                    FIM_SE
                    RETORNE total
                FIM
            FIM_REGRA_NEGOCIO

            FIM_PROGRAMA
            """;

        ThzLexer lexerPt = new ThzLexer(codigoPt);
        ThzParser parserPt = new ThzParser(lexerPt.tokenize());
        ProgramaAst astPt = parserPt.parse();

        // 1. Traduz PT-BR -> EN-US
        String codigoEn = Formatador.formatar(astPt, DialetoLinguagem.EN_US);
        assertTrue(codigoEn.contains("LANGUAGE: en-US"));
        assertTrue(codigoEn.contains("PROGRAM Faturamento"));
        assertTrue(codigoEn.contains("STRUCTURE ItemFatura"));
        assertTrue(codigoEn.contains("BUSINESS_RULE CalcularTotal"));
        assertTrue(codigoEn.contains("REQUIRES valor > 0"));
        assertTrue(codigoEn.contains("VARIABLE total: DECIMAL(18, 2) <- item.valor"));
        assertTrue(codigoEn.contains("IF total > 1000"));
        assertTrue(codigoEn.contains("PRINT \"Fatura Alta\""));
        assertTrue(codigoEn.contains("RETURN total"));
        assertTrue(codigoEn.contains("END_PROGRAM"));

        // 2. Faz o parsing do código EN-US gerado
        ThzLexer lexerEn = new ThzLexer(codigoEn);
        assertEquals(DialetoLinguagem.EN_US, lexerEn.getDialeto());
        ThzParser parserEn = new ThzParser(lexerEn.tokenize());
        ProgramaAst astEn = parserEn.parse();

        // 3. Traduz de volta EN-US -> PT-BR
        String codigoPtRevertido = Formatador.formatar(astEn, DialetoLinguagem.PT_BR);
        assertTrue(codigoPtRevertido.contains("LINGUAGEM: pt-BR"));
        assertTrue(codigoPtRevertido.contains("PROGRAMA Faturamento"));
        assertTrue(codigoPtRevertido.contains("REGRA_NEGOCIO CalcularTotal"));
        assertTrue(codigoPtRevertido.contains("EXIGE valor > 0"));
        assertTrue(codigoPtRevertido.contains("VARIAVEL total: DECIMAL(18, 2) <- item.valor"));
        assertTrue(codigoPtRevertido.contains("SE total > 1000"));
        assertTrue(codigoPtRevertido.contains("EXIBA \"Fatura Alta\""));
        assertTrue(codigoPtRevertido.contains("RETORNE total"));
        assertTrue(codigoPtRevertido.contains("FIM_PROGRAMA"));
    }

    @Test
    @DisplayName("Deve processar escapes Unicode e especiais em literais de texto")
    void deveProcessarEscapesUnicodeEEspeciais() {
        String codigo = """
            PROGRAMA TesteEscapes
            VARIAVEL msg : TEXTO <- "Linha 1\\nLinha 2\\tTabulado\\u00A9 Copyright"
            FIM_PROGRAMA
            """;

        ThzLexer lexer = new ThzLexer(codigo);
        var tokens = lexer.tokenize();

        var tokenString = tokens.stream()
                .filter(t -> t.type() == TokenType.STRING_LITERAL)
                .findFirst()
                .orElseThrow();

        assertEquals("Linha 1\nLinha 2\tTabulado© Copyright", tokenString.value());
    }
}
