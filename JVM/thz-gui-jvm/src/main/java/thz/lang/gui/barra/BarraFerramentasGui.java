package thz.lang.gui.barra;

import thz.lang.gui.PaletaThz;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Construtor e gerenciador do Cabeçalho e da Barra de Ferramentas (Toolbar) da IDE THZ-LANG Desktop.
 */
public class BarraFerramentasGui {

    private final JPanel header = new JPanel(new BorderLayout());
    private final JLabel logoBrand = new JLabel("THZ");
    private final JLabel tituloBrand = new JLabel("ENGINE");
    private final JLabel subtBrand = new JLabel("JVM v2.3");

    private final JPanel pillArquivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel infoArquivo = new JLabel("Novo arquivo");

    private final JToggleButton toggleEstrito = new JToggleButton("Lint Estrito");
    private final JToggleButton toggleTema = new JToggleButton("Tema");

    private final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final List<JButton> botoesSecundarios = new ArrayList<>();
    private final List<JSeparator> separadores = new ArrayList<>();

    private final JButton btnExecutar = new JButton("▶ Executar (F5)");
    private final JButton btnVerificar = new JButton("✓ Verificar (F6)");
    private final JButton btnFormatar = new JButton("✨ Formatar");
    private final JButton btnDoc = new JButton("📘 Doc (F7)");
    private final JButton btnAudit = new JButton("🛡️ Auditoria (F8)");
    private final JButton btnIr = new JButton("🧩 IR (F9)");
    private final JButton btnLimpar = new JButton("🗑 Limpar Console");

    public BarraFerramentasGui(BarraMenuGui.AcoesGui acoes) {
        // Montar Header
        JPanel esqBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esqBrand.setOpaque(false);

        logoBrand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tituloBrand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        subtBrand.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        esqBrand.add(logoBrand);
        esqBrand.add(tituloBrand);
        esqBrand.add(subtBrand);

        pillArquivo.setOpaque(true);
        infoArquivo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pillArquivo.add(new JLabel("📄"));
        pillArquivo.add(infoArquivo);

        JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centro.setOpaque(false);
        centro.add(pillArquivo);

        JPanel dirToggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dirToggles.setOpaque(false);

        toggleEstrito.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        toggleEstrito.setFocusPainted(false);
        toggleEstrito.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleEstrito.addActionListener(e -> acoes.alternarModoEstrito());

        toggleTema.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        toggleTema.setFocusPainted(false);
        toggleTema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleTema.addActionListener(e -> acoes.alternarTema());

        dirToggles.add(toggleEstrito);
        dirToggles.add(toggleTema);

        header.add(esqBrand, BorderLayout.WEST);
        header.add(centro, BorderLayout.CENTER);
        header.add(dirToggles, BorderLayout.EAST);

        // Montar Toolbar
        configurarBotaoPrincipal(btnExecutar, acoes::executarCodigo);
        configurarBotaoSecundario(btnVerificar, acoes::verificarCodigo);
        configurarBotaoSecundario(btnFormatar, acoes::formatarCodigo);
        configurarBotaoSecundario(btnDoc, acoes::gerarDocumentacao);
        configurarBotaoSecundario(btnAudit, acoes::auditarGovernanca);
        configurarBotaoSecundario(btnIr, acoes::gerarIrELlvm);
        configurarBotaoSecundario(btnLimpar, acoes::limparSaida);

        toolbar.add(btnExecutar);
        toolbar.add(btnVerificar);
        toolbar.add(btnFormatar);
        adicionarSeparador();
        toolbar.add(btnDoc);
        toolbar.add(btnAudit);
        toolbar.add(btnIr);
        adicionarSeparador();
        toolbar.add(btnLimpar);
    }

    private void configurarBotaoPrincipal(JButton btn, Runnable acao) {
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btn.setBackground(new Color(37, 99, 235)); // Azul vibrante
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(29, 78, 216), 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        btn.addActionListener(e -> acao.run());
    }

    private void configurarBotaoSecundario(JButton btn, Runnable acao) {
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> acao.run());
        botoesSecundarios.add(btn);
    }

    private void adicionarSeparador() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(6, 22));
        separadores.add(sep);
        toolbar.add(sep);
    }

    public JPanel getHeader() {
        return header;
    }

    public JPanel getToolbar() {
        return toolbar;
    }

    public JToggleButton getToggleEstrito() {
        return toggleEstrito;
    }

    public JToggleButton getToggleTema() {
        return toggleTema;
    }

    public void atualizarInfoArquivo(String texto) {
        infoArquivo.setText(texto);
    }

    public void aplicarTema(PaletaThz tema) {
        header.setBackground(tema.fundoJanela);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, tema.corBorda),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        toolbar.setBackground(tema.fundoToolbar);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, tema.corBordaSuave),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        pillArquivo.setBackground(tema.fundoPainel);
        pillArquivo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tema.corBordaSuave, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        logoBrand.setForeground(tema.corAcento);
        tituloBrand.setForeground(tema.corTextoTitulo);
        subtBrand.setForeground(tema.corTextoSecundario);
        infoArquivo.setForeground(tema.corTextoSecundario);

        for (JButton b : botoesSecundarios) {
            b.setBackground(tema.fundoPainel);
            b.setForeground(tema.corTextoTitulo);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tema.corBorda, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        }

        for (JSeparator sep : separadores) {
            sep.setForeground(tema.corBordaSuave);
        }

        toggleEstrito.setBackground(toggleEstrito.isSelected() ? tema.corAcento : tema.fundoPainel);
        toggleEstrito.setForeground(toggleEstrito.isSelected() ? Color.WHITE : tema.corTextoSecundario);

        toggleTema.setBackground(tema.fundoPainel);
        toggleTema.setForeground(tema.corTextoSecundario);
        toggleTema.setText(tema == PaletaThz.CLARO ? "☀️ Modo Claro" : "🌙 Modo Escuro");
    }
}
