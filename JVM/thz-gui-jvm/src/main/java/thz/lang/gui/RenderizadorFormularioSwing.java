package thz.lang.gui;

import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ParametroOperacaoAst;
import thz.lang.gui.formulario.ExportadorFormularioGui;
import thz.lang.gui.formulario.FabricaCamposFormulario;
import thz.lang.gui.formulario.PainelTabelaFatia;
import thz.lang.interpretador.ErroContrato;
import thz.lang.interpretador.ErroExecucao;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderizador Host Swing para interfaces declarativas THZ-LANG.
 * Constrói dinamicamente formulários Desktop modernos a partir de uma ESTRUTURA
 * e conecta eventos de submissão a operações de negócio com validação de contratos EXIGE/GARANTE.
 */
public class RenderizadorFormularioSwing {

    private final ValorThz.Registro registro;
    private final String operacaoAlvo;
    private final InterpretadorThz interpretador;

    private JFrame frame;
    private final Map<String, JComponent> camposEntrada = new LinkedHashMap<>();
    private final Map<String, DefaultTableModel> fatiaModelos = new LinkedHashMap<>();
    private final Map<String, ValorThz.Fatia> fatiaTemplates = new LinkedHashMap<>();
    private final Map<String, ButtonGroup> radioGrupos = new LinkedHashMap<>();
    private final Map<String, JSlider> sliderCampos = new LinkedHashMap<>();
    private final Map<String, JSpinner> spinnerCampos = new LinkedHashMap<>();
    private final Map<String, JProgressBar> progressBarCampos = new LinkedHashMap<>();
    private final Map<String, JList<String>> listCampos = new LinkedHashMap<>();
    private final Map<String, JPasswordField> passwordCampos = new LinkedHashMap<>();
    private final Map<String, JToggleButton> switchCampos = new LinkedHashMap<>();

    private JLabel lblStatus;
    private JPanel painelStatus;
    private JButton btnAcao;

    public RenderizadorFormularioSwing(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador) {
        this.registro = registro;
        this.operacaoAlvo = operacaoAlvo;
        this.interpretador = interpretador;
    }

    public static String renderizar(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador) {
        String titulo = extrairTitulo(registro);
        if (GraphicsEnvironment.isHeadless()) {
            return "Formulário '" + titulo + "' preparado com sucesso (Modo Headless).";
        }

        SwingUtilities.invokeLater(() -> {
            RenderizadorFormularioSwing renderer = new RenderizadorFormularioSwing(registro, operacaoAlvo, interpretador);
            renderer.exibir();
        });

        return "Formulário '" + titulo + "' aberto com sucesso.";
    }

    public static String extrairTitulo(ValorThz.Registro reg) {
        String raw;
        if (reg != null && reg.campos().containsKey("titulo")) {
            ValorThz t = reg.campos().get("titulo");
            if (t instanceof ValorThz.Texto txt && !txt.valor().isBlank()) {
                raw = txt.valor();
            } else {
                raw = reg != null ? "Formulário THZ — " + reg.nomeEstrutura() : "Formulário THZ";
            }
        } else {
            raw = reg != null ? "Formulário THZ — " + reg.nomeEstrutura() : "Formulário THZ";
        }
        // Sanitiza caracteres que quebram title bar no Windows (em dash/cp1252)
        return raw.replace("—", "-").replace("–", "-").replace("â€”", "-").replace("â€", "-");
    }

    public void exibir() {
        configurarLookAndFeel();
        construirInterface();
        frame.setVisible(true);
    }

    public JFrame getFrame() {
        return frame;
    }

    private void configurarLookAndFeel() {
        thz.lang.gui.util.LookAndFeelHelper.configurar();
    }

    private void construirInterface() {
        String titulo = extrairTitulo(registro);
        frame = new JFrame(titulo);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
        contentPane.setBackground(new Color(24, 24, 27)); // Zinc 900
        contentPane.setBorder(new EmptyBorder(20, 24, 20, 24));
        frame.setContentPane(contentPane);

        // ---- Header ----
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTitulo.setForeground(new Color(244, 244, 245)); // Zinc 100

        JLabel lblSubtitulo = new JLabel("Estrutura: " + registro.nomeEstrutura() + "  |  Operação Alvo: " + operacaoAlvo);
        lblSubtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(161, 161, 170)); // Zinc 400

