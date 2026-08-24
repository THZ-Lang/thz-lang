package thz.lang.gui.ui;

import thz.lang.ui.ThzUiComponente;
import thz.lang.ui.ThzUiTema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Renderizador declarativo de árvores ThzUiComponente para componentes nativos Swing / FlatLaf.
 */
public final class ThzUiSwingRenderer {

    private ThzUiSwingRenderer() {}

    public static JComponent renderizar(ThzUiComponente componente, ThzUiTema tema, BiConsumer<String, String> despachadorAcoes) {
        if (componente == null) return new JPanel();

        ThzUiTema t = tema != null ? tema : ThzUiTema.escuroGlass();
        Color bgCard = Color.decode("#1e293b");
        Color textPrimary = Color.decode("#f8fafc");
        Color textMuted = Color.decode("#94a3b8");
        Color primaryBtn = Color.decode("#3b82f6");

        switch (componente.tipo()) {
            case CONTAINER, COLUNA -> {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setOpaque(false);
                panel.setBorder(new EmptyBorder(8, 8, 8, 8));
                for (ThzUiComponente filho : componente.filhos()) {
                    JComponent c = renderizar(filho, t, despachadorAcoes);
                    c.setAlignmentX(Component.LEFT_ALIGNMENT);
                    panel.add(c);
                    panel.add(Box.createVerticalStrut(10));
                }
                return panel;
            }
            case LINHA -> {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
                panel.setOpaque(false);
                for (ThzUiComponente filho : componente.filhos()) {
                    panel.add(renderizar(filho, t, despachadorAcoes));
                }
                return panel;
            }
            case GRADE -> {
                int colunas = componente.getPropriedade("colunas", 2);
                JPanel panel = new JPanel(new GridLayout(0, Math.max(1, colunas), 12, 12));
                panel.setOpaque(false);
                for (ThzUiComponente filho : componente.filhos()) {
                    panel.add(renderizar(filho, t, despachadorAcoes));
                }
                return panel;
            }
            case CARD, PAINEL -> {
                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBackground(bgCard);
                card.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(Color.decode("#334155"), 1, true),
                        new EmptyBorder(14, 16, 14, 16)
                ));
                String titulo = componente.getPropriedade("titulo", "");
                if (!titulo.isBlank()) {
                    JLabel lblTitulo = new JLabel(titulo);
                    lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    lblTitulo.setForeground(textPrimary);
                    lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
                    card.add(lblTitulo);
                    card.add(Box.createVerticalStrut(10));
                }
                for (ThzUiComponente filho : componente.filhos()) {
                    JComponent c = renderizar(filho, t, despachadorAcoes);
                    c.setAlignmentX(Component.LEFT_ALIGNMENT);
                    card.add(c);
                    card.add(Box.createVerticalStrut(8));
                }
                return card;
            }
            case BOTAO -> {
                String rotulo = componente.getPropriedade("rotulo", "Botão");
                JButton btn = new JButton(rotulo);
                btn.setBackground(primaryBtn);
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
                btn.setFocusPainted(false);
                String acao = componente.eventos().getOrDefault("aoClicar", "");
                if (!acao.isBlank() && despachadorAcoes != null) {
                    btn.addActionListener(e -> despachadorAcoes.accept(acao, componente.id()));
                }
                return btn;
            }
            case CAMPO_TEXTO, CAMPO_MOEDA, CAMPO_DATA, CAMPO_NUMERO -> {
                String rotulo = componente.getPropriedade("rotulo", "");
                JPanel group = new JPanel();
                group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
                group.setOpaque(false);
                if (!rotulo.isBlank()) {
                    JLabel lbl = new JLabel(rotulo);
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    lbl.setForeground(textMuted);
                    group.add(lbl);
                    group.add(Box.createVerticalStrut(4));
                }
                JTextField txt = new JTextField(componente.getPropriedade("valor", "").toString(), 20);
                txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                group.add(txt);
                return group;
            }
            case SELECAO -> {
                String rotulo = componente.getPropriedade("rotulo", "");
                JPanel group = new JPanel();
                group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
                group.setOpaque(false);
                if (!rotulo.isBlank()) {
                    JLabel lbl = new JLabel(rotulo);
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    lbl.setForeground(textMuted);
                    group.add(lbl);
                    group.add(Box.createVerticalStrut(4));
                }
                List<?> opcoes = componente.getPropriedade("opcoes", List.of());
                JComboBox<Object> combo = new JComboBox<>(opcoes.toArray());
                combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                group.add(combo);
                return group;
            }
            case INTERRUPTOR, CHECKBOX -> {
                String rotulo = componente.getPropriedade("rotulo", "");
                JCheckBox chk = new JCheckBox(rotulo);
                chk.setOpaque(false);
                chk.setForeground(textPrimary);
                return chk;
            }
            case METRICA_CARD -> {
                JPanel mCard = new JPanel();
                mCard.setLayout(new BoxLayout(mCard, BoxLayout.Y_AXIS));
                mCard.setBackground(bgCard);
                mCard.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(Color.decode("#334155"), 1, true),
                        new EmptyBorder(12, 14, 12, 14)
                ));
                JLabel lblTitulo = new JLabel(componente.getPropriedade("rotulo", ""));
                lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lblTitulo.setForeground(textMuted);
                JLabel lblVal = new JLabel(componente.getPropriedade("valor", "0"));
                lblVal.setFont(new Font("Segoe UI", Font.BOLD, 22));
                lblVal.setForeground(textPrimary);
                mCard.add(lblTitulo);
                mCard.add(lblVal);
                return mCard;
            }
            case ALERTA -> {
                String texto = componente.getPropriedade("texto", componente.getPropriedade("rotulo", ""));
                JLabel lbl = new JLabel("ℹ " + texto);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                lbl.setForeground(Color.decode("#60a5fa"));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(Color.decode("#1e3a8a"), 1, true),
                        new EmptyBorder(8, 12, 8, 12)
                ));
                return lbl;
            }
            case DIVISOR -> {
                JSeparator sep = new JSeparator();
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
                return sep;
            }
            default -> {
                return new JLabel("Componente: " + componente.tipo());
            }
        }
    }
}
