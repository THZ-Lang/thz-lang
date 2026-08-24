package thz.lang.gui.barra;

import thz.lang.gui.PaletaThz;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Barra de Status inferior da IDE THZ-LANG Desktop.
 * Exibe status do lint/execução, badge do modo estrito, posição do cursor (Linha:Coluna) e versão da JVM.
 */
public class BarraStatusGui {

    private final JPanel painel = new JPanel(new BorderLayout());
    private final JLabel infoStatus = new JLabel("Pronto");
    private final JLabel badgeEstrito = new JLabel("ESTRITO");
    private final JLabel infoCursor = new JLabel("Ln 1, Col 1");
    private final JLabel versaoLabel = new JLabel("v2.3.0 JVM");
    private final JLabel jvmBadge = new JLabel("☕ JVM");

    public BarraStatusGui() {
        painel.setLayout(new BorderLayout());

        JPanel esquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        esquerda.setOpaque(false);
        infoStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        esquerda.add(infoStatus);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        direita.setOpaque(false);

        badgeEstrito.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        badgeEstrito.setOpaque(true);
        badgeEstrito.setBackground(new Color(220, 38, 38)); // Vermelho
        badgeEstrito.setForeground(Color.WHITE);
        badgeEstrito.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        badgeEstrito.setVisible(false);

        jvmBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        jvmBadge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jvmBadge.setToolTipText("Clique para inspecionar ou alterar o ambiente JVM");

        infoCursor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        versaoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        direita.add(badgeEstrito);
        direita.add(jvmBadge);
        direita.add(infoCursor);
        direita.add(versaoLabel);

        painel.add(esquerda, BorderLayout.WEST);
        painel.add(direita, BorderLayout.EAST);
    }

    public JPanel getComponente() {
        return painel;
    }

    public JLabel getJvmBadge() {
        return jvmBadge;
    }

    public void atualizarCursor(int linha, int coluna) {
        infoCursor.setText("Ln " + linha + ", Col " + coluna);
    }

    public void definirStatus(String texto, Color corTexto) {
        infoStatus.setText(texto);
        if (corTexto != null) {
            infoStatus.setForeground(corTexto);
        }
    }

    public void definirPronto(PaletaThz tema) {
        infoStatus.setText("Pronto");
        if (tema != null) {
            infoStatus.setForeground(tema.corTextoSecundario);
        }
    }

    public void atualizarEstrito(boolean estrito) {
        badgeEstrito.setVisible(estrito);
    }

    public void atualizarJvmBadge(String texto) {
        jvmBadge.setText("☕ " + texto);
    }

    public void aplicarTema(PaletaThz tema) {
        painel.setBackground(tema.fundoStatus);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, tema.corBordaSuave),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        infoStatus.setForeground(tema.corTextoSecundario);
        infoCursor.setForeground(tema.corTextoSecundario);
        versaoLabel.setForeground(tema.corTextoSecundario);
        jvmBadge.setForeground(tema.corAcento);
    }
}
