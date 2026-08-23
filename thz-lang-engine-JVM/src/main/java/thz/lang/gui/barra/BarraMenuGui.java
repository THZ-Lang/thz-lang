package thz.lang.gui.barra;

import thz.lang.gui.EditorThz;
import thz.lang.gui.GaleriaExemplos;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Construtor e gerenciador da Barra de Menus da IDE THZ-LANG Desktop.
 * Desacopla toda a definição de itens, aceleradores e atalhos de teclado da janela principal.
 */
public class BarraMenuGui {

    public interface AcoesGui {
        void novoArquivo();
        void abrirArquivo();
        void salvarArquivo();
        void salvarArquivoComo();
        void carregarArquivo(File f);
        void exportarRelatorio(String formato);
        void alternarTema();
        void alternarModoEstrito();
        void executarCodigo();
        void verificarCodigo();
        void formatarCodigo();
        void gerarDocumentacao();
        void auditarGovernanca();
        void gerarIrELlvm();
        void abrirConfiguracaoJvm();
        void alternarPainelSaida();
        void limparSaida();
        void exibirManual();
        void exibirAtalhos();
        void exibirSobre();
    }

    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu menuRecentes = new JMenu("Abrir Recentes");
    private final JCheckBoxMenuItem miTemaMenu = new JCheckBoxMenuItem("Modo Claro (Tema Claro)");
    private final JCheckBoxMenuItem miEstritoMenu = new JCheckBoxMenuItem("Modo Estrito (Lint Restritivo)");

