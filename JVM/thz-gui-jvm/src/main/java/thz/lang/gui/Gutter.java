package thz.lang.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * Gutter lateral com numeração de linhas ancorada no layout real do documento (View/modelToView2D).
 * Decomposto de EditorThz para responsabilidade única (SRP).
 */

public final class Gutter extends JPanel {
    private final JTextPane paneRef;
    private PaletaThz paleta;

    public Gutter(JTextPane pane, PaletaThz paleta) {
        this.paneRef = pane;
        this.paleta = paleta;
        setPreferredSize(new Dimension(56, 0));
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        aplicarTema(paleta);
    }

    public void aplicarTema(PaletaThz nova) {
        this.paleta = nova;
        setBackground(nova.fundoGutter);
        setForeground(nova.frenteGutter);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, nova.corBordaSuave));
        repaint();
    }

    public void atualizarLargura(int linhas) {
        int digitos = String.valueOf(Math.max(linhas, 1)).length();
        FontMetrics fm = getFontMetrics(getFont().deriveFont(Font.BOLD, 12f));
        int w = fm.stringWidth("9".repeat(digitos)) + 24;
        w = Math.max(44, Math.min(72, w));
        setPreferredSize(new Dimension(w, 0));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        StyledDocument doc = (StyledDocument) paneRef.getDocument();
        Element root = doc.getDefaultRootElement();
        int linhas = root.getElementCount();
        atualizarLargura(linhas);

        int caretLine = root.getElementIndex(paneRef.getCaretPosition()) + 1;
        FontMetrics fmGutter = g.getFontMetrics(getFont());
        int fallbackH = paneRef.getFontMetrics(paneRef.getFont()).getHeight();

        for (int i = 0; i < linhas; i++) {
            Element elem = root.getElement(i);
            int startOff = elem.getStartOffset();
            int y;
            try {
                Rectangle2D r = paneRef.modelToView2D(startOff);
                if (r == null) {
                    y = 10 + i * fallbackH + fmGutter.getAscent();
                } else {
                    double rh = r.getHeight();
                    if (rh <= 1) rh = fallbackH;
                    y = (int) (r.getY() + fmGutter.getAscent() + (rh - fmGutter.getHeight()) / 2.0);
                }
            } catch (BadLocationException ex) {
                y = 10 + i * fallbackH + fmGutter.getAscent();
            }

            boolean ativo = (i + 1) == caretLine;
            g2.setColor(ativo ? paleta.frenteGutterAtiva : getForeground());
            g2.setFont(getFont().deriveFont(ativo ? Font.BOLD : Font.PLAIN, ativo ? 12f : 11f));
            String s = String.valueOf(i + 1);
            FontMetrics fmAtivo = g2.getFontMetrics();
            int w = fmAtivo.stringWidth(s);
            g2.drawString(s, getWidth() - w - 10, y);
        }

        g2.setColor(paleta.corBordaSuave);
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
    }
}
