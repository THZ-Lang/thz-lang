package thz.lang.cli;

import thz.lang.ast.ExprAst;
import thz.lang.interpretador.BibliotecaPadrao;
import thz.lang.interpretador.ErroExecucao;
import thz.lang.interpretador.ValorThz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Extensões de stdlib do módulo thz-cli: equivalentes de console/headless das funções TELA.*.
 *
 * Registradas na BibliotecaPadrao via ponto de extensão público, mantendo o
 * thz-core autônomo. Chamado por ThzCli e Repl na inicialização.
 */
public final class BibliotecaConsole {

    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private BibliotecaConsole() {}

    /** Registra as funções TELA.* em modo console (sem Swing). */
    public static void registrar() {
        BibliotecaPadrao.registrar("TELA.renderizarFormulario", (args, ctx, interp) -> {
            exigirAridade("TELA.renderizarFormulario", args, 2, ctx);
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TELA.renderizarFormulario exige a IDE Desktop (módulo thz-gui). A CLI não possui renderização gráfica.");
        });

        BibliotecaPadrao.registrar("TELA.alerta", (args, ctx, interp) -> {
            exigirAridade("TELA.alerta", args, 2, ctx);
            exigirClasse("TELA.alerta", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.alerta", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            System.err.println("[ALERTA] " + titulo + ": " + mensagem);
            return ValorThz.TEXTO("OK");
        });

        BibliotecaPadrao.registrar("TELA.confirmar", (args, ctx, interp) -> {
            exigirAridade("TELA.confirmar", args, 2, ctx);
            exigirClasse("TELA.confirmar", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.confirmar", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            if (Boolean.getBoolean("thz.nao_interativo")) {
                return ValorThz.LOGICO(true);
            }
            String resposta = lerLinha(titulo + " " + mensagem + " [S/N, padrão S]: ");
            boolean confirmado = resposta == null || resposta.isBlank() || resposta.trim().equalsIgnoreCase("S") || resposta.trim().equalsIgnoreCase("SIM");
            return ValorThz.LOGICO(confirmado);
        });

        BibliotecaPadrao.registrar("TELA.pedirTexto", (args, ctx, interp) -> {
            exigirAridade("TELA.pedirTexto", args, 2, ctx);
            exigirClasse("TELA.pedirTexto", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.pedirTexto", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String prompt = ((ValorThz.Texto) args.get(1)).valor();
            if (Boolean.getBoolean("thz.nao_interativo")) {
                return ValorThz.TEXTO("");
            }
            String resposta = lerLinha(titulo + " " + prompt + ": ");
            return ValorThz.TEXTO(resposta != null ? resposta : "");
        });
    }

    private static String lerLinha(String aviso) {
        try {
            System.err.print("[ENTRADA] " + aviso);
            System.err.flush();
            return STDIN.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static void exigirAridade(String nome, List<ValorThz> args, int esperada, ExprAst ctx) {
        if (args.size() != esperada) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + esperada + " argumento(s), recebidos " + args.size() + ".");
        }
    }

    private static void exigirClasse(String nome, ValorThz v, String classeEsperada, ExprAst ctx) {
        if (!v.classe().equals(classeEsperada)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + classeEsperada + ", recebido " + v.classe() + ".");
        }
    }
}
