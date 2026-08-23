package thz.lang.gui;

import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.formato.Formatador;
import thz.lang.formato.JsonEscritor;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * THZ-LANG Desktop — visual polido (FlatLaf + cards + status bar).
 */
public final class ThzGui extends JFrame {
    private static final Pattern POSICAO = Pattern.compile("\\[Linha (\\d+):(\\d+)]");
    private static final String FONTE_INICIAL = """
            VERSAO_LINGUAGEM "2.3"

            PROGRAMA ExemploDesktop

            METADADOS_ARQUITETURA
                DOMINIO: "Desktop"
                SLO_LATENCIA_MAXIMA: "interativo"
                CONFORMIDADE: "DEMO"
            FIM_METADADOS

            # Bem-vindo ao THZ Desktop — experimentar colorização, erros e execução

            PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Olá pela interface Swing da THZ-LANG!"
                FIM

            FIM_PROGRAMA
            """;
    private final EditorThz editor = new EditorThz();
    private final JTextArea saida = new JTextArea();
    private final JLabel infoArquivo = new JLabel("Novo arquivo");
    private final JLabel infoCursor = new JLabel("Ln 1, Col 1");
    private final JLabel infoStatus = new JLabel("Pronto");
    private final JLabel badgeEstrito = new JLabel("Estrito");
    private final JToggleButton toggleTema = new JToggleButton("☀ Claro");
    private final JToggleButton toggleEstrito = new JToggleButton("Estrito");
    private final JPanel pillArquivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel header = new JPanel(new BorderLayout());
    private final JPanel toolbar = new JPanel();
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private PaletaThz tema = PaletaThz.ESCURO;
    private File arquivoSelecionado;

