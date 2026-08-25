package thz.lang.lexico;

import java.util.Locale;

/**
 * Dialeto léxico e sintático da linguagem THZ-LANG.
 * Suporta modo canônico em português (pt-BR) e modo equivalente internacional (en-US).
 */
public enum DialetoLinguagem {
    PT_BR("pt-BR", "LINGUAGEM"),
    EN_US("en-US", "LANGUAGE");

    private final String codigo;
    private final String pragma;

    DialetoLinguagem(String codigo, String pragma) {
        this.codigo = codigo;
        this.pragma = pragma;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPragma() {
        return pragma;
    }

    public static DialetoLinguagem detectar(String texto) {
        if (texto == null || texto.isBlank()) return PT_BR;
        String normalizado = texto.trim().toLowerCase(Locale.ROOT);
        if (normalizado.contains("en-us") || normalizado.contains("en_us") || normalizado.equals("en")
                || normalizado.startsWith("language") || normalizado.contains("english")) {
            return EN_US;
        }
        return PT_BR;
    }

    @Override
    public String toString() {
        return codigo;
    }
}
