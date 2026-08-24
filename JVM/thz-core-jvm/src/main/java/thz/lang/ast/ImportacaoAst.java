package thz.lang.ast;

public record ImportacaoAst(
        String modulo,
        String caminho,
        int linha,
        int coluna
) {}
