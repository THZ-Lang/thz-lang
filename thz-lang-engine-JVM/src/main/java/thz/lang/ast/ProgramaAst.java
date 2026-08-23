package thz.lang.ast;

import java.util.List;

public record ProgramaAst(
        String nome,
        String versaoLinguagem,
        MetadadosArquiteturaAst metadados,
        List<EstruturaAst> estruturas,
        List<EnumeracaoAst> enumeracoes,
        List<RegraNegocioAst> regras,
        List<ProcedimentoAst> procedimentos) {}
