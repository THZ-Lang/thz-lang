package thz.lang.gui.formulario;

import thz.lang.documento.MotorDocumentos;
import thz.lang.interpretador.ValorThz;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Módulo de exportação de formulários para documentos corporativos (PDF, Excel, Word).
 */
public class ExportadorFormularioGui {

    public static void abrirMenuExportacao(JComponent anchor, JFrame parentFrame, String tituloRelatorio, String nomeEstrutura,
                                           Supplier<ValorThz.Registro> provedorDados,
                                           Consumer<String> callbackSucesso,
                                           Consumer<String> callbackErro) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(new Color(39, 39, 42));
        popup.setBorder(BorderFactory.createLineBorder(new Color(63, 63, 70), 1, true));

        JMenuItem miPdf = new JMenuItem("📄 Relatório PDF (.pdf)");
        estilizarItemPopup(miPdf);
        miPdf.addActionListener(e -> exportarParaArquivo("pdf", "Relatório PDF", "pdf", parentFrame, tituloRelatorio, nomeEstrutura, provedorDados, callbackSucesso, callbackErro));

        JMenuItem miXlsx = new JMenuItem("📊 Planilha Excel (.xlsx)");
        estilizarItemPopup(miXlsx);
        miXlsx.addActionListener(e -> exportarParaArquivo("xlsx", "Planilha Excel", "xlsx", parentFrame, tituloRelatorio, nomeEstrutura, provedorDados, callbackSucesso, callbackErro));

        JMenuItem miDocx = new JMenuItem("📝 Documento Word (.docx)");
        estilizarItemPopup(miDocx);
        miDocx.addActionListener(e -> exportarParaArquivo("docx", "Documento Word", "docx", parentFrame, tituloRelatorio, nomeEstrutura, provedorDados, callbackSucesso, callbackErro));

        popup.add(miPdf);
        popup.add(miXlsx);
        popup.add(miDocx);
        popup.show(anchor, 0, anchor.getHeight() + 4);
    }

    private static void estilizarItemPopup(JMenuItem item) {
        item.setBackground(new Color(39, 39, 42));
        item.setForeground(new Color(244, 244, 245));
        item.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }

    private static void exportarParaArquivo(String formato, String descricaoFormato, String extensao,
                                            JFrame parentFrame, String tituloRelatorio, String nomeEstrutura,
                                            Supplier<ValorThz.Registro> provedorDados,
                                            Consumer<String> callbackSucesso,
                                            Consumer<String> callbackErro) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Salvar " + descricaoFormato);
        String nomeBase = sanitizarNomeArquivo(nomeEstrutura.toLowerCase()) + "_exportado." + extensao;
        fc.setSelectedFile(new File(nomeBase));

        int res = fc.showSaveDialog(parentFrame);
        if (res == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            File arq = fc.getSelectedFile();
            if (!arq.getName().toLowerCase().endsWith("." + extensao)) {
                arq = new File(arq.getAbsolutePath() + "." + extensao);
            }
            try {
                ValorThz.Registro dadosAtualizados = provedorDados.get();
                MotorDocumentos.exportar(arq.toPath(), tituloRelatorio, dadosAtualizados);
                if (callbackSucesso != null) callbackSucesso.accept("Documento exportado com sucesso: " + arq.getName());
            } catch (Exception ex) {
                if (callbackErro != null) callbackErro.accept("Erro ao exportar " + formato.toUpperCase() + ": " + ex.getMessage());
            }
        }
    }

    private static String sanitizarNomeArquivo(String nome) {
        return nome.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
