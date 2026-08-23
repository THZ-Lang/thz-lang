package thz.lang.ast;
import java.util.List;
public record OperacaoAst(String nome, List<ParametroOperacaoAst> parametros, String tipoRetorno, List<ComandoAst> corpo) {}
