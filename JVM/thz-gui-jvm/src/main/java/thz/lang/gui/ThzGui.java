package thz.lang.gui;

import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.documento.MotorDocumentos;
import thz.lang.gui.barra.BarraFerramentasGui;
import thz.lang.gui.barra.BarraMenuGui;
import thz.lang.gui.barra.BarraStatusGui;
import thz.lang.gui.config.ConfiguracaoDesktop;
import thz.lang.gui.config.DetectorJvm;
import thz.lang.gui.config.DialogoConfiguracaoJvm;
import thz.lang.gui.config.GerenciadorConfiguracao;
import thz.lang.gui.execucao.ExecutorMotorGui;
import thz.lang.interpretador.InjetorLoteDemo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.runtime.BlocoMemoria;
import thz.lang.sintatico.ThzParser;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * THZ-LANG Desktop — Interface Swing moderna, desacoplada e aderente a SRP.
 * Orquestra os módulos de barra de ferramentas, menu, status, editor e execução do motor.
 */

public final class ThzGui extends JFrame implements BarraMenuGui.AcoesGui {

    private static final Pattern POSICAO = Pattern.compile("\\[Linha (\\d+):(\\d+)]");
    private static final String FONTE_INICIAL = """

            PROGRAMA ExemploDesktop

            METADADOS_ARQUITETURA
                DOMINIO: "Desktop"
                SUBDOMINIO: "Interface"
                CAMADA: "Aplicacao"
                VERSAO: "1.0.0"
                AUTOR: "THZ-LANG Team"
                SLO_LATENCIA_MAXIMA: "10ms"
                CONFORMIDADE: "DEMO"
            FIM_METADADOS

            ESTRUTURA Usuario
                id: TEXTO
                ativo: LOGICO
                saldo: DECIMAL(12, 2)
            FIM_ESTRUTURA

            PROCEDIMENTO Principal()
            INICIO
                VARIAVEL u : Usuario <- CRIAR Usuario(id: "USR-001", ativo: VERDADEIRO, saldo: 250.00)
                EXIBA "THZ-LANG Desktop pronto! Usuário: " + u.id + " | Saldo: R$ " + u.saldo
            FIM

            FIM_PROGRAMA
            """;

    private final EditorThz editor = new EditorThz();
    private final JTextArea saida = new JTextArea();
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private JPanel cardEditor;
    private JPanel cardSaida;
    private JLabel lblCardEditor;
    private JLabel lblCardSaida;

    private BarraStatusGui barraStatus;
    private BarraFerramentasGui barraFerramentas;
    private BarraMenuGui barraMenu;

    private PaletaThz tema = PaletaThz.ESCURO;
    private File arquivoSelecionado;
    private ConfiguracaoDesktop config;

