package thz.lang.lexico;

import java.util.Map;
import java.util.Set;

/**
 * Fonte da verdade léxica para todas as palavras reservadas e conectivos do THZ-LANG.
 *
 * <p>Esta classe contêm a tabela imutável de palavras-chave da linguagem, categorizando
 * cada lexema e mapeando-o para o respectivo {@link TokenType} e {@link CategoriaPalavra}.</p>
 *
 * @author THZ-LANG Core Team
 * @version 2.4.0
 */
public final class PalavrasReservadas {

    /**
     * Versão corrente da especificação léxica e sintática da linguagem THZ-LANG.
     */
    public static final String VERSAO_LINGUAGEM_ATUAL = "2.4.0";

    /**
     * Registro que encapsula o token e a categoria de uma palavra reservada.
     *
     * @param token Tipo de token associado à palavra reservada
     * @param categoria Categoria sintático-semântica da palavra
     */
    public record EntradaPalavra(TokenType token, CategoriaPalavra categoria) {}

    private static final Map<String, EntradaPalavra> TABELA = Map.ofEntries(
        Map.entry("PROGRAMA", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("VISUAL", new EntradaPalavra(TokenType.VISUAL, CategoriaPalavra.MODIFICADOR)),
        Map.entry("NEGOCIO", new EntradaPalavra(TokenType.NEGOCIO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ARQUITETURA", new EntradaPalavra(TokenType.ARQUITETURA, CategoriaPalavra.MODIFICADOR)),
        Map.entry("BIBLIOTECA", new EntradaPalavra(TokenType.BIBLIOTECA, CategoriaPalavra.DECLARACAO)),
        Map.entry("EXTENSAO", new EntradaPalavra(TokenType.EXTENSAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FERRAMENTA", new EntradaPalavra(TokenType.FERRAMENTA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TESTE", new EntradaPalavra(TokenType.TESTE, CategoriaPalavra.DECLARACAO)),
        Map.entry("TELA", new EntradaPalavra(TokenType.TELA, CategoriaPalavra.DECLARACAO)),
        Map.entry("METADADOS_ARQUITETURA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ESTRUTURA", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ENUMERACAO", new EntradaPalavra(TokenType.ENUMERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("REGRA_NEGOCIO", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROCEDIMENTO", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("OPERACAO", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("VARIAVEL", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("VERSAO_LINGUAGEM", new EntradaPalavra(TokenType.VERSAO_LINGUAGEM, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_PROGRAMA", new EntradaPalavra(TokenType.FIM_PROGRAMA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_BIBLIOTECA", new EntradaPalavra(TokenType.FIM_BIBLIOTECA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_EXTENSAO", new EntradaPalavra(TokenType.FIM_EXTENSAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_FERRAMENTA", new EntradaPalavra(TokenType.FIM_FERRAMENTA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TESTE", new EntradaPalavra(TokenType.FIM_TESTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TELA", new EntradaPalavra(TokenType.FIM_TELA, CategoriaPalavra.FIM_BLOCO)),
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
        Map.entry("IMPORTAR", new EntradaPalavra(TokenType.IMPORTAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("CASO_RESULTADO", new EntradaPalavra(TokenType.CASO_RESULTADO, CategoriaPalavra.CONTROLE)),
        Map.entry("FIM_CASO", new EntradaPalavra(TokenType.FIM_CASO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("SUCESSO", new EntradaPalavra(TokenType.SUCESSO, CategoriaPalavra.CONTROLE)),
        Map.entry("ERRO", new EntradaPalavra(TokenType.ERRO, CategoriaPalavra.CONTROLE)),
        Map.entry("IDEMPOTENTE", new EntradaPalavra(TokenType.IDEMPOTENTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CHAVE_IDEMPOTENCIA", new EntradaPalavra(TokenType.CHAVE_IDEMPOTENCIA, CategoriaPalavra.CONTRATO)),
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

    /**
     * Verifica se uma dada palavra (lexema) é uma palavra reservada da linguagem.
     *
     * @param palavra Palavra a ser verificada
     * @return {@code true} se for palavra reservada; {@code false} caso contrário
     */
    public static boolean ehPalavraReservada(String palavra) {
        return palavra != null && TABELA.containsKey(palavra);
    }

    /**
     * Retorna o {@link TokenType} associado a uma palavra reservada.
     *
     * @param palavra Palavra reservada
     * @return O token correspondente ou {@code null} se não for reservada
     */
    public static TokenType tokenDe(String palavra) {
        if (palavra == null) return null;
        EntradaPalavra e = TABELA.get(palavra);
        return e == null ? null : e.token();
    }

    /**
     * Retorna a {@link CategoriaPalavra} associada a uma palavra reservada.
     *
     * @param palavra Palavra reservada
     * @return A categoria correspondente ou {@code null} se não for reservada
     */
    public static CategoriaPalavra categoriaDe(String palavra) {
        if (palavra == null) return null;
        EntradaPalavra e = TABELA.get(palavra);
        return e == null ? null : e.categoria();
    }

    /**
     * Retorna o conjunto completo de lexemas de palavras reservadas.
     *
     * @return Conjunto imutável de palavras reservadas
     */
    public static Set<String> palavras() {
        return TABELA.keySet();
    }

    /**
     * Retorna a tabela completa de mapeamento de palavras reservadas.
     *
     * @return Mapa imutável com as entradas de palavras reservadas
     */
    public static Map<String, EntradaPalavra> tabela() {
        return TABELA;
    }
}
