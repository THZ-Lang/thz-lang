package thz.lang.interpretador;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import thz.lang.ast.EstruturaAst;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ParametroOperacaoAst;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;

/**
 * Provedor canônico de dados de demonstração (LOTE) e construção de argumentos
 * para OPERACAO/PROCEDIMENTO.
 * Centralizado para eliminar duplicações entre ThzCli e ThzGui (DRY).
 */
public final class InjetorLoteDemo {

    public static final Object[][] LOTE = {
            new Object[] { "a1b2c3d4-0000-0000-0000-000000000001", "PROD-SKU-901", 10, "150.5000", "18.00", "0" },
            new Object[] { "a1b2c3d4-0000-0000-0000-000000000002", "PROD-SKU-902", 5, "320.0000", "12.00", "0" }
    };

    private static final Pattern PADRAO_FATIA = Pattern.compile("^FATIA\\[(\\w+)\\]$");

    private InjetorLoteDemo() {
    }

    /**
     * Constrói registro estruturado com validação de invariantes.
     */
    public static ValorThz registroDe(EstruturaAst est, Object[] vals, Consumer<ValorThz> validar) {
        Map<String, ValorThz> campos = new HashMap<>();
        for (int i = 0; i < est.campos().size(); i++) {
            var campo = est.campos().get(i);
            Object bruto = i < vals.length ? vals[i] : null;
            if (bruto != null) {
                campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(), bruto));
            } else if (campo.tipo().startsWith("NATURAL") || campo.tipo().startsWith("INTEIRO")) {
                campos.put(campo.nome(), ValorThz.INTEIRO(BigInteger.ZERO));
            } else if (campo.tipo().startsWith("DECIMAL") || campo.tipo().startsWith("MONETARIO")) {
                campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(), "0"));
            } else {
                campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(), ""));
            }
        }
        ValorThz reg = new ValorThz.Registro(est.nome(), campos);
        if (validar != null) {
            validar.accept(reg);
        }
        return reg;
    }

    /**
     * Constrói argumentos para uma OPERACAO, injetando LOTE para parâmetros FATIA e
     * solicitando os demais.
     */
    public static Map<String, ValorThz> construirArgsOperacao(
            OperacaoAst op,
            ProgramaAst ast,
            Consumer<ValorThz> validar,
            Function<ParametroOperacaoAst, String> provedorParametro) {

        Map<String, ValorThz> out = new HashMap<>();
        for (ParametroOperacaoAst p : op.parametros()) {
            var m = PADRAO_FATIA.matcher(p.tipo());
            if (m.matches()) {
                String nomeEstrutura = m.group(1);
                EstruturaAst est = ast.estruturas().stream()
                        .filter(e -> e.nome().equals(nomeEstrutura))
                        .findFirst()
                        .orElse(null);
                if (est == null) {
                    throw new RuntimeException(
                            "Estrutura '" + nomeEstrutura + "' referenciada por '" + p.tipo() + "' não declarada.");
                }
                List<ValorThz> elems = new ArrayList<>();
                for (Object[] linha : LOTE) {
                    elems.add(registroDe(est, linha, validar));
                }
                out.put(p.nome(), new ValorThz.Fatia(nomeEstrutura, elems));
            } else {
                String val = provedorParametro != null ? provedorParametro.apply(p) : null;
                out.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), val != null ? val : "0"));
            }
        }
        return out;
    }

    /**
     * Constrói argumentos para PROCEDIMENTO parametrizado.
     */
    public static Map<String, ValorThz> construirArgsProc(
            ProcedimentoAst proc,
            Function<ParametroOperacaoAst, String> provedorParametro) {

        Map<String, ValorThz> out = new HashMap<>();
        for (ParametroOperacaoAst p : proc.parametros()) {
            String val = provedorParametro != null ? provedorParametro.apply(p) : null;
            if (val == null) {
                throw new RuntimeException("[Erro de Execução] Parâmetro '" + p.nome() + "' não fornecido.");
            }
            out.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), val));
        }
        return out;
    }
}
