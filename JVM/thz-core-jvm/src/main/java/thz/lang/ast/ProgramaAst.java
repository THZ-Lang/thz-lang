package thz.lang.ast;

import java.util.List;

public record ProgramaAst(
        TipoModulo tipoModulo,
        String nome,
        String versaoLinguagem,
        List<ImportacaoAst> importacoes,
        MetadadosArquiteturaAst metadados,
        List<EstruturaAst> estruturas,
        List<EnumeracaoAst> enumeracoes,
        List<RegraNegocioAst> regras,
        List<ProcedimentoAst> procedimentos) {

    public ProgramaAst(
            TipoModulo tipoModulo,
            String nome,
            String versaoLinguagem,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos) {
        this(tipoModulo, nome, versaoLinguagem, List.of(), metadados, estruturas, enumeracoes, regras, procedimentos);
    }

    public ProgramaAst(
            String nome,
            String versaoLinguagem,
            MetadadosArquiteturaAst metadados,
            List<EstruturaAst> estruturas,
            List<EnumeracaoAst> enumeracoes,
            List<RegraNegocioAst> regras,
            List<ProcedimentoAst> procedimentos) {
        this(TipoModulo.PROGRAMA, nome, versaoLinguagem, List.of(), metadados, estruturas, enumeracoes, regras, procedimentos);
    }
}
