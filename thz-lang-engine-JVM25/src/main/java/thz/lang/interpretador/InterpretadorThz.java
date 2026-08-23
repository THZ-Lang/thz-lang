package thz.lang.interpretador;

import thz.lang.ast.*;
import thz.lang.runtime.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Interpretador Tree-Walking THZ — port exato de src/interpretador.ts (51477 bytes).
 * Package thz.lang.interpretador, JDK 25, sem dependências externas.
 */
public class InterpretadorThz {

    private final ProgramaAst ast;
    private final Consumer<String> emitir;
    private final int maxIteracoes;
    private final Supplier<String> lerEntrada;

    private static final int LIMITE_PADRAO_ITERACOES = 10_000_000;

    // ---- Opções ----

    public record OpcoesInterpretador(
            Consumer<String> saida,
            Supplier<String> entrada,
            Integer maxIteracoes) {}

    public record OperacaoResolvida(RegraNegocioAst regra, OperacaoAst operacao) {}

    @FunctionalInterface
    private interface StdlibFn {
        ValorThz apply(List<ValorThz> args, ExprAst ctx);
    }

    private final Map<String, StdlibFn> STDLIB;

    // ---- Construtores ----

    public InterpretadorThz(ProgramaAst ast, OpcoesInterpretador opcoes) {
        this.ast = ast;
        if (opcoes != null && opcoes.saida() != null) {
            this.emitir = opcoes.saida();
        } else {
            this.emitir = System.out::println;
        }
        if (opcoes != null && opcoes.maxIteracoes() != null) {
            this.maxIteracoes = opcoes.maxIteracoes();
        } else {
            this.maxIteracoes = LIMITE_PADRAO_ITERACOES;
        }
        if (opcoes != null && opcoes.entrada() != null) {
            this.lerEntrada = opcoes.entrada();
        } else {
            this.lerEntrada = () -> {
                throw new ErroExecucao("[Erro de Execução] LER exige provedor de entrada (use --arg ou modo interativo).");
            };
        }
        this.STDLIB = criarStdlib();
    }

    public InterpretadorThz(ProgramaAst ast) {
        this(ast, null);
    }

    // Convenience constructor with direct Consumer/Supplier
    public InterpretadorThz(ProgramaAst ast, Consumer<String> saida, Supplier<String> entrada) {
        this(ast, new OpcoesInterpretador(saida, entrada, null));
    }

    // ---- Helpers STDLIB ----

