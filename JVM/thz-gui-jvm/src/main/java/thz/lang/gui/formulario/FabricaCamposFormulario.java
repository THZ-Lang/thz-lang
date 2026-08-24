package thz.lang.gui.formulario;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;
import thz.lang.runtime.Monetario;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fábrica especializada para criação e conversão de componentes de formulário Swing do THZ-LANG.
 * Suporta detecção semântica de tipos: Senha, Cor, Arquivo, Slider, Spinner, Switch, Radio, Lista e Textos.
 */
public class FabricaCamposFormulario {

    public static String formatarRotulo(String camelCaseOuSnake) {
        if (camelCaseOuSnake == null || camelCaseOuSnake.isBlank()) return "";
        String s = camelCaseOuSnake.replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return sb.toString().trim();
    }

    public static boolean ehCampoSenha(String n) {
        String s = n.toLowerCase();
        return s.contains("senha") || s.contains("password") || s.contains("pin") || s.contains("token") || s.contains("secret") || s.contains("cvv");
    }

    public static boolean ehCampoCor(String n) {
        String s = n.toLowerCase();
        return s.contains("cor") || s.contains("color") || s.contains("hex") || s.contains("paleta") || s.contains("tema");
    }

    public static boolean ehCampoArquivo(String n) {
        String s = n.toLowerCase();
        return s.contains("arquivo") || s.contains("caminho") || s.contains("path") || s.contains("file") || s.contains("pasta") || s.contains("diretorio") || s.contains("anexo") || s.contains("foto");
    }

    public static boolean ehCampoProgresso(String n) {
        String s = n.toLowerCase();
        return s.contains("progresso") || s.contains("progress") || s.contains("conclusao") || s.contains("porcentagem") || s.contains("percentual");
    }

    public static boolean ehCampoSlider(String n) {
        String s = n.toLowerCase();
        return s.contains("opacidade") || s.contains("volume") || s.contains("brilho") || s.contains("escala") || s.contains("zoom") || s.contains("intensidade") || s.contains("nivel");
    }

    public static boolean ehCampoSpinner(String n) {
        String s = n.toLowerCase();
        return s.contains("quantidade") || s.contains("qtd") || s.contains("idade") || s.contains("ano") || s.contains("mes") || s.contains("dia") || s.contains("parcelas") || s.contains("itens");
    }

    public static boolean ehCampoSwitch(String n) {
        String s = n.toLowerCase();
        return s.contains("modo") || s.contains("habilitar") || s.contains("permitir") || s.contains("notificar") || s.contains("dark") || s.contains("escuro") || s.contains("som") || s.contains("auto");
    }

    public static boolean ehCampoRadio(String n, ValorThz.Enumerado en) {
        return n.toLowerCase().contains("opcao") || n.toLowerCase().contains("tipo") || n.toLowerCase().contains("prioridade") || n.toLowerCase().contains("genero");
    }

    public static boolean ehCampoTextoLongo(String n) {
        String s = n.toLowerCase();
        return s.contains("obs") || s.contains("descricao") || s.contains("detalhe") || s.contains("motivo") || s.contains("comentario") || s.contains("mensagem") || s.contains("endereco");
    }

    public static JPanel criarPainelSenha(String nomeCampo, ValorThz valorInicial, Map<String, JPasswordField> passwordCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);

