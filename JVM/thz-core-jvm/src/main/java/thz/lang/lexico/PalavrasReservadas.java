package thz.lang.lexico;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tabela e utilitários de palavras reservadas e conectivos do THZ-LANG.
 * Suporta dialeto primário PT-BR e dialeto internacional EN-US com estrita pureza sintática.
 */
public final class PalavrasReservadas {

    public static final String VERSAO_LINGUAGEM_ATUAL = "2.4.0";

    public record EntradaPalavra(TokenType token, CategoriaPalavra categoria) {}

    // 1. Tabela Canônica PT-BR
    private static final Map<String, EntradaPalavra> TABELA_PT_BR = Map.ofEntries(
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
        Map.entry("FIM_REGRA", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
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
        Map.entry("RETORNAR", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
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
        Map.entry("PIPELINE_DADOS", new EntradaPalavra(TokenType.PIPELINE_DADOS, CategoriaPalavra.DECLARACAO)),
        Map.entry("FONTE_ENTRADA", new EntradaPalavra(TokenType.FONTE_ENTRADA, CategoriaPalavra.DECLARACAO)),
        Map.entry("DESTINO_SAIDA", new EntradaPalavra(TokenType.DESTINO_SAIDA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRANSFORMACAO", new EntradaPalavra(TokenType.TRANSFORMACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_PIPELINE", new EntradaPalavra(TokenType.FIM_PIPELINE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_FONTE", new EntradaPalavra(TokenType.FIM_FONTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_DESTINO", new EntradaPalavra(TokenType.FIM_DESTINO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_TRANSFORMACAO", new EntradaPalavra(TokenType.FIM_TRANSFORMACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_VETORIZAR", new EntradaPalavra(TokenType.FIM_VETORIZAR, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("ENTAO", new EntradaPalavra(TokenType.ENTAO, CategoriaPalavra.CONTROLE)),
        Map.entry("FACA", new EntradaPalavra(TokenType.FACA, CategoriaPalavra.CONTROLE)),
        Map.entry("STREAMING", new EntradaPalavra(TokenType.STREAMING, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LOTE", new EntradaPalavra(TokenType.LOTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CONECTOR", new EntradaPalavra(TokenType.CONECTOR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FORMATO", new EntradaPalavra(TokenType.FORMATO, CategoriaPalavra.MODIFICADOR)),
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
        Map.entry("NAO", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        // Consultas Tipadas (LINQ)
        Map.entry("CONSULTAR", new EntradaPalavra(TokenType.CONSULTAR, CategoriaPalavra.CONTROLE)),
        Map.entry("ONDE", new EntradaPalavra(TokenType.ONDE, CategoriaPalavra.CONTROLE)),
        Map.entry("ORDENAR_POR", new EntradaPalavra(TokenType.ORDENAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("AGRUPAR_POR", new EntradaPalavra(TokenType.AGRUPAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIMITE", new EntradaPalavra(TokenType.LIMITE, CategoriaPalavra.DECLARACAO)),
        Map.entry("PULAR", new EntradaPalavra(TokenType.PULAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("ASC", new EntradaPalavra(TokenType.ASC, CategoriaPalavra.DECLARACAO)),
        Map.entry("DESC", new EntradaPalavra(TokenType.DESC, CategoriaPalavra.DECLARACAO)),
        Map.entry("BLOCO_NATIVO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("NATIVO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("CODIGO_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("FIM_BLOCO_NATIVO", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("FIM_NATIVO", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO))
    );

    // 2. Tabela Equivalente EN-US
    private static final Map<String, EntradaPalavra> TABELA_EN_US = Map.ofEntries(
        Map.entry("PROGRAM", new EntradaPalavra(TokenType.PROGRAMA, CategoriaPalavra.DECLARACAO)),
        Map.entry("VISUAL", new EntradaPalavra(TokenType.VISUAL, CategoriaPalavra.MODIFICADOR)),
        Map.entry("BUSINESS", new EntradaPalavra(TokenType.NEGOCIO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ARCHITECTURE", new EntradaPalavra(TokenType.ARQUITETURA, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIBRARY", new EntradaPalavra(TokenType.BIBLIOTECA, CategoriaPalavra.DECLARACAO)),
        Map.entry("EXTENSION", new EntradaPalavra(TokenType.EXTENSAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("TOOL", new EntradaPalavra(TokenType.FERRAMENTA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TEST", new EntradaPalavra(TokenType.TESTE, CategoriaPalavra.DECLARACAO)),
        Map.entry("SCREEN", new EntradaPalavra(TokenType.TELA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ARCHITECTURE_METADATA", new EntradaPalavra(TokenType.METADADOS_ARQUITETURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("STRUCTURE", new EntradaPalavra(TokenType.ESTRUTURA, CategoriaPalavra.DECLARACAO)),
        Map.entry("ENUM", new EntradaPalavra(TokenType.ENUMERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("BUSINESS_RULE", new EntradaPalavra(TokenType.REGRA_NEGOCIO, CategoriaPalavra.DECLARACAO)),
        Map.entry("PROCEDURE", new EntradaPalavra(TokenType.PROCEDIMENTO, CategoriaPalavra.DECLARACAO)),
        Map.entry("OPERATION", new EntradaPalavra(TokenType.OPERACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("VARIABLE", new EntradaPalavra(TokenType.VARIAVEL, CategoriaPalavra.DECLARACAO)),
        Map.entry("LANGUAGE_VERSION", new EntradaPalavra(TokenType.VERSAO_LINGUAGEM, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_PROGRAM", new EntradaPalavra(TokenType.FIM_PROGRAMA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_LIBRARY", new EntradaPalavra(TokenType.FIM_BIBLIOTECA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_EXTENSION", new EntradaPalavra(TokenType.FIM_EXTENSAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TOOL", new EntradaPalavra(TokenType.FIM_FERRAMENTA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TEST", new EntradaPalavra(TokenType.FIM_TESTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_SCREEN", new EntradaPalavra(TokenType.FIM_TELA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_METADATA", new EntradaPalavra(TokenType.FIM_METADADOS, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_STRUCTURE", new EntradaPalavra(TokenType.FIM_ESTRUTURA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_ENUM", new EntradaPalavra(TokenType.FIM_ENUMERACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_BUSINESS_RULE", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_RULE", new EntradaPalavra(TokenType.FIM_REGRA_NEGOCIO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_FOR", new EntradaPalavra(TokenType.FIM_PARA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_MEMORY_BLOCK", new EntradaPalavra(TokenType.FIM_BLOCO_MEMORIA, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_IF", new EntradaPalavra(TokenType.FIM_SE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_WHILE", new EntradaPalavra(TokenType.FIM_ENQUANTO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END", new EntradaPalavra(TokenType.FIM, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("REQUIRES", new EntradaPalavra(TokenType.EXIGE, CategoriaPalavra.CONTRATO)),
        Map.entry("ENSURES", new EntradaPalavra(TokenType.GARANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INVARIANT", new EntradaPalavra(TokenType.INVARIANTE, CategoriaPalavra.CONTRATO)),
        Map.entry("INPUT_CONTRACT", new EntradaPalavra(TokenType.CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("END_INPUT_CONTRACT", new EntradaPalavra(TokenType.FIM_CONTRATO_ENTRADA, CategoriaPalavra.CONTRATO)),
        Map.entry("OUTPUT_CONTRACT", new EntradaPalavra(TokenType.CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("END_OUTPUT_CONTRACT", new EntradaPalavra(TokenType.FIM_CONTRATO_SAIDA, CategoriaPalavra.CONTRATO)),
        Map.entry("START", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("BEGIN", new EntradaPalavra(TokenType.INICIO, CategoriaPalavra.CONTROLE)),
        Map.entry("IF", new EntradaPalavra(TokenType.SE, CategoriaPalavra.CONTROLE)),
        Map.entry("ELSE", new EntradaPalavra(TokenType.SENAO, CategoriaPalavra.CONTROLE)),
        Map.entry("WHILE", new EntradaPalavra(TokenType.ENQUANTO, CategoriaPalavra.CONTROLE)),
        Map.entry("RETURN", new EntradaPalavra(TokenType.RETORNE, CategoriaPalavra.CONTROLE)),
        Map.entry("FAIL_WITH", new EntradaPalavra(TokenType.FALHAR_COM, CategoriaPalavra.CONTROLE)),
        Map.entry("DISPLAY", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("PRINT", new EntradaPalavra(TokenType.EXIBA, CategoriaPalavra.CONTROLE)),
        Map.entry("READ", new EntradaPalavra(TokenType.LER, CategoriaPalavra.CONTROLE)),
        Map.entry("VECTORIZE_FOR", new EntradaPalavra(TokenType.VETORIZAR_PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("USE_MEMORY_BLOCK", new EntradaPalavra(TokenType.USAR_BLOCO_MEMORIA, CategoriaPalavra.MEMORIA)),
        Map.entry("COLUMNAR_LAYOUT", new EntradaPalavra(TokenType.LAYOUT_COLUNAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("IMPORT", new EntradaPalavra(TokenType.IMPORTAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("MATCH_RESULT", new EntradaPalavra(TokenType.CASO_RESULTADO, CategoriaPalavra.CONTROLE)),
        Map.entry("END_MATCH", new EntradaPalavra(TokenType.FIM_CASO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("SUCCESS", new EntradaPalavra(TokenType.SUCESSO, CategoriaPalavra.CONTROLE)),
        Map.entry("ERROR", new EntradaPalavra(TokenType.ERRO, CategoriaPalavra.CONTROLE)),
        Map.entry("IDEMPOTENT", new EntradaPalavra(TokenType.IDEMPOTENTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("IDEMPOTENCY_KEY", new EntradaPalavra(TokenType.CHAVE_IDEMPOTENCIA, CategoriaPalavra.CONTRATO)),
        Map.entry("IN", new EntradaPalavra(TokenType.EM, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DATA_PIPELINE", new EntradaPalavra(TokenType.PIPELINE_DADOS, CategoriaPalavra.DECLARACAO)),
        Map.entry("INPUT_SOURCE", new EntradaPalavra(TokenType.FONTE_ENTRADA, CategoriaPalavra.DECLARACAO)),
        Map.entry("OUTPUT_TARGET", new EntradaPalavra(TokenType.DESTINO_SAIDA, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRANSFORMATION", new EntradaPalavra(TokenType.TRANSFORMACAO, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_PIPELINE", new EntradaPalavra(TokenType.FIM_PIPELINE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_SOURCE", new EntradaPalavra(TokenType.FIM_FONTE, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TARGET", new EntradaPalavra(TokenType.FIM_DESTINO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_TRANSFORMATION", new EntradaPalavra(TokenType.FIM_TRANSFORMACAO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_VECTORIZE", new EntradaPalavra(TokenType.FIM_VETORIZAR, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("THEN", new EntradaPalavra(TokenType.ENTAO, CategoriaPalavra.CONTROLE)),
        Map.entry("DO", new EntradaPalavra(TokenType.FACA, CategoriaPalavra.CONTROLE)),
        Map.entry("BATCH", new EntradaPalavra(TokenType.LOTE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CONNECTOR", new EntradaPalavra(TokenType.CONECTOR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FORMAT", new EntradaPalavra(TokenType.FORMATO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("SIMD_STEP", new EntradaPalavra(TokenType.PASSO_SIMD, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FOR", new EntradaPalavra(TokenType.PARA, CategoriaPalavra.CONTROLE)),
        Map.entry("STEP", new EntradaPalavra(TokenType.PASSO, CategoriaPalavra.MODIFICADOR)),
        Map.entry("FROM", new EntradaPalavra(TokenType.DE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("TO", new EntradaPalavra(TokenType.ATE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("CREATE", new EntradaPalavra(TokenType.CRIAR, CategoriaPalavra.DECLARACAO)),
        Map.entry("TRUE", new EntradaPalavra(TokenType.VERDADEIRO, CategoriaPalavra.LITERAL)),
        Map.entry("FALSE", new EntradaPalavra(TokenType.FALSO, CategoriaPalavra.LITERAL)),
        Map.entry("NULL", new EntradaPalavra(TokenType.NULO, CategoriaPalavra.LITERAL)),
        Map.entry("AND", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("OR", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        Map.entry("NOT", new EntradaPalavra(TokenType.OPERADOR_LOGICO, CategoriaPalavra.CONECTIVO_LOGICO)),
        // Query DSL EN-US
        Map.entry("QUERY", new EntradaPalavra(TokenType.CONSULTAR, CategoriaPalavra.CONTROLE)),
        Map.entry("WHERE", new EntradaPalavra(TokenType.ONDE, CategoriaPalavra.CONTROLE)),
        Map.entry("ORDER_BY", new EntradaPalavra(TokenType.ORDENAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("GROUP_BY", new EntradaPalavra(TokenType.AGRUPAR_POR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("LIMIT", new EntradaPalavra(TokenType.LIMITE, CategoriaPalavra.MODIFICADOR)),
        Map.entry("OFFSET", new EntradaPalavra(TokenType.PULAR, CategoriaPalavra.MODIFICADOR)),
        Map.entry("ASC", new EntradaPalavra(TokenType.ASC, CategoriaPalavra.MODIFICADOR)),
        Map.entry("DESC", new EntradaPalavra(TokenType.DESC, CategoriaPalavra.MODIFICADOR)),
        Map.entry("NATIVE_RUST_BLOCK", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("NATIVE_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("INLINE_RUST", new EntradaPalavra(TokenType.BLOCO_NATIVO_RUST, CategoriaPalavra.DECLARACAO)),
        Map.entry("END_NATIVE_BLOCK", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO)),
        Map.entry("END_NATIVE", new EntradaPalavra(TokenType.FIM_BLOCO_NATIVO, CategoriaPalavra.FIM_BLOCO))
    );

    // Mapeamentos de Tradução Bidirecional
    private static final Map<String, String> TRADUCAO_PT_PARA_EN = new HashMap<>();
    private static final Map<String, String> TRADUCAO_EN_PARA_PT = new HashMap<>();

    static {
        // Inicializa mapeamento bi-direcional através da correspondência de TokenType
        for (var entryPt : TABELA_PT_BR.entrySet()) {
            for (var entryEn : TABELA_EN_US.entrySet()) {
                if (entryPt.getValue().token() == entryEn.getValue().token()) {
                    TRADUCAO_PT_PARA_EN.putIfAbsent(entryPt.getKey(), entryEn.getKey());
                    TRADUCAO_EN_PARA_PT.putIfAbsent(entryEn.getKey(), entryPt.getKey());
                }
            }
        }
    }

    private PalavrasReservadas() {}

    public static boolean ehPalavraReservada(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return false;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        return tab.containsKey(palavra);
    }

    public static boolean ehPalavraReservada(String palavra) {
        return ehPalavraReservada(palavra, DialetoLinguagem.PT_BR) || ehPalavraReservada(palavra, DialetoLinguagem.EN_US);
    }

    public static TokenType tokenDe(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return null;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        EntradaPalavra e = tab.get(palavra);
        return e == null ? null : e.token();
    }

    public static TokenType tokenDe(String palavra) {
        return tokenDe(palavra, DialetoLinguagem.PT_BR);
    }

    public static CategoriaPalavra categoriaDe(String palavra, DialetoLinguagem dialeto) {
        if (palavra == null) return null;
        Map<String, EntradaPalavra> tab = (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
        EntradaPalavra e = tab.get(palavra);
        return e == null ? null : e.categoria();
    }

    public static CategoriaPalavra categoriaDe(String palavra) {
        return categoriaDe(palavra, DialetoLinguagem.PT_BR);
    }

    public static Set<String> palavras(DialetoLinguagem dialeto) {
        return (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US.keySet() : TABELA_PT_BR.keySet();
    }

    public static Set<String> palavras() {
        return palavras(DialetoLinguagem.PT_BR);
    }

    public static Map<String, EntradaPalavra> tabela(DialetoLinguagem dialeto) {
        return (dialeto == DialetoLinguagem.EN_US) ? TABELA_EN_US : TABELA_PT_BR;
    }

    public static Map<String, EntradaPalavra> tabela() {
        return TABELA_PT_BR;
    }

    public static String traduzir(String palavra, DialetoLinguagem destino) {
        if (palavra == null) return "";
        if (destino == DialetoLinguagem.EN_US) {
            return TRADUCAO_PT_PARA_EN.getOrDefault(palavra, palavra);
        } else {
            return TRADUCAO_EN_PARA_PT.getOrDefault(palavra, palavra);
        }
    }

    public static void validarPurezaDialeto(String palavra, DialetoLinguagem dialetoAtivo, int linha, int col) {
        if (palavra == null) return;
        if (dialetoAtivo == DialetoLinguagem.EN_US) {
            if (TABELA_PT_BR.containsKey(palavra) && !TABELA_EN_US.containsKey(palavra)) {
                String equivalente = traduzir(palavra, DialetoLinguagem.EN_US);
                throw new ErroLexico(linha, col,
                        "Palavra-chave '" + palavra + "' pertence ao dialeto [pt-BR] e não pode ser usada em arquivo configurado para [en-US]. " +
                        "Use a palavra equivalente em inglês '" + equivalente + "' ou altere a diretiva para 'LINGUAGEM: pt-BR'.");
            }
        } else {
            if (TABELA_EN_US.containsKey(palavra) && !TABELA_PT_BR.containsKey(palavra)) {
                String equivalente = traduzir(palavra, DialetoLinguagem.PT_BR);
                throw new ErroLexico(linha, col,
                        "Palavra-chave '" + palavra + "' pertence ao dialeto [en-US] e não pode ser usada em arquivo configurado para [pt-BR]. " +
                        "Use a palavra equivalente em português '" + equivalente + "' ou declare 'LANGUAGE: en-US' no cabeçalho.");
            }
        }
    }
}
