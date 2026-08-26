package thz.lang.cli;

import thz.lang.ast.ExprAst;
import thz.lang.interpretador.BibliotecaPadrao;
import thz.lang.interpretador.ErroExecucao;
import thz.lang.interpretador.ValorThz;
import thz.lang.ui.HtmlEscape;

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

    /** Registra as funções TELA.* em modo console com fallback WebView autônomo (sem Swing). */
    public static void registrar() {
        BibliotecaPadrao.registrar("TELA.renderizarFormulario", (args, ctx, interp) -> {
            exigirAridade("TELA.renderizarFormulario", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Registro reg)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TELA.renderizarFormulario exige REGISTRO como 1º argumento, recebido " + args.get(0).classe());
            }
            exigirClasse("TELA.renderizarFormulario", args.get(1), "TEXTO", ctx);
            String opAlvo = ((ValorThz.Texto) args.get(1)).valor();
            // Tenta renderer WebView autônomo (thz-core, sem Swing). Fallback para mensagem amigável se falhar.
            try {
                String msg = thz.lang.ui.RenderizadorFormularioWeb.renderizar(reg, opAlvo, interp);
                return ValorThz.TEXTO(msg);
            } catch (Exception e) {
                System.err.println("[THZ WebView] Falha ao renderizar formulário: " + e.getMessage());
                System.err.println("[THZ] Use thz ui --html ou instale a IDE Desktop (./gradlew :thz-gui:gui) para render Swing.");
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao abrir formulário '" + thz.lang.ui.ConversorFormularioUi.extrairTitulo(reg) + "': " + e.getMessage());
            }
        });

        BibliotecaPadrao.registrar("TELA.alerta", (args, ctx, interp) -> {
            exigirAridade("TELA.alerta", args, 2, ctx);
            exigirClasse("TELA.alerta", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.alerta", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            if (!Boolean.getBoolean("thz.nao_interativo")) {
                try { exibirDialogoWebview(titulo, mensagem, "alerta"); } catch (Exception ignore) {}
            }
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
            try { exibirDialogoWebview(titulo, mensagem, "confirmar"); } catch (Exception ignore) {}
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
            try { exibirDialogoWebview(titulo, prompt, "pedirTexto"); } catch (Exception ignore) {}
            String resposta = lerLinha(titulo + " " + prompt + ": ");
            return ValorThz.TEXTO(resposta != null ? resposta : "");
        });
    }

    private static void exibirDialogoWebview(String titulo, String mensagem, String tipo) {
        String html = """
                <!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8"><title>%s</title>
                <style>body{font-family:'Segoe UI',sans-serif;background:#0f172a;color:#f8fafc;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
                .card{background:rgba(30,41,59,0.9);border:1px solid rgba(255,255,255,0.12);border-radius:12px;padding:32px;max-width:560px;width:90%%;box-shadow:0 8px 32px rgba(0,0,0,0.4)}
                h2{margin:0 0 12px;font-size:1.25rem} p{color:#94a3b8;line-height:1.6} .badge{display:inline-block;padding:4px 10px;border-radius:9999px;font-size:0.7rem;font-weight:700;text-transform:uppercase;background:rgba(59,130,246,0.2);color:#60a5fa;border:1px solid rgba(59,130,246,0.4);margin-bottom:12px}</style>
                </head><body><div class="card"><span class="badge">%s</span><h2>%s</h2><p>%s</p>
                <p style="margin-top:16px;font-size:0.8rem;color:#64748b">Veja o console para interação. Feche esta janela para continuar.</p></div></body></html>
                """.formatted(HtmlEscape.escapeHtml(titulo), HtmlEscape.escapeHtml(tipo), HtmlEscape.escapeHtml(titulo), HtmlEscape.escapeHtml(mensagem));
        thz.lang.webview.LancadorWebviewNativo.abrirHtml(titulo, html, 560, 320);
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