        String texto = (valorInicial instanceof ValorThz.Texto t) ? t.valor() : "";
        JPasswordField pf = new JPasswordField(texto);
        pf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        pf.setBackground(new Color(39, 39, 42));
        pf.setForeground(new Color(250, 250, 250));
        pf.setCaretColor(Color.WHITE);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));

        char echoPadrao = pf.getEchoChar();
        JToggleButton btnVer = new JToggleButton("👁");
        btnVer.setToolTipText("Mostrar/Ocultar Senha");
        btnVer.setBackground(new Color(63, 63, 70));
        btnVer.setForeground(Color.WHITE);
        btnVer.setFocusPainted(false);
        btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVer.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnVer.addActionListener(e -> {
            if (btnVer.isSelected()) {
                pf.setEchoChar((char) 0);
            } else {
                pf.setEchoChar(echoPadrao);
            }
        });

        passwordCampos.put(nomeCampo, pf);
        camposEntrada.put(nomeCampo, pf);

        p.add(pf, BorderLayout.CENTER);
        p.add(btnVer, BorderLayout.EAST);
        return p;
    }

    public static JPanel criarPainelCor(String nomeCampo, ValorThz valorInicial, Map<String, JComponent> camposEntrada, JFrame parentFrame) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);

        String corHex = (valorInicial instanceof ValorThz.Texto t && !t.valor().isBlank()) ? t.valor() : "#3b82f6";
        JTextField tf = new JTextField(corHex);
        tf.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        tf.setBackground(new Color(39, 39, 42));
        tf.setForeground(new Color(250, 250, 250));
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));

        JButton btnCor = new JButton("   ");
        btnCor.setToolTipText("Escolher Cor");
        btnCor.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCor.setFocusPainted(false);
        Color corInicialParsed = parsearCorHex(corHex, new Color(59, 130, 246));
        btnCor.setBackground(corInicialParsed);
        btnCor.setBorder(BorderFactory.createLineBorder(new Color(113, 113, 122), 1));
        btnCor.setPreferredSize(new Dimension(38, 30));

        btnCor.addActionListener(e -> {
            Color nova = JColorChooser.showDialog(parentFrame, "Selecionar Cor — " + formatarRotulo(nomeCampo), btnCor.getBackground());
            if (nova != null) {
                String hex = String.format("#%02x%02x%02x", nova.getRed(), nova.getGreen(), nova.getBlue());
                tf.setText(hex);
                btnCor.setBackground(nova);
            }
        });

        tf.addActionListener(e -> {
            Color c = parsearCorHex(tf.getText(), btnCor.getBackground());
            btnCor.setBackground(c);
        });

        camposEntrada.put(nomeCampo, tf);
        p.add(tf, BorderLayout.CENTER);
        p.add(btnCor, BorderLayout.EAST);
        return p;
    }

    public static Color parsearCorHex(String hex, Color padrao) {
        if (hex == null || hex.isBlank()) return padrao;
        try {
            String clean = hex.trim();
            if (!clean.startsWith("#")) clean = "#" + clean;
            return Color.decode(clean);
        } catch (Exception e) {
            return padrao;
        }
    }

    public static JPanel criarPainelArquivo(String nomeCampo, ValorThz valorInicial, Map<String, JComponent> camposEntrada, JFrame parentFrame) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);

        String caminhoInicial = (valorInicial instanceof ValorThz.Texto t) ? t.valor() : "";
        JTextField tf = new JTextField(caminhoInicial);
        tf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tf.setBackground(new Color(39, 39, 42));
        tf.setForeground(new Color(250, 250, 250));
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));

        JButton btnProcurar = new JButton("📁 Navegar...");
        btnProcurar.setFocusPainted(false);
        btnProcurar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnProcurar.setBackground(new Color(63, 63, 70));
        btnProcurar.setForeground(new Color(244, 244, 245));
        btnProcurar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnProcurar.setBorder(new EmptyBorder(6, 12, 6, 12));

        btnProcurar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (nomeCampo.toLowerCase().contains("pasta") || nomeCampo.toLowerCase().contains("diretorio")) {
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
            int res = fc.showOpenDialog(parentFrame);
            if (res == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
                tf.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        camposEntrada.put(nomeCampo, tf);
        p.add(tf, BorderLayout.CENTER);
        p.add(btnProcurar, BorderLayout.EAST);
        return p;
    }

    public static JPanel criarPainelProgresso(String nomeCampo, ValorThz valorInicial, Map<String, JProgressBar> progressBarCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        int valor = 50;
        if (valorInicial instanceof ValorThz.Inteiro in) valor = in.valor().intValue();
        else if (valorInicial instanceof ValorThz.Decimal dec) {
            try { valor = (int) Double.parseDouble(dec.valor().formatar()); } catch (Exception ignored) {}
        }

        valor = Math.max(0, Math.min(100, valor));
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(valor);
        pb.setStringPainted(true);
        pb.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        pb.setForeground(new Color(37, 99, 235));
        pb.setBackground(new Color(39, 39, 42));
        pb.setPreferredSize(new Dimension(260, 26));

        JSlider slider = new JSlider(0, 100, valor);
        slider.setOpaque(false);
        slider.addChangeListener(e -> pb.setValue(slider.getValue()));

        progressBarCampos.put(nomeCampo, pb);
        camposEntrada.put(nomeCampo, slider);

        p.add(slider, BorderLayout.CENTER);
        p.add(pb, BorderLayout.EAST);
        return p;
    }

    public static JPanel criarPainelSlider(String nomeCampo, ValorThz valorInicial, Map<String, JSlider> sliderCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        int valor = 50;
        if (valorInicial instanceof ValorThz.Inteiro in) valor = in.valor().intValue();
        else if (valorInicial instanceof ValorThz.Decimal dec) {
            try { valor = (int) Double.parseDouble(dec.valor().formatar()); } catch (Exception ignored) {}
        }

        JSlider slider = new JSlider(0, 100, valor);
        slider.setOpaque(false);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);

        JLabel lblVal = new JLabel(valor + "%");
        lblVal.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        lblVal.setForeground(new Color(244, 244, 245));
        lblVal.setPreferredSize(new Dimension(50, 20));

        slider.addChangeListener(e -> lblVal.setText(slider.getValue() + "%"));

        sliderCampos.put(nomeCampo, slider);
        camposEntrada.put(nomeCampo, slider);

        p.add(slider, BorderLayout.CENTER);
        p.add(lblVal, BorderLayout.EAST);
        return p;
    }

    public static JPanel criarPainelSpinner(String nomeCampo, ValorThz valorInicial, Map<String, JSpinner> spinnerCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);

        SpinnerNumberModel model;
        if (valorInicial instanceof ValorThz.Decimal dec) {
            double val = Double.parseDouble(dec.valor().formatar());
            model = new SpinnerNumberModel(val, 0.0, 1000000.0, 1.0);
        } else {
            int val = (valorInicial instanceof ValorThz.Inteiro in) ? in.valor().intValue() : 1;
            model = new SpinnerNumberModel(val, 0, 1000000, 1);
        }

        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        spinner.setPreferredSize(new Dimension(140, 32));

        spinnerCampos.put(nomeCampo, spinner);
        camposEntrada.put(nomeCampo, spinner);

        p.add(spinner);
        return p;
    }

    public static JPanel criarPainelSwitch(String nomeCampo, ValorThz.Logico l, Map<String, JToggleButton> switchCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);

        JToggleButton toggle = new JToggleButton(l.valor() ? "LIGADO" : "DESLIGADO", l.valor());
        toggle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        toggle.setFocusPainted(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        atualizarEstiloSwitch(toggle);

        toggle.addActionListener(e -> {
            toggle.setText(toggle.isSelected() ? "LIGADO" : "DESLIGADO");
            atualizarEstiloSwitch(toggle);
        });

        switchCampos.put(nomeCampo, toggle);
        camposEntrada.put(nomeCampo, toggle);

        p.add(toggle);
        return p;
    }

    public static void atualizarEstiloSwitch(JToggleButton toggle) {
        if (toggle.isSelected()) {
            toggle.setBackground(new Color(22, 163, 74)); // Green 600
            toggle.setForeground(Color.WHITE);
            toggle.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(34, 197, 94), 1, true),
                    new EmptyBorder(6, 16, 6, 16)
            ));
        } else {
            toggle.setBackground(new Color(63, 63, 70)); // Zinc 700
            toggle.setForeground(new Color(212, 212, 216));
            toggle.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(82, 82, 91), 1, true),
                    new EmptyBorder(6, 16, 6, 16)
            ));
        }
    }

    public static JPanel criarPainelRadio(String nomeCampo, ValorThz.Enumerado en, Map<String, ButtonGroup> radioGrupos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        List<String> opcoes = obterOpcoesEnum(en);

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);

        for (String op : opcoes) {
            JRadioButton rb = new JRadioButton(op, op.equalsIgnoreCase(en.valor()));
            rb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            rb.setForeground(new Color(228, 228, 231));
            rb.setOpaque(false);
            rb.setActionCommand(op);
            group.add(rb);
            p.add(rb);
        }

        radioGrupos.put(nomeCampo, group);
        camposEntrada.put(nomeCampo, wrapper);
        return p;
    }

    public static JPanel criarPainelListaMultipla(String nomeCampo, ValorThz.Fatia fatia, Map<String, JList<String>> listCampos, Map<String, JComponent> camposEntrada) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<Integer> selectedIndices = new ArrayList<>();
        int idx = 0;
        for (ValorThz el : fatia.elementos()) {
            listModel.addElement(el.formatar());
            selectedIndices.add(idx++);
        }

        JList<String> jlist = new JList<>(listModel);
        jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jlist.setBackground(new Color(39, 39, 42));
        jlist.setForeground(new Color(244, 244, 245));
        jlist.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        int[] selArr = selectedIndices.stream().mapToInt(i -> i).toArray();
        jlist.setSelectedIndices(selArr);

        JScrollPane sp = new JScrollPane(jlist);
        sp.setPreferredSize(new Dimension(650, 90));
        sp.setBorder(BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true));

        listCampos.put(nomeCampo, jlist);
        camposEntrada.put(nomeCampo, jlist);

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    public static List<String> obterOpcoesEnum(ValorThz.Enumerado en) {
        List<String> lista = new ArrayList<>();
        if (en != null && en.valor() != null) {
            lista.add(en.valor());
        }
        for (String op : List.of("PENDENTE", "APROVADO", "CANCELADO", "REJEITADO", "PROCESSANDO", "CONCLUIDO", "ATIVO", "INATIVO", "ALTA", "MEDIA", "BAIXA")) {
            if (!lista.contains(op)) lista.add(op);
        }
        return lista;
    }

    public static String formatarTextoInicial(ValorThz v) {
        if (v == null) return "";
        if (v instanceof ValorThz.Texto t) return t.valor();
        if (v instanceof ValorThz.Decimal d) return d.valor().formatar();
        if (v instanceof ValorThz.Inteiro in) return in.valor().toString();
        if (v instanceof ValorThz.Monetario m) return m.valor().quantia.formatar();
        if (v instanceof ValorThz.Data d) return d.valor().formatar();
        if (v instanceof ValorThz.DataHora dh) return dh.valor().formatar();
        return v.formatar();
    }

    public static ValorThz converterEntradaParaTipo(JComponent comp, String tipo, String nomeCampo,
                                                    Map<String, JPasswordField> passwordCampos,
                                                    Map<String, ButtonGroup> radioGrupos,
                                                    Map<String, JSlider> sliderCampos,
                                                    Map<String, JSpinner> spinnerCampos,
                                                    Map<String, JToggleButton> switchCampos,
                                                    Map<String, JList<String>> listCampos) {
        if (passwordCampos.containsKey(nomeCampo)) {
            return ValorThz.TEXTO(new String(passwordCampos.get(nomeCampo).getPassword()));
        }
        if (radioGrupos.containsKey(nomeCampo)) {
            ButtonGroup bg = radioGrupos.get(nomeCampo);
            ButtonModel bm = bg.getSelection();
            String sel = bm != null ? bm.getActionCommand() : "PENDENTE";
            return new ValorThz.Enumerado(tipo, sel);
        }
        if (switchCampos.containsKey(nomeCampo)) {
            return ValorThz.LOGICO(switchCampos.get(nomeCampo).isSelected());
        }
        if (sliderCampos.containsKey(nomeCampo)) {
            int val = sliderCampos.get(nomeCampo).getValue();
            if (tipo.equalsIgnoreCase("DECIMAL")) return ValorThz.DECIMAL(DecimalFixo.deTexto(String.valueOf(val), 2));
            return ValorThz.INTEIRO(val);
        }
        if (spinnerCampos.containsKey(nomeCampo)) {
            Object val = spinnerCampos.get(nomeCampo).getValue();
            if (tipo.equalsIgnoreCase("DECIMAL") || val instanceof Double) {
                return ValorThz.DECIMAL(DecimalFixo.deTexto(String.valueOf(val), 2));
            }
            return ValorThz.INTEIRO(((Number) val).longValue());
        }
        if (listCampos.containsKey(nomeCampo)) {
            List<String> selecionados = listCampos.get(nomeCampo).getSelectedValuesList();
            List<ValorThz> lista = new ArrayList<>();
            for (String s : selecionados) lista.add(ValorThz.TEXTO(s));
            return new ValorThz.Fatia("TEXTO", lista);
        }
        if (comp instanceof JCheckBox cb) {
            return ValorThz.LOGICO(cb.isSelected());
        }
        if (comp instanceof JComboBox<?> cb) {
            Object sel = cb.getSelectedItem();
            return new ValorThz.Enumerado(tipo, sel != null ? sel.toString() : "");
        }
        if (comp instanceof JTextArea ta) {
            return ValorThz.TEXTO(ta.getText());
        }
        if (comp instanceof JTextField tf) {
            String texto = tf.getText().trim();
            try {
                if (tipo.equalsIgnoreCase("INTEIRO") || tipo.equalsIgnoreCase("INTEIRO32") || tipo.equalsIgnoreCase("INTEIRO64")) {
                    return ValorThz.INTEIRO(new BigInteger(texto));
                } else if (tipo.equalsIgnoreCase("DECIMAL")) {
                    return ValorThz.DECIMAL(DecimalFixo.deTexto(texto.isEmpty() ? "0.00" : texto, 2));
                } else if (tipo.equalsIgnoreCase("MONETARIO")) {
                    return new ValorThz.Monetario(Monetario.deTexto(texto.isEmpty() ? "0.00" : texto, "BRL"));
                } else if (tipo.equalsIgnoreCase("DATA")) {
                    return new ValorThz.Data(DataThz.deTexto(texto));
                } else if (tipo.equalsIgnoreCase("DATA_HORA")) {
                    return new ValorThz.DataHora(DataHoraThz.deTexto(texto));
                } else if (tipo.equalsIgnoreCase("LOGICO")) {
                    return ValorThz.LOGICO(Boolean.parseBoolean(texto) || texto.equalsIgnoreCase("verdadeiro") || texto.equalsIgnoreCase("sim"));
                }
            } catch (Exception e) {
                // Em caso de formato inválido, preserva como texto para validação de contratos
                return ValorThz.TEXTO(texto);
            }
            return ValorThz.TEXTO(texto);
        }
        return ValorThz.NULO;
    }
}
