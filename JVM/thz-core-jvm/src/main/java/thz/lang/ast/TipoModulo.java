package thz.lang.ast;

/**
 * Enumeração dos Arquétipos de Módulo reconhecidos no THZ-LANG v2.4.
 *
 * <p>Cada arquivo fonte declara um arquétipo declarativo explícito que define
 * sua semântica de execução e seu terminador obrigatório pareado.</p>
 *
 * @author THZ-LANG Core Team
 * @version 2.4.0
 */
public enum TipoModulo {
    /** Programa genérico. */
    PROGRAMA("PROGRAMA"),
    /** Programa de aplicação visual interativa. */
    PROGRAMA_VISUAL("PROGRAMA VISUAL"),
    /** Programa de serviços de regras de negócio corporativo. */
    PROGRAMA_NEGOCIO("PROGRAMA NEGOCIO"),
    /** Programa de mapeamento de arquitetura de software. */
    PROGRAMA_ARQUITETURA("PROGRAMA ARQUITETURA"),
    /** Biblioteca reutilizável de funções e procedimentos. */
    BIBLIOTECA("BIBLIOTECA"),
    /** Módulo de extensão de sistema. */
    EXTENSAO("EXTENSAO"),
    /** Script ou utilitário de linha de comando. */
    FERRAMENTA("FERRAMENTA"),
    /** Suíte declarativa de testes automatizados. */
    TESTE("TESTE"),
    /** Interface gráfica declarativa (.thzui). */
    TELA("TELA");

    private final String descricao;

    TipoModulo(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna a descrição canônica do arquétipo em texto.
     *
     * @return Nome descritivo do arquétipo
     */
    public String descricao() {
        return descricao;
    }

    /**
     * Verifica se o arquétipo atual representa uma modalidade executável de programa ou tela.
     *
     * @return {@code true} se for um tipo de programa ou tela; {@code false} caso contrário
     */
    public boolean ehPrograma() {
        return this == PROGRAMA || this == PROGRAMA_VISUAL || this == PROGRAMA_NEGOCIO || this == PROGRAMA_ARQUITETURA || this == TELA;
    }

    /**
     * Retorna o token terminador de bloco obrigatório correspondente ao arquétipo.
     *
     * @return Nome do terminador (ex: {@code "FIM_PROGRAMA"}, {@code "FIM_BIBLIOTECA"}, {@code "FIM_TELA"})
     */
    public String terminadorPadrao() {
        return switch (this) {
            case PROGRAMA, PROGRAMA_VISUAL, PROGRAMA_NEGOCIO, PROGRAMA_ARQUITETURA -> "FIM_PROGRAMA";
            case BIBLIOTECA -> "FIM_BIBLIOTECA";
            case EXTENSAO -> "FIM_EXTENSAO";
            case FERRAMENTA -> "FIM_FERRAMENTA";
            case TESTE -> "FIM_TESTE";
            case TELA -> "FIM_TELA";
        };
    }
}
