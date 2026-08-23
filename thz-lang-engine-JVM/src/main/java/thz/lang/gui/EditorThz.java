package thz.lang.gui;

import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.lexico.ErroLexico;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Editor THZ realçado — zero-dependência no motor, FlatLaf-aware no chrome.
 * Gutter com linha ativa, highlight de linha atual, fonte de luxo e debounce.
 */
public final class EditorThz extends JPanel {

    private final JTextPane pane = new JTextPane();
    private final JScrollPane scroll;
    private final Gutter gutter;
    private final UndoManager undo = new UndoManager();
    private final Timer debounce;
    private final java.util.List<Object> errorTags = new java.util.ArrayList<>();
    private PaletaThz paleta = PaletaThz.ESCURO;
    private boolean highlighting = false;
    private boolean suppressAuto = false;
    private Object currentLineTag = null;

    public EditorThz() {
        super(new BorderLayout());
        pane.setFont(escolherFonteMono());
        // deixa FlatLaf cuidar do fundo quando possível
        refreshChrome();
        pane.putClientProperty("JTextPane.lineWrap", Boolean.FALSE);

        pane.getDocument().addUndoableEditListener(undo);
        bindUndoRedo();

        gutter = new Gutter(pane, paleta);
        scroll = new JScrollPane(pane);
        scroll.setRowHeaderView(gutter);

        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(paleta.fundoEditor);
        // FlatLaf ‘scroll’ style
        scroll.putClientProperty("JScrollBar.showButtons", Boolean.TRUE);
        add(scroll, BorderLayout.CENTER);

        debounce = new Timer(300, _ -> realcar());
        debounce.setRepeats(false);
        pane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!highlighting) {
                    agendarRealce();
                    gutter.repaint();
                    atualizarLinhaAtual();
                }
            }

            public void removeUpdate(DocumentEvent e) {
                if (!highlighting) {
                    agendarRealce();
                    gutter.repaint();
                    atualizarLinhaAtual();
                }
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
        pane.addCaretListener(_ -> {
            gutter.repaint();
            atualizarLinhaAtual();
        });

        pane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() == 0) handleEnter(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '"' && !suppressAuto) handleQuote(e);
            }
        });

        // margem interna
        pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        aplicarTema(PaletaThz.ESCURO);
        realcar();
    }

    private static Font escolherFonteMono() {
        String[] cands = {"JetBrains Mono", "Cascadia Code", "Cascadia Mono", "Fira Code", "Consolas", Font.MONOSPACED};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> fams = java.util.Set.of(ge.getAvailableFontFamilyNames());
        for (String c : cands)
            if (fams.contains(c) || c.equals(Font.MONOSPACED)) return new Font(c, Font.PLAIN, (int) (float) 14.0);
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) (float) 14.0);
    }

    private static int[] computeLineStarts(String text) {
        java.util.ArrayList<Integer> s = new java.util.ArrayList<>();
        s.add(0);
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') s.add(i + 1);
        int[] a = new int[s.size()];
        for (int i = 0; i < s.size(); i++) a[i] = s.get(i);
        return a;
    }

    private static int offsetDe(int linha, int coluna, int[] lineStarts) {
        int li = linha - 1;
        if (li < 0) return 0;
        if (li >= lineStarts.length) return lineStarts[lineStarts.length - 1];
        return lineStarts[li] + (coluna - 1);
    }

    private static int comprimentoBruto(Token tok, String text, int start) {
        if (tok.type() == TokenType.STRING_LITERAL) {
            for (int i = start + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') return i - start;
                if (c == '"') return i - start + 1;
            }
            return text.length() - start;
        }
        if (tok.type() == TokenType.NUMERO_LITERAL) {
            int i = start;
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (Character.isDigit(ch) || ch == '_' || ch == '.') i++;
                else break;
            }
            return i - start;
        }
        return tok.value().length();
    }

    public Dimension getSize() {
        return new Dimension();
    }

    private void refreshChrome() {
        pane.setCaretColor(paleta.corCaret);
        pane.setSelectionColor(paleta.corSelecao);
        pane.setBackground(paleta.fundoEditor);
        pane.setForeground(paleta.frenteEditor);
        if (scroll != null) {
            scroll.getViewport().setBackground(paleta.fundoEditor);
            scroll.setBackground(paleta.fundoEditor);
        }
    }

    private void bindUndoRedo() {
        InputMap im = pane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = pane.getActionMap();
        im.put(KeyStroke.getKeyStroke("control Z"), "Undo");
        im.put(KeyStroke.getKeyStroke("control Y"), "Redo");
        im.put(KeyStroke.getKeyStroke("meta Z"), "Undo");
        im.put(KeyStroke.getKeyStroke("meta Y"), "Redo");
        am.put("Undo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undo.canUndo()) undo.undo();
            }
        });
        am.put("Redo", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undo.canRedo()) undo.redo();
            }
        });
    }

    private void handleEnter(KeyEvent e) {
        try {
            int caret = pane.getCaretPosition();
            String text = pane.getText();
            int lineStart = text.lastIndexOf('\n', Math.max(-1, caret - 1)) + 1;
            int i = lineStart;
            StringBuilder indent = new StringBuilder();
            while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
                indent.append(text.charAt(i));
                i++;
            }
            String line = text.substring(lineStart, caret);
            String trimmed = line.stripTrailing();
            if (trimmed.equals("INICIO") || trimmed.equals("METADADOS_ARQUITETURA") || trimmed.equals("ESTRUTURA") || trimmed.equals("ENUMERACAO") || trimmed.equals("REGRA_NEGOCIO") || trimmed.equals("OPERACAO") || trimmed.equals("CONTRATO_ENTRADA") || trimmed.equals("CONTRATO_SAIDA") || trimmed.endsWith(" INICIO")) {
                indent.append("    ");
            }
            final String toInsert = indent.toString();
            if (toInsert.isEmpty()) return;
            e.consume();
            SwingUtilities.invokeLater(() -> {
                try {
                    pane.getDocument().insertString(pane.getCaretPosition(), "\n" + toInsert, null);
                } catch (BadLocationException ignore) {
                }
            });
        } catch (Exception ignore) {
        }
    }

    private void handleQuote(KeyEvent e) {
        int caret = pane.getCaretPosition();
        String text = pane.getText();
        if (pane.getSelectedText() != null) return;
        if (caret < text.length()) {
            char nxt = text.charAt(caret);
            if (Character.isLetterOrDigit(nxt) || nxt == '_') return;
        }
        e.consume();
        suppressAuto = true;
        try {
            pane.getDocument().insertString(caret, "\"\"", null);
            pane.setCaretPosition(caret + 1);
        } catch (BadLocationException ignore) {
        } finally {
            suppressAuto = false;
        }
    }

    public void agendarRealce() {
        debounce.restart();
    }

    public void aplicarTema(PaletaThz nova) {
        this.paleta = nova;
        pane.setFont(escolherFonteMono());
        refreshChrome();
        gutter.aplicarTema(nova);
        atualizarLinhaAtual();
        realcar();
    }


    public JTextPane getPane() {
        return pane;
    }

    public String getText() {
        return pane.getText();
    }

    public void setText(String t) {
        pane.setText(t);
        pane.setCaretPosition(0);
        undo.discardAllEdits();
        agendarRealce();
        atualizarLinhaAtual();
    }

    private void atualizarLinhaAtual() {
        Highlighter hl = pane.getHighlighter();
        if (currentLineTag != null) {
            hl.removeHighlight(currentLineTag);
            currentLineTag = null;
        }
        try {
            int caret = pane.getCaretPosition();
            String text = pane.getText();
            int lineStart = text.lastIndexOf('\n', Math.max(-1, caret - 1)) + 1;
            int lineEnd = text.indexOf('\n', caret);
            if (lineEnd == -1) lineEnd = text.length();
            // não destacar se texto vazio
            if (lineEnd > lineStart) {
                Highlighter.HighlightPainter p = new DefaultHighlighter.DefaultHighlightPainter(paleta.fundoLinhaAtual);
                currentLineTag = hl.addHighlight(lineStart, lineEnd, p);
                // garantir que error highlights fiquem por cima: reordenar não é necessário, pintaremos erro depois
            }
        } catch (BadLocationException ignore) {
        }
    }

    // ---- highlight ----
    private void realcar() {
        if (highlighting) return;
        highlighting = true;
        try {
            String text = pane.getText();
            StyledDocument doc = pane.getStyledDocument();
            int len = text.length();
            int[] lineStarts = computeLineStarts(text);
            doc.setCharacterAttributes(0, len, paleta.attrPadrao, true);
            aplicarComentarios(text, doc);
            List<Token> tokens;
            int errorOffset;
            try {
                tokens = new ThzLexer(text).tokenize();
            } catch (ErroLexico ex) {
                errorOffset = offsetDe(ex.linha(), ex.coluna(), lineStarts);
                String prefix = text.substring(0, Math.min(errorOffset, text.length()));
                try {
                    tokens = new ThzLexer(prefix).tokenize();
                } catch (ErroLexico ex2) {
                    tokens = List.of();
                }
                if (errorOffset < len) {
                    int errLen = 1;
                    String msg = ex.getMessage() == null ? "" : ex.getMessage();
                    if (msg.contains("não terminado") || msg.contains("nao terminado")) {
                        int lineEnd = text.indexOf('\n', errorOffset);
                        if (lineEnd == -1) lineEnd = len;
                        errLen = lineEnd - errorOffset;
                    }
                    if (errLen <= 0) errLen = 1;
                    if (errorOffset + errLen > len) errLen = len - errorOffset;
                    doc.setCharacterAttributes(errorOffset, errLen, paleta.attrErro, true);
                }
            }
            if (tokens != null) {
                for (Token tok : tokens) {
                    if (tok.type() == TokenType.EOF) continue;
                    int start = offsetDe(tok.line(), tok.column(), lineStarts);
                    if (start < 0 || start >= len) continue;
                    int rawLen = comprimentoBruto(tok, text, start);
                    if (rawLen <= 0) continue;
                    if (start + rawLen > len) rawLen = len - start;
                    doc.setCharacterAttributes(start, rawLen, paleta.atributoPara(tok), true);
                }
            }
        } catch (Exception ignore) {
        } finally {
            highlighting = false;
            gutter.repaint();
            atualizarLinhaAtual();
        }
    }

    private void aplicarComentarios(String text, StyledDocument doc) {
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                inString = false;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString && c == '#') {
                int end = text.indexOf('\n', i);
                if (end == -1) end = text.length();
                doc.setCharacterAttributes(i, end - i, paleta.attrComentario, true);
                i = end - 1;
            }
        }
    }

    public void limparMarcacoesErro() {
        Highlighter hl = pane.getHighlighter();
        for (Object t : errorTags) hl.removeHighlight(t);
        errorTags.clear();
    }

    public void desfazer() {
        if (undo.canUndo()) undo.undo();
    }

    public void refazer() {
        if (undo.canRedo()) undo.redo();
    }

    public void marcarErros(List<DiagnosticoEntrada> erros) {
        limparMarcacoesErro();
        if (erros == null || erros.isEmpty()) return;
        String text = pane.getText();
        int[] lineStarts = computeLineStarts(text);
        Highlighter hl = pane.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(paleta.fundoErro);
        for (DiagnosticoEntrada e : erros) {
            int off = offsetDe(e.linha(), e.coluna(), lineStarts);
            int lineEnd = text.indexOf('\n', off);
            if (lineEnd == -1) lineEnd = text.length();
            int end = Math.min(lineEnd, off + 140);
            if (end <= off) end = Math.min(text.length(), off + 1);
            try {
                errorTags.add(hl.addHighlight(off, end, painter));
            } catch (BadLocationException ignore) {
            }
        }
        if (!erros.isEmpty()) {
            DiagnosticoEntrada primeiro = erros.getFirst();
            int off = offsetDe(primeiro.linha(), primeiro.coluna(), lineStarts);
            pane.setCaretPosition(Math.min(off, text.length()));
        }
    }
}

