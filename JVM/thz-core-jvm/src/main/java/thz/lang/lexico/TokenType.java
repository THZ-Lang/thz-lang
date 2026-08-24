package thz.lang.lexico;

/**
 * Enumeração canônica dos tipos de tokens reconhecidos pelo analisador léxico ({@link ThzLexer})
 * do THZ-LANG.
 *
 * <p>Em conformidade com as diretrizes de governança da linguagem, cada palavra reservada,
 * delimitador ou literal possui uma constante associada nesta classe.</p>
 *
 * @author THZ-LANG Core Team
 * @version 2.4.0
 */
public enum TokenType {
    /** Palavra reservada 'PROGRAMA'. */
    PROGRAMA,
    /** Palavra reservada 'VISUAL'. */
    VISUAL,
    /** Palavra reservada 'NEGOCIO'. */
    NEGOCIO,
    /** Palavra reservada 'ARQUITETURA'. */
    ARQUITETURA,
    /** Palavra reservada 'BIBLIOTECA'. */
    BIBLIOTECA,
    /** Palavra reservada 'EXTENSAO'. */
    EXTENSAO,
    /** Palavra reservada 'FERRAMENTA'. */
    FERRAMENTA,
    /** Palavra reservada 'TESTE'. */
    TESTE,
    /** Palavra reservada 'TELA'. */
    TELA,
    /** Terminador obrigatorio 'FIM_PROGRAMA'. */
    FIM_PROGRAMA,
    /** Terminador obrigatorio 'FIM_BIBLIOTECA'. */
    FIM_BIBLIOTECA,
    /** Terminador obrigatorio 'FIM_EXTENSAO'. */
    FIM_EXTENSAO,
    /** Terminador obrigatorio 'FIM_FERRAMENTA'. */
    FIM_FERRAMENTA,
    /** Terminador obrigatorio 'FIM_TESTE'. */
    FIM_TESTE,
    /** Terminador obrigatorio 'FIM_TELA'. */
    FIM_TELA,
    /** Bloco de metadados de arquitetura (ISO/IEC/IEEE 42010). */
    METADADOS_ARQUITETURA,
    /** Encerramento do bloco de metadados de arquitetura. */
    FIM_METADADOS,
    /** Declaracao de estrutura de dados. */
    ESTRUTURA,
    /** Encerramento de estrutura de dados. */
    FIM_ESTRUTURA,
    /** Declaracao de enumeração finita. */
    ENUMERACAO,
    /** Encerramento de enumeração. */
    FIM_ENUMERACAO,
    /** Declaracao de regra de negócio autônoma auditável. */
    REGRA_NEGOCIO,
    /** Encerramento de regra de negócio. */
    FIM_REGRA_NEGOCIO,
    /** Declaracao de procedimento. */
    PROCEDIMENTO,
    /** Inicio de bloco de codigo. */
    INICIO,
    /** Encerramento de bloco de codigo ou procedimento. */
    FIM,
    /** Cláusula de pre-condição contratual. */
    EXIGE,
    /** Cláusula de pos-condição contratual. */
    GARANTE,
    /** Invariante de entidade ou estado. */
    INVARIANTE,
    /** Disparo de falha explicita de resultado. */
    FALHAR_COM,
    /** Cláusula de contrato de entrada. */
    CONTRATO_ENTRADA,
    /** Encerramento de contrato de entrada. */
    FIM_CONTRATO_ENTRADA,
    /** Cláusula de contrato de saída. */
    CONTRATO_SAIDA,
    /** Encerramento de contrato de saída. */
    FIM_CONTRATO_SAIDA,
    /** Declaracao de variável local. */
    VARIAVEL,
    /** Comando de retorno de valor. */
    RETORNE,
    /** Comando de exibicao no console. */
    EXIBA,
    /** Declaracao de operacao. */
    OPERACAO,
    /** Condicional SE. */
    SE,
    /** Condicional SENAO. */
    SENAO,
    /** Laço de repeticao ENQUANTO. */
    ENQUANTO,
    /** Encerramento de condicional SE. */
    FIM_SE,
    /** Encerramento de laço ENQUANTO. */
    FIM_ENQUANTO,
    /** Literal booleano verdadeiro. */
    VERDADEIRO,
    /** Literal booleano falso. */
    FALSO,
    /** Literal nulo. */
    NULO,
    /** Laço vetorizado SIMD (Single Instruction, Multiple Data). */
    VETORIZAR_PARA,
    /** Pertencimento em conjunto. */
    EM,
    /** Tamanho do passo vetorial SIMD (AVX/AVX-512). */
    PASSO_SIMD,
    /** Laço de repetição PARA. */
    PARA,
    /** Tamanho de passo de iteracao. */
    PASSO,
    /** Inicio de intervalo. */
    DE,
    /** Fim de intervalo. */
    ATE,
    /** Instanciacao de entidade. */
    CRIAR,
    /** Leitura de propriedade. */
    LER,
    /** Encerramento de laço PARA. */
    FIM_PARA,
    /** Alocacao contigua de memoria em Arena O(1). */
    USAR_BLOCO_MEMORIA,
    /** Encerramento de bloco de memoria em Arena. */
    FIM_BLOCO_MEMORIA,
    /** Layout colunar Structure of Arrays (SoA). */
    LAYOUT_COLUNAR,
    /** Pragma de declaracao de versão da linguagem. */
    VERSAO_LINGUAGEM,
    /** Importação de símbolos e módulos. */
    IMPORTAR,
    /** Desempacotamento declarativo de resultado. */
    CASO_RESULTADO,
    /** Encerramento de CASO_RESULTADO. */
    FIM_CASO,
    /** Ramo de sucesso de um resultado. */
    SUCESSO,
    /** Ramo de erro de um resultado. */
    ERRO,
    /** Seta de associação de caso. */
    SETA_CASO,
    /** Modificador de operacao idempotente. */
    IDEMPOTENTE,
    /** Chave de rastreamento de idempotência. */
    CHAVE_IDEMPOTENCIA,
    /** Operador logico (E, OU, NAO). */
    OPERADOR_LOGICO,
    /** Seta de atribuicao (<-). */
    SETA_ATRIBUICAO,

    /** Identificador alfanumérico de variável ou função. */
    IDENTIFICADOR,
    /** Literal de texto delimitado por aspas. */
    STRING_LITERAL,
    /** Literal numérico (inteiro ou decimal exato ISO 10967). */
    NUMERO_LITERAL,
    /** Delimitador dois-pontos (:). */
    DOIS_PONTOS,
    /** Delimitador ponto (.). */
    PONTO,
    /** Delimitador vírgula (,). */
    VIRGULA,
    /** Abre parêntese ((). */
    ABRE_PARENTESE,
    /** Fecha parêntese ()). */
    FECHA_PARENTESE,
    /** Abre colchete ([). */
    ABRE_COLCHETE,
    /** Fecha colchete (]). */
    FECHA_COLCHETE,
    /** Operador relacional (==, !=, >, <, >=, <=). */
    OPERADOR_RELACIONAL,
    /** Operador aritmético (+, -, *, /). */
    OPERADOR_ARITMETICO,
    /** Fim de arquivo (End of File). */
    EOF
}
