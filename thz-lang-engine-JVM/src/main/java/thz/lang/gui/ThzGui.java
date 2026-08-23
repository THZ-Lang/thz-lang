package thz.lang.gui;

import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.formato.Formatador;
import thz.lang.formato.JsonEscritor;
import thz.lang.interpretador.InjetorLoteDemo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;

import thz.lang.gui.config.ConfiguracaoDesktop;
import thz.lang.gui.config.DetectorJvm;
import thz.lang.gui.config.DialogoConfiguracaoJvm;
import thz.lang.gui.config.GerenciadorConfiguracao;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * THZ-LANG Desktop — Interface Swing moderna e modularizada.
 * Decomposto e aderente a SRP, SOLID, DRY e KISS com persistência de configuração em JSON.
 */
public final class ThzGui extends JFrame {
    private static final Pattern POSICAO = Pattern.compile("\\[Linha (\\d+):(\\d+)]");
    private static final String FONTE_INICIAL = """
            VERSAO_LINGUAGEM "2.3"

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

    // Componentes de topo
    private final JPanel header = new JPanel(new BorderLayout());
    private final JLabel logoBrand = new JLabel("⚡");
    private final JLabel tituloBrand = new JLabel("THZ-LANG");
    private final JLabel subtBrand = new JLabel("Engine JVM");
    private final JPanel pillArquivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel infoArquivo = new JLabel("Novo arquivo");
    private final JToggleButton toggleEstrito = new JToggleButton("Lint Estrito");
    private final JToggleButton toggleTema = new JToggleButton("☾ Escuro");

    // Editor, Saída e Split
    private final EditorThz editor = new EditorThz();
    private final JTextArea saida = new JTextArea();
    private final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    // Barra de Status
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JLabel infoStatus = new JLabel("Pronto");
    private final JLabel badgeEstrito = new JLabel("ESTRITO");
    private final JLabel infoCursor = new JLabel("Ln 1, Col 1");
    private final JLabel versaoLabel = new JLabel("v2.3.0 JVM");

    // Toolbar e Coleções de Widgets para reestilização
    private final JPanel toolbar = new JPanel();
    private final List<JButton> botoesSecundarios = new ArrayList<>();
    private final List<JSeparator> separadores = new ArrayList<>();

    private JButton btnExecutar;
    private JButton btnLimpar;
    private JPanel cardEditor;
    private JPanel cardSaida;
    private JLabel lblCardEditor;
    private JLabel lblCardSaida;
    private JCheckBoxMenuItem miTemaMenu;
    private JMenu menuRecentes;

    private PaletaThz tema = PaletaThz.ESCURO;
    private File arquivoSelecionado;
    private ConfiguracaoDesktop config;

    public ThzGui() {
        super("THZ-LANG Desktop — thz-lang-engine-JVM");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));

        this.config = GerenciadorConfiguracao.carregar();
        setSize(config.larguraJanela(), config.alturaJanela());
        if (config.posicaoX() >= 0 && config.posicaoY() >= 0) {
            setLocation(config.posicaoX(), config.posicaoY());
        } else {
            setLocationRelativeTo(null);
        }
        if (config.maximizada()) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }

        montarInterface();
        montarMenu();

        if (config.posicaoDivisor() > 50) {
            split.setDividerLocation(config.posicaoDivisor());
        }

        toggleEstrito.setSelected(config.modoEstrito());
        aplicarTema("CLARO".equalsIgnoreCase(config.tema()) ? PaletaThz.CLARO : PaletaThz.ESCURO);

        if (config.ultimoArquivo() != null && !config.ultimoArquivo().isBlank() && new File(config.ultimoArquivo()).exists()) {
            carregarArquivoDireto(new File(config.ultimoArquivo()));
        } else {
            editor.setText(FONTE_INICIAL);
        }
        atualizarCursorInfo();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salvarConfiguracaoAtual();
            }
        });
    }

    static void main(String[] args) {
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

    // ---- Tema e Estilização Dinâmica ----

    private void aplicarTema(PaletaThz nova) {
        this.tema = nova;
        try {
            if (nova == PaletaThz.ESCURO)
                Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
            else
                Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignore) {
        }
        editor.aplicarTema(nova);

        if (config != null) {
            config = config.comTema(nova == PaletaThz.CLARO ? "CLARO" : "ESCURO");
            GerenciadorConfiguracao.salvar(config);
        }


        getContentPane().setBackground(nova.fundoJanela);
        header.setBackground(nova.fundoJanela);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, nova.corBorda),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        toolbar.setBackground(nova.fundoToolbar);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, nova.corBordaSuave),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));

        statusBar.setBackground(nova.fundoStatus);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, nova.corBordaSuave),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        split.setBackground(nova.fundoJanela);

        pillArquivo.setBackground(nova.fundoPainel);
        pillArquivo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(nova.corBordaSuave, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        logoBrand.setForeground(nova.corAcento);
        tituloBrand.setForeground(nova.corTextoTitulo);
        subtBrand.setForeground(nova.corTextoSecundario);
        infoArquivo.setForeground(nova.corTextoSecundario);
        infoCursor.setForeground(nova.corTextoSecundario);
        infoStatus.setForeground(nova.corTextoSecundario);
        versaoLabel.setForeground(nova.corTextoSecundario);

        for (JButton b : botoesSecundarios) {
            b.setBackground(nova.fundoPainel);
            b.setForeground(nova.corTextoTitulo);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(nova.corBorda, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        }

        if (btnExecutar != null) {
            btnExecutar.setBackground(nova.corAcento);
            btnExecutar.setForeground(nova.corAcentoFg);
            btnExecutar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(nova.corAcento, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        }

        if (btnLimpar != null) {
            btnLimpar.setBackground(nova.fundoPainel);
            btnLimpar.setForeground(nova.corTextoSecundario);
            btnLimpar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(nova.corBorda, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        }

        for (JSeparator sep : separadores) {
            sep.setForeground(nova.corBordaSuave);
        }

        atualizarEstiloCard(cardEditor, lblCardEditor, nova);
        atualizarEstiloCard(cardSaida, lblCardSaida, nova);

        saida.setBackground(nova == PaletaThz.ESCURO ? new Color(0x1E, 0x1E, 0x1E) : Color.WHITE);
        saida.setForeground(nova.frenteEditor);
        saida.setCaretColor(nova.corCaret);
        saida.setSelectionColor(nova.corSelecao);

        toggleTema.setSelected(nova == PaletaThz.CLARO);
        toggleTema.setText(nova == PaletaThz.CLARO ? "☾ Escuro" : "☀ Claro");
        if (miTemaMenu != null) {
            miTemaMenu.setSelected(nova == PaletaThz.CLARO);
        }
        reestilizarToggle(toggleEstrito, nova);
        reestilizarToggle(toggleTema, nova);

        badgeEstrito.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(nova.corBordaSuave, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        badgeEstrito.setForeground(nova == PaletaThz.ESCURO ? new Color(0xC5, 0x86, 0xC0) : new Color(0xAF, 0x00, 0xDB));

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

    private void reestilizarToggle(JToggleButton btn, PaletaThz p) {
        if (btn == null) return;
        if (btn.isSelected()) {
            btn.setBackground(p.corAcento);
            btn.setForeground(p.corAcentoFg);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(p.corAcento, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        } else {
            btn.setBackground(p.fundoPainel);
            btn.setForeground(p.corTextoSecundario);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(p.corBorda, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        }
    }

    // ---- Construção de Interface ----

    private void montarInterface() {
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        logoBrand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        tituloBrand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        subtBrand.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        brand.add(logoBrand);
        brand.add(tituloBrand);
        brand.add(Box.createHorizontalStrut(6));
        brand.add(subtBrand);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        pillArquivo.setOpaque(true);
        JLabel icArq = new JLabel("📄");
        infoArquivo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        pillArquivo.add(icArq);
        pillArquivo.add(infoArquivo);

        estilizarToggle(toggleEstrito);
        toggleEstrito.addActionListener(e -> {
            reestilizarToggle(toggleEstrito, tema);
            atualizarBadgeEstrito();
            if (config != null) {
                config = config.comModoEstrito(toggleEstrito.isSelected());
                GerenciadorConfiguracao.salvar(config);
            }
        });
        estilizarToggle(toggleTema);
        toggleTema.addActionListener(e -> aplicarTema(toggleTema.isSelected() ? PaletaThz.CLARO : PaletaThz.ESCURO));


        headerRight.add(pillArquivo);
        headerRight.add(toggleEstrito);
        headerRight.add(toggleTema);
        header.add(brand, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 8));

        toolbar.add(criarBotao("📄 Novo", "Novo arquivo (Ctrl+N)", false, this::novoArquivo));
        toolbar.add(criarBotao("📂 Abrir", "Abrir arquivo .thz (Ctrl+O)", false, this::abrirArquivo));
        toolbar.add(criarBotao("💾 Salvar", "Salvar arquivo (Ctrl+S)", false, this::salvarArquivo));
        toolbar.add(separadorToolbar());
        toolbar.add(criarBotao("🔍 Verificar", "Verifica sintaxe e semântica (F7)", false, this::verificarCodigo));

        btnExecutar = criarBotao("▶ Executar", "Executa Principal / primeira OPERACAO (F5)", true, this::executarCodigo);
        toolbar.add(btnExecutar);

        toolbar.add(separadorToolbar());
        toolbar.add(criarBotao("✨ Formatar", "Formata canonicamente", false, this::formatarCodigo));
        toolbar.add(criarBotao("{ } AST", "Mostra AST em JSON", false, this::mostrarAst));
        toolbar.add(criarBotao("📘 Doc", "Gera documentação técnica com diagramas Mermaid", false, this::mostrarDoc));
        toolbar.add(criarBotao("🛡️ Auditoria", "Gera relatório de auditoria e governança (G4)", false, this::mostrarAuditoria));
        toolbar.add(criarBotao("🧩 IR", "Gera Representação Intermediária THZ-IR / SIMD (G5)", false, this::mostrarIr));

        btnLimpar = criarBotao("🧹 Limpar", "Limpa saída e marcações", false, () -> {


            saida.setText("");
            editor.limparMarcacoesErro();
            infoStatus.setText("Pronto");
        });
        toolbar.add(btnLimpar);

        cardEditor = criarCard("Editor  —  THZ-LANG (realçado por ThzLexer)", editor, lb -> lblCardEditor = lb);

        saida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        saida.setEditable(false);
        saida.setLineWrap(true);
        saida.setWrapStyleWord(true);
        saida.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane spSaida = new JScrollPane(saida);
        spSaida.setBorder(BorderFactory.createEmptyBorder());
        spSaida.getVerticalScrollBar().setUnitIncrement(16);
        cardSaida = criarCard("Saída  /  Diagnósticos  —  [THZ CHECK/RUN/FMT/AST]", spSaida, lb -> lblCardSaida = lb);
        cardSaida.setPreferredSize(new Dimension(0, 220));

        split.setTopComponent(cardEditor);
        split.setBottomComponent(cardSaida);
        split.setResizeWeight(0.68);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        try {
            split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
                public void paint(Graphics g, JComponent jc) {
                    g.setColor(tema.fundoJanela);
                    g.fillRect(0, 0, jc.getWidth(), jc.getHeight());
                }
            });
        } catch (Exception ignore) {
        }

        JPanel stLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        stLeft.setOpaque(false);
        infoStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        badgeEstrito.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        badgeEstrito.setVisible(false);
        stLeft.add(infoStatus);
        stLeft.add(badgeEstrito);

        JPanel stRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        stRight.setOpaque(false);
        infoCursor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        versaoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        JSeparator sepStatus = new JSeparator(SwingConstants.VERTICAL);
        sepStatus.setPreferredSize(new Dimension(1, 14));
        separadores.add(sepStatus);

        stRight.add(infoCursor);
        stRight.add(sepStatus);
        stRight.add(versaoLabel);
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

        editor.getPane().addCaretListener(e -> atualizarCursorInfo());
        configurarAtalhos();
    }

    private void configurarAtalhos() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();

        im.put(KeyStroke.getKeyStroke("control N"), "novo");
        am.put("novo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                novoArquivo();
            }
        });

        im.put(KeyStroke.getKeyStroke("control O"), "abrir");
        am.put("abrir", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                abrirArquivo();
            }
        });

        im.put(KeyStroke.getKeyStroke("control S"), "salvar");
        am.put("salvar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                salvarArquivo();
            }
        });

        im.put(KeyStroke.getKeyStroke("control shift S"), "salvarComo");
        am.put("salvarComo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                salvarComoArquivo();
            }
        });

        im.put(KeyStroke.getKeyStroke("F7"), "verificar");
        am.put("verificar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                verificarCodigo();
            }
        });

        im.put(KeyStroke.getKeyStroke("F5"), "executar");
        am.put("executar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                executarCodigo();
            }
        });
    }

    private JPanel criarCard(String titulo, JComponent conteudo, java.util.function.Consumer<JLabel> labelSink) {
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
        if (labelSink != null) {
            labelSink.accept(lb);
        }
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
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addChangeListener(e -> reestilizarToggle(btn, tema));
        reestilizarToggle(btn, tema);
    }

    private JButton criarBotao(String texto, String tooltip, boolean primario, Runnable acao) {
        JButton b = new JButton(texto);
        b.setToolTipText(tooltip);
        b.setFont(new Font(Font.SANS_SERIF, primario ? Font.BOLD : Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                if (!primario)
                    b.setBackground(tema.fundoPainel);
                else
                    b.setBackground(tema.corAcento);
            }
        });
        if (!primario) {
            botoesSecundarios.add(b);
        }
        return b;
    }

    private JComponent separadorToolbar() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 22));
        s.setForeground(tema.corBordaSuave);
        separadores.add(s);
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
        JMenuItem miNovo = new JMenuItem("Novo  Ctrl+N");
        miNovo.addActionListener(e -> novoArquivo());
        JMenuItem miAbrir = new JMenuItem("Abrir…  Ctrl+O");
        miAbrir.addActionListener(e -> abrirArquivo());
        menuRecentes = new JMenu("Arquivos Recentes");
        atualizarMenuRecentes();
        JMenuItem miSalvar = new JMenuItem("Salvar  Ctrl+S");
        miSalvar.addActionListener(e -> salvarArquivo());
        JMenuItem miSalvarComo = new JMenuItem("Salvar Como…  Ctrl+Shift+S");
        miSalvarComo.addActionListener(e -> salvarComoArquivo());
        JMenuItem miSair = new JMenuItem("Sair");
        miSair.addActionListener(e -> dispose());
        arquivo.add(miNovo);
        arquivo.add(miAbrir);
        arquivo.add(menuRecentes);
        arquivo.add(miSalvar);
        arquivo.add(miSalvarComo);
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
        miTemaMenu = new JCheckBoxMenuItem("Tema claro");
        miTemaMenu.addActionListener(e -> {
            toggleTema.setSelected(miTemaMenu.isSelected());
            aplicarTema(miTemaMenu.isSelected() ? PaletaThz.CLARO : PaletaThz.ESCURO);
        });
        ver.add(miTemaMenu);

        JMenu acoes = new JMenu("Ações");
        JMenuItem miVerificar = new JMenuItem("Verificar  F7");
        miVerificar.addActionListener(e -> verificarCodigo());
        JMenuItem miExecutar = new JMenuItem("Executar  F5");
        miExecutar.addActionListener(e -> executarCodigo());
        JMenuItem miFormatar = new JMenuItem("Formatar");
        miFormatar.addActionListener(e -> formatarCodigo());
        JMenuItem miAst = new JMenuItem("AST (JSON)");
        miAst.addActionListener(e -> mostrarAst());
        JMenuItem miDoc = new JMenuItem("Gerar Documentação (DocGen)");
        miDoc.addActionListener(e -> mostrarDoc());
        JMenuItem miAudit = new JMenuItem("Auditoria de Governança (G4)");
        miAudit.addActionListener(e -> mostrarAuditoria());
        JMenuItem miIr = new JMenuItem("Gerar THZ-IR / SIMD (G5)");
        miIr.addActionListener(e -> mostrarIr());
        acoes.add(miVerificar);
        acoes.add(miExecutar);
        acoes.add(miFormatar);
        acoes.add(miAst);
        acoes.add(miDoc);
        acoes.add(miAudit);
        acoes.add(miIr);




        bar.add(arquivo);
        bar.add(GaleriaExemplos.criarMenuExemplos(this::carregarExemplo));
        bar.add(editar);
        bar.add(ver);
        bar.add(acoes);

        JMenu configMenu = new JMenu("Configurações");
        JMenuItem miJvm = new JMenuItem("⚙ Configurar JVM / Java Runtime…");
        miJvm.addActionListener(e -> abrirConfiguracaoJvm());

        JMenuItem miInfoJvm = new JMenuItem("ℹ Informações do Ambiente JVM");
        miInfoJvm.addActionListener(e -> mostrarInfoJvm());

        configMenu.add(miJvm);
        configMenu.add(miInfoJvm);
        configMenu.addSeparator();

        JCheckBoxMenuItem miEstritoMenu = new JCheckBoxMenuItem("Lint Estrito (--estrito)");
        miEstritoMenu.setSelected(toggleEstrito.isSelected());
        miEstritoMenu.addActionListener(e -> {
            toggleEstrito.setSelected(miEstritoMenu.isSelected());
            reestilizarToggle(toggleEstrito, tema);
            atualizarBadgeEstrito();
            if (config != null) {
                config = config.comModoEstrito(toggleEstrito.isSelected());
                GerenciadorConfiguracao.salvar(config);
            }
        });
        configMenu.add(miEstritoMenu);

        bar.add(configMenu);
        setJMenuBar(bar);
    }

    private void abrirConfiguracaoJvm() {
        String escolhida = DialogoConfiguracaoJvm.exibir(this, config);
        if (escolhida != null) {
            config = config.comJvm(escolhida);
            GerenciadorConfiguracao.salvar(config);
            DetectorJvm.InfoJvm info = DetectorJvm.inspecionarJvm("Configurada", escolhida);
            escreverSaida("\n[CONFIG] JVM configurada para execução: " + (escolhida.isBlank() ? "Padrão do Sistema / Embutida" : escolhida + " (" + info.versao() + ")"));
            infoStatus.setText("JVM configurada — " + (escolhida.isBlank() ? "Padrão" : info.versao()));
        }
    }

    private void mostrarInfoJvm() {
        DetectorJvm.InfoJvm atual = DetectorJvm.obterJvmAtual();
        String jvmConfig = (config != null && config.caminhoJvm() != null && !config.caminhoJvm().isBlank())
                ? config.caminhoJvm() : "(Padrão do Sistema / Embutida)";

        String msg = String.format("""
                Informações do Java Runtime Environment (JVM):

