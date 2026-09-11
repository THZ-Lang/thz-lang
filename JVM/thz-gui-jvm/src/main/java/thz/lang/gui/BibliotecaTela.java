package thz.lang.gui;

import thz.lang.ast.ExprAst;
import thz.lang.interpretador.BibliotecaPadrao;
import thz.lang.interpretador.ErroExecucao;
import thz.lang.interpretador.ValorThz;

/**
 * Extensões de stdlib do módulo thz-gui: funções TELA.* com renderização Swing.
 *
 * Registradas na BibliotecaPadrao via ponto de extensão público, mantendo o
 * thz-core autônomo (sem dependência de GUI). Chamado por ThzGui na inicialização.
 */
public final class BibliotecaTela {

    private BibliotecaTela() {}

    /** Registra as funções TELA.* (formulários e diálogos Swing) na stdlib. */
    public static void registrar() {
        BibliotecaPadrao.registrar("TELA.renderizarFormulario", (args, ctx, interp) -> {
            exigirAridade("TELA.renderizarFormulario", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Registro reg)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TELA.renderizarFormulario exige REGISTRO como 1º argumento, recebido " + args.get(0).classe());
            }
            exigirClasse("TELA.renderizarFormulario", args.get(1), "TEXTO", ctx);
            String opAlvo = ((ValorThz.Texto) args.get(1)).valor();
            String msg = RenderizadorFormularioSwing.renderizar(reg, opAlvo, interp);
            return ValorThz.TEXTO(msg);
        });

        /** TELA.alerta é uma função que exibe um diálogo de alerta usando Swing. */
        BibliotecaPadrao.registrar("TELA.alerta", (args, ctx, interp) -> {
            exigirAridade("TELA.alerta", args, 2, ctx);
            exigirClasse("TELA.alerta", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.alerta", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        javax.swing.JOptionPane.showMessageDialog(null, mensagem, titulo, javax.swing.JOptionPane.INFORMATION_MESSAGE)
                );
            }
            return ValorThz.TEXTO("OK");
        });

        /**
         * TELA.confirmar é uma função que exibe um diálogo de confirmação usando Swing.
         * Ela recebe dois argumentos: o título do diálogo e a mensagem a ser exibida.
         * Retorna um valor lógico (true/false) dependendo da escolha do usuário.
         * Se o modo não interativo estiver ativado ou se o ambiente gráfico não estiver disponível 
         * a função retorna true por padrão. 
         */
        BibliotecaPadrao.registrar("TELA.confirmar", (args, ctx, interp) -> {
            exigirAridade("TELA.confirmar", args, 2, ctx);
            exigirClasse("TELA.confirmar", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.confirmar", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                int r = javax.swing.JOptionPane.showConfirmDialog(null, mensagem, titulo, javax.swing.JOptionPane.YES_NO_OPTION);
                return ValorThz.LOGICO(r == javax.swing.JOptionPane.YES_OPTION);
            }
            return ValorThz.LOGICO(true);
        });
        
        /** 
        * TELA.pedirTexto é uma função que exibe um diálogo de entrada de texto usando Swing. 
        * Ela recebe dois argumentos: o título do diálogo e o prompt para o usuário. 
        * Se o modo não interativo estiver ativado ou se o ambiente gráfico não estiver disponível, a função retorna uma string vazia.
        */
        BibliotecaPadrao.registrar("TELA.pedirTexto", (args, ctx, interp) -> {
            exigirAridade("TELA.pedirTexto", args, 2, ctx);
            exigirClasse("TELA.pedirTexto", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.pedirTexto", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String prompt = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                String r = javax.swing.JOptionPane.showInputDialog(null, prompt, titulo, javax.swing.JOptionPane.QUESTION_MESSAGE);
                return ValorThz.TEXTO(r != null ? r : "");
            }
            return ValorThz.TEXTO("");
        });
    }

    private static void exigirAridade(String nome, java.util.List<ValorThz> args, int esperada, ExprAst ctx) {
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
