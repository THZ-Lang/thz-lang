package thz.lang.gui.execucao;

import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.docgen.ThzDocGen;
import thz.lang.formato.Formatador;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.sintatico.ThzParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor desacoplado do Motor THZ-LANG para a IDE Desktop.
 * Isola as regras de parsing, verificação semântica, formatação, compilação IR/LLVM,
 * auditoria de governança e despacho de execução.
 */
public class ExecutorMotorGui {

    public record ResultadoVerificacao(boolean sucesso, ProgramaAst ast, List<String> mensagensFormatadas, int totalErros) {}

    public static ResultadoVerificacao verificar(String fonte, boolean estrito) {
        try {
            List<Token> tokens = new ThzLexer(fonte).tokenize();
            ProgramaAst ast = new ThzParser(tokens).parse();
            AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
            List<ErroSemantico> erros = semantico.analisar(new OpcoesAnalise(estrito));

            if (erros.isEmpty()) {
                return new ResultadoVerificacao(true, ast, List.of("✓ Verificação concluída com sucesso: Nenhum erro sintático ou semântico encontrado."), 0);
            }

            List<DiagnosticoEntrada> diagEntradas = new ArrayList<>();
            for (ErroSemantico e : erros) {
                diagEntradas.add(new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem()));
            }
            List<String> formatados = Diagnosticos.formatarDiagnosticos(fonte, diagEntradas, "Semântico");
            return new ResultadoVerificacao(false, ast, formatados, erros.size());

        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            return new ResultadoVerificacao(false, null, List.of(msg), 1);
        }
    }

    public static String formatar(String fonte) throws Exception {
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();
        return Formatador.formatar(ast);
    }

    public static String gerarDocumentacao(String fonte) throws Exception {
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();
        return ThzDocGen.gerarDocumentacao(ast);
    }

    public static String auditar(String fonte, boolean estrito) throws Exception {
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();
        RelatorioAuditoria rel = AuditorGovernanca.auditar(ast);
        return AuditorGovernanca.gerarMarkdownGovernanca(rel);
    }

    public static String gerarIrELlvm(String fonte) throws Exception {
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("   VALIDAÇÃO VETORIAL SIMD (Regras R1 a R5)\n");
        sb.append("================================================================================\n\n");

        List<ResultadoValidacaoSimd> simd = ValidadorSimd.analisarTudo(ast);
        if (simd.isEmpty()) {
            sb.append("(Nenhum laço VETORIZAR_PARA encontrado no programa)\n\n");
        } else {
            for (ResultadoValidacaoSimd r : simd) {
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
        IrPrograma ir = GeradorIr.baixarParaIr(ast);
        sb.append(GeradorIr.serializarIrJson(ir)).append("\n\n");

        sb.append("================================================================================\n");
        sb.append("   LLVM IR (Emissão Preliminar AOT)\n");
        sb.append("================================================================================\n\n");
        sb.append(GeradorIr.emitirLlvm(ast)).append("\n");

        return sb.toString();
    }
}
