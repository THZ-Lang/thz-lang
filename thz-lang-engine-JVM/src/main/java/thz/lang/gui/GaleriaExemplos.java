package thz.lang.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Construtor e gerenciador da galeria de exemplos (.thz) para o menu da IDE Swing.
 * Decomposto de ThzGui para respeitar o princípio de responsabilidade única (SRP).
 */
public final class GaleriaExemplos {

    private GaleriaExemplos() {}

    /**
     * Varre exemplos/colecao e exemplos/ (ordenados) e monta o JMenu de exemplos.
     */
    public static JMenu criarMenuExemplos(Consumer<File> onCarregarExemplo) {
        JMenu menu = new JMenu("Exemplos");
        menu.setToolTipText("Galeria de programas de partida — clique para carregar no editor");
        File raiz = new File("exemplos");
        if (!raiz.isDirectory()) {
            JMenuItem vazio = new JMenuItem("(pasta 'exemplos' não encontrada ao lado do jar)");
            vazio.setEnabled(false);
            menu.add(vazio);
            return menu;
        }

        boolean algum = false;
        File colecao = new File(raiz, "colecao");
        File[] daColecao = colecao.isDirectory() ? listarThzOrdenados(colecao) : new File[0];
        if (daColecao.length > 0) {
            JMenuItem cab = new JMenuItem("— Coleção de partida —");
            cab.setEnabled(false);
            menu.add(cab);
            for (File f : daColecao) {
                menu.add(criarItemExemplo(f, onCarregarExemplo));
                algum = true;
            }
        }

        File[] canonicos = listarThzOrdenados(raiz);
        if (canonicos.length > 0) {
            if (algum) {
                menu.addSeparator();
            }
            JMenuItem cab2 = new JMenuItem("— Canônicos (paridade TS ⇄ JVM) —");
            cab2.setEnabled(false);
            menu.add(cab2);
            for (File f : canonicos) {
                menu.add(criarItemExemplo(f, onCarregarExemplo));
                algum = true;
            }
        }

        if (!algum) {
            JMenuItem nenhum = new JMenuItem("(nenhum arquivo .thz encontrado)");
            nenhum.setEnabled(false);
            menu.add(nenhum);
        }

        return menu;
    }

    public static File[] listarThzOrdenados(File pasta) {
        File[] fs = pasta.listFiles((d, n) -> n.toLowerCase().endsWith(".thz"));
        if (fs == null) return new File[0];
        Arrays.sort(fs, Comparator.comparing(File::getName));
        return fs;
    }

    private static JMenuItem criarItemExemplo(File f, Consumer<File> onCarregarExemplo) {
        String nome = f.getName();
        String rotulo = nome.substring(0, nome.length() - 4).replaceFirst("^\\d+-", "").replace('-', ' ');
        JMenuItem item = new JMenuItem(rotulo);
        item.setToolTipText(f.getAbsolutePath());
        item.addActionListener(e -> onCarregarExemplo.accept(f));
        return item;
    }
}