                • JVM em Execução: %s
                • Versão da JVM: %s
                • Fornecedor: %s
                • Diretório java.home: %s
                • JVM Selecionada para Programas: %s
                • Sistema Operacional: %s (%s)
                """,
                atual.rotulo(),
                atual.versao(),
                atual.fornecedor(),
                atual.caminho(),
                jvmConfig,
                System.getProperty("os.name"),
                System.getProperty("os.arch")
        );

        JOptionPane.showMessageDialog(this, msg, "Informações do Ambiente JVM", JOptionPane.INFORMATION_MESSAGE);
    }


    // ---- Ações de Arquivo ----

    private void novoArquivo() {
        arquivoSelecionado = null;
        editor.setText(FONTE_INICIAL);
        editor.limparMarcacoesErro();
        saida.setText("");
        infoArquivo.setText("Novo arquivo");
        infoArquivo.setToolTipText(null);
        infoStatus.setText("Novo arquivo criado");
        escreverSaida("[GUI] Novo arquivo iniciado.");
    }

    private void carregarExemplo(File f) {
        try {
            editor.setText(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            editor.limparMarcacoesErro();
            saida.setText("");
            arquivoSelecionado = null;
            infoArquivo.setText(f.getName());
            infoArquivo.setToolTipText(f.getAbsolutePath());
            infoStatus.setText("Exemplo carregado — " + f.getName());
            escreverSaida("[GUI] Exemplo carregado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao carregar exemplo", ex);
        }
    }

    public void carregarArquivoDireto(File f) {
        if (f == null || !f.exists()) return;
        try {
            arquivoSelecionado = f;
            editor.setText(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            editor.limparMarcacoesErro();
            infoArquivo.setText(f.getName());
            infoArquivo.setToolTipText(f.getAbsolutePath());
            infoStatus.setText("Arquivo aberto — " + f.getName());
            if (config != null) {
                config = config.comArquivoRecente(f.getAbsolutePath());
                GerenciadorConfiguracao.salvar(config);
                atualizarMenuRecentes();
            }
            escreverSaida("[GUI] Arquivo carregado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao abrir arquivo", ex);
        }
    }

    private void atualizarMenuRecentes() {
        if (menuRecentes == null) return;
        menuRecentes.removeAll();
        if (config == null || config.arquivosRecentes() == null || config.arquivosRecentes().isEmpty()) {
            JMenuItem miVazio = new JMenuItem("(Nenhum arquivo recente)");
            miVazio.setEnabled(false);
            menuRecentes.add(miVazio);
        } else {
            for (String caminho : config.arquivosRecentes()) {
                File f = new File(caminho);
                JMenuItem item = new JMenuItem(f.getName() + "  (" + (f.getParent() != null ? f.getParent() : ".") + ")");
                item.setToolTipText(caminho);
                item.addActionListener(e -> carregarArquivoDireto(f));
                menuRecentes.add(item);
            }
            menuRecentes.addSeparator();
            JMenuItem miLimpar = new JMenuItem("Limpar Histórico Recente");
            miLimpar.addActionListener(e -> {
                if (config != null) {
                    config = new ConfiguracaoDesktop(config.tema(), config.modoEstrito(), config.ultimoArquivo(),
                            config.larguraJanela(), config.alturaJanela(), config.posicaoX(), config.posicaoY(),
                            config.maximizada(), config.posicaoDivisor(), config.tamanhoFonte(), config.caminhoJvm(), List.of());
                    GerenciadorConfiguracao.salvar(config);
                    atualizarMenuRecentes();
                }
            });
            menuRecentes.add(miLimpar);
        }
    }

    private void salvarConfiguracaoAtual() {
        if (config == null) return;
        int estado = getExtendedState();
        boolean max = (estado & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
        Point loc = getLocation();
        Dimension dim = getSize();
        int div = split.getDividerLocation();
        String t = (tema == PaletaThz.CLARO) ? "CLARO" : "ESCURO";
        boolean est = toggleEstrito.isSelected();
        String arq = arquivoSelecionado != null ? arquivoSelecionado.getAbsolutePath() : config.ultimoArquivo();

        config = new ConfiguracaoDesktop(
                t,
                est,
                arq != null ? arq : "",
                dim.width,
                dim.height,
                loc.x,
                loc.y,
                max,
                div,
                config.tamanhoFonte(),
                config.caminhoJvm(),
                config.arquivosRecentes()
        );
        GerenciadorConfiguracao.salvar(config);
    }


    private void abrirArquivo() {
        JFileChooser sel = novoSeletor("Abrir arquivo THZ");
        if (sel.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        carregarArquivoDireto(sel.getSelectedFile());
    }

    private void salvarArquivo() {
        try {
            if (arquivoSelecionado == null) {
                salvarComoArquivo();
                return;
            }
            Files.writeString(arquivoSelecionado.toPath(), editor.getText(), StandardCharsets.UTF_8);
            infoArquivo.setText(arquivoSelecionado.getName());
            infoArquivo.setToolTipText(arquivoSelecionado.getAbsolutePath());
            infoStatus.setText("Salvo — " + arquivoSelecionado.getName());
            if (config != null) {
                config = config.comArquivoRecente(arquivoSelecionado.getAbsolutePath());
                GerenciadorConfiguracao.salvar(config);
                atualizarMenuRecentes();
            }
            escreverSaida("[GUI] Arquivo salvo: " + arquivoSelecionado.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao salvar arquivo", ex);
        }
    }

    private void salvarComoArquivo() {
        try {
            JFileChooser sel = novoSeletor("Salvar arquivo THZ");
            sel.setSelectedFile(new File(arquivoSelecionado != null ? arquivoSelecionado.getName() : "programa.thz"));
            if (sel.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return;
            arquivoSelecionado = garantirExtensaoThz(sel.getSelectedFile());
            Files.writeString(arquivoSelecionado.toPath(), editor.getText(), StandardCharsets.UTF_8);
            infoArquivo.setText(arquivoSelecionado.getName());
            infoArquivo.setToolTipText(arquivoSelecionado.getAbsolutePath());
            infoStatus.setText("Salvo — " + arquivoSelecionado.getName());
            if (config != null) {
                config = config.comArquivoRecente(arquivoSelecionado.getAbsolutePath());
                GerenciadorConfiguracao.salvar(config);
                atualizarMenuRecentes();
            }
            escreverSaida("[GUI] Arquivo salvo: " + arquivoSelecionado.getAbsolutePath());
        } catch (Exception ex) {
            mostrarErro("Erro ao salvar arquivo", ex);
        }
    }


    // ---- Ações do Motor (Verificar, Executar, Formatar, AST) ----

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

                    escreverSaida("================================================================================");
                    escreverSaida("   EXECUTANDO MOTOR NATIVO THZ-LANG: " + ast.nome());
                    escreverSaida("================================================================================\n");

                    if (ast.metadados() != null) {
                        String dom = ast.metadados().dominio() != null ? ast.metadados().dominio() : "N/A";
                        String slo = ast.metadados().sloLatencia() != null ? ast.metadados().sloLatencia() : "N/A";
                        String conf = ast.metadados().conformidade() != null ? String.join(", ", ast.metadados().conformidade()) : "N/A";
                        escreverSaida("[ARQUITETURA] Domínio: " + dom + " | SLO: " + slo);
                        escreverSaida("[CONFORMIDADE] Diretrizes ativas: " + conf + "\n");
                    }

                    InterpretadorThz interp = new InterpretadorThz(ast, ThzGui.this::escreverSaida,
                            ThzGui.this::solicitarEntrada);

                    ProcedimentoAst principal = ast.procedimentos() == null ? null
                            : ast.procedimentos().stream().filter(p -> p.nome().equals("Principal")).findFirst().orElse(null);

                    if (principal != null) {
                        escreverSaida("[PROCEDIMENTO] Principal()\n");
                        Map<String, ValorThz> args = principal.parametros().isEmpty() ? Map.of()
                                : InjetorLoteDemo.construirArgsProc(principal, p -> solicitarParametro("Procedimento " + principal.nome() + " :: Parâmetro " + p.nome() + " (" + p.tipo() + "):", ""));
                        interp.executarProcedimento("Principal", args);
                    } else {
                        var execs = interp.listarOperacoesExecutaveis();
                        if (execs.isEmpty()) {
                            throw new IllegalStateException(
                                    "Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal.");
                        }
                        var prim = execs.get(0);
                        escreverSaida("[REGRA] " + prim.regra().nome() + (prim.regra().identificador() != null ? " (" + prim.regra().identificador() + ")" : "") + " :: " + prim.operacao().nome() + "()\n");
                        Map<String, ValorThz> args = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), ast, interp::validarInvariantes,
                                p -> solicitarParametro("Operação " + prim.operacao().nome() + " :: Parâmetro " + p.nome() + " (" + p.tipo() + "):", "0"));
                        ValorThz r = interp.executarOperacao(prim.operacao().nome(), args);
                        if (r != null) {
                            escreverSaida("--------------------------------------------------------------");
                            escreverSaida("[RESULTADO] " + interp.formatar(r));
                        }
                    }
                    escreverSaida("\n[THZ RUN] Execução finalizada com sucesso.  ✓");
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

    private void mostrarAuditoria() {
        saida.setText("");
        editor.limparMarcacoesErro();
        try {
            ProgramaAst ast = analisarFonte(editor.getText());
            thz.lang.governanca.RelatorioAuditoria rel = thz.lang.governanca.AuditorGovernanca.auditar(ast);
            String md = thz.lang.governanca.AuditorGovernanca.gerarMarkdownGovernanca(rel);
            escreverSaida(md);
            saida.setCaretPosition(0);
            infoStatus.setText("Auditoria — Score: " + rel.metricas().percentualConformidade() + "% ("
                    + (rel.metricas().aprovado() ? "Aprovado" : "Pendências") + ")");
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Falha ao gerar auditoria");
        }
    }

    private void mostrarDoc() {
        saida.setText("");
        editor.limparMarcacoesErro();
        try {
            ProgramaAst ast = analisarFonte(editor.getText());
            String doc = thz.lang.docgen.ThzDocGen.gerarDocumentacao(ast);
            escreverSaida(doc);
            saida.setCaretPosition(0);
            infoStatus.setText("Documentação DocGen (Markdown + Mermaid) gerada com sucesso");
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Falha ao gerar documentação");
        }
    }

    private void mostrarIr() {
        saida.setText("");
        editor.limparMarcacoesErro();
        try {
            ProgramaAst ast = analisarFonte(editor.getText());
            var ir = thz.lang.ir.GeradorIr.baixarParaIr(ast);
            String json = thz.lang.ir.GeradorIr.serializarIrJson(ir);
            escreverSaida(json);
            saida.setCaretPosition(0);
            infoStatus.setText("THZ-IR/1 gerado — Funções: " + ir.funcoes().size() + " | Loops SIMD: " + ir.loopsSimd().size());
        } catch (Exception ex) {
            imprimirExcecaoComoDiagnostico(editor.getText(), ex);
            infoStatus.setText("✗ Falha ao gerar THZ-IR");
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

    private String solicitarParametro(String prompt, String padrao) {
        if (SwingUtilities.isEventDispatchThread()) {
            return JOptionPane.showInputDialog(this, prompt, padrao);
        }
        AtomicReference<String> r = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> r.set(JOptionPane.showInputDialog(this, prompt, padrao)));
            return r.get();
        } catch (Exception ex) {
            return padrao;
        }
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
