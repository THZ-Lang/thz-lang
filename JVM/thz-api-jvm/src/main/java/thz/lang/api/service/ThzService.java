package thz.lang.api.service;

import org.springframework.stereotype.Service;
import thz.lang.api.dto.*;
import thz.lang.fachada.ThzCompilerFacade;
import thz.lang.interpretador.InterpretadorThz;

import java.util.ArrayList;
import java.util.List;

@Service
public class ThzService {

    /**
     * Pipeline completo: léxico -> sintático -> semântico.
     * Retorna a lista unificada de diagnósticos + AST + símbolos.
     */
    public AnaliseResult analisar(String fonte, boolean estrito) {
        ThzCompilerFacade.ResultadoAnalise r = ThzCompilerFacade.analisar(fonte, estrito);

        List<DiagnosticoApi> diagnosticos = r.diagnosticos().stream()
                .map(d -> new DiagnosticoApi(d.linha(), d.coluna(), d.mensagem(), d.origem(), d.severidade()))
                .toList();

        String astJson = r.ast() != null ? thz.lang.formato.JsonEscritor.paraJson(r.ast()) : null;

        List<SimboloApi> simbolos = r.simbolos().stream()
                .map(s -> new SimboloApi(s.nome(), s.categoria(), s.detalhe(), s.linha(), s.coluna(), s.container()))
                .toList();

        return new AnaliseResult(astJson, diagnosticos, r.textoDiagnosticos(), r.temErros(), simbolos, astJson);
    }

    /**
     * Formata o código fonte THZ de forma canônica e idempotente.
     */
    public FormatacaoResult formatar(String fonte) {
        ThzCompilerFacade.ResultadoFormatacao r = ThzCompilerFacade.formatar(fonte);
        return new FormatacaoResult(r.resultado(), r.alterou());
    }

    /**
     * Gera documentação Markdown + Mermaid a partir do código fonte.
     */
    public DocumentacaoResult gerarDocumentacao(String fonte) {
        String markdown = ThzCompilerFacade.gerarDocumentacao(fonte);
        return new DocumentacaoResult(markdown);
    }

    /**
     * Realiza auditoria de governança (G4).
     */
    public AuditoriaResult auditar(String fonte) {
        ThzCompilerFacade.ResultadoAuditoria r = ThzCompilerFacade.auditar(fonte);
        return new AuditoriaResult(r.json(), r.markdown());
    }

    /**
     * Gera a representação intermediária THZ-IR/1.
     */
    public IrResult gerarIr(String fonte) {
        ThzCompilerFacade.ResultadoIr r = ThzCompilerFacade.gerarIr(fonte);
        return new IrResult(r.json(), r.llvm());
    }

    /**
     * Valida regras de vetorização SIMD (R1-R5).
     */
    public SimdResult validarSimd(String fonte) {
        ThzCompilerFacade.ResultadoSimd r = ThzCompilerFacade.validarSimd(fonte);
        List<SimdResultadoApi> api = r.resultados().stream().map(vr ->
                new SimdResultadoApi(
                        vr.loopIdentificador(),
                        vr.variavel(),
                        vr.passoSimd(),
                        vr.vetorizavel(),
                        vr.regrasAtendidas(),
                        vr.violacoes(),
                        vr.avisos()
                )
        ).toList();
        return new SimdResult(api);
    }

    /**
     * Executa o código THZ e retorna a saída.
     */
    public ExecucaoResult executar(String fonte, String operacao) {
        var ast = ThzCompilerFacade.parseAst(fonte);
        if (ast == null) return new ExecucaoResult(List.of(), List.of("Falha ao parsear o código"), null);

        List<String> saida = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            InterpretadorThz interp = new InterpretadorThz(ast, saida::add, null);
            if (operacao != null && !operacao.isBlank()) {
                interp.executarOperacao(operacao);
            } else {
                var ops = interp.listarOperacoesExecutaveis();
                if (!ops.isEmpty()) {
                    interp.executarOperacao(ops.getFirst().operacao().nome());
                } else {
                    var procs = interp.listarProcedimentos();
                    if (!procs.isEmpty()) {
                        interp.executarProcedimento(procs.getFirst().nome());
                    }
                }
            }
        } catch (Exception e) {
            erros.add(e.getMessage());
        }

        return new ExecucaoResult(saida, erros, null);
    }

    /**
     * Retorna o AST como JSON.
     */
    public AstResult obterAst(String fonte) {
        var ast = ThzCompilerFacade.parseAst(fonte);
        if (ast == null) return new AstResult(null, null);
        return new AstResult(thz.lang.formato.JsonEscritor.paraJson(ast), ast.nome());
    }

    /**
     * Resolve hover (tipo/assinatura) para a posição informada.
     */
    public HoverResult obterHover(String fonte, int linha, int coluna) {
        ThzCompilerFacade.HoverInfo h = ThzCompilerFacade.obterHover(fonte, linha, coluna);
        if (h == null) return null;
        return new HoverResult(h.conteudo(), new HoverResponse.HoverRange(h.linha(), h.colunaInicio(), h.colunaFim() - h.colunaInicio()));
    }

    // ---- Records internos para resultados ----
    public record AnaliseResult(String astJson, List<DiagnosticoApi> diagnosticos, List<String> textoDiagnosticos,
                                boolean temErros, List<SimboloApi> simbolos, String ast) {}
    public record FormatacaoResult(String resultado, boolean alterou) {}
    public record DocumentacaoResult(String markdown) {}
    public record AuditoriaResult(String relatorioJson, String markdown) {}
    public record IrResult(String irJson, String llvm) {}
    public record SimdResult(List<SimdResultadoApi> resultados) {}
    public record ExecucaoResult(List<String> saida, List<String> erros, String resultado) {}
    public record AstResult(String astJson, String nomePrograma) {}
    public record HoverResult(String conteudo, HoverResponse.HoverRange range) {}
}
