package thz.lang.gui.execucao;

import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.fachada.ThzCompilerFacade;



import java.util.List;

/**
 * Executor desacoplado do Motor THZ-LANG para a IDE Desktop.
 * Delega pipeline para ThzCompilerFacade (thz-core-jvm).
 */
public class ExecutorMotorGui {

    public record ResultadoVerificacao(boolean sucesso, ProgramaAst ast, List<String> mensagensFormatadas, int totalErros) {}

    public static ResultadoVerificacao verificar(String fonte, boolean estrito) {
        ThzCompilerFacade.ResultadoAnalise r = ThzCompilerFacade.analisar(fonte, estrito);

        if (r.ast() == null) {
            String msg = r.diagnosticos().isEmpty() ? "Erro desconhecido" : r.diagnosticos().getFirst().mensagem();
            return new ResultadoVerificacao(false, null, List.of(msg), 1);
        }

        if (r.diagnosticos().isEmpty()) {
            return new ResultadoVerificacao(true, r.ast(), List.of("✓ Verificação concluída com sucesso: Nenhum erro sintático ou semântico encontrado."), 0);
        }

        List<DiagnosticoEntrada> diagEntradas = r.diagnosticos().stream()
                .map(d -> new DiagnosticoEntrada(d.linha(), d.coluna(), d.mensagem()))
                .toList();
        List<String> formatados = Diagnosticos.formatarDiagnosticos(fonte, diagEntradas, "Semântico");
        return new ResultadoVerificacao(false, r.ast(), formatados, r.diagnosticos().size());
    }

    public static String formatar(String fonte) throws Exception {
        ThzCompilerFacade.ResultadoFormatacao r = ThzCompilerFacade.formatar(fonte);
        return r.resultado();
    }

    public static String gerarDocumentacao(String fonte) throws Exception {
        return ThzCompilerFacade.gerarDocumentacao(fonte);
    }

    public static String auditar(String fonte, boolean estrito) throws Exception {
        ThzCompilerFacade.ResultadoAuditoria r = ThzCompilerFacade.auditar(fonte);
        return r.markdown();
    }

    public static String gerarIrELlvm(String fonte) throws Exception {
        var ast = ThzCompilerFacade.parseAst(fonte);
        if (ast == null) throw new Exception("Falha ao parsear o código fonte.");

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("   VALIDAÇÃO VETORIAL SIMD (Regras R1 a R5)\n");
        sb.append("================================================================================\n\n");

        var simd = ThzCompilerFacade.validarSimd(fonte);
        if (simd.resultados().isEmpty()) {
            sb.append("(Nenhum laço VETORIZAR_PARA encontrado no programa)\n\n");
        } else {
            for (var r : simd.resultados()) {
                sb.append("• Variável: ").append(r.variavel()).append(" | Passo SIMD: ").append(r.passoSimd())
                        .append(" | Vetorizável: ").append(r.vetorizavel() ? "SIM (AVX-512/AVX2)" : "NÃO").append("\n");
                for (String req : r.regrasAtendidas()) sb.append("   ✓ ").append(req).append("\n");
                for (String viol : r.violacoes()) sb.append("   ✗ ").append(viol).append("\n");
                sb.append("\n");
            }
        }

        sb.append("================================================================================\n");
        sb.append("   THZ-IR/1 (Representação Intermediária Canônica)\n");
        sb.append("================================================================================\n\n");
        var ir = ThzCompilerFacade.gerarIr(fonte);
        sb.append(ir.json()).append("\n\n");

        sb.append("================================================================================\n");
        sb.append("   LLVM IR (Emissão Preliminar AOT)\n");
        sb.append("================================================================================\n\n");
        sb.append(ThzCompilerFacade.emitirLlvm(fonte)).append("\n");

        return sb.toString();
    }
}
