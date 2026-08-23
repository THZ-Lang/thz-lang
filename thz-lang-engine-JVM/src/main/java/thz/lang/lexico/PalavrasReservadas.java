package thz.lang.lexico;

import java.util.Map;

public final class PalavrasReservadas {

    public static final String VERSAO_LINGUAGEM_ATUAL = "2.3.0";

    public record EntradaPalavra(TokenType token, CategoriaPalavra categoria) {}

    private static final Map<String, EntradaPalavra> TABELA = Map.ofEntries(
        Map.entry("PROGRAMA", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("METADADOS_ARQUITETURA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ESTRUTURA", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ENUMERACAO", new EntradaPalavra(TokenType.ENUMERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("REGRA_NEGOCIO", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROCEDIMENTO", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("OPERACAO", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("VARIAVEL", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VERSAO_LINGUAGEM", new EntradaPalavra(TokenType.VERSAO_LINGUAGEM, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_PROGRAMA", new EntradaPalavra(TokenType.FIM_PROGRAMA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_METADADOS", new EntradaPalavra(TokenType.FIM_METADADOS, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ESTRUTURA", new EntradaPalavra(TokenType.FIM_ESTRUTURA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ENUMERACAO", new EntradaPalavra(TokenType.FIM_ENUMERACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_REGRA_NEGOCIO", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_PARA", new EntradaPalavra(TokenType.FIM_PARA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_BLOCO_MEMORIA", new EntradaPalavra(TokenType.FIM_BLOCO_MEMORIA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_SE", new EntradaPalavra(TokenType.FIM_SE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_ENQUANTO", new EntradaPalavra(TokenType.FIM_ENQUANTO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("EXIGE", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("GARANTE", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INVARIANTE", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("CONTRATO_ENTRADA", new EntradaPalavra(TokenType.CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("FIM_CONTRATO_ENTRADA", new EntradaPalavra(TokenType.FIM_CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("CONTRATO_SAIDA", new EntradaPalavra(TokenType.CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("FIM_CONTRATO_SAIDA", new EntradaPalavra(TokenType.FIM_CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("INICIO", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("SE", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("SENAO", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("ENQUANTO", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("RETORNE", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("FALHAR_COM", new EntradaPalavra(TokenType.FALHAR_COM, CategoriaPalavra.CONTROLE)),
        Map.entry("EXIBA", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("LER", new EntradaPalavra(TokenType.LER, CategoriaPalavra.CONTROLE)),
        Map.entry("VETORIZAR_PARA", new EntradaPalavra(TokenType.VETORIZAR_PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("USAR_BLOCO_MEMORIA", new EntradaPalavra(TokenType.USAR_BLOCO_MEMORIA, CategoriaPalavra.MEMORIA)),
        Map.entry("LAYOUT_COLUNAR", new EntradaPalavra(TokenType.LAYOUT_COLUNAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("EM", new EntradaPalavra(TokenType.EM, CategoriaPalavra.MODIFICADOR)),
        Map.entry("PASSO_SIMD", new EntradaPalavra(TokenType.PASSO_SIMD, CategoriaPalavra.MODIFICADOR)),
        Map.entry("PARA", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("PASSO", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DE", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ATE", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CRIAR", new EntradaPalavra(TokenType.CRIAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("VERDADEIRO", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("FALSO", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("NULO", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("E", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("OU", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("NAO", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO))
    );

    private PalavrasReservadas() {}

    public static boolean ehPalavraReservada(String palavra) {
        return TABELA.containsKey(palavra);
    }

    public static TokenType tokenDe(String palavra) {
        EntradaPalavra e = TABELA.get(palavra);
        return e == null ? null : e.token();
    }

    public static CategoriaPalavra categoriaDe(String palavra) {
        EntradaPalavra e = TABELA.get(palavra);
        return e == null ? null : e.categoria();
    }

    public static java.util.Set<String> palavras() {
        return TABELA.keySet();
    }

    public static Map<String, EntradaPalavra> tabela() {
        return TABELA;
    }
}
