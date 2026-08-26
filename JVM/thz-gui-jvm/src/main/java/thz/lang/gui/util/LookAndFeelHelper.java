package thz.lang.gui.util;

import javax.swing.UIManager;

/**
 * Configura o LookAndFeel da aplicação Swing.
 * Tenta FlatDarkLaf (flatlaf) como tema padrão; fallback para L&F do sistema.
 */
public final class LookAndFeelHelper {

    private LookAndFeelHelper() {
    }

    public static void configurar() {
        try {
            Class.forName("com.formdev.flatlaf.FlatDarkLaf").getMethod("setup").invoke(null);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
    }
}