    public BarraMenuGui(JFrame parentFrame, EditorThz editor, AcoesGui acoes) {
        // 1. Menu Arquivo
        JMenu menuArquivo = new JMenu("Arquivo");
        menuArquivo.setMnemonic(KeyEvent.VK_A);

        JMenuItem miNovo = new JMenuItem("Novo Arquivo");
        miNovo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        miNovo.addActionListener(e -> acoes.novoArquivo());

        JMenuItem miAbrir = new JMenuItem("Abrir...");
        miAbrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        miAbrir.addActionListener(e -> acoes.abrirArquivo());

        JMenuItem miSalvar = new JMenuItem("Salvar");
        miSalvar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        miSalvar.addActionListener(e -> acoes.salvarArquivo());

        JMenuItem miSalvarComo = new JMenuItem("Salvar Como...");
        miSalvarComo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        miSalvarComo.addActionListener(e -> acoes.salvarArquivoComo());

        JMenu menuExportar = new JMenu("Exportar Relatório");
        JMenuItem miExpPdf = new JMenuItem("📄 Relatório PDF (.pdf)");
        miExpPdf.addActionListener(e -> acoes.exportarRelatorio("pdf"));
        JMenuItem miExpXlsx = new JMenuItem("📊 Planilha Excel (.xlsx)");
        miExpXlsx.addActionListener(e -> acoes.exportarRelatorio("xlsx"));
        JMenuItem miExpDocx = new JMenuItem("📝 Documento Word (.docx)");
        miExpDocx.addActionListener(e -> acoes.exportarRelatorio("docx"));
        menuExportar.add(miExpPdf);
        menuExportar.add(miExpXlsx);
        menuExportar.add(miExpDocx);

        JMenuItem miSair = new JMenuItem("Sair");
        miSair.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        miSair.addActionListener(e -> {
            parentFrame.dispose();
            System.exit(0);
        });

        menuArquivo.add(miNovo);
        menuArquivo.add(miAbrir);
        menuArquivo.add(menuRecentes);
        menuArquivo.addSeparator();
        menuArquivo.add(miSalvar);
        menuArquivo.add(miSalvarComo);
        menuArquivo.add(menuExportar);
        menuArquivo.addSeparator();
        menuArquivo.add(miSair);

        // 2. Menu Exemplos
        JMenu menuExemplos = GaleriaExemplos.criarMenuExemplos(acoes::carregarArquivo);
        menuExemplos.setMnemonic(KeyEvent.VK_E);

        // 3. Menu Editar
        JMenu menuEditar = new JMenu("Editar");
        JMenuItem miDesfazer = new JMenuItem("Desfazer");
        miDesfazer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        miDesfazer.addActionListener(e -> editor.desfazer());

        JMenuItem miRefazer = new JMenuItem("Refazer");
        miRefazer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        miRefazer.addActionListener(e -> editor.refazer());

        JMenuItem miCortar = new JMenuItem("Recortar");
        miCortar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        miCortar.addActionListener(e -> editor.cut());

        JMenuItem miCopiar = new JMenuItem("Copiar");
        miCopiar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        miCopiar.addActionListener(e -> editor.copy());

        JMenuItem miColar = new JMenuItem("Colar");
        miColar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        miColar.addActionListener(e -> editor.paste());

        JMenuItem miLimpar = new JMenuItem("Limpar Editor");
        miLimpar.addActionListener(e -> editor.setText(""));

        JMenuItem miSelTudo = new JMenuItem("Selecionar Tudo");
        miSelTudo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        miSelTudo.addActionListener(e -> editor.selectAll());

        menuEditar.add(miDesfazer);
        menuEditar.add(miRefazer);
        menuEditar.addSeparator();
        menuEditar.add(miCortar);
        menuEditar.add(miCopiar);
        menuEditar.add(miColar);
        menuEditar.addSeparator();
        menuEditar.add(miLimpar);
        menuEditar.add(miSelTudo);

        // 4. Menu Ver
        JMenu menuVer = new JMenu("Ver");
        miTemaMenu.addActionListener(e -> acoes.alternarTema());

        JMenuItem miZoomMais = new JMenuItem("Aumentar Fonte");
        miZoomMais.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
        miZoomMais.addActionListener(e -> editor.alterarTamanhoFonte(1));

        JMenuItem miZoomMenos = new JMenuItem("Diminuir Fonte");
        miZoomMenos.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        miZoomMenos.addActionListener(e -> editor.alterarTamanhoFonte(-1));

        JMenuItem miZoomPadrao = new JMenuItem("Restaurar Fonte (100%)");
        miZoomPadrao.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        miZoomPadrao.addActionListener(e -> editor.definirTamanhoFonte(13));

        JMenuItem miAlternarSaida = new JMenuItem("Alternar Painel de Saída");
        miAlternarSaida.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK));
        miAlternarSaida.addActionListener(e -> acoes.alternarPainelSaida());

        JMenuItem miLimparSaida = new JMenuItem("Limpar Console de Saída");
        miLimparSaida.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK));
        miLimparSaida.addActionListener(e -> acoes.limparSaida());

        menuVer.add(miTemaMenu);
        menuVer.addSeparator();
        menuVer.add(miZoomMais);
        menuVer.add(miZoomMenos);
        menuVer.add(miZoomPadrao);
        menuVer.addSeparator();
        menuVer.add(miAlternarSaida);
        menuVer.add(miLimparSaida);

        // 5. Menu Ações
        JMenu menuAcoes = new JMenu("Ações");
        JMenuItem miExecutar = new JMenuItem("Executar Programa");
        miExecutar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        miExecutar.addActionListener(e -> acoes.executarCodigo());

        JMenuItem miVerificar = new JMenuItem("Verificar Sintaxe & Semântica");
        miVerificar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        miVerificar.addActionListener(e -> acoes.verificarCodigo());

        JMenuItem miFormatar = new JMenuItem("Formatar Código (Canônico)");
        miFormatar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        miFormatar.addActionListener(e -> acoes.formatarCodigo());

        JMenuItem miDoc = new JMenuItem("Gerar Documentação (Markdown + Mermaid)");
        miDoc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        miDoc.addActionListener(e -> acoes.gerarDocumentacao());

        JMenuItem miAudit = new JMenuItem("Auditoria de Governança (DbC + Matriz)");
        miAudit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
        miAudit.addActionListener(e -> acoes.auditarGovernanca());

        JMenuItem miIr = new JMenuItem("Inspecionar THZ-IR & LLVM");
        miIr.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        miIr.addActionListener(e -> acoes.gerarIrELlvm());

        menuAcoes.add(miExecutar);
        menuAcoes.add(miVerificar);
        menuAcoes.add(miFormatar);
        menuAcoes.addSeparator();
        menuAcoes.add(miDoc);
        menuAcoes.add(miAudit);
        menuAcoes.add(miIr);

        // 6. Menu Configurações
        JMenu menuConfig = new JMenu("Configurações");
        JMenuItem miConfigJvm = new JMenuItem("☕ Ambiente JVM...");
        miConfigJvm.addActionListener(e -> acoes.abrirConfiguracaoJvm());

        miEstritoMenu.addActionListener(e -> acoes.alternarModoEstrito());

        menuConfig.add(miConfigJvm);
        menuConfig.addSeparator();
        menuConfig.add(miEstritoMenu);

        // 7. Menu Ajuda
        JMenu menuAjuda = new JMenu("Ajuda");
        JMenuItem miManual = new JMenuItem("Manual Oficial da Linguagem (v2.3)...");
        miManual.addActionListener(e -> acoes.exibirManual());

        JMenuItem miAtalhos = new JMenuItem("Atalhos de Teclado");
        miAtalhos.addActionListener(e -> acoes.exibirAtalhos());

        JMenuItem miSobre = new JMenuItem("Sobre o THZ-LANG...");
        miSobre.addActionListener(e -> acoes.exibirSobre());

        menuAjuda.add(miManual);
        menuAjuda.add(miAtalhos);
        menuAjuda.addSeparator();
        menuAjuda.add(miSobre);

        menuBar.add(menuArquivo);
        menuBar.add(menuExemplos);
        menuBar.add(menuEditar);
        menuBar.add(menuVer);
        menuBar.add(menuAcoes);
        menuBar.add(menuConfig);
        menuBar.add(menuAjuda);
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void atualizarRecentes(List<String> recentes, Consumer<File> abrirArquivoConsumer) {
        menuRecentes.removeAll();
        if (recentes == null || recentes.isEmpty()) {
            JMenuItem vazio = new JMenuItem("(Nenhum arquivo recente)");
            vazio.setEnabled(false);
            menuRecentes.add(vazio);
            return;
        }

        for (String caminho : recentes) {
            File f = new File(caminho);
            JMenuItem item = new JMenuItem(f.getName() + "  [" + f.getParent() + "]");
            item.setToolTipText(caminho);
            item.addActionListener(e -> abrirArquivoConsumer.accept(f));
            menuRecentes.add(item);
        }
    }

    public void sincronizarTema(boolean ehClaro) {
        miTemaMenu.setSelected(ehClaro);
    }

    public void sincronizarModoEstrito(boolean estrito) {
        miEstritoMenu.setSelected(estrito);
    }
}
