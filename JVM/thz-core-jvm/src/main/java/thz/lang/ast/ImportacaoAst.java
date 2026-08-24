package thz.lang.ast;

/**
 * Nó da Árvore Sintática Abstrata (AST) que representa uma cláusula de importação de módulo.
 *
 * @param modulo Nome dos símbolos ou módulo a ser importado
 * @param caminho Caminho relativo ou absoluto do arquivo de origem (.thz / .thzui)
 * @param linha Posição da linha no código-fonte onde a cláusula foi declarada (1-indexed)
 * @param coluna Posição da coluna no código-fonte onde a cláusula foi declarada (1-indexed)
 * @author THZ-LANG Core Team
 * @version 2.4.0
 */
public record ImportacaoAst(
        String modulo,
        String caminho,
        int linha,
        int coluna
) {}
