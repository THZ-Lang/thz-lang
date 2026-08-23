package thz.lang.ast;
import java.util.List;
public record RegraNegocioAst(String nome, String identificador, String rastreioRequisito, String descricao, List<ClausulaContratoAst> clausulasEntrada, List<ClausulaContratoAst> clausulasSaida, List<OperacaoAst> operacoes) {}