    public ThzGui() {
        super("THZ-LANG Desktop — thz-lang-engine-JVM");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1180, 780);
        setLocationRelativeTo(null);
        montarInterface();
        montarMenu();
        aplicarTema(PaletaThz.ESCURO);
        editor.setText(FONTE_INICIAL);
        atualizarCursorInfo();
    }

    static void main(String[] args) {
        // tenta FlatLaf escuro logo no boot
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
            }
        }
        SwingUtilities.invokeLater(() -> new ThzGui().setVisible(true));
    }

    private static File garantirExtensaoThz(File a) {
        return a.getName().toLowerCase().endsWith(".thz") ? a : new File(a.getParentFile(), a.getName() + ".thz");
    }

    // ---- tema ----
    private void aplicarTema(PaletaThz nova) {
        this.tema = nova;
        // tenta trocar L&F FlatLaf para combinar (sem bloquear se falhar)
        try {
            if (nova == PaletaThz.ESCURO)
                Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
            else
                Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignore) {
        }
        editor.aplicarTema(nova);
        // chrome
        Color bgWin = nova.fundoJanela, bgPanel = nova.fundoPainel, bgToolbar = nova.fundoToolbar,
                bgStatus = nova.fundoStatus;
        getContentPane().setBackground(bgWin);
        header.setBackground(bgWin);
        toolbar.setBackground(bgToolbar);
        statusBar.setBackground(bgStatus);
        saida.setBackground(nova == PaletaThz.ESCURO ? new Color(0x1E, 0x1E, 0x1E) : Color.WHITE);
        saida.setForeground(nova.frenteEditor);
        saida.setCaretColor(nova.corCaret);
        saida.setSelectionColor(nova.corSelecao);
        infoArquivo.setForeground(nova.corTextoSecundario);
        infoCursor.setForeground(nova.corTextoSecundario);
        infoStatus.setForeground(nova.corTextoSecundario);
        // toggle cores
        toggleTema.setSelected(nova == PaletaThz.CLARO);
        toggleTema.setText(nova == PaletaThz.CLARO ? "☾ Escuro" : "☀ Claro");
        // badge estrito
        badgeEstrito
                .setForeground(nova == PaletaThz.ESCURO ? new Color(0xC5, 0x86, 0xC0) : new Color(0xAF, 0x00, 0xDB));
        repaint();
    }

    private void montarInterface() {
        // header marca
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, tema.corBorda),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel("◆");
        logo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        logo.setForeground(tema.corAcento);
        JLabel titulo = new JLabel("THZ-LANG");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        titulo.setForeground(tema.corTextoTitulo);
        JLabel subt = new JLabel("Desktop  •  thz-lang-engine-JVM25  •  v2.3");
        subt.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subt.setForeground(tema.corTextoSecundario);
        brand.add(logo);
        brand.add(titulo);
        brand.add(Box.createHorizontalStrut(6));
        brand.add(subt);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);
        // pill arquivo
        JPanel pillArquivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pillArquivo.setOpaque(true);
        pillArquivo.setBackground(tema.fundoPainel);
        pillArquivo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tema.corBordaSuave, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        JLabel icArq = new JLabel("📄");
        infoArquivo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        pillArquivo.add(icArq);
        pillArquivo.add(infoArquivo);
        // toggles
        estilizarToggle(toggleEstrito);
        toggleEstrito.addActionListener(e -> atualizarBadgeEstrito());
        estilizarToggle(toggleTema);
        toggleTema.addActionListener(e -> aplicarTema(toggleTema.isSelected() ? PaletaThz.CLARO : PaletaThz.ESCURO));
        headerRight.add(pillArquivo);
        headerRight.add(toggleEstrito);
        headerRight.add(toggleTema);
        header.add(brand, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // toolbar moderna
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 8));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, tema.corBordaSuave),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        // botões primários/secundários
        toolbar.add(criarBotao("📂 Abrir", "Abrir arquivo .thz (Ctrl+O)", false, this::abrirArquivo));
        toolbar.add(criarBotao("💾 Salvar", "Salvar arquivo (Ctrl+S)", false, this::salvarArquivo));
        toolbar.add(separadorToolbar());
        toolbar.add(criarBotao("🔍 Verificar", "Verifica sintaxe e semântica", false, this::verificarCodigo));
        toolbar.add(criarBotao("▶ Executar", "Executa Principal / primeira OPERACAO", true, this::executarCodigo));
        toolbar.add(separadorToolbar());
        toolbar.add(criarBotao("✨ Formatar", "Formata canonicamente", false, this::formatarCodigo));
        toolbar.add(criarBotao("{ } AST", "Mostra AST em JSON", false, this::mostrarAst));
        JButton btnLimpar = criarBotao("🧹 Limpar", "Limpa saída e marcações", false, () -> {
            saida.setText("");
            editor.limparMarcacoesErro();
            infoStatus.setText("Pronto");
        });
        btnLimpar.setForeground(new Color(0x9A, 0x9A, 0x9A));
        toolbar.add(btnLimpar);

        // editor card
        JPanel cardEditor = criarCard("Editor  —  THZ-LANG (realçado por ThzLexer)", editor);
        // saída card
        saida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        saida.setEditable(false);
        saida.setLineWrap(true);
        saida.setWrapStyleWord(true);
        saida.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane spSaida = new JScrollPane(saida);
        spSaida.setBorder(BorderFactory.createEmptyBorder());
        spSaida.getVerticalScrollBar().setUnitIncrement(16);
        JPanel cardSaida = criarCard("Saída  /  Diagnósticos  —  [THZ CHECK/RUN/FMT/AST]", spSaida);
        cardSaida.setPreferredSize(new Dimension(0, 220));

        split.setTopComponent(cardEditor);
        split.setBottomComponent(cardSaida);
        split.setResizeWeight(0.68);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        split.setBackground(tema.fundoJanela);
        try {
            split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
                public void paint(Graphics g, JComponent jc) {
                    g.setColor(tema.fundoJanela);
                    g.fillRect(0, 0, jc.getWidth(), jc.getHeight());
                }
            });
        } catch (Exception ignore) {
        }

        // status bar
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, tema.corBordaSuave),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        JPanel stLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        stLeft.setOpaque(false);
        infoStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        badgeEstrito.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        badgeEstrito.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tema.corBordaSuave, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        badgeEstrito.setVisible(false);
        stLeft.add(infoStatus);
        stLeft.add(badgeEstrito);
        JPanel stRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        stRight.setOpaque(false);
        infoCursor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JLabel versao = new JLabel("thz-ir/1  •  v2.3.0");
        versao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        versao.setForeground(tema.corTextoSecundario);
        stRight.add(infoCursor);
        stRight.add(new JSeparator(SwingConstants.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 14));
            }
        });
        stRight.add(versao);
        statusBar.add(stLeft, BorderLayout.WEST);
        statusBar.add(stRight, BorderLayout.EAST);

        setLayout(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // listener cursor -> status
        editor.getPane().addCaretListener(e -> atualizarCursorInfo());
        // atalhos globais
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control O"), "abrir");
        getRootPane().getActionMap().put("abrir", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirArquivo();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control S"), "salvar");
        getRootPane().getActionMap().put("salvar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                salvarArquivo();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "executar");
        getRootPane().getActionMap().put("executar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                executarCodigo();
            }
        });
    }

    private JPanel criarCard(String titulo, JComponent conteudo) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(tema.fundoPainel);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tema.corBorda, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        JLabel lb = new JLabel(titulo);
        lb.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lb.setForeground(tema.corTextoSecundario);
        lb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, tema.corBordaSuave),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        lb.setOpaque(true);
        lb.setBackground(tema.fundoToolbar);
        card.add(lb, BorderLayout.NORTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        wrap.add(conteudo, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    private void estilizarToggle(JToggleButton btn) {
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tema.corBorda, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btn.setBackground(tema.fundoPainel);
        btn.setForeground(tema.corTextoSecundario);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setBackground(tema.corAcento);
                btn.setForeground(tema.corAcentoFg);
                btn.setBorder(
                        BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(tema.corAcento, 1, true),
                                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            } else {
                btn.setBackground(tema.fundoPainel);
                btn.setForeground(tema.corTextoSecundario);
                btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(tema.corBorda, 1, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            }
        });
    }

    private JButton criarBotao(String texto, String tooltip, boolean primario, Runnable acao) {
        JButton b = new JButton(texto);
        b.setToolTipText(tooltip);
        b.setFont(new Font(Font.SANS_SERIF, primario ? Font.BOLD : Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primario ? tema.corAcento : tema.corBorda, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        b.setBackground(primario ? tema.corAcento : tema.fundoPainel);
        b.setForeground(primario ? tema.corAcentoFg : tema.corTextoTitulo);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(e -> acao.run());
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!primario)
                    b.setBackground(tema.corBordaSuave);
                else
                    b.setBackground(tema.corAcentoHover);
            }

            public void mouseExited(MouseEvent e) {
                b.setBackground(primario ? tema.corAcento : tema.fundoPainel);
            }
        });
        return b;
    }

    private JComponent separadorToolbar() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 22));
        s.setForeground(tema.corBordaSuave);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    private void atualizarCursorInfo() {
        try {
            int caret = editor.getPane().getCaretPosition();
            String text = editor.getText();
            int line = 1, col = 1;
            for (int i = 0; i < caret && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else
                    col++;
            }
            // col correto considerando última quebra
            int lastNl = text.lastIndexOf('\n', Math.max(-1, caret - 1));
            col = caret - lastNl;
            infoCursor.setText("Ln " + line + ", Col " + col);
        } catch (Exception ignore) {
        }
    }

    private void atualizarBadgeEstrito() {
        badgeEstrito.setVisible(toggleEstrito.isSelected());
        infoStatus.setText(
                toggleEstrito.isSelected() ? "Modo estrito ativo — pragma/SLO/contratos verificados" : "Pronto");
    }

    private void montarMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu arquivo = new JMenu("Arquivo");
        JMenuItem miAbrir = new JMenuItem("Abrir…  Ctrl+O");
        miAbrir.addActionListener(e -> abrirArquivo());
        JMenuItem miSalvar = new JMenuItem("Salvar  Ctrl+S");
        miSalvar.addActionListener(e -> salvarArquivo());
        JMenuItem miSair = new JMenuItem("Sair");
        miSair.addActionListener(e -> dispose());
        arquivo.add(miAbrir);
        arquivo.add(miSalvar);
        arquivo.addSeparator();
        arquivo.add(miSair);
        JMenu editar = new JMenu("Editar");
        JMenuItem miDesfazer = new JMenuItem("Desfazer  Ctrl+Z");
        miDesfazer.addActionListener(e -> editor.desfazer());
        JMenuItem miRefazer = new JMenuItem("Refazer  Ctrl+Y");
        miRefazer.addActionListener(e -> editor.refazer());
        editar.add(miDesfazer);
        editar.add(miRefazer);
        JMenu ver = new JMenu("Ver");
        JCheckBoxMenuItem miTema = new JCheckBoxMenuItem("Tema claro");
        miTema.addActionListener(e -> {
            toggleTema.setSelected(miTema.isSelected());
            aplicarTema(miTema.isSelected() ? PaletaThz.CLARO : PaletaThz.ESCURO);
        });
        ver.add(miTema);
        JMenu acoes = new JMenu("Ações");
        JMenuItem miVerificar = new JMenuItem("Verificar");
        miVerificar.addActionListener(e -> verificarCodigo());
        JMenuItem miExecutar = new JMenuItem("Executar  F5");
        miExecutar.addActionListener(e -> executarCodigo());
        JMenuItem miFormatar = new JMenuItem("Formatar");
        miFormatar.addActionListener(e -> formatarCodigo());
        JMenuItem miAst = new JMenuItem("AST (JSON)");
        miAst.addActionListener(e -> mostrarAst());
        acoes.add(miVerificar);
        acoes.add(miExecutar);
        acoes.add(miFormatar);
        acoes.add(miAst);
        bar.add(arquivo);
        bar.add(montarMenuExemplos());
        bar.add(editar);
        bar.add(ver);
        bar.add(acoes);
        setJMenuBar(bar);
    }

    /** Galeria de exemplos: varre exemplos/colecao e exemplos/ (ordenado). */
    private JMenu montarMenuExemplos() {
        JMenu menu = new JMenu("Exemplos");
        menu.setToolTipText("Galeria de programas de partida — clique para carregar no editor");
        File raiz = new File("exemplos");
        if (!raiz.isDirectory()) {
            JMenuItem vazio = new JMenuItem("(pasta 'exemplos' não encontrada ao lado do jar)");
            vazio.setEnabled(false);
            menu.add(vazio);
            return menu;
        }
        boolean algum = false;
        File colecao = new File(raiz, "colecao");
        File[] daColecao = colecao.isDirectory() ? listarThzOrdenados(colecao) : new File[0];
        if (daColecao.length > 0) {
            JMenuItem cab = new JMenuItem("— Coleção de partida —");
            cab.setEnabled(false);
            menu.add(cab);
            for (File f : daColecao) {
                menu.add(itemExemplo(f));
                algum = true;
            }
        }
        File[] canonicos = listarThzOrdenados(raiz);
        if (canonicos.length > 0) {
            if (algum)
                menu.addSeparator();
            JMenuItem cab2 = new JMenuItem("— Canônicos (paridade TS ⇄ JVM) —");
            cab2.setEnabled(false);
            menu.add(cab2);
            for (File f : canonicos) {
                menu.add(itemExemplo(f));
                algum = true;
            }
        }
        if (!algum) {
            JMenuItem nenhum = new JMenuItem("(nenhum arquivo .thz encontrado)");
            nenhum.setEnabled(false);
            menu.add(nenhum);
        }
        return menu;
    }

    private static File[] listarThzOrdenados(File pasta) {
        File[] fs = pasta.listFiles((d, n) -> n.toLowerCase().endsWith(".thz"));
        if (fs == null)
            return new File[0];
        Arrays.sort(fs, Comparator.comparing(File::getName));
        return fs;
    }

    private JMenuItem itemExemplo(File f) {
        String nome = f.getName();
        String rotulo = nome.substring(0, nome.length() - 4).replaceFirst("^\\d+-", "").replace('-', ' ');
        JMenuItem item = new JMenuItem(rotulo);
        item.setToolTipText(f.getAbsolutePath());
        item.addActionListener(e -> carregarExemplo(f));
        return item;
    }

    /**
     * Carrega arquivo THZ para o editor.
     * 
     * @param f Arquivo THZ a ser carregado
     */
    private void carregarExemplo(File f) {
        try {
            editor.setText(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            editor.limparMarcacoesErro();
            saida.setText("");
            arquivoSelecionado = null; // exemplo não está "salvo" pelo usuário ainda
            infoArquivo.setText(f.getName());
            infoArquivo.setToolTipText(f.getAbsolutePath());
            infoStatus.setText("Exemplo carregado — " + f.getName());
            escreverSaida("[GUI] Exemplo carregado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao carregar exemplo", ex);
        }
    }

    /**
     * Método para abrir arquivo THZ
     */
    private void abrirArquivo() {
        JFileChooser sel = novoSeletor("Abrir arquivo THZ");
        if (sel.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            arquivoSelecionado = sel.getSelectedFile();
            editor.setText(Files.readString(arquivoSelecionado.toPath(), StandardCharsets.UTF_8));
            editor.limparMarcacoesErro();
            infoArquivo.setText(arquivoSelecionado.getName());
            infoArquivo.setToolTipText(arquivoSelecionado.getAbsolutePath());
            infoStatus.setText("Arquivo aberto — " + arquivoSelecionado.getName());
            escreverSaida("[GUI] Arquivo aberto: " + arquivoSelecionado.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao abrir arquivo", ex);
        }
    }

    /**
     * Método para salvar arquivo THZ
     */
    private void salvarArquivo() {
        try {
            if (arquivoSelecionado == null) {
                JFileChooser sel = novoSeletor("Salvar arquivo THZ");
                sel.setSelectedFile(new File("programa.thz"));
                if (sel.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                    return;
                arquivoSelecionado = garantirExtensaoThz(sel.getSelectedFile());
            }
            Files.writeString(arquivoSelecionado.toPath(), editor.getText(), StandardCharsets.UTF_8);
            infoArquivo.setText(arquivoSelecionado.getName());
            infoArquivo.setToolTipText(arquivoSelecionado.getAbsolutePath());
            infoStatus.setText("Salvo — " + arquivoSelecionado.getName());
            escreverSaida("[GUI] Arquivo salvo: " + arquivoSelecionado.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao salvar arquivo", ex);
        }
    }

    /**
     * Método para verificar o código
     */
    private void verificarCodigo() {
        saida.setText("");
        editor.limparMarcacoesErro();
        infoStatus.setText("Verificando…");
        try {
            String fonte = editor.getText();
            ProgramaAst ast = analisarFonte(fonte);
            List<ErroSemantico> erros = analisarSemantica(ast);
            if (!erros.isEmpty()) {
                List<DiagnosticoEntrada> ent = erros.stream()
                        .map(e -> new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
                imprimirErrosSemanticos(fonte, erros);
                editor.marcarErros(ent);
                escreverSaida("[THZ CHECK] " + erros.size() + " erro(s) semântico(s).");
                infoStatus.setText("✗ " + erros.size() + " erro(s) — veja marcações no editor");
                return;
            }
            escreverSaida("[THZ CHECK] Código validado com sucesso.  ✓ " + ast.nome());
            escreverSaida("[VERSÃO] "
                    + (ast.versaoLinguagem() == null ? "sem pragma — versão corrente" : ast.versaoLinguagem()));
            if (toggleEstrito.isSelected())
                escreverSaida("[MODO] Lint estrito aprovado.");
            infoStatus.setText("✓ Validado — " + ast.nome());
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Erro léxico/sintático");
        }
    }

    /**
     * Método para executar o código
     */
    private void executarCodigo() {
        saida.setText("");
        editor.limparMarcacoesErro();
        infoStatus.setText("Executando…");
        String fonte = editor.getText();
        boolean estrito = toggleEstrito.isSelected();
        setBotoesHabilitados(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    ProgramaAst ast = analisarFonte(fonte);
                    List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
                    if (!erros.isEmpty()) {
                        List<DiagnosticoEntrada> ent = erros.stream()
                                .map(e -> new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
                        imprimirErrosSemanticos(fonte, erros);
                        SwingUtilities.invokeLater(() -> editor.marcarErros(ent));
                        escreverSaida("[THZ RUN] Execução cancelada por erro semântico.");
                        SwingUtilities.invokeLater(() -> infoStatus.setText("✗ Bloqueado por erro semântico"));
                        return null;
                    }
                    escreverSaida("[THZ RUN] Executando " + ast.nome() + "…");
                    InterpretadorThz interp = new InterpretadorThz(ast, ThzGui.this::escreverSaida,
                            ThzGui.this::solicitarEntrada);
                    var principal = ast.procedimentos() == null ? null
                            : ast.procedimentos().stream().filter(p -> p.nome().equals("Principal")).findFirst()
                                    .orElse(null);
                    if (principal != null) {
                        if (!principal.parametros().isEmpty())
                            throw new IllegalStateException(
                                    "PROCEDIMENTO Principal exige parâmetros; use a CLI com --arg.");
                        interp.executarProcedimento("Principal", Map.of());
                    } else {
                        var op = interp.listarOperacoesExecutaveis().stream()
                                .filter(it -> it.operacao().parametros().isEmpty()).findFirst().orElse(null);
                        if (op == null)
                            throw new IllegalStateException(
                                    "Nenhum PROCEDIMENTO Principal ou operação sem parâmetros foi encontrado.");
                        escreverSaida("[REGRA] " + op.regra().nome() + " :: " + op.operacao().nome() + "()");
                        ValorThz r = interp.executarOperacao(op.operacao().nome(), Map.of());
                        if (r != null)
                            escreverSaida("[RESULTADO] " + interp.formatar(r));
                    }
                    escreverSaida("[THZ RUN] Execução finalizada com sucesso.  ✓");
                    SwingUtilities.invokeLater(() -> infoStatus.setText("✓ Execução concluída"));
                } catch (Exception ex) {
                    imprimirExcecaoComoDiagnostico(fonte, ex);
                    SwingUtilities.invokeLater(() -> infoStatus.setText("✗ Falha na execução"));
                }
                return null;
            }

            @Override
            protected void done() {
                setBotoesHabilitados(true);
            }
        }.execute();
    }

    private void formatarCodigo() {
        try {
            String fmt = Formatador.formatar(analisarFonte(editor.getText()));
            editor.setText(fmt);
            editor.limparMarcacoesErro();
            escreverSaida("[THZ FMT] Código formatado — canônico ✓");
            infoStatus.setText("✨ Formatado");
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Falha ao formatar");
        }
    }

    private void mostrarAst() {
        saida.setText("");
        try {
            escreverSaida(JsonEscritor.paraJson(analisarFonte(editor.getText())));
            infoStatus.setText("AST JSON gerado");
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Falha ao gerar AST");
        }
    }

    private ProgramaAst analisarFonte(String f) {
        List<Token> t = new ThzLexer(f).tokenize();
        return new ThzParser(t).parse();
    }

    private List<ErroSemantico> analisarSemantica(ProgramaAst a) {
        return new AnalisadorSemantico(a).analisar(new OpcoesAnalise(toggleEstrito.isSelected()));
    }

    private void imprimirErrosSemanticos(String fonte, List<ErroSemantico> erros) {
        List<DiagnosticoEntrada> ent = erros.stream()
                .map(e -> new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
        Diagnosticos.formatarDiagnosticos(fonte, ent, "Semântico")
                .forEach(b -> escreverSaida(b + System.lineSeparator()));
    }

    private void imprimirExcecaoComoDiagnostico(String fonte, Exception erro) {
        String msg = erro.getMessage() == null ? erro.toString() : erro.getMessage();
        var m = POSICAO.matcher(msg);
        if (m.find()) {
            int l = Integer.parseInt(m.group(1)), c = Integer.parseInt(m.group(2));
            var e = new DiagnosticoEntrada(l, c, msg);
            editor.marcarErros(List.of(e));
            escreverSaida(Diagnosticos.formatarDiagnosticos(fonte, List.of(e), "").getFirst());
        } else
            escreverSaida("[ERRO] " + msg);
    }

    private String solicitarEntrada() {
        if (SwingUtilities.isEventDispatchThread())
            return JOptionPane.showInputDialog(this, "Entrada solicitada pelo programa:");
        AtomicReference<String> r = new AtomicReference<>();
        try {
            SwingUtilities
                    .invokeAndWait(() -> r.set(JOptionPane.showInputDialog(this, "Entrada solicitada pelo programa:")));
            return r.get();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível solicitar a entrada.", ex);
        }
    }

    private void escreverSaida(String texto) {
        Runnable w = () -> {
            saida.append(texto);
            saida.append(System.lineSeparator());
            saida.setCaretPosition(saida.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread())
            w.run();
        else
            SwingUtilities.invokeLater(w);
    }

    private void setBotoesHabilitados(boolean h) {
        for (Component c : toolbar.getComponents())
            c.setEnabled(h);
    }

    private JFileChooser novoSeletor(String t) {
        JFileChooser s = new JFileChooser(
                arquivoSelecionado == null ? new File(".") : arquivoSelecionado.getParentFile());
        s.setDialogTitle(t);
        s.setFileFilter(new FileNameExtensionFilter("Programas THZ (*.thz)", "thz"));
        return s;
    }

    private void mostrarErro(String titulo, Exception erro) {
        String msg = erro.getMessage() == null ? erro.toString() : erro.getMessage();
        JOptionPane.showMessageDialog(this, msg, titulo, JOptionPane.ERROR_MESSAGE);
        escreverSaida("[ERRO] " + msg);
        infoStatus.setText("✗ " + titulo);
    }
}
