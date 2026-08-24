package thz.lang.gui.formulario;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DecimalFixo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente especialista para visualização e edição dinâmica de fatias e coleções estruturadas (FATIA[Estrutura]).
 */
public class PainelTabelaFatia {

    public static JPanel criarPainelTabela(String nomeCampo, ValorThz.Fatia fatia,
                                           Map<String, DefaultTableModel> fatiaModelos,
                                           Map<String, JComponent> camposEntrada) {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setOpaque(false);

        List<String> colunas = extrairColunasFatia(fatia);
        
        DefaultTableModel model = new DefaultTableModel(colunas.toArray(new Object[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        for (ValorThz el : fatia.elementos()) {
            if (el instanceof ValorThz.Registro reg) {
                Object[] row = new Object[colunas.size()];
                for (int i = 0; i < colunas.size(); i++) {
                    String col = colunas.get(i);
                    ValorThz v = reg.campos().get(col);
                    row[i] = FabricaCamposFormulario.formatarTextoInicial(v);
                }
                model.addRow(row);
            }
        }

        JTable table = new JTable(model);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setBackground(new Color(24, 24, 27));
        table.setForeground(new Color(244, 244, 245));
        table.setGridColor(new Color(63, 63, 70));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(39, 39, 42));
        table.getTableHeader().setForeground(new Color(228, 228, 231));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(750, 140));
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbar.setOpaque(false);

        JButton btnAdicionar = new JButton("➕ Adicionar Linha");
        btnAdicionar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnAdicionar.setBackground(new Color(63, 63, 70));
        btnAdicionar.setForeground(Color.WHITE);
        btnAdicionar.setFocusPainted(false);
        btnAdicionar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdicionar.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnAdicionar.addActionListener(e -> {
            Object[] novaLinha = new Object[colunas.size()];
            Arrays.fill(novaLinha, "");
            model.addRow(novaLinha);
        });

        JButton btnRemover = new JButton("➖ Remover Selecionada");
        btnRemover.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnRemover.setBackground(new Color(63, 63, 70));
        btnRemover.setForeground(Color.WHITE);
        btnRemover.setFocusPainted(false);
        btnRemover.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRemover.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnRemover.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                model.removeRow(sel);
            }
        });

        toolbar.add(btnAdicionar);
        toolbar.add(btnRemover);

        fatiaModelos.put(nomeCampo, model);
        camposEntrada.put(nomeCampo, scrollTable);

        container.add(scrollTable, BorderLayout.CENTER);
        container.add(toolbar, BorderLayout.SOUTH);
        return container;
    }

    public static List<String> extrairColunasFatia(ValorThz.Fatia fatia) {
        List<String> colunas = new ArrayList<>();
        if (fatia != null && !fatia.elementos().isEmpty()) {
            ValorThz prim = fatia.elementos().get(0);
            if (prim instanceof ValorThz.Registro reg) {
                colunas.addAll(reg.campos().keySet());
            }
        }
        if (colunas.isEmpty()) {
            colunas.add("valor");
        }
        return colunas;
    }

    public static ValorThz.Fatia extrairFatiaDaTabela(String nomeCampo, DefaultTableModel model, ValorThz.Fatia template) {
        if (model == null) return template != null ? template : new ValorThz.Fatia("DADO", List.of());
        String tipoInterno = (template != null) ? template.tipoInterno() : "Registro";
        List<ValorThz> lista = new ArrayList<>();

        int rows = model.getRowCount();
        int cols = model.getColumnCount();

        for (int r = 0; r < rows; r++) {
            Map<String, ValorThz> camposLinha = new LinkedHashMap<>();
            for (int c = 0; c < cols; c++) {
                String nomeCol = model.getColumnName(c);
                Object val = model.getValueAt(r, c);
                String strVal = val != null ? val.toString().trim() : "";
                camposLinha.put(nomeCol, converterValorGenerico(strVal));
            }
            lista.add(new ValorThz.Registro(tipoInterno, camposLinha));
        }
        return new ValorThz.Fatia(tipoInterno, lista);
    }

    private static ValorThz converterValorGenerico(String s) {
        if (s.isEmpty()) return ValorThz.TEXTO("");
        try {
            if (s.matches("^-?\\d+$")) return ValorThz.INTEIRO(new BigInteger(s));
            if (s.matches("^-?\\d+\\.\\d+$")) return ValorThz.DECIMAL(DecimalFixo.deTexto(s, 2));
            if (s.equalsIgnoreCase("verdadeiro") || s.equalsIgnoreCase("sim") || s.equalsIgnoreCase("true")) return ValorThz.LOGICO(true);
            if (s.equalsIgnoreCase("falso") || s.equalsIgnoreCase("nao") || s.equalsIgnoreCase("false")) return ValorThz.LOGICO(false);
        } catch (Exception ignored) {}
        return ValorThz.TEXTO(s);
    }
}