    public ThzGui() {
        super("THZ-LANG Desktop - JVM");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));

        this.config = GerenciadorConfiguracao.carregar();
        configurarDimensoesJanela();

        this.barraStatus = new BarraStatusGui();
        this.barraFerramentas = new BarraFerramentasGui(this);
        this.barraMenu = new BarraMenuGui(this, editor, this);

        setJMenuBar(barraMenu.getMenuBar());
        montarLayout();

        if (config.posicaoDivisor() > 50) {
            split.setDividerLocation(config.posicaoDivisor());
        }

        barraFerramentas.getToggleEstrito().setSelected(config.modoEstrito());
        barraMenu.sincronizarModoEstrito(config.modoEstrito());
        barraStatus.atualizarEstrito(config.modoEstrito());

        aplicarTema("CLARO".equalsIgnoreCase(config.tema()) ? PaletaThz.CLARO : PaletaThz.ESCURO);
        atualizarJvmAtiva();

        if (config.ultimoArquivo() != null && !config.ultimoArquivo().isBlank() && new File(config.ultimoArquivo()).exists()) {
            carregarArquivo(new File(config.ultimoArquivo()));
        } else {
            editor.setText(FONTE_INICIAL);
        }

        editor.getPane().addCaretListener(e -> atualizarCursorInfo());
        saida.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tratarCliqueSaida();
            }
        });

        barraStatus.getJvmBadge().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirConfiguracaoJvm();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salvarConfiguracaoAtual();
            }
        });
    }

    public static void main(String[] args) {
        BibliotecaTela.registrar();
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {}
        }
        SwingUtilities.invokeLater(() -> new ThzGui().setVisible(true));
    }

    private void configurarDimensoesJanela() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle telaBounds = (gc != null) ? gc.getBounds() : new Rectangle(0, 0, 1024, 768);
        Insets insets = (gc != null) ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : new Insets(0, 0, 0, 0);

        int maxLarguraUtil = Math.max(700, telaBounds.width - insets.left - insets.right);
        int maxAlturaUtil = Math.max(500, telaBounds.height - insets.top - insets.bottom);

        int larg = Math.min(Math.max(config.larguraJanela(), Math.min(1200, (int) (maxLarguraUtil * 0.80))), maxLarguraUtil);
        int alt = Math.min(Math.max(config.alturaJanela(), Math.min(760, (int) (maxAlturaUtil * 0.85))), maxAlturaUtil);
        setSize(larg, alt);

        if (config.posicaoX() >= 0 && config.posicaoY() >= 0 &&
                config.posicaoX() + larg <= telaBounds.x + telaBounds.width &&
                config.posicaoY() + alt <= telaBounds.y + telaBounds.height) {
            setLocation(config.posicaoX(), config.posicaoY());
        } else {
            int posX = telaBounds.x + insets.left + Math.max(0, (maxLarguraUtil - larg) / 2);
            int posY = telaBounds.y + insets.top + Math.max(0, (maxAlturaUtil - alt) / 2);
            setLocation(posX, posY);
        }
        if (config.maximizada()) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    private void montarLayout() {
        cardEditor = criarCard("Editor — THZ-LANG (Realce Sintático Nativo)", editor, lb -> lblCardEditor = lb);

        saida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        saida.setEditable(false);
        saida.setLineWrap(true);
        saida.setWrapStyleWord(true);
        saida.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane spSaida = new JScrollPane(saida);
        spSaida.setBorder(BorderFactory.createEmptyBorder());
        spSaida.getVerticalScrollBar().setUnitIncrement(16);
        cardSaida = criarCard("Saída / Diagnósticos — Console de Execução & Tooling", spSaida, lb -> lblCardSaida = lb);
        cardSaida.setPreferredSize(new Dimension(0, 220));

        split.setTopComponent(cardEditor);
        split.setBottomComponent(cardSaida);
        split.setResizeWeight(0.68);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setLayout(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(barraFerramentas.getHeader(), BorderLayout.NORTH);
        north.add(barraFerramentas.getToolbar(), BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(barraStatus.getComponente(), BorderLayout.SOUTH);
    }

    private JPanel criarCard(String titulo, JComponent conteudo, java.util.function.Consumer<JLabel> labelConsumer) {
        JPanel card = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lbl.setOpaque(true);
        labelConsumer.accept(lbl);
        card.add(lbl, BorderLayout.NORTH);
        card.add(conteudo, BorderLayout.CENTER);
        return card;
    }

    private void aplicarTema(PaletaThz nova) {
        this.tema = nova;
        try {
            if (nova == PaletaThz.ESCURO)
                Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
            else
                Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignore) {}

        editor.aplicarTema(nova);
        barraFerramentas.aplicarTema(nova);
        barraStatus.aplicarTema(nova);
        barraMenu.sincronizarTema(nova == PaletaThz.CLARO);

        if (config != null) {
            config = config.comTema(nova == PaletaThz.CLARO ? "CLARO" : "ESCURO");
            GerenciadorConfiguracao.salvar(config);
        }

        getContentPane().setBackground(nova.fundoJanela);
        split.setBackground(nova.fundoJanela);

        atualizarEstiloCard(cardEditor, lblCardEditor, nova);
        atualizarEstiloCard(cardSaida, lblCardSaida, nova);

        saida.setBackground(nova == PaletaThz.ESCURO ? new Color(0x1E, 0x1E, 0x1E) : Color.WHITE);
        saida.setForeground(nova.frenteEditor);
        saida.setCaretColor(nova.corCaret);
        saida.setSelectionColor(nova.corSelecao);

        repaint();
    }

    private void atualizarEstiloCard(JPanel card, JLabel lbl, PaletaThz p) {
        if (card != null) {
            card.setBackground(p.fundoPainel);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(p.corBorda, 1, true),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        }
        if (lbl != null) {
            lbl.setForeground(p.corTextoSecundario);
            lbl.setBackground(p.fundoToolbar);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, p.corBordaSuave),
                    BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        }
    }

    private void atualizarCursorInfo() {
        int[] pos = editor.obterLinhaColunaCaret();
        barraStatus.atualizarCursor(pos[0], pos[1]);
    }

    private void atualizarJvmAtiva() {
        DetectorJvm.InfoJvm atual = DetectorJvm.obterJvmAtual();
        barraStatus.atualizarJvmBadge(atual.versao());
    }

    private void salvarConfiguracaoAtual() {
        if (config != null) {
            config = new ConfiguracaoDesktop(
                    tema == PaletaThz.CLARO ? "CLARO" : "ESCURO",
                    barraFerramentas.getToggleEstrito().isSelected(),
                    arquivoSelecionado != null ? arquivoSelecionado.getAbsolutePath() : config.ultimoArquivo(),
                    getWidth(),
                    getHeight(),
                    getX(),
                    getY(),
                    (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0,
                    split.getDividerLocation(),
                    editor.getTamanhoFonteAtual(),
                    config.caminhoJvm(),
                    config.arquivosRecentes()
            );
            GerenciadorConfiguracao.salvar(config);
        }
    }

    // ---- Implementação das Ações da IDE (AcoesGui) ----

    @Override
    public void novoArquivo() {
        arquivoSelecionado = null;
        editor.setText("");
        barraFerramentas.atualizarInfoArquivo("Sem título");
        editor.limparMarcacoesErro();
        saida.setText("");
        barraStatus.definirPronto(tema);
    }

    @Override
    public void abrirArquivo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Arquivos THZ (*.thz)", "thz"));
        if (arquivoSelecionado != null && arquivoSelecionado.getParentFile() != null) {
            fc.setCurrentDirectory(arquivoSelecionado.getParentFile());
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregarArquivo(fc.getSelectedFile());
        }
    }

    @Override
    public void carregarArquivo(File f) {
        try {
            String c = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            arquivoSelecionado = f;
            editor.setText(c);
            barraFerramentas.atualizarInfoArquivo(f.getName());
            editor.limparMarcacoesErro();
            saida.setText("");
            barraStatus.definirPronto(tema);

            if (config != null) {
                config = config.comArquivoRecente(f.getAbsolutePath());
                GerenciadorConfiguracao.salvar(config);
                barraMenu.atualizarRecentes(config.arquivosRecentes(), this::carregarArquivo);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void carregarCodigoTexto(String codigo) {
        editor.setText(codigo);
        arquivoSelecionado = null;
        barraFerramentas.atualizarInfoArquivo("Exemplo Carregado");
        editor.limparMarcacoesErro();
        saida.setText("");
        barraStatus.definirPronto(tema);
    }

    @Override
    public void salvarArquivo() {
        if (arquivoSelecionado == null) {
            salvarArquivoComo();
            return;
        }
        try {
            Files.writeString(arquivoSelecionado.toPath(), editor.getText(), StandardCharsets.UTF_8);
            barraStatus.definirStatus("Salvo com sucesso.", new Color(34, 197, 94));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void salvarArquivoComo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Arquivos THZ (*.thz)", "thz"));
        if (arquivoSelecionado != null && arquivoSelecionado.getParentFile() != null) {
            fc.setCurrentDirectory(arquivoSelecionado.getParentFile());
        }
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".thz")) {
                f = new File(f.getParentFile(), f.getName() + ".thz");
            }
            arquivoSelecionado = f;
            salvarArquivo();
            barraFerramentas.atualizarInfoArquivo(f.getName());
            if (config != null) {
                config = config.comArquivoRecente(f.getAbsolutePath());
                GerenciadorConfiguracao.salvar(config);
                barraMenu.atualizarRecentes(config.arquivosRecentes(), this::carregarArquivo);
            }
        }
    }

    @Override
    public void exportarRelatorio(String formato) {
        String fonte = editor.getText();
        if (fonte.isBlank()) {
            JOptionPane.showMessageDialog(this, "Editor vazio. Escreva ou abra um programa THZ.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Relatório (" + formato.toUpperCase() + ")");
        fc.setSelectedFile(new File("relatorio_thz." + formato));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            File dest = fc.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith("." + formato)) {
                dest = new File(dest.getAbsolutePath() + "." + formato);
            }
            try {
                List<Token> tokens = new ThzLexer(fonte).tokenize();
                ProgramaAst ast = new ThzParser(tokens).parse();

                Map<String, ValorThz> campos = new LinkedHashMap<>();
                campos.put("programa", ValorThz.TEXTO(ast.nome()));
                if (ast.metadados() != null) {
                    campos.put("dominio", ValorThz.TEXTO(ast.metadados().dominio()));
                    campos.put("slo", ValorThz.TEXTO(ast.metadados().sloLatencia()));
                }
                ValorThz.Registro reg = new ValorThz.Registro("RelatorioPrograma", campos);

                MotorDocumentos.exportar(dest.toPath(), "Relatório de Governança — " + ast.nome(), reg);
                JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso para:\n" + dest.getAbsolutePath(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar documento: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void alternarTema() {
        aplicarTema(tema == PaletaThz.ESCURO ? PaletaThz.CLARO : PaletaThz.ESCURO);
    }

    @Override
    public void alternarModoEstrito() {
        boolean estrito = !barraFerramentas.getToggleEstrito().isSelected();
        barraFerramentas.getToggleEstrito().setSelected(estrito);
        barraMenu.sincronizarModoEstrito(estrito);
        barraStatus.atualizarEstrito(estrito);
        if (config != null) {
            config = config.comModoEstrito(estrito);
            GerenciadorConfiguracao.salvar(config);
        }
    }

    @Override
    public void verificarCodigo() {
        editor.limparMarcacoesErro();
        boolean estrito = barraFerramentas.getToggleEstrito().isSelected();
        String fonte = editor.getText();

        var res = ExecutorMotorGui.verificar(fonte, estrito);
        saida.setText(String.join("\n", res.mensagensFormatadas()));

        if (res.sucesso()) {
            barraStatus.definirStatus("✓ Sintaxe e Semântica Válidas (0 erros)", new Color(34, 197, 94));
        } else {
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ " + res.totalErros() + " erro(s) encontrado(s)", new Color(239, 68, 68));
        }
    }

    @Override
    public void formatarCodigo() {
        try {
            String fonte = editor.getText();
            String fmt = ExecutorMotorGui.formatar(fonte);
            editor.setText(fmt);
            barraStatus.definirStatus("✨ Formatado com sucesso.", new Color(34, 197, 94));
        } catch (Exception ex) {
            saida.setText("[Erro na Formatação]\n" + ex.getMessage());
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ Erro na Formatação", new Color(239, 68, 68));
        }
    }

    @Override
    public void gerarDocumentacao() {
        try {
            String doc = ExecutorMotorGui.gerarDocumentacao(editor.getText());
            saida.setText(doc);
            editor.limparMarcacoesErro();
            barraStatus.definirStatus("📘 Documentação gerada com sucesso.", new Color(34, 197, 94));
        } catch (Exception ex) {
            saida.setText("[Erro ao gerar Documentação]\n" + ex.getMessage());
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ Erro no DocGen", new Color(239, 68, 68));
        }
    }

    @Override
    public void auditarGovernanca() {
        try {
            boolean estrito = barraFerramentas.getToggleEstrito().isSelected();
            String md = ExecutorMotorGui.auditar(editor.getText(), estrito);
            saida.setText(md);
            editor.limparMarcacoesErro();
            barraStatus.definirStatus("🛡️ Auditoria de Governança concluída.", new Color(34, 197, 94));
        } catch (Exception ex) {
            saida.setText("[Erro na Auditoria]\n" + ex.getMessage());
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ Erro na Auditoria", new Color(239, 68, 68));
        }
    }

    @Override
    public void gerarIrELlvm() {
        try {
            String irLlvm = ExecutorMotorGui.gerarIrELlvm(editor.getText());
            saida.setText(irLlvm);
            editor.limparMarcacoesErro();
            barraStatus.definirStatus("🧩 THZ-IR e LLVM gerados com sucesso.", new Color(34, 197, 94));
        } catch (Exception ex) {
            saida.setText("[Erro ao gerar IR / LLVM]\n" + ex.getMessage());
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ Erro no IR/LLVM", new Color(239, 68, 68));
        }
    }

    @Override
    public void executarCodigo() {
        editor.limparMarcacoesErro();
        String fonte = editor.getText();
        boolean estrito = barraFerramentas.getToggleEstrito().isSelected();

        var resVerif = ExecutorMotorGui.verificar(fonte, estrito);
        if (!resVerif.sucesso()) {
            saida.setText(String.join("\n", resVerif.mensagensFormatadas()));
            marcarErrosNoEditor(saida.getText());
            barraStatus.definirStatus("✗ Impossível executar: corrija os erros primeiro.", new Color(239, 68, 68));
            return;
        }

        ProgramaAst ast = resVerif.ast();
        List<String> logs = new ArrayList<>();
        BlocoMemoria bloco = new BlocoMemoria(64);
        bloco.alocar(2048);

        InterpretadorThz interp = new InterpretadorThz(ast, logs::add, () -> "");
        ProcedimentoAst procPrincipal = ast.procedimentos() != null ?
                ast.procedimentos().stream().filter(p -> p.nome().equalsIgnoreCase("Principal")).findFirst().orElse(null) : null;

        if (procPrincipal != null) {
            try {
                interp.executarProcedimento(procPrincipal.nome(), Map.of());
                bloco.liberarTudo();
                logs.add("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                saida.setText(String.join("\n", logs));
                barraStatus.definirStatus("✓ Execução concluída com sucesso.", new Color(34, 197, 94));
            } catch (Exception ex) {
                bloco.liberarTudo();
                logs.add("\n[ERRO DE EXECUÇÃO] " + ex.getMessage());
                saida.setText(String.join("\n", logs));
                marcarErrosNoEditor(saida.getText());
                barraStatus.definirStatus("✗ Erro na execução.", new Color(239, 68, 68));
            }
            return;
        }

        var opsExec = interp.listarOperacoesExecutaveis();
        if (!opsExec.isEmpty()) {
            var prim = opsExec.get(0);
            try {
                Map<String, ValorThz> args = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), ast, interp::validarInvariantes, p -> null);
                ValorThz ret = interp.executarOperacao(prim.operacao().nome(), args);
                bloco.liberarTudo();
                logs.add("[REGRA] " + prim.regra().nome() + " :: " + prim.operacao().nome() + "()");
                if (ret != null) logs.add("[RESULTADO] " + interp.formatar(ret));
                logs.add("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                saida.setText(String.join("\n", logs));
                barraStatus.definirStatus("✓ Operação executada com sucesso.", new Color(34, 197, 94));
            } catch (Exception ex) {
                bloco.liberarTudo();
                logs.add("\n[ERRO DE EXECUÇÃO] " + ex.getMessage());
                saida.setText(String.join("\n", logs));
                marcarErrosNoEditor(saida.getText());
                barraStatus.definirStatus("✗ Erro na execução.", new Color(239, 68, 68));
            }
        } else {
            bloco.liberarTudo();
            saida.setText("[AVISO] Nenhum PROCEDIMENTO Principal() ou OPERACAO executável encontrada.");
            barraStatus.definirStatus("Aviso: Nada para executar.", new Color(234, 179, 8));
        }
    }

    @Override
    public void abrirConfiguracaoJvm() {
        String novaJvm = DialogoConfiguracaoJvm.exibir(this, config);
        if (novaJvm != null) {
            config = config.comJvm(novaJvm);
            GerenciadorConfiguracao.salvar(config);
            atualizarJvmAtiva();
        }
    }

    @Override
    public void alternarPainelSaida() {
        cardSaida.setVisible(!cardSaida.isVisible());
        split.resetToPreferredSizes();
    }

    @Override
    public void limparSaida() {
        saida.setText("");
        editor.limparMarcacoesErro();
        barraStatus.definirPronto(tema);
    }

    @Override
    public void exibirManual() {
        JOptionPane.showMessageDialog(this,
                "Consulte a documentação completa da linguagem em docs/MANUAL_LINGUAGEM.md\n\n" +
                        "Principais recursos:\n" +
                        "• Atribuição: <-\n" +
                        "• Contratos: EXIGE / GARANTE / INVARIANTE\n" +
                        "• Precisão Bancária: DECIMAL(P, S) e MONETARIO\n" +
                        "• UI Declarativa: TELA e Formulários Swing\n" +
                        "• Documentos: PDF, XLSX, DOCX\n" +
                        "• Performance: Bloco de Memória Temporária e SIMD",
                "Manual da Linguagem THZ-LANG", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void exibirAtalhos() {
        JOptionPane.showMessageDialog(this,
                "Atalhos de Teclado:\n\n" +
                        "• F5 : Executar Programa\n" +
                        "• F6 : Verificar Sintaxe e Semântica\n" +
                        "• F7 : Gerar Documentação Técnica (DocGen)\n" +
                        "• F8 : Auditoria de Governança (DbC)\n" +
                        "• F9 : Inspecionar THZ-IR & LLVM\n" +
                        "• Ctrl + N : Novo Arquivo\n" +
                        "• Ctrl + O : Abrir Arquivo\n" +
                        "• Ctrl + S : Salvar Arquivo\n" +
                        "• Ctrl + Shift + F : Formatar Código\n" +
                        "• Ctrl + J : Alternar Console de Saída\n" +
                        "• Ctrl + K : Limpar Console de Saída",
                "Atalhos da IDE THZ-LANG", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void exibirSobre() {
        JOptionPane.showMessageDialog(this,
                "THZ-LANG Engine — JVM v2.3.0\n\n" +
                        "Linguagem Corporativa de Sistemas, Governança de Negócio e Alta Performance.\n" +
                        "Plataforma: Java 25 (LTS) sobre Gradle (Kotlin DSL)\n\n" +
                        "© 2026 THZ-LANG Project.",
                "Sobre o THZ-LANG", JOptionPane.INFORMATION_MESSAGE);
    }

    private void marcarErrosNoEditor(String textoSaida) {
        var matcher = POSICAO.matcher(textoSaida);
        while (matcher.find()) {
            try {
                int l = Integer.parseInt(matcher.group(1));
                int c = Integer.parseInt(matcher.group(2));
                editor.marcarErroLinha(l, c);
            } catch (Exception ignore) {}
        }
    }

    private void tratarCliqueSaida() {
        try {
            Point mousePos = saida.getMousePosition();
            int offset = (mousePos != null && saida.viewToModel2D(mousePos) >= 0) ?
                    saida.viewToModel2D(mousePos) : saida.getCaretPosition();
            int linhaOffset = saida.getLineOfOffset(offset);
            int inicioLinha = saida.getLineStartOffset(linhaOffset);
            int fimLinha = saida.getLineEndOffset(linhaOffset);
            String textoLinha = saida.getText(inicioLinha, fimLinha - inicioLinha);

            var matcher = POSICAO.matcher(textoLinha);
            if (matcher.find()) {
                int linha = Integer.parseInt(matcher.group(1));
                int coluna = Integer.parseInt(matcher.group(2));
                editor.irParaLinhaColuna(linha, coluna);
            }
        } catch (Exception ignore) {}
    }
}
