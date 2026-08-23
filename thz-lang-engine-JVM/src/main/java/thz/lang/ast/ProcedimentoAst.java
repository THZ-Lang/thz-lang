package thz.lang.ast;
import java.util.List;
public record ProcedimentoAst(String nome, List<ParametroOperacaoAst> parametros, List<ComandoAst> corpo) {}
