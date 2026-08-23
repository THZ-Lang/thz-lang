package thz.lang.semantico;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Assinaturas estáticas de tipos e aridade para as 28 funções da biblioteca padrão da THZ-LANG.
 */
public final class AssinaturasStdlib {

    public record Assinatura(int paramMin, int paramMax, Function<List<TipoThz>, TipoThz> retornoFn) {
        public Assinatura(int paramMin, int paramMax, TipoThz retornoFixo) {
            this(paramMin, paramMax, args -> retornoFixo);
        }
    }

    private static final TipoThz TIPO_LOGICO = Tipos.TIPOS_PRIMITIVOS.get("LOGICO");
    private static final TipoThz TIPO_TEXTO = Tipos.TIPOS_PRIMITIVOS.get("TEXTO");
    private static final TipoThz TIPO_INTEIRO_GENERICO = new TipoThz("INTEIRO64", CategoriaTipo.INTEIRO);
    private static final TipoThz TIPO_DATA = Tipos.TIPOS_PRIMITIVOS.get("DATA");
    private static final TipoThz TIPO_DATA_HORA = Tipos.TIPOS_PRIMITIVOS.get("DATA_HORA");

    private static final Map<String, Assinatura> ASSINATURAS = criarAssinaturas();

    private AssinaturasStdlib() {}

    public static boolean ehStdlib(String nome) {
        return nome != null && ASSINATURAS.containsKey(nome);
    }

    public static Assinatura obter(String nome) {
        return ASSINATURAS.get(nome);
    }

    private static Map<String, Assinatura> criarAssinaturas() {
        Map<String, Assinatura> m = new LinkedHashMap<>();

        // ---- TEXTO ----
        m.put("TEXTO.comprimento", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("TEXTO.maiusculas", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.minusculas", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.aparar", new Assinatura(1, 1, TIPO_TEXTO));
        m.put("TEXTO.contem", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("TEXTO.subtexto", new Assinatura(2, 3, TIPO_TEXTO));
        m.put("TEXTO.substituir", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("TEXTO.dividir", new Assinatura(2, 2, new TipoThz("FATIA[TEXTO]", CategoriaTipo.FATIA, null, null, "TEXTO", null)));
        m.put("TEXTO.juntar", new Assinatura(2, 2, TIPO_TEXTO));

        // ---- MATEMATICA ----
        m.put("MATEMATICA.abs", new Assinatura(1, 1, (Function<List<TipoThz>, TipoThz>) args -> Tipos.ehInteiro(args.get(0)) ? TIPO_INTEIRO_GENERICO : args.get(0)));
        m.put("MATEMATICA.min", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.max", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.potencia", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.raiz", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("MATEMATICA.arredondar", new Assinatura(2, 2, (Function<List<TipoThz>, TipoThz>) args -> args.get(0)));
        m.put("MATEMATICA.aleatorio", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));

        // ---- DATA ----
        m.put("DATA.hoje", new Assinatura(0, 0, TIPO_DATA));
        m.put("DATA.agora", new Assinatura(0, 0, TIPO_DATA_HORA));
        m.put("DATA.criar", new Assinatura(3, 3, TIPO_DATA));
        m.put("DATA.criarDataHora", new Assinatura(5, 6, TIPO_DATA_HORA));
        m.put("DATA.adicionarDias", new Assinatura(2, 2, TIPO_DATA));
        m.put("DATA.adicionarHoras", new Assinatura(2, 2, TIPO_DATA_HORA));
        m.put("DATA.diferencaDias", new Assinatura(2, 2, TIPO_INTEIRO_GENERICO));
        m.put("DATA.ano", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.mes", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.dia", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.diaDaSemana", new Assinatura(1, 1, TIPO_INTEIRO_GENERICO));
        m.put("DATA.texto", new Assinatura(1, 1, TIPO_TEXTO));

        // ---- TELA ----
        m.put("TELA.renderizarFormulario", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("TELA.alerta", new Assinatura(2, 2, TIPO_TEXTO));
        m.put("TELA.confirmar", new Assinatura(2, 2, TIPO_LOGICO));
        m.put("TELA.pedirTexto", new Assinatura(2, 2, TIPO_TEXTO));

        // ---- DOCUMENTO ----
        m.put("DOCUMENTO.exportar", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarPdf", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarXlsx", new Assinatura(3, 3, TIPO_TEXTO));
        m.put("DOCUMENTO.exportarDocx", new Assinatura(3, 3, TIPO_TEXTO));

        return Collections.unmodifiableMap(m);
    }
}