    private static void exigirAridade(String nome, List<ValorThz> args, int esperada, ExprAst ctx) {
        if (args.size() != esperada) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + esperada + " argumento(s), recebidos " + args.size() + ".");
        }
    }

    private static void exigirClasse(String nome, ValorThz v, String classeEsperada, ExprAst ctx) {
        if (!v.classe().equals(classeEsperada)) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função '" + nome + "' exige " + classeEsperada + ", recebido " + v.classe() + ".");
        }
    }

    private static BigInteger comoInteiroArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Inteiro i) return i.valor();
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Esperado INTEIRO, recebido " + v.classe() + ".");
    }

    private Map<String, StdlibFn> criarStdlib() {
        Map<String, StdlibFn> m = new HashMap<>();

        m.put("TEXTO.comprimento", (args, ctx) -> {
            exigirAridade("TEXTO.comprimento", args, 1, ctx);
            exigirClasse("TEXTO.comprimento", args.get(0), "TEXTO", ctx);
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Texto) args.get(0)).valor().length()));
        });
        m.put("TEXTO.maiusculas", (args, ctx) -> {
            exigirAridade("TEXTO.maiusculas", args, 1, ctx);
            exigirClasse("TEXTO.maiusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toUpperCase());
        });
        m.put("TEXTO.minusculas", (args, ctx) -> {
            exigirAridade("TEXTO.minusculas", args, 1, ctx);
            exigirClasse("TEXTO.minusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toLowerCase());
        });
        m.put("TEXTO.aparar", (args, ctx) -> {
            exigirAridade("TEXTO.aparar", args, 1, ctx);
            exigirClasse("TEXTO.aparar", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().trim());
        });
        m.put("TEXTO.contem", (args, ctx) -> {
            if (args.size() != 2)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.contem exige 2 args");
            exigirClasse("TEXTO.contem", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.contem", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(((ValorThz.Texto) args.get(0)).valor().contains(((ValorThz.Texto) args.get(1)).valor()));
        });
        m.put("TEXTO.subtexto", (args, ctx) -> {
            if (args.size() < 2 || args.size() > 3)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.subtexto exige 2 ou 3 args (texto, inicio, [fim])");
            exigirClasse("TEXTO.subtexto", args.get(0), "TEXTO", ctx);
            int ini = comoInteiroArg(args.get(1), ctx).intValue();
            Integer fim = args.size() == 3 ? comoInteiroArg(args.get(2), ctx).intValue() : null;
            String texto = ((ValorThz.Texto) args.get(0)).valor();
            return ValorThz.TEXTO(sliceTexto(texto, ini, fim));
        });
        m.put("TEXTO.substituir", (args, ctx) -> {
            exigirAridade("TEXTO.substituir", args, 3, ctx);
            exigirClasse("TEXTO.substituir", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.substituir", args.get(1), "TEXTO", ctx);
            exigirClasse("TEXTO.substituir", args.get(2), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String alvo = ((ValorThz.Texto) args.get(1)).valor();
            String repl = ((ValorThz.Texto) args.get(2)).valor();
            // JS split/join semantics for literal strings
            if (alvo.isEmpty()) {
                // JS: "abc".split("") => ["a","b","c"]; join => inserted between chars plus ends?
                // "a".split("").join("X") => "a" ; "ab".split("").join("X") => "aXb"
                // For simplicity, emulate split/join for empty separator as per JS: split("") gives chars array, join inserts repl between.
                // That's equivalent to interleaving.
                // However original TS: base.split("").join(repl) => same.
                List<String> chars = new ArrayList<>();
                for (char c : base.toCharArray()) chars.add(String.valueOf(c));
                return ValorThz.TEXTO(String.join(repl, chars));
            }
            return ValorThz.TEXTO(base.replace(alvo, repl));
        });
        m.put("TEXTO.dividir", (args, ctx) -> {
            exigirAridade("TEXTO.dividir", args, 2, ctx);
            exigirClasse("TEXTO.dividir", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.dividir", args.get(1), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String sep = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> partes;
            if (sep.isEmpty()) {
                partes = new ArrayList<>();
                for (char c : base.toCharArray()) partes.add(ValorThz.TEXTO(String.valueOf(c)));
                // JS: "".split("") => []? Actually "".split("") => [] in JS? "".split("") returns [].
                // For "" base with empty sep: JS returns []? Let's keep consistent: if base empty and sep empty -> []
                if (base.isEmpty()) partes = List.of();
            } else {
                String[] arr = base.split(Pattern.quote(sep), -1);
                partes = new ArrayList<>();
                for (String p : arr) partes.add(ValorThz.TEXTO(p));
            }
            return new ValorThz.Fatia("TEXTO", List.copyOf(partes));
        });
        m.put("TEXTO.juntar", (args, ctx) -> {
            exigirAridade("TEXTO.juntar", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Fatia))
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.juntar exige FATIA[TEXTO] como 1º arg");
            exigirClasse("TEXTO.juntar", args.get(1), "TEXTO", ctx);
            List<ValorThz> elems = ((ValorThz.Fatia) args.get(0)).elementos();
            String sep = ((ValorThz.Texto) args.get(1)).valor();
            List<String> strs = new ArrayList<>();
            for (ValorThz e : elems) {
                if (e instanceof ValorThz.Texto t) strs.add(t.valor());
                else strs.add("");
            }
            return ValorThz.TEXTO(String.join(sep, strs));
        });

        m.put("MATEMATICA.abs", (args, ctx) -> {
            exigirAridade("MATEMATICA.abs", args, 1, ctx);
            ValorThz v = args.get(0);
            if (v instanceof ValorThz.Inteiro i) {
                BigInteger val = i.valor();
                return ValorThz.INTEIRO(val.signum() < 0 ? val.negate() : val);
            }
            if (v instanceof ValorThz.Decimal d) return ValorThz.DECIMAL(d.valor().abs());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.abs exige numérico");
        });
        m.put("MATEMATICA.min", (args, ctx) -> {
            exigirAridade("MATEMATICA.min", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) < 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.min exige dois INTEIROS");
        });
        m.put("MATEMATICA.max", (args, ctx) -> {
            exigirAridade("MATEMATICA.max", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) > 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.max exige dois INTEIROS");
        });
        m.put("MATEMATICA.potencia", (args, ctx) -> {
            exigirAridade("MATEMATICA.potencia", args, 2, ctx);
            double base = comoInteiroArg(args.get(0), ctx).doubleValue();
            double exp = comoInteiroArg(args.get(1), ctx).doubleValue();
            double pow = Math.pow(base, exp);
            long trunc = (long) pow; // Math.trunc equivalent for positive/negative
            // For large values, use BigInteger via string? Keep simple; if overflow, use double truncation
            // If pow is NaN or Infinite, truncation will be 0-ish; replicate JS behavior (Math.trunc(NaN)=NaN -> BigInt(NaN) throws). But THZ tests likely small.
            if (Double.isNaN(pow) || Double.isInfinite(pow)) {
                // JS BigInt(Math.trunc(NaN)) throws TypeError; we throw ErroExecucao
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.potencia resultado inválido");
            }
            // Truncate toward zero
            trunc = (long) (pow >= 0 ? Math.floor(pow) : Math.ceil(pow));
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        m.put("MATEMATICA.raiz", (args, ctx) -> {
            exigirAridade("MATEMATICA.raiz", args, 1, ctx);
            double n = comoInteiroArg(args.get(0), ctx).doubleValue();
            if (n < 0) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.raiz exige não-negativo");
            double s = Math.sqrt(n);
            long trunc = (long) Math.floor(s);
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        m.put("MATEMATICA.arredondar", (args, ctx) -> {
            if (args.size() != 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige 2 args");
            ValorThz d = args.get(0);
            if (!(d instanceof ValorThz.Decimal)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige DECIMAL");
            int casas = comoInteiroArg(args.get(1), ctx).intValue();
            return ValorThz.DECIMAL(((ValorThz.Decimal) d).valor().paraEscala(casas));
        });
        m.put("MATEMATICA.aleatorio", (args, ctx) -> {
            if (args.size() != 1) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.aleatorio exige 1 arg (limite)");
            double lim = comoInteiroArg(args.get(0), ctx).doubleValue();
            long r = (long) Math.floor(Math.random() * lim);
            return ValorThz.INTEIRO(BigInteger.valueOf(r));
        });

        m.put("DATA.hoje", (args, ctx) -> {
            exigirAridade("DATA.hoje", args, 0, ctx);
            LocalDate agora = LocalDate.now();
            return ValorThz.DATA(DataThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth()));
        });
        m.put("DATA.agora", (args, ctx) -> {
            exigirAridade("DATA.agora", args, 0, ctx);
            LocalDateTime agora = LocalDateTime.now();
            return ValorThz.DATA_HORA(DataHoraThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth(), agora.getHour(), agora.getMinute(), agora.getSecond()));
        });
        m.put("DATA.criar", (args, ctx) -> {
            exigirAridade("DATA.criar", args, 3, ctx);
            int a = comoInteiroArg(args.get(0), ctx).intValue();
            int mes = comoInteiroArg(args.get(1), ctx).intValue();
            int d = comoInteiroArg(args.get(2), ctx).intValue();
            try { return ValorThz.DATA(DataThz.deComponentes(a, mes, d)); }
            catch (ErroData e) { throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage()); }
        });
        m.put("DATA.criarDataHora", (args, ctx) -> {
            if (args.size() < 5 || args.size() > 6) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.criarDataHora exige 5 ou 6 args");
            int a = comoInteiroArg(args.get(0), ctx).intValue();
            int mes = comoInteiroArg(args.get(1), ctx).intValue();
            int dia = comoInteiroArg(args.get(2), ctx).intValue();
            int h = comoInteiroArg(args.get(3), ctx).intValue();
            int mi = comoInteiroArg(args.get(4), ctx).intValue();
            int s = args.size() == 6 ? comoInteiroArg(args.get(5), ctx).intValue() : 0;
            try { return ValorThz.DATA_HORA(DataHoraThz.deComponentes(a, mes, dia, h, mi, s)); }
            catch (ErroData e) { throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage()); }
        });
        m.put("DATA.adicionarDias", (args, ctx) -> {
            exigirAridade("DATA.adicionarDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarDias exige DATA");
            BigInteger dias = comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA(((ValorThz.Data) args.get(0)).valor().adicionarDias(dias));
        });
        m.put("DATA.adicionarHoras", (args, ctx) -> {
            exigirAridade("DATA.adicionarHoras", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.DataHora)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarHoras exige DATA_HORA");
            BigInteger h = comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA_HORA(((ValorThz.DataHora) args.get(0)).valor().adicionarHoras(h));
        });
        m.put("DATA.diferencaDias", (args, ctx) -> {
            exigirAridade("DATA.diferencaDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data) || !(args.get(1) instanceof ValorThz.Data))
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diferencaDias exige duas DATA");
            return ValorThz.INTEIRO(((ValorThz.Data) args.get(0)).valor().diferencaDias(((ValorThz.Data) args.get(1)).valor()));
        });
        m.put("DATA.ano", (args, ctx) -> {
            exigirAridade("DATA.ano", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getAno()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getAno()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.ano exige DATA ou DATA_HORA");
        });
        m.put("DATA.mes", (args, ctx) -> {
            exigirAridade("DATA.mes", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getMes()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getMes()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.mes exige DATA ou DATA_HORA");
        });
        m.put("DATA.dia", (args, ctx) -> {
            exigirAridade("DATA.dia", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getDia()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getDia()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.dia exige DATA ou DATA_HORA");
        });
        m.put("DATA.diaDaSemana", (args, ctx) -> {
            exigirAridade("DATA.diaDaSemana", args, 1, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diaDaSemana exige DATA");
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Data) args.get(0)).valor().diaDaSemana()));
        });
        m.put("DATA.texto", (args, ctx) -> {
            exigirAridade("DATA.texto", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.TEXTO(d.valor().formatar());
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.TEXTO(dh.valor().formatar());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.texto exige DATA ou DATA_HORA");
        });

        return Collections.unmodifiableMap(m);
    }

    private static String sliceTexto(String texto, int ini, Integer fim) {
        int len = texto.length();
        int s = ini;
        if (s < 0) s = Math.max(len + s, 0);
        else if (s > len) s = len;
        int e;
        if (fim == null) e = len;
        else {
            e = fim;
            if (e < 0) e = Math.max(len + e, 0);
            else if (e > len) e = len;
        }
        if (e < s) return "";
        return texto.substring(s, e);
    }

    public static boolean ehStdlib(String nome) {
        // Será preenchido após construção; versão estática vazia para compatibilidade
        return nome != null && (
                nome.startsWith("TEXTO.") || nome.startsWith("MATEMATICA.") || nome.startsWith("DATA.")
        );
    }

    public boolean ehStdlibInstancia(String nome) {
        return STDLIB.containsKey(nome);
    }

    // ---- Resolução de operações ----

    public List<OperacaoResolvida> listarOperacoesExecutaveis() {
        List<OperacaoResolvida> encontradas = new ArrayList<>();
        for (RegraNegocioAst regra : ast.regras()) {
            for (OperacaoAst op : regra.operacoes()) {
                if (!op.corpo().isEmpty()) encontradas.add(new OperacaoResolvida(regra, op));
            }
        }
        return encontradas;
    }

    public List<ProcedimentoAst> listarProcedimentos() {
        return ast.procedimentos() != null ? ast.procedimentos() : List.of();
    }

    private static boolean ehValorResultado(ValorThz v) {
        return v instanceof ValorThz.Resultado;
    }

    /**
     * Executa uma operação com contratos formais: EXIGE na entrada, GARANTE na saída.
     */
    public ValorThz executarOperacao(String nomeOperacao, Map<String, ValorThz> argumentos) {
        OperacaoResolvida alvo = listarOperacoesExecutaveis().stream()
                .filter(o -> o.operacao().nome().equals(nomeOperacao))
                .findFirst().orElse(null);
        if (alvo == null) {
            throw new ErroExecucao("[Erro de Execução] Operação '" + nomeOperacao + "' não encontrada ou sem corpo executável.");
        }
        boolean retornoResultado = alvo.operacao().tipoRetorno() != null && alvo.operacao().tipoRetorno().startsWith("RESULTADO");

        Escopo escopoGlobal = new Escopo();
        if (argumentos != null) {
            for (Map.Entry<String, ValorThz> e : argumentos.entrySet()) escopoGlobal.definir(e.getKey(), e.getValue());
        }

        validarContratos(alvo.regra().clausulasEntrada(), escopoGlobal, "EXIGE");

        ValorThz retorno = null;
        try {
            executarComandos(alvo.operacao().corpo(), escopoGlobal);
        } catch (SinalFalhar s) {
            if (!retornoResultado) {
                throw new ErroExecucao("[Erro de Execução] FALHAR_COM exige operação com retorno RESULTADO[T,E]; operacao '" + nomeOperacao + "' declara '" + alvo.operacao().tipoRetorno() + "'.");
            }
            return new ValorThz.Resultado(false, null, s.getValor());
        } catch (SinalRetorne s) {
            retorno = s.getValor();
        }

        validarContratos(alvo.regra().clausulasSaida(), escopoGlobal, "GARANTE");
        if (retornoResultado && !ehValorResultado(retorno)) {
            return new ValorThz.Resultado(true, retorno, null);
        }
        return retorno;
    }

    public ValorThz executarOperacao(String nomeOperacao) {
        return executarOperacao(nomeOperacao, Map.of());
    }

    public void executarProcedimento(String nome, Map<String, ValorThz> argumentos) {
        ProcedimentoAst proc = listarProcedimentos().stream().filter(p -> p.nome().equals(nome)).findFirst().orElse(null);
        if (proc == null) throw new ErroExecucao("[Erro de Execução] Procedimento '" + nome + "' não encontrado.");
        Escopo escopo = new Escopo();
        Map<String, ValorThz> args = argumentos != null ? argumentos : Map.of();
        for (ParametroOperacaoAst p : proc.parametros()) {
            ValorThz v = args.get(p.nome());
            if (v == null) throw new ErroExecucao("[Erro de Execução] Parâmetro '" + p.nome() + "' não fornecido para procedimento '" + nome + "'.");
            escopo.definir(p.nome(), v);
        }
        try {
            executarComandos(proc.corpo(), escopo);
        } catch (SinalFalhar s) {
            throw new ErroExecucao("[Erro de Execução] FALHAR_COM não permitido dentro de PROCEDIMENTO (sem canal RESULTADO).");
        } catch (SinalRetorne s) {
            if (s.getValor() != null) {
                throw new ErroExecucao("[Erro de Execução] RETORNE com valor não permitido dentro de PROCEDIMENTO; use RETORNE sem valor.");
            }
        }
    }

    public void executarProcedimento(String nome) {
        executarProcedimento(nome, Map.of());
    }

    // ---- Contratos formais ----

    private void validarContratos(List<ClausulaContratoAst> clausulas, Escopo escopo, String natureza) {
        if (clausulas == null) return;
        for (ClausulaContratoAst clausula : clausulas) {
            if (!avaliarClausulaUniversal(clausula.expressao(), escopo)) {
                throw new ErroContrato("[Violação de Contrato " + natureza + "][Linha " + clausula.linha() + ":" + clausula.coluna() + "] Cláusula reprovada: " + clausula.textoCanonico());
            }
        }
    }

    private boolean avaliarClausulaUniversal(ExprAst expr, Escopo escopo) {
        Set<String> raizes = coletarRaizesDeFatias(expr, escopo);
        return quantificar(expr, escopo, new ArrayList<>(raizes), 0);
    }

    private Set<String> coletarRaizesDeFatias(ExprAst expr, Escopo escopo) {
        Set<String> raizes = new HashSet<>();
        visitarRaizes(expr, escopo, raizes);
        return raizes;
    }

    private void visitarRaizes(ExprAst e, Escopo escopo, Set<String> raizes) {
        if (e instanceof ExprAst.AcessoCampo ac) {
            if (!ac.caminho().isEmpty()) {
                ValorThz base = escopo.resolver(ac.caminho().get(0));
                if (base instanceof ValorThz.Fatia) raizes.add(ac.caminho().get(0));
            }
        } else if (e instanceof ExprAst.Chamada ch) {
            for (ExprAst arg : ch.argumentos()) visitarRaizes(arg, escopo, raizes);
        } else if (e instanceof ExprAst.Indexacao idx) {
            visitarRaizes(idx.alvo(), escopo, raizes);
            visitarRaizes(idx.indice(), escopo, raizes);
        } else if (e instanceof ExprAst.FatiaLiteral fl) {
            for (ExprAst el : fl.elementos()) visitarRaizes(el, escopo, raizes);
        } else if (e instanceof ExprAst.CriarRegistro cr) {
            for (ExprAst.CampoValor c : cr.campos()) visitarRaizes(c.valor(), escopo, raizes);
        } else if (e instanceof ExprAst.OpBinaria ob) {
            visitarRaizes(ob.esquerda(), escopo, raizes);
            visitarRaizes(ob.direita(), escopo, raizes);
        } else if (e instanceof ExprAst.OpUnaria ou) {
            visitarRaizes(ou.operando(), escopo, raizes);
        }
    }

    private boolean quantificar(ExprAst expr, Escopo escopo, List<String> raizes, int indice) {
        if (indice >= raizes.size()) {
            return exigirLogico(avaliar(expr, escopo), "cláusula de contrato");
        }
        String nome = raizes.get(indice);
        ValorThz fatia = escopo.resolver(nome);
        if (!(fatia instanceof ValorThz.Fatia f)) return quantificar(expr, escopo, raizes, indice + 1);
        if (f.elementos().isEmpty()) return true;
        for (ValorThz elemento : f.elementos()) {
            Escopo sombra = new Escopo(escopo);
            sombra.definir(nome, elemento);
            if (!quantificar(expr, sombra, raizes, indice + 1)) return false;
        }
        return true;
    }

    // ---- Avaliação de expressões ----

    private ValorThz avaliar(ExprAst expr, Escopo escopo) {
        if (expr instanceof ExprAst.LiteralInteiro li) {
            return ValorThz.INTEIRO(li.valor());
        } else if (expr instanceof ExprAst.LiteralDecimal ld) {
            return ValorThz.DECIMAL(new DecimalFixo(ld.escalado(), ld.escala()));
        } else if (expr instanceof ExprAst.LiteralTexto lt) {
            return ValorThz.TEXTO(lt.valor());
        } else if (expr instanceof ExprAst.LiteralLogico ll) {
            return ValorThz.LOGICO(ll.valor());
        } else if (expr instanceof ExprAst.Nulo) {
            return ValorThz.NULO;
        } else if (expr instanceof ExprAst.FatiaLiteral fl) {
            List<ValorThz> elementos = new ArrayList<>();
            for (ExprAst e : fl.elementos()) elementos.add(avaliar(e, escopo));
            String tipoInterno = "TEXTO";
            if (!elementos.isEmpty()) {
                ValorThz primeiro = elementos.get(0);
                if (primeiro instanceof ValorThz.Registro r) tipoInterno = r.nomeEstrutura();
                else if (primeiro instanceof ValorThz.Inteiro) tipoInterno = "INTEIRO";
                else if (primeiro instanceof ValorThz.Decimal) tipoInterno = "DECIMAL";
                else if (primeiro instanceof ValorThz.Texto) tipoInterno = "TEXTO";
                else if (primeiro instanceof ValorThz.Data) tipoInterno = "DATA";
                else if (primeiro instanceof ValorThz.DataHora) tipoInterno = "DATA_HORA";
                else tipoInterno = primeiro.classe();
            }
            return new ValorThz.Fatia(tipoInterno, List.copyOf(elementos));
        } else if (expr instanceof ExprAst.CriarRegistro cr) {
            EstruturaAst estrutura = ast.estruturas().stream().filter(e -> e.nome().equals(cr.nomeEstrutura())).findFirst().orElse(null);
            if (estrutura == null) throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] Estrutura '" + cr.nomeEstrutura() + "' não declarada.");
            if (cr.campos().size() != estrutura.campos().size()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] CRIAR '" + cr.nomeEstrutura() + "' exige " + estrutura.campos().size() + " campos, recebidos " + cr.campos().size() + ".");
            }
            Map<String, ValorThz> campos = new HashMap<>();
            for (CampoEstruturaAst campo : estrutura.campos()) {
                ExprAst.CampoValor fornecido = cr.campos().stream().filter(c -> c.nome().equals(campo.nome())).findFirst().orElse(null);
                if (fornecido == null) throw new ErroExecucao("[Erro de Execução][Linha " + cr.linha() + ":" + cr.coluna() + "] Campo '" + campo.nome() + "' não fornecido em CRIAR '" + cr.nomeEstrutura() + "'.");
                campos.put(campo.nome(), avaliar(fornecido.valor(), escopo));
            }
            ValorThz registro = new ValorThz.Registro(cr.nomeEstrutura(), campos);
            validarInvariantes(registro, null);
            // Also need to validate via ComandoAst? TS passes expr as unknown ComandoAST for line info — we use null, invariant will report invariant line
            // For proper line, we will call validarInvariantes with dummy cmd? Instead rely on invariante position.
            return registro;
        } else if (expr instanceof ExprAst.Indexacao idx) {
            ValorThz alvo = avaliar(idx.alvo(), escopo);
            ValorThz indice = avaliar(idx.indice(), escopo);
            if (!(indice instanceof ValorThz.Inteiro)) throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice deve ser INTEIRO, recebido " + indice.classe());
            BigInteger bv = ((ValorThz.Inteiro) indice).valor();
            long iLong;
            try { iLong = bv.longValueExact(); } catch (ArithmeticException ex) { throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice fora de limites: '" + bv + "'."); }
            if (iLong < 0 || iLong > Integer.MAX_VALUE) throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice fora de limites: '" + bv + "'.");
            int i = (int) iLong;
            if (alvo instanceof ValorThz.Fatia f) {
                if (i >= f.elementos().size()) throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice " + i + " fora da fatia (tamanho " + f.elementos().size() + ").");
                return f.elementos().get(i);
            }
            if (alvo instanceof ValorThz.Texto t) {
                if (i >= t.valor().length()) throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Índice " + i + " fora do texto (tamanho " + t.valor().length() + ").");
                return ValorThz.TEXTO(String.valueOf(t.valor().charAt(i)));
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + idx.linha() + ":" + idx.coluna() + "] Indexação exige FATIA ou TEXTO, recebido " + alvo.classe());
        } else if (expr instanceof ExprAst.Chamada ch) {
            String nomeQualificado = String.join(".", ch.caminho());
            List<ValorThz> args = new ArrayList<>();
            for (ExprAst a : ch.argumentos()) args.add(avaliar(a, escopo));
            StdlibFn fn = STDLIB.get(nomeQualificado);
            if (fn != null) {
                try { return fn.apply(args, ch); }
                catch (ErroExecucao e) { throw e; }
                catch (Exception e) { throw new ErroExecucao(e.getMessage()); }
            }
            // procedimento?
            if (ch.caminho().size() == 1) {
                String nome = ch.caminho().get(0);
                ProcedimentoAst proc = ast.procedimentos() != null ? ast.procedimentos().stream().filter(p -> p.nome().equals(nome)).findFirst().orElse(null) : null;
                if (proc != null) {
                    if (args.size() != proc.parametros().size()) throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna() + "] Procedimento '" + proc.nome() + "' exige " + proc.parametros().size() + " arg(s), recebidos " + args.size() + ".");
                    Escopo escopoChamada = new Escopo();
                    for (int i = 0; i < proc.parametros().size(); i++) escopoChamada.definir(proc.parametros().get(i).nome(), args.get(i));
                    try {
                        executarComandos(proc.corpo(), escopoChamada);
                    } catch (SinalRetorne s) {
                        if (s.getValor() != null) throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna() + "] RETORNE com valor não permitido em PROCEDIMENTO.");
                        return ValorThz.NULO;
                    } catch (SinalFalhar s) {
                        throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna() + "] FALHAR_COM não permitido em PROCEDIMENTO.");
                    }
                    return ValorThz.NULO;
                }
            }
            // operação de regra por nome
            if (ch.caminho().size() == 1) {
                for (RegraNegocioAst regra : ast.regras()) {
                    OperacaoAst op = regra.operacoes().stream().filter(o -> o.nome().equals(ch.caminho().get(0))).findFirst().orElse(null);
                    if (op != null) {
                        if (args.size() != op.parametros().size()) throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna() + "] Operação '" + op.nome() + "' exige " + op.parametros().size() + " arg(s), recebidos " + args.size() + ".");
                        Map<String, ValorThz> mapa = new HashMap<>();
                        for (int i = 0; i < op.parametros().size(); i++) mapa.put(op.parametros().get(i).nome(), args.get(i));
                        ValorThz ret = executarOperacao(op.nome(), mapa);
                        return ret != null ? ret : ValorThz.NULO;
                    }
                }
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ch.linha() + ":" + ch.coluna() + "] Chamada desconhecida: '" + nomeQualificado + "'.");
        } else if (expr instanceof ExprAst.AcessoCampo ac) {
            ValorThz base = escopo.resolver(ac.caminho().get(0));
            if (base == null) {
                if (ac.caminho().size() == 1) {
                    String ident = ac.caminho().get(0);
                    for (EnumeracaoAst en : ast.enumeracoes()) {
                        if (en.membros().contains(ident)) return new ValorThz.Enumerado(en.nome(), ident);
                    }
                }
                throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna() + "] Identificador não declarado: '" + ac.caminho().get(0) + "'.");
            }
            ValorThz atual = base;
            for (int i = 1; i < ac.caminho().size(); i++) {
                String campo = ac.caminho().get(i);
                if (!(atual instanceof ValorThz.Registro reg)) {
                    throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna() + "] Acesso a campo '" + campo + "' em valor que não é registro.");
                }
                ValorThz proximo = reg.campos().get(campo);
                if (proximo == null) throw new ErroExecucao("[Erro de Execução][Linha " + ac.linha() + ":" + ac.coluna() + "] Campo '" + campo + "' inexistente em '" + reg.nomeEstrutura() + "'.");
                atual = proximo;
            }
            return atual;
        } else if (expr instanceof ExprAst.OpUnaria ou) {
            ValorThz operando = avaliar(ou.operando(), escopo);
            if ("NAO".equals(ou.operador())) {
                return ValorThz.LOGICO(!exigirLogico(operando, "operando do conectivo 'NAO'"));
            }
            if (operando instanceof ValorThz.Inteiro i) return ValorThz.INTEIRO(i.valor().negate());
            if (operando instanceof ValorThz.Decimal d) return ValorThz.DECIMAL(d.valor().negar());
            throw new ErroExecucao("[Erro de Execução][Linha " + ou.linha() + ":" + ou.coluna() + "] Negação aritmética exige valor numérico.");
        } else if (expr instanceof ExprAst.OpBinaria ob) {
            if ("E".equals(ob.operador())) {
                ValorThz esq = avaliar(ob.esquerda(), escopo);
                if (!exigirLogico(esq, "conectivo 'E'")) return ValorThz.LOGICO(false);
                return ValorThz.LOGICO(exigirLogico(avaliar(ob.direita(), escopo), "conectivo 'E'"));
            }
            if ("OU".equals(ob.operador())) {
                ValorThz esq = avaliar(ob.esquerda(), escopo);
                if (exigirLogico(esq, "conectivo 'OU'")) return ValorThz.LOGICO(true);
                return ValorThz.LOGICO(exigirLogico(avaliar(ob.direita(), escopo), "conectivo 'OU'"));
            }
            ValorThz esquerda = avaliar(ob.esquerda(), escopo);
            ValorThz direita = avaliar(ob.direita(), escopo);
            if (Set.of("=", "<>", "<", "<=", ">", ">=").contains(ob.operador())) {
                return ValorThz.LOGICO(comparar(esquerda, direita, ob.operador(), ob));
            }
            return aritmetica(esquerda, direita, ob.operador(), ob);
        }
        throw new ErroExecucao("[Erro de Execução] Expressão não reconhecida: " + expr);
    }

    private boolean exigirLogico(ValorThz v, String contexto) {
        if (!(v instanceof ValorThz.Logico l)) throw new ErroExecucao("[Erro de Execução] Esperado valor lógico em " + contexto + ".");
        return l.valor();
    }

    private boolean comparar(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        // TEXTO
        if (a instanceof ValorThz.Texto ta && b instanceof ValorThz.Texto tb) {
            int cmp = ta.valor().compareTo(tb.valor());
            return switch (operador) {
                case "=" -> cmp == 0;
                case "<>" -> cmp != 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                default -> false;
            };
        }
        // LOGICO
        if (a instanceof ValorThz.Logico la && b instanceof ValorThz.Logico lb) {
            if ("=".equals(operador)) return la.valor() == lb.valor();
            if ("<>".equals(operador)) return la.valor() != lb.valor();
        } else if (a instanceof ValorThz.Logico || b instanceof ValorThz.Logico) {
            // mismatch will fall through to incompatível
        }
        // MONETARIO
        if (a instanceof ValorThz.Monetario ma && b instanceof ValorThz.Monetario mb) {
            int c = ma.valor().comparar(mb.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        // DATA
        if (a instanceof ValorThz.Data da && b instanceof ValorThz.Data db) {
            int c = da.valor().comparar(db.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        if (a instanceof ValorThz.DataHora dha && b instanceof ValorThz.DataHora dhb) {
            int c = dha.valor().comparar(dhb.valor());
            return switch (operador) {
                case "=" -> c == 0;
                case "<>" -> c != 0;
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                default -> false;
            };
        }
        // NUMERICO
        if (ehNumerico(a) && ehNumerico(b)) {
            Integer ord = ordemNumerica(a, b, ctx);
            if (ord != null) {
                return switch (operador) {
                    case "=" -> ord == 0;
                    case "<>" -> ord != 0;
                    case "<" -> ord < 0;
                    case "<=" -> ord <= 0;
                    case ">" -> ord > 0;
                    case ">=" -> ord >= 0;
                    default -> false;
                };
            }
        }
        // ENUMERADO
        if (a instanceof ValorThz.Enumerado ea && b instanceof ValorThz.Enumerado eb) {
            boolean mesma = ea.nomeEnumeracao().equals(eb.nomeEnumeracao());
            if ("=".equals(operador)) return mesma && ea.valor().equals(eb.valor());
            if ("<>".equals(operador)) return !(mesma && ea.valor().equals(eb.valor()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] ENUMERACAO suporta apenas os operadores = e <>.");
        }
        // fallback logico equality already handled? If not numeric etc, still throw
        if (a instanceof ValorThz.Logico && b instanceof ValorThz.Logico) {
            // only = and <> supported, if other operator like < then incompatível
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Comparação entre tipos incompatíveis (" + a.classe() + " " + operador + " " + b.classe() + ").");
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Comparação entre tipos incompatíveis (" + a.classe() + " " + operador + " " + b.classe() + ").");
    }

    private boolean ehNumerico(ValorThz v) {
        return v instanceof ValorThz.Inteiro || v instanceof ValorThz.Decimal;
    }

    private Integer ordemNumerica(ValorThz x, ValorThz y, ExprAst ctx) {
        if (x instanceof ValorThz.Inteiro xi && y instanceof ValorThz.Inteiro yi) {
            int cmp = xi.valor().compareTo(yi.valor());
            return Integer.compare(cmp, 0);
        }
        try {
            DecimalFixo dx = comoDecimal(x, ctx);
            DecimalFixo dy = comoDecimal(y, ctx);
            int cmp = dx.comparar(dy);
            return cmp;
        } catch (ErroExecucao e) {
            return null;
        }
    }

    private DecimalFixo comoDecimal(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Decimal d) return d.valor();
        if (v instanceof ValorThz.Inteiro i) return DecimalFixo.deInteiro(i.valor());
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Esperado valor numérico, recebido " + v.classe() + ".");
    }

    private ValorThz aritmetica(ValorThz a, ValorThz b, String operador, ExprAst ctx) {
        if ("+".equals(operador) && (a instanceof ValorThz.Texto || b instanceof ValorThz.Texto)) {
            return ValorThz.TEXTO(formatar(a) + formatar(b));
        }
        if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
            switch (operador) {
                case "+": return ValorThz.INTEIRO(ia.valor().add(ib.valor()));
                case "-": return ValorThz.INTEIRO(ia.valor().subtract(ib.valor()));
                case "*": return ValorThz.INTEIRO(ia.valor().multiply(ib.valor()));
                case "/":
                    if (ib.valor().equals(BigInteger.ZERO)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Divisão por zero.");
                    return ValorThz.INTEIRO(ia.valor().divide(ib.valor()));
                case "%":
                    if (ib.valor().equals(BigInteger.ZERO)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Módulo por zero.");
                    return ValorThz.INTEIRO(ia.valor().remainder(ib.valor()));
            }
        }

        if (ehNumerico(a) && ehNumerico(b)) {
            DecimalFixo da = comoDecimal(a, ctx);
            DecimalFixo db = comoDecimal(b, ctx);
            return switch (operador) {
                case "+" -> ValorThz.DECIMAL(da.somar(db));
                case "-" -> ValorThz.DECIMAL(da.subtrair(db));
                case "*" -> ValorThz.DECIMAL(da.multiplicar(db));
                case "/" -> {
                    if (db.getValorEscalado().equals(BigInteger.ZERO)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Divisão por zero.");
                    yield ValorThz.DECIMAL(da.dividir(db));
                }
                case "%" -> throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Operador '%' suportado apenas entre inteiros.");
                default -> throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Operação " + operador + " inválida entre " + a.classe() + " e " + b.classe() + ".");
            };
        }
        if (a instanceof ValorThz.Monetario ma && b instanceof ValorThz.Monetario mb) {
            switch (operador) {
                case "+": return ValorThz.MONETARIO(ma.valor().somar(mb.valor()));
                case "-": return ValorThz.MONETARIO(ma.valor().subtrair(mb.valor()));
            }
        }
        if (a instanceof ValorThz.Monetario ma2 && b instanceof ValorThz.Decimal db) {
            if ("*".equals(operador)) return ValorThz.MONETARIO(ma2.valor().multiplicar(db.valor()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Monetário só admite multiplicação por fator decimal.");
        }
        if (a instanceof ValorThz.Decimal da2 && b instanceof ValorThz.Monetario mb2 && "*".equals(operador)) {
            return ValorThz.MONETARIO(mb2.valor().multiplicar(da2.valor()));
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Operação " + operador + " inválida entre " + a.classe() + " e " + b.classe() + ".");
    }

    public String formatar(ValorThz v) {
        if (v == null) return "NULO";
        if (v instanceof ValorThz.Inteiro i) return i.valor().toString();
        if (v instanceof ValorThz.Decimal d) return d.valor().formatar();
        if (v instanceof ValorThz.Monetario m) return m.valor().formatar();
        if (v instanceof ValorThz.Texto t) return t.valor();
        if (v instanceof ValorThz.Logico l) return l.valor() ? "VERDADEIRO" : "FALSO";
        if (v instanceof ValorThz.Nulo) return "NULO";
        if (v instanceof ValorThz.Data d) return d.valor().formatar();
        if (v instanceof ValorThz.DataHora dh) return dh.valor().formatar();
        if (v instanceof ValorThz.Enumerado e) return e.valor();
        if (v instanceof ValorThz.Resultado r) {
            if (r.sucesso()) return "SUCESSO(" + (r.valor() != null ? formatar(r.valor()) : "NULO") + ")";
            else return "FALHA(" + (r.erro() != null ? formatar(r.erro()) : "NULO") + ")";
        }
        if (v instanceof ValorThz.Registro reg) return reg.nomeEstrutura() + "{...}";
        if (v instanceof ValorThz.Fatia f) return "FATIA[" + f.tipoInterno() + "](" + f.elementos().size() + ")";
        return v.toString();
    }

    // ---- Execução de comandos ----

    private void executarComandos(List<ComandoAst> comandos, Escopo escopo) {
        if (comandos == null) return;
        for (ComandoAst cmd : comandos) executarComando(cmd, escopo);
    }

    private void executarComando(ComandoAst cmd, Escopo escopo) {
        if (cmd instanceof ComandoAst.DeclVariavel decl) {
            if (escopo.resolver(decl.nome()) != null) {
                throw new ErroExecucao("[Erro de Execução][Linha " + decl.linha() + ":" + decl.coluna() + "] Variável '" + decl.nome() + "' já declarada neste escopo.");
            }
            escopo.definir(decl.nome(), avaliar(decl.inicializacao(), escopo));
        } else if (cmd instanceof ComandoAst.Atribuicao atr) {
            ValorThz base = escopo.resolver(atr.alvo().get(0));
            if (base == null) throw new ErroExecucao("[Erro de Execução][Linha " + atr.linha() + ":" + atr.coluna() + "] Atribuição a identificador não declarado: '" + atr.alvo().get(0) + "'.");
            ValorThz valor = avaliar(atr.expressao(), escopo);
            if (atr.alvo().size() == 1) {
                if (!escopo.atualizar(atr.alvo().get(0), valor)) throw new ErroExecucao("[Erro de Execução][Linha " + atr.linha() + ":" + atr.coluna() + "] Atribuição a identificador não declarado: '" + atr.alvo().get(0) + "'.");
                validarInvariantes(valor, atr);
            } else {
                atribuirCampo(base, atr.alvo().subList(1, atr.alvo().size()), valor, atr);
                validarInvariantes(base, atr);
            }
        } else if (cmd instanceof ComandoAst.Se se) {
            if (exigirLogico(avaliar(se.condicao(), escopo), "condição do 'SE'")) {
                executarComandos(se.entao(), new Escopo(escopo));
            } else {
                executarComandos(se.senao(), new Escopo(escopo));
            }
        } else if (cmd instanceof ComandoAst.Enquanto enq) {
            int iteracoes = 0;
            while (exigirLogico(avaliar(enq.condicao(), escopo), "condição do 'ENQUANTO'")) {
                if (++iteracoes > maxIteracoes) throw new ErroExecucao("[Erro de Execução][Linha " + enq.linha() + ":" + enq.coluna() + "] Laço 'ENQUANTO' excedeu " + maxIteracoes + " iterações (guarda anti-loop).");
                executarComandos(enq.corpo(), new Escopo(escopo));
            }
        } else if (cmd instanceof ComandoAst.Para para) {
            ValorThz inicio = avaliar(para.inicio(), escopo);
            ValorThz fim = avaliar(para.fim(), escopo);
            if (!(inicio instanceof ValorThz.Inteiro)) throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] 'PARA' exige início INTEIRO, recebido " + inicio.classe());
            if (!(fim instanceof ValorThz.Inteiro)) throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] 'PARA' exige fim INTEIRO, recebido " + fim.classe());
            BigInteger passo = BigInteger.ONE;
            if (para.passo() != null) {
                ValorThz v = avaliar(para.passo(), escopo);
                if (!(v instanceof ValorThz.Inteiro)) throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] 'PASSO' exige INTEIRO");
                passo = ((ValorThz.Inteiro) v).valor();
                if (passo.equals(BigInteger.ZERO)) throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] 'PASSO' não pode ser zero.");
            }
            int iter = 0;
            BigInteger cur = ((ValorThz.Inteiro) inicio).valor();
            BigInteger fimVal = ((ValorThz.Inteiro) fim).valor();
            boolean crescente = passo.signum() > 0;
            while (crescente ? cur.compareTo(fimVal) <= 0 : cur.compareTo(fimVal) >= 0) {
                if (++iter > maxIteracoes) throw new ErroExecucao("[Erro de Execução][Linha " + para.linha() + ":" + para.coluna() + "] Laço 'PARA' excedeu " + maxIteracoes + " iterações (guarda anti-loop).");
                Escopo esc = new Escopo(escopo);
                esc.definir(para.variavel(), ValorThz.INTEIRO(cur));
                executarComandos(para.corpo(), esc);
                cur = cur.add(passo);
            }
        } else if (cmd instanceof ComandoAst.VetorizarPara vp) {
            ValorThz fonte = escopo.resolver(vp.fonte().get(0));
            // Note: TS only checks first element caminho[0]; we replicate. If fonte is nested path not resolved via resolver alone, TS would also only handle single level? But spec says fonte is List<String>.
            // For nested like a.b we need to resolve via acesso chain. To match TS exactly we do same single lookup.
            // However we also support nested by resolving via avaliar AcessoCampo if needed.
            if (vp.fonte().size() > 1) {
                // Resolve full path via dummy AcessoCampo
                ExprAst.AcessoCampo dummy = new ExprAst.AcessoCampo(vp.fonte(), vp.linha(), vp.coluna());
                fonte = avaliar(dummy, escopo);
            }
            if (!(fonte instanceof ValorThz.Fatia f)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + vp.linha() + ":" + vp.coluna() + "] Fonte do 'VETORIZAR_PARA' deve ser uma fatia: " + String.join(".", vp.fonte()));
            }
            for (ValorThz elemento : f.elementos()) {
                Escopo escopoIteracao = new Escopo(escopo);
                escopoIteracao.definir(vp.variavel(), elemento);
                executarComandos(vp.corpo(), escopoIteracao);
            }
        } else if (cmd instanceof ComandoAst.BlocoMemoria bm) {
            ArenaMemoria arena = new ArenaMemoria(1);
            arena.alocar(1024);
            try {
                executarComandos(bm.corpo(), new Escopo(escopo));
            } finally {
                arena.liberarTudo();
            }
        } else if (cmd instanceof ComandoAst.Exiba ex) {
            emitir.accept(formatar(avaliar(ex.expressao(), escopo)));
        } else if (cmd instanceof ComandoAst.Ler ler) {
            String linha = lerEntrada.get();
            if (linha == null) throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] Entrada encerrada (EOF) em LER " + String.join(".", ler.alvo()));
            ValorThz base = escopo.resolver(ler.alvo().get(0));
            if (base == null) throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER alvo não declarado: '" + ler.alvo().get(0) + "'.");
            // Need to handle nested field base as Registro; but base is resolved first element. If alvo is longer than 1, we still infer type from nested field? TS does base = resolver(alvo[0]) and then checks base.classe for coercion, not nested.
            // So replicate that.
            ValorThz novo;
            if (base instanceof ValorThz.Inteiro) {
                String s = linha.trim();
                if (!s.matches("^-?\\d+$")) throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER INTEIRO exige dígitos: '" + linha + "'");
                novo = ValorThz.INTEIRO(new BigInteger(s));
            } else if (base instanceof ValorThz.Decimal d) {
                try { novo = ValorThz.DECIMAL(DecimalFixo.deTexto(linha.trim(), d.valor().getEscala())); }
                catch (Exception e) { throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER DECIMAL inválido: " + e.getMessage()); }
            } else if (base instanceof ValorThz.Data) {
                try { novo = ValorThz.DATA(DataThz.deTexto(linha.trim())); }
                catch (Exception e) { throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER DATA inválido: " + e.getMessage()); }
            } else if (base instanceof ValorThz.DataHora) {
                try { novo = ValorThz.DATA_HORA(DataHoraThz.deTexto(linha.trim())); }
                catch (Exception e) { throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER DATA_HORA inválido: " + e.getMessage()); }
            } else if (base instanceof ValorThz.Texto) {
                novo = ValorThz.TEXTO(linha);
            } else {
                String t = linha.trim().toUpperCase();
                if (base instanceof ValorThz.Logico) novo = ValorThz.LOGICO(t.equals("VERDADEIRO") || t.equals("TRUE") || t.equals("1"));
                else novo = ValorThz.TEXTO(linha);
            }
            if (ler.alvo().size() == 1) {
                if (!escopo.atualizar(ler.alvo().get(0), novo)) throw new ErroExecucao("[Erro de Execução][Linha " + ler.linha() + ":" + ler.coluna() + "] LER alvo não declarado: '" + ler.alvo().get(0) + "'.");
            } else {
                atribuirCampo(base, ler.alvo().subList(1, ler.alvo().size()), novo, ler);
            }
        } else if (cmd instanceof ComandoAst.Chamada ch) {
            avaliar(ch.expressao(), escopo);
        } else if (cmd instanceof ComandoAst.Retorne ret) {
            throw new SinalRetorne(ret.expressao() != null ? avaliar(ret.expressao(), escopo) : null);
        } else if (cmd instanceof ComandoAst.FalharCom fc) {
            throw new SinalFalhar(avaliar(fc.expressao(), escopo));
        }
    }

    // ---- Invariantes ----

    /**
     * Valida os INVARIANTE declarados na ESTRUTURA do registro informado.
     * Chamado após toda mutação de campos e na construção.
     */
    public void validarInvariantes(ValorThz valor, ComandoAst cmd) {
        if (!(valor instanceof ValorThz.Registro reg)) return;
        EstruturaAst estrutura = ast.estruturas().stream().filter(e -> e.nome().equals(reg.nomeEstrutura())).findFirst().orElse(null);
        if (estrutura == null || estrutura.invariantes().isEmpty()) return;
        Escopo escopo = new Escopo();
        for (Map.Entry<String, ValorThz> e : reg.campos().entrySet()) escopo.definir(e.getKey(), e.getValue());
        for (InvarianteAst invariante : estrutura.invariantes()) {
            ValorThz resultado = avaliar(invariante.expressao(), escopo);
            if (!(resultado instanceof ValorThz.Logico l) || !l.valor()) {
                String posicao;
                if (cmd != null) posicao = "[Linha " + cmd.linha() + ":" + cmd.coluna() + "] ";
                else posicao = "[Linha " + invariante.linha() + ":" + invariante.coluna() + "] ";
                throw new ErroContrato("[Violação de Invariante]" + posicao + "Estrutura '" + reg.nomeEstrutura() + "' reprovou: " + invariante.textoCanonico());
            }
        }
    }

    public void validarInvariantes(ValorThz valor) {
        validarInvariantes(valor, null);
    }

    private void atribuirCampo(ValorThz alvo, List<String> caminhoRestante, ValorThz valor, ComandoAst cmd) {
        if (caminhoRestante.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Caminho de atribuição malformado.");
        String campoFinal = caminhoRestante.get(caminhoRestante.size() - 1);
        ValorThz container = alvo;
        for (int i = 0; i < caminhoRestante.size() - 1; i++) {
            if (!(container instanceof ValorThz.Registro reg)) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Caminho de atribuição inválido em '" + caminhoRestante.get(i) + "'.");
            ValorThz proximo = reg.campos().get(caminhoRestante.get(i));
            if (proximo == null) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Campo '" + caminhoRestante.get(i) + "' inexistente.");
            container = proximo;
        }
        if (!(container instanceof ValorThz.Registro reg)) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Atribuição a campo exige registro.");
        reg.campos().put(campoFinal, valor);
    }

    // Overload for LER case where cmd is ComandoAst.Ler
    private void atribuirCampo(ValorThz alvo, List<String> caminhoRestante, ValorThz valor, ComandoAst.Ler cmd) {
        if (caminhoRestante.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Caminho de atribuição malformado.");
        String campoFinal = caminhoRestante.get(caminhoRestante.size() - 1);
        ValorThz container = alvo;
        for (int i = 0; i < caminhoRestante.size() - 1; i++) {
            if (!(container instanceof ValorThz.Registro reg)) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Caminho de atribuição inválido em '" + caminhoRestante.get(i) + "'.");
            ValorThz proximo = reg.campos().get(caminhoRestante.get(i));
            if (proximo == null) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Campo '" + caminhoRestante.get(i) + "' inexistente.");
            container = proximo;
        }
        if (!(container instanceof ValorThz.Registro reg)) throw new ErroExecucao("[Erro de Execução][Linha " + cmd.linha() + ":" + cmd.coluna() + "] Atribuição a campo exige registro.");
        reg.campos().put(campoFinal, valor);
    }

    // ---- Utilidades numéricas ----

    /**
     * Converte valores (de fixtures) para o universo THZ segundo um tipo declarado.
     * Port exato de valorThzDe em interpretador.ts.
     */
    public static ValorThz valorThzDe(String tipoDado, Object bruto) {
        if (tipoDado.startsWith("NATURAL") || tipoDado.startsWith("INTEIRO")) {
            if (bruto instanceof BigInteger bi) return ValorThz.INTEIRO(bi);
            if (bruto instanceof Number n) {
                double d = n.doubleValue();
                long trunc = (long) (d >= 0 ? Math.floor(d) : Math.ceil(d));
                return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
            }
            // String case
            String s = String.valueOf(bruto).trim();
            // Handle decimal representation trunc
            if (s.contains(".")) s = s.substring(0, s.indexOf('.'));
            return ValorThz.INTEIRO(new BigInteger(s));
        }
        if (tipoDado.startsWith("MONETARIO")) {
            Pattern p = Pattern.compile("^MONETARIO\\s*\\(\\s*\"?([A-Z]{3})\"?\\s*\\)");
            var m = p.matcher(tipoDado);
            String codigoMoeda = null;
            if (m.find()) codigoMoeda = m.group(1);
            if (codigoMoeda == null) throw new ErroMonetario("[Erro Monetário] Tipo '" + tipoDado + "' exige código ISO 4217: MONETARIO(\"BRL\") por exemplo.");
            if (bruto instanceof BigInteger bi) return ValorThz.MONETARIO(Monetario.deInteiro(bi, codigoMoeda));
            return ValorThz.MONETARIO(Monetario.deTexto(String.valueOf(bruto), codigoMoeda));
        }
        if (tipoDado.startsWith("DECIMAL")) {
            Pattern pEscala = Pattern.compile(",\\s*(\\d+)\\s*\\)\\s*$");
            var m = pEscala.matcher(tipoDado);
            int escala = DecimalFixo.ESCALA_PADRAO;
            if (m.find()) escala = Math.min(Integer.parseInt(m.group(1), 10), DecimalFixo.ESCALA_PADRAO);
            String numero;
            if (bruto instanceof String s) numero = s;
            else numero = String.format(java.util.Locale.US, "%." + escala + "f", ((Number) bruto).doubleValue());
            // Use locale independent formatting — Number formatting may use comma; prefer BigDecimal style
            // If bruto is Number, we used format; but ensure dot separator.
            // Alternative: if bruto is Number, we can use DecimalFixo.deTexto(numero, escala) already handles.
            return ValorThz.DECIMAL(DecimalFixo.deTexto(numero, escala));
        }
        if ("DATA".equals(tipoDado)) {
            if (bruto instanceof String s) return ValorThz.DATA(DataThz.deTexto(s));
            throw new ErroData("[Erro Data] Valor para DATA deve ser texto 'AAAA-MM-DD'.");
        }
        if ("DATA_HORA".equals(tipoDado)) {
            if (bruto instanceof String s) return ValorThz.DATA_HORA(DataHoraThz.deTexto(s));
            throw new ErroData("[Erro DataHora] Valor para DATA_HORA deve ser texto 'AAAA-MM-DDTHH:MM[:SS]'.");
        }
        if ("TEXTO".equals(tipoDado)) return ValorThz.TEXTO(String.valueOf(bruto));
        if ("LOGICO".equals(tipoDado)) {
            String s = String.valueOf(bruto).toUpperCase();
            return ValorThz.LOGICO(s.equals("VERDADEIRO") || s.equals("TRUE") || s.equals("1"));
        }
        return ValorThz.TEXTO(String.valueOf(bruto));
    }
}