        headerPanel.add(lblTitulo, BorderLayout.NORTH);
        headerPanel.add(lblSubtitulo, BorderLayout.SOUTH);
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // ---- Form Body (Scrollable para ajuste e rolagem suaves) ----
        JPanel formPanel = new PainelScrollavelForm();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(0, 0, 0, 16));

        for (Map.Entry<String, ValorThz> entry : registro.campos().entrySet()) {
            String nomeCampo = entry.getKey();
            if ("titulo".equalsIgnoreCase(nomeCampo)) continue; // Titulo já usado no cabeçalho

            ValorThz val = entry.getValue();
            JPanel campoRow = criarLinhaCampo(nomeCampo, val);
            formPanel.add(campoRow);
            formPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(800, 340));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // ---- Footer (Status + Botões) — não pode ser cortado, reserva altura dinâmica ----
        JPanel footerPanel = new JPanel(new BorderLayout(0, 8));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        painelStatus = new JPanel(new BorderLayout(8, 0));
        painelStatus.setBackground(new Color(39, 39, 42)); // Zinc 800
        painelStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        lblStatus = new JLabel("Preencha os campos e clique em Salvar para submeter.");
        lblStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        lblStatus.setForeground(new Color(212, 212, 216));
        painelStatus.add(lblStatus, BorderLayout.CENTER);

        JPanel acoesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        acoesPanel.setOpaque(false);

        String rotuloBotao = extrairNomeSimplesOperacao(operacaoAlvo);
        btnAcao = new JButton(rotuloBotao);
        btnAcao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnAcao.setBackground(new Color(37, 99, 235)); // Blue 600
        btnAcao.setForeground(Color.WHITE);
        btnAcao.setFocusPainted(false);
        btnAcao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(29, 78, 216), 1, true),
                new EmptyBorder(8, 20, 8, 20)
        ));
        btnAcao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAcao.addActionListener(e -> executarSubmissao());

        JButton btnExportar = new JButton("📄 Exportar...");
        btnExportar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnExportar.setBackground(new Color(63, 63, 70));
        btnExportar.setForeground(new Color(228, 228, 231));
        btnExportar.setFocusPainted(false);
        btnExportar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(82, 82, 91), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btnExportar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportar.addActionListener(e -> ExportadorFormularioGui.abrirMenuExportacao(
                btnExportar, frame, titulo, registro.nomeEstrutura(),
                this::coletarRegistroAtual,
                this::mostrarStatusSucesso,
                this::mostrarStatusErro
        ));

        JButton btnLimpar = new JButton("Restaurar");
        btnLimpar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnLimpar.setBackground(new Color(63, 63, 70));
        btnLimpar.setForeground(new Color(228, 228, 231));
        btnLimpar.setFocusPainted(false);
        btnLimpar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(82, 82, 91), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btnLimpar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLimpar.addActionListener(e -> restaurarValoresPadrao());

        acoesPanel.add(btnExportar);
        acoesPanel.add(btnLimpar);
        acoesPanel.add(btnAcao);

        footerPanel.add(painelStatus, BorderLayout.NORTH);
        footerPanel.add(acoesPanel, BorderLayout.SOUTH);
        contentPane.add(footerPanel, BorderLayout.SOUTH);

        ajustarTamanhoECentralizar();
    }

    private JPanel criarLinhaCampo(String nomeCampo, ValorThz valorInicial) {
        JPanel painel = new JPanel(new BorderLayout(0, 4));
        painel.setOpaque(false);

        String rotuloFormatado = FabricaCamposFormulario.formatarRotulo(nomeCampo);
        String tipoDesc = "[" + valorInicial.classe() + "]";

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        labelPanel.setOpaque(false);

        JLabel lblNome = new JLabel(rotuloFormatado);
        lblNome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblNome.setForeground(new Color(228, 228, 231));

        JLabel lblTipo = new JLabel(tipoDesc);
        lblTipo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        lblTipo.setForeground(new Color(113, 113, 122));

        labelPanel.add(lblNome);
        labelPanel.add(lblTipo);
        painel.add(labelPanel, BorderLayout.NORTH);

        if (valorInicial instanceof ValorThz.Texto && FabricaCamposFormulario.ehCampoSenha(nomeCampo)) {
            painel.add(FabricaCamposFormulario.criarPainelSenha(nomeCampo, valorInicial, passwordCampos, camposEntrada), BorderLayout.CENTER);
        } else if (valorInicial instanceof ValorThz.Texto && FabricaCamposFormulario.ehCampoCor(nomeCampo)) {
            painel.add(FabricaCamposFormulario.criarPainelCor(nomeCampo, valorInicial, camposEntrada, frame), BorderLayout.CENTER);
        } else if (valorInicial instanceof ValorThz.Texto && FabricaCamposFormulario.ehCampoArquivo(nomeCampo)) {
            painel.add(FabricaCamposFormulario.criarPainelArquivo(nomeCampo, valorInicial, camposEntrada, frame), BorderLayout.CENTER);
        } else if (FabricaCamposFormulario.ehCampoProgresso(nomeCampo)) {
            painel.add(FabricaCamposFormulario.criarPainelProgresso(nomeCampo, valorInicial, progressBarCampos, camposEntrada), BorderLayout.CENTER);
        } else if (FabricaCamposFormulario.ehCampoSlider(nomeCampo)) {
            painel.add(FabricaCamposFormulario.criarPainelSlider(nomeCampo, valorInicial, sliderCampos, camposEntrada), BorderLayout.CENTER);
        } else if (FabricaCamposFormulario.ehCampoSpinner(nomeCampo) && (valorInicial instanceof ValorThz.Inteiro || valorInicial instanceof ValorThz.Decimal)) {
            painel.add(FabricaCamposFormulario.criarPainelSpinner(nomeCampo, valorInicial, spinnerCampos, camposEntrada), BorderLayout.CENTER);
        } else if (valorInicial instanceof ValorThz.Logico l) {
            if (FabricaCamposFormulario.ehCampoSwitch(nomeCampo)) {
                painel.add(FabricaCamposFormulario.criarPainelSwitch(nomeCampo, l, switchCampos, camposEntrada), BorderLayout.CENTER);
            } else {
                JCheckBox cb = new JCheckBox(" " + rotuloFormatado + " (Ativo / Sim)", l.valor());
                cb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                cb.setForeground(new Color(228, 228, 231));
                cb.setOpaque(false);
                camposEntrada.put(nomeCampo, cb);
                painel.add(cb, BorderLayout.CENTER);
            }
        } else if (valorInicial instanceof ValorThz.Enumerado en) {
            if (FabricaCamposFormulario.ehCampoRadio(nomeCampo, en)) {
                painel.add(FabricaCamposFormulario.criarPainelRadio(nomeCampo, en, radioGrupos, camposEntrada), BorderLayout.CENTER);
            } else {
                List<String> opcoes = FabricaCamposFormulario.obterOpcoesEnum(en);
                JComboBox<String> cb = new JComboBox<>(opcoes.toArray(new String[0]));
                cb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                cb.setBackground(new Color(39, 39, 42));
                cb.setForeground(new Color(250, 250, 250));
                cb.setSelectedItem(en.valor());
                camposEntrada.put(nomeCampo, cb);
                painel.add(cb, BorderLayout.CENTER);
            }
        } else if (valorInicial instanceof ValorThz.Fatia fatia) {
            if (ehFatiaEstrutura(fatia)) {
                fatiaTemplates.put(nomeCampo, fatia);
                JPanel tabelaPainel = PainelTabelaFatia.criarPainelTabela(nomeCampo, fatia, fatiaModelos, camposEntrada);
                painel.add(tabelaPainel, BorderLayout.CENTER);
            } else {
                painel.add(FabricaCamposFormulario.criarPainelListaMultipla(nomeCampo, fatia, listCampos, camposEntrada), BorderLayout.CENTER);
            }
        } else if (valorInicial instanceof ValorThz.Texto t && FabricaCamposFormulario.ehCampoTextoLongo(nomeCampo)) {
            JTextArea ta = new JTextArea(t.valor(), 3, 20);
            ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            ta.setBackground(new Color(39, 39, 42));
            ta.setForeground(new Color(250, 250, 250));
            ta.setCaretColor(Color.WHITE);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            JScrollPane spTa = new JScrollPane(ta);
            spTa.setPreferredSize(new Dimension(520, 80));
            spTa.setMaximumSize(new Dimension(Short.MAX_VALUE, 120));
            spTa.setBorder(BorderFactory.createEmptyBorder());
            camposEntrada.put(nomeCampo, ta);
            painel.add(spTa, BorderLayout.CENTER);
        } else {
            String textoInicial = FabricaCamposFormulario.formatarTextoInicial(valorInicial);
            JTextField tf = new JTextField(textoInicial);
            tf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            tf.setBackground(new Color(39, 39, 42));
            tf.setForeground(new Color(250, 250, 250));
            tf.setCaretColor(Color.WHITE);
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                    new EmptyBorder(7, 10, 7, 10)
            ));
            camposEntrada.put(nomeCampo, tf);
            painel.add(tf, BorderLayout.CENTER);
        }

        return painel;
    }

    private boolean ehFatiaEstrutura(ValorThz.Fatia f) {
        return f != null && !f.elementos().isEmpty() && f.elementos().get(0) instanceof ValorThz.Registro;
    }

    public ValorThz.Registro coletarRegistroAtual() {
        Map<String, ValorThz> mapaCampos = new LinkedHashMap<>();
        for (Map.Entry<String, ValorThz> entry : registro.campos().entrySet()) {
            String campo = entry.getKey();
            ValorThz original = entry.getValue();
            if (fatiaModelos.containsKey(campo)) {
                mapaCampos.put(campo, PainelTabelaFatia.extrairFatiaDaTabela(campo, fatiaModelos.get(campo), fatiaTemplates.get(campo)));
                continue;
            }
            JComponent comp = camposEntrada.get(campo);
            if (comp != null) {
                String tipo = original != null ? original.classe() : "TEXTO";
                mapaCampos.put(campo, FabricaCamposFormulario.converterEntradaParaTipo(
                        comp, tipo, campo, passwordCampos, radioGrupos, sliderCampos, spinnerCampos, switchCampos, listCampos
                ));
            } else {
                mapaCampos.put(campo, original);
            }
        }
        return new ValorThz.Registro(registro.nomeEstrutura(), mapaCampos);
    }

    public void executarSubmissao() {
        try {
            mostrarStatusProcessando();
            ValorThz.Registro regAtualizado = coletarRegistroAtual();
            Map<String, ValorThz> argsOperacao = montarArgumentosOperacao(regAtualizado);

            String nomeSimplesOp = extrairNomeSimplesOperacao(operacaoAlvo);
            ValorThz resultado = interpretador.executarOperacao(nomeSimplesOp, argsOperacao);

            String resFormatado = resultado != null ? resultado.formatar() : "(Concluído sem retorno)";
            mostrarStatusSucesso("✓ Sucesso: " + resFormatado);

        } catch (ErroContrato ec) {
            mostrarStatusErro("⚠️ Violação de Contrato EXIGE: " + ec.getMessage());
        } catch (ErroExecucao ee) {
            mostrarStatusErro("❌ Erro de Execução: " + ee.getMessage());
        } catch (Exception ex) {
            mostrarStatusErro("❌ Erro: " + ex.getMessage());
        }
    }

    private Map<String, ValorThz> montarArgumentosOperacao(ValorThz.Registro reg) {
        Map<String, ValorThz> args = new LinkedHashMap<>();
        OperacaoAst op = localizarOperacaoAst(operacaoAlvo);

        if (op != null) {
            for (ParametroOperacaoAst param : op.parametros()) {
                String nomeParam = param.nome();
                if (reg.campos().containsKey(nomeParam)) {
                    args.put(nomeParam, reg.campos().get(nomeParam));
                } else if (op.parametros().size() == 1 && (param.tipo().equalsIgnoreCase(reg.nomeEstrutura()) || param.tipo().equalsIgnoreCase("REGISTRO"))) {
                    args.put(nomeParam, reg);
                } else {
                    args.put(nomeParam, reg.campos().getOrDefault(nomeParam, ValorThz.NULO));
                }
            }
        } else {
            args.putAll(reg.campos());
        }
        return args;
    }

    private OperacaoAst localizarOperacaoAst(String alvo) {
        String nomeSimples = extrairNomeSimplesOperacao(alvo);
        for (var exec : interpretador.listarOperacoesExecutaveis()) {
            if (exec.operacao().nome().equalsIgnoreCase(nomeSimples)) {
                return exec.operacao();
            }
        }
        return null;
    }

    private String extrairNomeSimplesOperacao(String alvo) {
        if (alvo == null) return "Salvar";
        int ponto = alvo.lastIndexOf('.');
        return ponto >= 0 ? alvo.substring(ponto + 1) : alvo;
    }

    private void restaurarValoresPadrao() {
        for (Map.Entry<String, ValorThz> entry : registro.campos().entrySet()) {
            String campo = entry.getKey();
            ValorThz val = entry.getValue();
            JComponent comp = camposEntrada.get(campo);
            if (comp instanceof JTextField tf) {
                tf.setText(FabricaCamposFormulario.formatarTextoInicial(val));
            } else if (comp instanceof JCheckBox cb && val instanceof ValorThz.Logico l) {
                cb.setSelected(l.valor());
            } else if (comp instanceof JComboBox<?> cb && val instanceof ValorThz.Enumerado en) {
                cb.setSelectedItem(en.valor());
            } else if (passwordCampos.containsKey(campo) && val instanceof ValorThz.Texto t) {
                passwordCampos.get(campo).setText(t.valor());
            } else if (switchCampos.containsKey(campo) && val instanceof ValorThz.Logico l) {
                switchCampos.get(campo).setSelected(l.valor());
                switchCampos.get(campo).setText(l.valor() ? "LIGADO" : "DESLIGADO");
                FabricaCamposFormulario.atualizarEstiloSwitch(switchCampos.get(campo));
            } else if (sliderCampos.containsKey(campo) && val instanceof ValorThz.Inteiro in) {
                sliderCampos.get(campo).setValue(in.valor().intValue());
            }
        }
        mostrarStatusInfo("Valores restaurados para o padrão original.");
    }

    private void ajustarTamanhoECentralizar() {
        if (frame == null) return;
        // Garante que o footer nunca seja cortado: pack mede preferido real
        frame.pack();
        // Heurística: nome longo da operação exige largura extra para não truncar botão
        String rotulo = extrairNomeSimplesOperacao(operacaoAlvo);
        int extraLargura = Math.max(0, (rotulo.length() - 8) * 8);

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle telaBounds = (gc != null) ? gc.getBounds() : new Rectangle(0, 0, 1280, 800);
        Insets insets = (gc != null) ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : new Insets(0, 0, 0, 0);

        int maxLarguraUtil = Math.max(520, telaBounds.width - insets.left - insets.right);
        int maxAlturaUtil = Math.max(400, telaBounds.height - insets.top - insets.bottom);

        boolean temControlesAmplos = !fatiaModelos.isEmpty() || !listCampos.isEmpty() || !radioGrupos.isEmpty() || registro.campos().size() >= 4;
        int larguraBase = (temControlesAmplos ? 960 : 820) + extraLargura;
        int larguraPack = frame.getWidth();
        int larguraDesejada = Math.max(larguraPack, larguraBase);
        larguraDesejada = Math.min(larguraDesejada, (int) (maxLarguraUtil * 0.88));
        // Altura: respeita pack, mas nunca menor que footer (status 42 + botoes 48 + header 80)
        int alturaMinima = 420 + Math.min(registro.campos().size(), 6) * 40;
        int alturaPack = frame.getHeight();
        int alturaDesejada = Math.max(Math.max(alturaPack, alturaMinima), frame.getPreferredSize().height);
        alturaDesejada = Math.min(alturaDesejada, (int) (maxAlturaUtil * 0.88));

        frame.setSize(larguraDesejada, alturaDesejada);
        frame.setMinimumSize(new Dimension(Math.min(860, maxLarguraUtil), Math.min(520, maxAlturaUtil)));

        int posX = telaBounds.x + insets.left + Math.max(0, (maxLarguraUtil - larguraDesejada) / 2);
        int posY = telaBounds.y + insets.top + Math.max(0, (maxAlturaUtil - alturaDesejada) / 2);
        frame.setLocation(posX, posY);
        // RHSA: força relayout após setSize para FlowLayout do footer recalcular wrap
        frame.revalidate();
    }

    private void mostrarStatusProcessando() {
        if (painelStatus == null || lblStatus == null) return;
        painelStatus.setBackground(new Color(30, 41, 59)); // Slate 800
        lblStatus.setForeground(new Color(56, 189, 248)); // Sky 400
        lblStatus.setText("⏳ Processando e validando contratos...");
    }

    private void mostrarStatusSucesso(String msg) {
        if (painelStatus == null || lblStatus == null) return;
        painelStatus.setBackground(new Color(20, 83, 45)); // Green 900
        lblStatus.setForeground(new Color(187, 247, 208)); // Green 200
        lblStatus.setText(msg);
    }

    private void mostrarStatusErro(String msg) {
        if (painelStatus == null || lblStatus == null) return;
        painelStatus.setBackground(new Color(127, 29, 29)); // Red 900
        lblStatus.setForeground(new Color(254, 202, 202)); // Red 200
        lblStatus.setText(msg);
    }

    private void mostrarStatusInfo(String msg) {
        if (painelStatus == null || lblStatus == null) return;
        painelStatus.setBackground(new Color(39, 39, 42)); // Zinc 800
        lblStatus.setForeground(new Color(212, 212, 216)); // Zinc 300
        lblStatus.setText(msg);
    }

    
    private static class PainelScrollavelForm extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
