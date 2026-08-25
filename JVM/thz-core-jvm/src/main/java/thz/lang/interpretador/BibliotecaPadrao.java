package thz.lang.interpretador;

import thz.lang.ast.ExprAst;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.ErroData;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Biblioteca padrão da THZ-LANG (32 funções integradas: TEXTO, MATEMATICA, DATA e TELA).
 * Decomposto do InterpretadorThz para respeitar o princípio de responsabilidade única (SRP).
 */
public final class BibliotecaPadrao {

    @FunctionalInterface
    public interface FuncaoStdlib {
        ValorThz apply(List<ValorThz> args, ExprAst ctx, InterpretadorThz interp);
    }

    @FunctionalInterface
    public interface FuncaoSimplesStdlib {
        ValorThz apply(List<ValorThz> args, ExprAst ctx);
    }

    private static final Map<String, FuncaoStdlib> FUNCOES = criarStdlib();

    private BibliotecaPadrao() {}

    public static boolean ehStdlib(String nome) {
        return nome != null && FUNCOES.containsKey(nome);
    }

    public static ValorThz executar(String nome, List<ValorThz> args, ExprAst ctx) {
        return executar(nome, args, ctx, null);
    }

    public static ValorThz executar(String nome, List<ValorThz> args, ExprAst ctx, InterpretadorThz interp) {
        FuncaoStdlib fn = FUNCOES.get(nome);
        if (fn == null) {
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Função de biblioteca desconhecida: '" + nome + "'.");
        }
        return fn.apply(args, ctx, interp);
    }

    /**
     * Ponto de extensão da stdlib: módulos autônomos (thz-gui, thz-cli) registram aqui
     * suas funções nativas (ex.: TELA.*), mantendo o core livre de dependências superiores.
     */
    public static void registrar(String nome, FuncaoStdlib fn) {
        FUNCOES.put(nome, fn);
    }

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

    private static float[] extrairVetorArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Texto t) {
            return thz.lang.vetor.ThzVetorSimd.parseVetor(t.valor());
        }
        if (v instanceof ValorThz.Fatia fatia) {
            float[] res = new float[fatia.elementos().size()];
            for (int i = 0; i < fatia.elementos().size(); i++) {
                ValorThz elem = fatia.elementos().get(i);
                if (elem instanceof ValorThz.Decimal d) res[i] = Float.parseFloat(d.valor().formatar());
                else if (elem instanceof ValorThz.Inteiro in) res[i] = in.valor().floatValue();
                else if (elem instanceof ValorThz.Texto tx) res[i] = Float.parseFloat(tx.valor());
            }
            return res;
        }
        throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Esperado TEXTO formatado de vetor ou FATIA numérica, recebido " + v.classe() + ".");
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

    private static void registrar(Map<String, FuncaoStdlib> m, String nome, FuncaoSimplesStdlib fn) {
        m.put(nome, (args, ctx, interp) -> fn.apply(args, ctx));
    }

    private static void registrar(Map<String, FuncaoStdlib> m, String nome, FuncaoStdlib fn) {
        m.put(nome, fn);
    }

    private static Map<String, FuncaoStdlib> criarStdlib() {
        Map<String, FuncaoStdlib> m = new HashMap<>();

        // ---- TEXTO ----
        registrar(m, "TEXTO.comprimento", (args, ctx) -> {
            exigirAridade("TEXTO.comprimento", args, 1, ctx);
            exigirClasse("TEXTO.comprimento", args.get(0), "TEXTO", ctx);
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Texto) args.get(0)).valor().length()));
        });
        registrar(m, "TEXTO.maiusculas", (args, ctx) -> {
            exigirAridade("TEXTO.maiusculas", args, 1, ctx);
            exigirClasse("TEXTO.maiusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toUpperCase());
        });
        registrar(m, "TEXTO.minusculas", (args, ctx) -> {
            exigirAridade("TEXTO.minusculas", args, 1, ctx);
            exigirClasse("TEXTO.minusculas", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().toLowerCase());
        });
        registrar(m, "TEXTO.aparar", (args, ctx) -> {
            exigirAridade("TEXTO.aparar", args, 1, ctx);
            exigirClasse("TEXTO.aparar", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(((ValorThz.Texto) args.get(0)).valor().trim());
        });
        registrar(m, "TEXTO.contem", (args, ctx) -> {
            if (args.size() != 2)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.contem exige 2 args");
            exigirClasse("TEXTO.contem", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.contem", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(((ValorThz.Texto) args.get(0)).valor().contains(((ValorThz.Texto) args.get(1)).valor()));
        });
        registrar(m, "TEXTO.subtexto", (args, ctx) -> {
            if (args.size() < 2 || args.size() > 3)
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TEXTO.subtexto exige 2 ou 3 args (texto, inicio, [fim])");
            exigirClasse("TEXTO.subtexto", args.get(0), "TEXTO", ctx);
            int ini = comoInteiroArg(args.get(1), ctx).intValue();
            Integer fim = args.size() == 3 ? comoInteiroArg(args.get(2), ctx).intValue() : null;
            String texto = ((ValorThz.Texto) args.get(0)).valor();
            return ValorThz.TEXTO(sliceTexto(texto, ini, fim));
        });
        registrar(m, "TEXTO.substituir", (args, ctx) -> {
            exigirAridade("TEXTO.substituir", args, 3, ctx);
            exigirClasse("TEXTO.substituir", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.substituir", args.get(1), "TEXTO", ctx);
            exigirClasse("TEXTO.substituir", args.get(2), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String alvo = ((ValorThz.Texto) args.get(1)).valor();
            String repl = ((ValorThz.Texto) args.get(2)).valor();
            if (alvo.isEmpty()) {
                List<String> chars = new ArrayList<>();
                for (char c : base.toCharArray()) chars.add(String.valueOf(c));
                return ValorThz.TEXTO(String.join(repl, chars));
            }
            return ValorThz.TEXTO(base.replace(alvo, repl));
        });
        registrar(m, "TEXTO.dividir", (args, ctx) -> {
            exigirAridade("TEXTO.dividir", args, 2, ctx);
            exigirClasse("TEXTO.dividir", args.get(0), "TEXTO", ctx);
            exigirClasse("TEXTO.dividir", args.get(1), "TEXTO", ctx);
            String base = ((ValorThz.Texto) args.get(0)).valor();
            String sep = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> partes;
            if (sep.isEmpty()) {
                partes = new ArrayList<>();
                for (char c : base.toCharArray()) partes.add(ValorThz.TEXTO(String.valueOf(c)));
                if (base.isEmpty()) partes = List.of();
            } else {
                String[] arr = base.split(Pattern.quote(sep), -1);
                partes = new ArrayList<>();
                for (String p : arr) partes.add(ValorThz.TEXTO(p));
            }
            return new ValorThz.Fatia("TEXTO", List.copyOf(partes));
        });
        registrar(m, "TEXTO.juntar", (args, ctx) -> {
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
        registrar(m, "TEXTO.de", (args, ctx) -> {
            exigirAridade("TEXTO.de", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        registrar(m, "TEXTO.deDecimal", (args, ctx) -> {
            exigirAridade("TEXTO.deDecimal", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        registrar(m, "TEXTO.deInteiro", (args, ctx) -> {
            exigirAridade("TEXTO.deInteiro", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        registrar(m, "TEXTO.deLogico", (args, ctx) -> {
            exigirAridade("TEXTO.deLogico", args, 1, ctx);
            return ValorThz.TEXTO(args.get(0).formatar());
        });

        // ---- FATIA (Coleções) ----
        registrar(m, "FATIA.tamanho", (args, ctx) -> {
            exigirAridade("FATIA.tamanho", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                return ValorThz.INTEIRO(BigInteger.valueOf(f.elementos().size()));
            }
            if (args.get(0) instanceof ValorThz.Texto t) {
                return ValorThz.INTEIRO(BigInteger.valueOf(t.valor().length()));
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.tamanho exige FATIA ou TEXTO.");
        });
        registrar(m, "FATIA.primeiro", (args, ctx) -> {
            exigirAridade("FATIA.primeiro", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                if (f.elementos().isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA vazia.");
                return f.elementos().get(0);
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.primeiro exige FATIA.");
        });
        registrar(m, "FATIA.ultimo", (args, ctx) -> {
            exigirAridade("FATIA.ultimo", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                if (f.elementos().isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA vazia.");
                return f.elementos().get(f.elementos().size() - 1);
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.ultimo exige FATIA.");
        });
        registrar(m, "FATIA.vazia", (args, ctx) -> {
            exigirAridade("FATIA.vazia", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Fatia f) {
                return ValorThz.LOGICO(f.elementos().isEmpty());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] FATIA.vazia exige FATIA.");
        });

        // ---- MATEMATICA ----
        registrar(m, "MATEMATICA.abs", (args, ctx) -> {
            exigirAridade("MATEMATICA.abs", args, 1, ctx);
            ValorThz v = args.get(0);
            if (v instanceof ValorThz.Inteiro i) {
                BigInteger val = i.valor();
                return ValorThz.INTEIRO(val.signum() < 0 ? val.negate() : val);
            }
            if (v instanceof ValorThz.Decimal d) return ValorThz.DECIMAL(d.valor().abs());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.abs exige numérico");
        });
        registrar(m, "MATEMATICA.min", (args, ctx) -> {
            exigirAridade("MATEMATICA.min", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) < 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.min exige dois INTEIROS");
        });
        registrar(m, "MATEMATICA.max", (args, ctx) -> {
            exigirAridade("MATEMATICA.max", args, 2, ctx);
            ValorThz a = args.get(0); ValorThz b = args.get(1);
            if (a instanceof ValorThz.Inteiro ia && b instanceof ValorThz.Inteiro ib) {
                return ValorThz.INTEIRO(ia.valor().compareTo(ib.valor()) > 0 ? ia.valor() : ib.valor());
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.max exige dois INTEIROS");
        });
        registrar(m, "MATEMATICA.potencia", (args, ctx) -> {
            exigirAridade("MATEMATICA.potencia", args, 2, ctx);
            double base = comoInteiroArg(args.get(0), ctx).doubleValue();
            double exp = comoInteiroArg(args.get(1), ctx).doubleValue();
            double pow = Math.pow(base, exp);
            if (Double.isNaN(pow) || Double.isInfinite(pow)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.potencia resultado inválido");
            }
            long trunc = (long) (pow >= 0 ? Math.floor(pow) : Math.ceil(pow));
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        registrar(m, "MATEMATICA.raiz", (args, ctx) -> {
            exigirAridade("MATEMATICA.raiz", args, 1, ctx);
            double n = comoInteiroArg(args.get(0), ctx).doubleValue();
            if (n < 0) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.raiz exige não-negativo");
            double s = Math.sqrt(n);
            long trunc = (long) Math.floor(s);
            return ValorThz.INTEIRO(BigInteger.valueOf(trunc));
        });
        registrar(m, "MATEMATICA.arredondar", (args, ctx) -> {
            if (args.size() != 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige 2 args");
            ValorThz d = args.get(0);
            if (!(d instanceof ValorThz.Decimal)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.arredondar exige DECIMAL");
            int casas = comoInteiroArg(args.get(1), ctx).intValue();
            return ValorThz.DECIMAL(((ValorThz.Decimal) d).valor().paraEscala(casas));
        });
        registrar(m, "MATEMATICA.aleatorio", (args, ctx) -> {
            if (args.size() != 1) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MATEMATICA.aleatorio exige 1 arg (limite)");
            double lim = comoInteiroArg(args.get(0), ctx).doubleValue();
            long r = (long) Math.floor(Math.random() * lim);
            return ValorThz.INTEIRO(BigInteger.valueOf(r));
        });

        // ---- DATA ----
        registrar(m, "DATA.hoje", (args, ctx) -> {
            exigirAridade("DATA.hoje", args, 0, ctx);
            LocalDate agora = LocalDate.now();
            return ValorThz.DATA(DataThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth()));
        });
        registrar(m, "DATA.agora", (args, ctx) -> {
            exigirAridade("DATA.agora", args, 0, ctx);
            LocalDateTime agora = LocalDateTime.now();
            return ValorThz.DATA_HORA(DataHoraThz.deComponentes(agora.getYear(), agora.getMonthValue(), agora.getDayOfMonth(), agora.getHour(), agora.getMinute(), agora.getSecond()));
        });
        registrar(m, "DATA.criar", (args, ctx) -> {
            exigirAridade("DATA.criar", args, 3, ctx);
            int a = comoInteiroArg(args.get(0), ctx).intValue();
            int mes = comoInteiroArg(args.get(1), ctx).intValue();
            int d = comoInteiroArg(args.get(2), ctx).intValue();
            try { return ValorThz.DATA(DataThz.deComponentes(a, mes, d)); }
            catch (ErroData e) { throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage()); }
        });
        registrar(m, "DATA.criarDataHora", (args, ctx) -> {
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
        registrar(m, "DATA.adicionarDias", (args, ctx) -> {
            exigirAridade("DATA.adicionarDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarDias exige DATA");
            BigInteger dias = comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA(((ValorThz.Data) args.get(0)).valor().adicionarDias(dias));
        });
        registrar(m, "DATA.adicionarHoras", (args, ctx) -> {
            exigirAridade("DATA.adicionarHoras", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.DataHora)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.adicionarHoras exige DATA_HORA");
            BigInteger h = comoInteiroArg(args.get(1), ctx);
            return ValorThz.DATA_HORA(((ValorThz.DataHora) args.get(0)).valor().adicionarHoras(h));
        });
        registrar(m, "DATA.diferencaDias", (args, ctx) -> {
            exigirAridade("DATA.diferencaDias", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Data) || !(args.get(1) instanceof ValorThz.Data))
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diferencaDias exige duas DATA");
            return ValorThz.INTEIRO(((ValorThz.Data) args.get(0)).valor().diferencaDias(((ValorThz.Data) args.get(1)).valor()));
        });
        registrar(m, "DATA.ano", (args, ctx) -> {
            exigirAridade("DATA.ano", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getAno()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getAno()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.ano exige DATA ou DATA_HORA");
        });
        registrar(m, "DATA.mes", (args, ctx) -> {
            exigirAridade("DATA.mes", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getMes()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getMes()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.mes exige DATA ou DATA_HORA");
        });
        registrar(m, "DATA.dia", (args, ctx) -> {
            exigirAridade("DATA.dia", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.INTEIRO(BigInteger.valueOf(d.valor().getDia()));
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.INTEIRO(BigInteger.valueOf(dh.valor().getData().getDia()));
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.dia exige DATA ou DATA_HORA");
        });
        registrar(m, "DATA.diaDaSemana", (args, ctx) -> {
            exigirAridade("DATA.diaDaSemana", args, 1, ctx);
            if (!(args.get(0) instanceof ValorThz.Data)) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.diaDaSemana exige DATA");
            return ValorThz.INTEIRO(BigInteger.valueOf(((ValorThz.Data) args.get(0)).valor().diaDaSemana()));
        });
        registrar(m, "DATA.texto", (args, ctx) -> {
            exigirAridade("DATA.texto", args, 1, ctx);
            if (args.get(0) instanceof ValorThz.Data d) return ValorThz.TEXTO(d.valor().formatar());
            if (args.get(0) instanceof ValorThz.DataHora dh) return ValorThz.TEXTO(dh.valor().formatar());
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DATA.texto exige DATA ou DATA_HORA");
        });

        // ---- TELA ----
        // As funções TELA.* são registradas pelos módulos de apresentação:
        //  - thz-gui (BibliotecaTela): formulários e diálogos Swing;
        //  - thz-cli (BibliotecaConsole): equivalentes de console/headless.
        // O core permanece autônomo, sem dependência de GUI.

        // ---- DOCUMENTO ----
        registrar(m, "DOCUMENTO.exportar", (args, ctx, interp) -> {
            exigirAridade("DOCUMENTO.exportar", args, 3, ctx);
            exigirClasse("DOCUMENTO.exportar", args.get(0), "TEXTO", ctx);
            exigirClasse("DOCUMENTO.exportar", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportar(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar documento: " + e.getMessage());
            }
        });

        registrar(m, "DOCUMENTO.exportarPdf", (args, ctx, interp) -> {
            exigirAridade("DOCUMENTO.exportarPdf", args, 3, ctx);
            exigirClasse("DOCUMENTO.exportarPdf", args.get(0), "TEXTO", ctx);
            exigirClasse("DOCUMENTO.exportarPdf", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarPdf(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar PDF: " + e.getMessage());
            }
        });

        registrar(m, "DOCUMENTO.exportarXlsx", (args, ctx, interp) -> {
            exigirAridade("DOCUMENTO.exportarXlsx", args, 3, ctx);
            exigirClasse("DOCUMENTO.exportarXlsx", args.get(0), "TEXTO", ctx);
            exigirClasse("DOCUMENTO.exportarXlsx", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String nomeAba = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarXlsx(java.nio.file.Path.of(caminho), nomeAba, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar planilha Excel (XLSX): " + e.getMessage());
            }
        });

        registrar(m, "DOCUMENTO.exportarDocx", (args, ctx, interp) -> {
            exigirAridade("DOCUMENTO.exportarDocx", args, 3, ctx);
            exigirClasse("DOCUMENTO.exportarDocx", args.get(0), "TEXTO", ctx);
            exigirClasse("DOCUMENTO.exportarDocx", args.get(1), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String titulo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz dados = args.get(2);
            try {
                java.nio.file.Path resultado = thz.lang.documento.MotorDocumentos.exportarDocx(java.nio.file.Path.of(caminho), titulo, dados);
                return ValorThz.TEXTO(resultado.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao exportar documento Word (DOCX): " + e.getMessage());
            }
        });

        // ---------------- VERSAO ----------------
        registrar(m, "VERSAO.obter", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.version.ThzVersion.ATUAL.toString());
        });
        registrar(m, "VERSAO.satisfaz", (args, ctx, interp) -> {
            exigirAridade("VERSAO.satisfaz", args, 2, ctx);
            exigirClasse("VERSAO.satisfaz", args.get(0), "TEXTO", ctx);
            exigirClasse("VERSAO.satisfaz", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.version.ThzVersion.satisfaz(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });

        // ---------------- ARQUIVO & DIRETORIO ----------------
        registrar(m, "ARQUIVO.localizar", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.localizar", args, 1, ctx);
            exigirClasse("ARQUIVO.localizar", args.get(0), "TEXTO", ctx);
            String termo = ((ValorThz.Texto) args.get(0)).valor();
            var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(termo, java.nio.file.Path.of("."), null);
            return opt.map(path -> ValorThz.TEXTO(path.toAbsolutePath().toString().replace("\\", "/"))).orElse(ValorThz.TEXTO(""));
        });
        registrar(m, "ARQUIVO.lerTexto", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.lerTexto", args, 1, ctx);
            exigirClasse("ARQUIVO.lerTexto", args.get(0), "TEXTO", ctx);
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            if (!thz.lang.io.ThzIO.existe(caminho)) {
                var opt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(caminho, java.nio.file.Path.of("."), null);
                if (opt.isPresent()) {
                    caminho = opt.get().toString();
                }
            }
            return ValorThz.TEXTO(thz.lang.io.ThzIO.lerTexto(caminho));
        });
        registrar(m, "ARQUIVO.escreverTexto", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.escreverTexto", args, 2, ctx);
            exigirClasse("ARQUIVO.escreverTexto", args.get(0), "TEXTO", ctx);
            exigirClasse("ARQUIVO.escreverTexto", args.get(1), "TEXTO", ctx);
            thz.lang.io.ThzIO.escreverTexto(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "ARQUIVO.anexarTexto", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.anexarTexto", args, 2, ctx);
            exigirClasse("ARQUIVO.anexarTexto", args.get(0), "TEXTO", ctx);
            exigirClasse("ARQUIVO.anexarTexto", args.get(1), "TEXTO", ctx);
            thz.lang.io.ThzIO.anexarTexto(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "ARQUIVO.existe", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.existe", args, 1, ctx);
            exigirClasse("ARQUIVO.existe", args.get(0), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.io.ThzIO.existe(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "ARQUIVO.remover", (args, ctx, interp) -> {
            exigirAridade("ARQUIVO.remover", args, 1, ctx);
            exigirClasse("ARQUIVO.remover", args.get(0), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.io.ThzIO.remover(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "DIRETORIO.listar", (args, ctx, interp) -> {
            exigirAridade("DIRETORIO.listar", args, 1, ctx);
            exigirClasse("DIRETORIO.listar", args.get(0), "TEXTO", ctx);
            List<String> itens = thz.lang.io.ThzIO.listarDiretorio(((ValorThz.Texto) args.get(0)).valor());
            List<ValorThz> res = new ArrayList<>();
            for (String item : itens) res.add(ValorThz.TEXTO(item));
            return new ValorThz.Fatia("TEXTO", res);
        });
        registrar(m, "DIRETORIO.criar", (args, ctx, interp) -> {
            exigirAridade("DIRETORIO.criar", args, 1, ctx);
            exigirClasse("DIRETORIO.criar", args.get(0), "TEXTO", ctx);
            thz.lang.io.ThzIO.criarDiretorio(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });

        // ---------------- CONFIG ----------------
        registrar(m, "CONFIG.obter", (args, ctx, interp) -> {
            if (args.size() == 1) {
                exigirClasse("CONFIG.obter", args.get(0), "TEXTO", ctx);
                return ValorThz.TEXTO(thz.lang.config.ThzConfig.obter(((ValorThz.Texto) args.get(0)).valor()));
            } else if (args.size() == 2) {
                exigirClasse("CONFIG.obter", args.get(0), "TEXTO", ctx);
                exigirClasse("CONFIG.obter", args.get(1), "TEXTO", ctx);
                return ValorThz.TEXTO(thz.lang.config.ThzConfig.obter(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor()));
            }
            throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] CONFIG.obter exige 1 ou 2 argumentos.");
        });
        registrar(m, "CONFIG.carregarEnv", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                thz.lang.config.ThzConfig.carregarEnvPadrao();
            } else {
                exigirClasse("CONFIG.carregarEnv", args.get(0), "TEXTO", ctx);
                thz.lang.config.ThzConfig.carregarArquivoEnv(((ValorThz.Texto) args.get(0)).valor());
            }
            return ValorThz.LOGICO(true);
        });
        registrar(m, "CONFIG.projeto.nome", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().nome());
        });
        registrar(m, "CONFIG.projeto.versao", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().versao());
        });
        registrar(m, "CONFIG.projeto.autor", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().autor());
        });
        registrar(m, "CONFIG.projeto.dialeto", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.config.ThzProjectConfig.obterConfig().projeto().dialeto());
        });

        // ---------------- SEGURANCA ----------------
        registrar(m, "SEGURANCA.sha256", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.sha256", args, 1, ctx);
            exigirClasse("SEGURANCA.sha256", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.sha256(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "SEGURANCA.sha512", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.sha512", args, 1, ctx);
            exigirClasse("SEGURANCA.sha512", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.sha512(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "SEGURANCA.hmacSha256", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.hmacSha256", args, 2, ctx);
            exigirClasse("SEGURANCA.hmacSha256", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.hmacSha256", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hmacSha256(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.criptografarAes", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.criptografarAes", args, 2, ctx);
            exigirClasse("SEGURANCA.criptografarAes", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.criptografarAes", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.criptografarAes(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.descriptografarAes", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.descriptografarAes", args, 2, ctx);
            exigirClasse("SEGURANCA.descriptografarAes", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.descriptografarAes", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.descriptografarAes(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.hashSenha", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.hashSenha", args, 1, ctx);
            exigirClasse("SEGURANCA.hashSenha", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hashSenha(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "SEGURANCA.verificarSenha", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.verificarSenha", args, 2, ctx);
            exigirClasse("SEGURANCA.verificarSenha", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.verificarSenha", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.security.ThzSecurity.verificarSenha(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.argon2", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.argon2", args, 1, ctx);
            exigirClasse("SEGURANCA.argon2", args.get(0), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.hashArgon2(((ValorThz.Texto) args.get(0)).valor()));
        });
        registrar(m, "SEGURANCA.verificarArgon2", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.verificarArgon2", args, 2, ctx);
            exigirClasse("SEGURANCA.verificarArgon2", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.verificarArgon2", args.get(1), "TEXTO", ctx);
            return ValorThz.LOGICO(thz.lang.security.ThzSecurity.verificarArgon2(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.chacha20", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.chacha20", args, 2, ctx);
            exigirClasse("SEGURANCA.chacha20", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.chacha20", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.criptografarChaCha20(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.descriptografarChaCha20", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.descriptografarChaCha20", args, 2, ctx);
            exigirClasse("SEGURANCA.descriptografarChaCha20", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.descriptografarChaCha20", args.get(1), "TEXTO", ctx);
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.descriptografarChaCha20(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            ));
        });
        registrar(m, "SEGURANCA.cofreSalvar", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.cofreSalvar", args, 3, ctx);
            exigirClasse("SEGURANCA.cofreSalvar", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.cofreSalvar", args.get(1), "TEXTO", ctx);
            exigirClasse("SEGURANCA.cofreSalvar", args.get(2), "TEXTO", ctx);
            try {
                thz.lang.security.ThzVault.salvarTexto(
                        java.nio.file.Path.of(((ValorThz.Texto) args.get(0)).valor()),
                        ((ValorThz.Texto) args.get(1)).valor(),
                        ((ValorThz.Texto) args.get(2)).valor()
                );
                return ValorThz.LOGICO(true);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao salvar cofre: " + e.getMessage());
            }
        });
        registrar(m, "SEGURANCA.cofreLer", (args, ctx, interp) -> {
            exigirAridade("SEGURANCA.cofreLer", args, 2, ctx);
            exigirClasse("SEGURANCA.cofreLer", args.get(0), "TEXTO", ctx);
            exigirClasse("SEGURANCA.cofreLer", args.get(1), "TEXTO", ctx);
            try {
                String conteudo = thz.lang.security.ThzVault.lerTexto(
                        java.nio.file.Path.of(((ValorThz.Texto) args.get(0)).valor()),
                        ((ValorThz.Texto) args.get(1)).valor()
                );
                return ValorThz.TEXTO(conteudo);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao ler cofre: " + e.getMessage());
            }
        });
        registrar(m, "SEGURANCA.gerarToken", (args, ctx, interp) -> {
            int tamanho = args.isEmpty() ? 32 : ((ValorThz.Inteiro) args.get(0)).valor().intValue();
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.gerarToken(tamanho));
        });
        registrar(m, "SEGURANCA.uuid", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.security.ThzSecurity.gerarUuid());
        });

        // ---------------- VETOR (Álgebra & Busca Semântica SIMD) ----------------
        registrar(m, "VETOR.criar", (args, ctx, interp) -> {
            if (args.isEmpty()) return ValorThz.TEXTO("[]");
            if (args.get(0) instanceof ValorThz.Fatia fatia) {
                float[] v = new float[fatia.elementos().size()];
                for (int i = 0; i < fatia.elementos().size(); i++) {
                    ValorThz elem = fatia.elementos().get(i);
                    if (elem instanceof ValorThz.Decimal d) v[i] = Float.parseFloat(d.valor().formatar());
                    else if (elem instanceof ValorThz.Inteiro in) v[i] = in.valor().floatValue();
                    else if (elem instanceof ValorThz.Texto t) v[i] = Float.parseFloat(t.valor());
                }
                return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(v));
            }
            return ValorThz.TEXTO(args.get(0).formatar());
        });
        registrar(m, "VETOR.similaridadeCosseno", (args, ctx, interp) -> {
            exigirAridade("VETOR.similaridadeCosseno", args, 2, ctx);
            float[] a = extrairVetorArg(args.get(0), ctx);
            float[] b = extrairVetorArg(args.get(1), ctx);
            double sim = thz.lang.vetor.ThzVetorSimd.similaridadeCosseno(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", sim), 6));
        });
        registrar(m, "VETOR.distanciaEuclidiana", (args, ctx, interp) -> {
            exigirAridade("VETOR.distanciaEuclidiana", args, 2, ctx);
            float[] a = extrairVetorArg(args.get(0), ctx);
            float[] b = extrairVetorArg(args.get(1), ctx);
            double dist = thz.lang.vetor.ThzVetorSimd.distanciaEuclidiana(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", dist), 6));
        });
        registrar(m, "VETOR.produtoEscalar", (args, ctx, interp) -> {
            exigirAridade("VETOR.produtoEscalar", args, 2, ctx);
            float[] a = extrairVetorArg(args.get(0), ctx);
            float[] b = extrairVetorArg(args.get(1), ctx);
            double dot = thz.lang.vetor.ThzVetorSimd.produtoEscalar(a, b);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", dot), 6));
        });
        registrar(m, "VETOR.normalizar", (args, ctx, interp) -> {
            exigirAridade("VETOR.normalizar", args, 1, ctx);
            float[] a = extrairVetorArg(args.get(0), ctx);
            float[] norm = thz.lang.vetor.ThzVetorSimd.normalizar(a);
            return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(norm));
        });

        // ---------------- IA & ML ON-DEVICE (Zero Python) ----------------
        registrar(m, "IA.embedding", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] IA.embedding exige o texto como 1º argumento.");
            exigirClasse("IA.embedding", args.get(0), "TEXTO", ctx);
            String texto = ((ValorThz.Texto) args.get(0)).valor();
            int dim = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().intValue() : thz.lang.ia.ThzIaEngine.DIMENSAO_PADRAO;
            float[] emb = thz.lang.ia.ThzIaEngine.gerarEmbedding(texto, dim);
            return ValorThz.TEXTO(thz.lang.vetor.ThzVetorSimd.formatarVetor(emb));
        });
        registrar(m, "IA.similaridade", (args, ctx, interp) -> {
            exigirAridade("IA.similaridade", args, 2, ctx);
            exigirClasse("IA.similaridade", args.get(0), "TEXTO", ctx);
            exigirClasse("IA.similaridade", args.get(1), "TEXTO", ctx);
            double sim = thz.lang.ia.ThzIaEngine.similaridadeSemantica(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            );
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", sim), 6));
        });
        registrar(m, "ML.classificar", (args, ctx, interp) -> {
            exigirAridade("ML.classificar", args, 3, ctx);
            float[] features = extrairVetorArg(args.get(0), ctx);
            float[] pesos = extrairVetorArg(args.get(1), ctx);
            float bias = args.get(2) instanceof ValorThz.Decimal d ? Float.parseFloat(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(2)).valor().floatValue();
            double prob = thz.lang.ia.ThzMlEngine.classificarProbabilidade(features, pesos, bias);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", prob), 6));
        });
        registrar(m, "ML.predizer", (args, ctx, interp) -> {
            exigirAridade("ML.predizer", args, 3, ctx);
            float[] features = extrairVetorArg(args.get(0), ctx);
            float[] coeficientes = extrairVetorArg(args.get(1), ctx);
            float intercepto = args.get(2) instanceof ValorThz.Decimal d ? Float.parseFloat(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(2)).valor().floatValue();
            double pred = thz.lang.ia.ThzMlEngine.predizerRegressao(features, coeficientes, intercepto);
            return ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto(String.format(java.util.Locale.US, "%.6f", pred), 6));
        });

        // ---------------- MENSAGERIA & STREAMING EDA UNIVERSAL ----------------
        registrar(m, "MENSAGERIA.publicar", (args, ctx, interp) -> {
            exigirAridade("MENSAGERIA.publicar", args, 2, ctx);
            exigirClasse("MENSAGERIA.publicar", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            ValorThz msg = args.get(1);
            long offset = thz.lang.mensageria.ThzMessagingBridge.publicar(topico, msg);
            return ValorThz.INTEIRO(BigInteger.valueOf(offset));
        });
        registrar(m, "MENSAGERIA.consumir", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MENSAGERIA.consumir exige o tópico.");
            exigirClasse("MENSAGERIA.consumir", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            long timeout = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().longValue() : 500L;
            var evento = thz.lang.mensageria.ThzMessagingBridge.consumir(topico, timeout);
            return evento != null ? evento.payload() : ValorThz.NULO;
        });
        registrar(m, "MENSAGERIA.tamanhoFila", (args, ctx, interp) -> {
            exigirAridade("MENSAGERIA.tamanhoFila", args, 1, ctx);
            exigirClasse("MENSAGERIA.tamanhoFila", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            int sz = thz.lang.mensageria.ThzMessagingBridge.tamanhoFila(topico);
            return ValorThz.INTEIRO(BigInteger.valueOf(sz));
        });
        registrar(m, "MENSAGERIA.limparTopico", (args, ctx, interp) -> {
            exigirAridade("MENSAGERIA.limparTopico", args, 1, ctx);
            exigirClasse("MENSAGERIA.limparTopico", args.get(0), "TEXTO", ctx);
            String topico = ((ValorThz.Texto) args.get(0)).valor();
            thz.lang.mensageria.ThzMessagingBridge.limparTopico(topico);
            return ValorThz.LOGICO(true);
        });
        registrar(m, "MENSAGERIA.driverAtivo", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.mensageria.ThzMessagingBridge.driverAtivo());
        });
        registrar(m, "MENSAGERIA.statusConexao", (args, ctx, interp) -> {
            return ValorThz.LOGICO(thz.lang.mensageria.ThzMessagingBridge.statusConexao());
        });
        registrar(m, "MENSAGERIA.urlAtiva", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.mensageria.ThzMessagingBridge.urlAtiva());
        });
        registrar(m, "MENSAGERIA.conectar", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] MENSAGERIA.conectar exige o driver.");
            exigirClasse("MENSAGERIA.conectar", args.get(0), "TEXTO", ctx);
            String driver = ((ValorThz.Texto) args.get(0)).valor();
            String url = args.size() > 1 && args.get(1) instanceof ValorThz.Texto t ? t.valor() : "auto";
            thz.lang.mensageria.ThzMessagingBridge.conectar(driver, url);
            return ValorThz.LOGICO(true);
        });

        // ---------------- LOG ----------------
        registrar(m, "LOG.info", (args, ctx, interp) -> {
            exigirAridade("LOG.info", args, 1, ctx);
            thz.lang.log.ThzLog.info(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "LOG.aviso", (args, ctx, interp) -> {
            exigirAridade("LOG.aviso", args, 1, ctx);
            thz.lang.log.ThzLog.aviso(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "LOG.erro", (args, ctx, interp) -> {
            exigirAridade("LOG.erro", args, 1, ctx);
            thz.lang.log.ThzLog.erro(args.get(0).formatar());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "LOG.auditoria", (args, ctx, interp) -> {
            exigirAridade("LOG.auditoria", args, 3, ctx);
            thz.lang.log.ThzLog.auditoria(
                    args.get(0).formatar(),
                    args.get(1).formatar(),
                    args.get(2).formatar()
            );
            return ValorThz.LOGICO(true);
        });

        // ---------------- BANCO (UNIVERSAL / RAW SQL & ORM JPA-LIKE) ----------------
        registrar(m, "BANCO.conectar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.conectar exige pelo menos a URL de conexão.");
            }
            String url = ((ValorThz.Texto) args.get(0)).valor();
            if (args.size() == 1) {
                thz.lang.db.ThzDb.conectar(url);
            } else if (args.size() == 3) {
                thz.lang.db.ThzDb.conectar("padrao", url, ((ValorThz.Texto) args.get(1)).valor(), ((ValorThz.Texto) args.get(2)).valor());
            } else if (args.size() >= 4) {
                thz.lang.db.ThzDb.conectar(((ValorThz.Texto) args.get(0)).valor(), ((ValorThz.Texto) args.get(1)).valor(), ((ValorThz.Texto) args.get(2)).valor(), ((ValorThz.Texto) args.get(3)).valor());
            }
            return ValorThz.LOGICO(true);
        });
        registrar(m, "BANCO.executar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.executar exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            long afetadas = thz.lang.db.ThzDb.executar(sql, params);
            return ValorThz.INTEIRO(afetadas);
        });
        registrar(m, "BANCO.executarEm", (args, ctx, interp) -> {
            if (args.size() < 2) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.executarEm exige conexaoNome e SQL.");
            }
            String nomeConn = ((ValorThz.Texto) args.get(0)).valor();
            String sql = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> params = args.size() > 2 && args.get(2) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            long afetadas = thz.lang.db.ThzDb.executarEm(nomeConn, sql, params);
            return ValorThz.INTEIRO(afetadas);
        });
        registrar(m, "BANCO.consultar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultar exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            var linhas = thz.lang.db.ThzDb.consultar(sql, params);
            List<ValorThz> lista = new java.util.ArrayList<>(linhas);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "BANCO.consultarEm", (args, ctx, interp) -> {
            if (args.size() < 2) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarEm exige conexaoNome e SQL.");
            }
            String nomeConn = ((ValorThz.Texto) args.get(0)).valor();
            String sql = ((ValorThz.Texto) args.get(1)).valor();
            List<ValorThz> params = args.size() > 2 && args.get(2) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            var linhas = thz.lang.db.ThzDb.consultarEm(nomeConn, sql, params);
            List<ValorThz> lista = new java.util.ArrayList<>(linhas);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "BANCO.consultarValor", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarValor exige SQL.");
            }
            String sql = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz> params = args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f ? f.elementos() : java.util.Collections.emptyList();
            return thz.lang.db.ThzDb.consultarValor(sql, params);
        });
        registrar(m, "BANCO.iniciarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.iniciarTransacao();
            else thz.lang.db.ThzDb.iniciarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "BANCO.confirmarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.confirmarTransacao();
            else thz.lang.db.ThzDb.confirmarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "BANCO.cancelarTransacao", (args, ctx, interp) -> {
            if (args.isEmpty()) thz.lang.db.ThzDb.cancelarTransacao();
            else thz.lang.db.ThzDb.cancelarTransacaoEm(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "BANCO.executarScript", (args, ctx, interp) -> {
            exigirAridade("BANCO.executarScript", args, 1, ctx);
            exigirClasse("BANCO.executarScript", args.get(0), "TEXTO", ctx);
            thz.lang.db.ThzDb.executarScript(((ValorThz.Texto) args.get(0)).valor());
            return ValorThz.LOGICO(true);
        });
        registrar(m, "BANCO.driverAtivo", (args, ctx, interp) -> {
            return ValorThz.TEXTO(thz.lang.db.ThzDatabaseBridge.driverAtivo());
        });
        registrar(m, "BANCO.salvar", (args, ctx, interp) -> {
            exigirAridade("BANCO.salvar", args, 2, ctx);
            exigirClasse("BANCO.salvar", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return thz.lang.db.ThzDatabaseBridge.salvar(tabela, args.get(1));
        });
        registrar(m, "BANCO.buscarPorId", (args, ctx, interp) -> {
            exigirAridade("BANCO.buscarPorId", args, 2, ctx);
            exigirClasse("BANCO.buscarPorId", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return thz.lang.db.ThzDatabaseBridge.buscarPorId(tabela, args.get(1));
        });
        registrar(m, "BANCO.removerPorId", (args, ctx, interp) -> {
            exigirAridade("BANCO.removerPorId", args, 2, ctx);
            exigirClasse("BANCO.removerPorId", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            return ValorThz.LOGICO(thz.lang.db.ThzDatabaseBridge.removerPorId(tabela, args.get(1)));
        });
        registrar(m, "BANCO.criarTabela", (args, ctx, interp) -> {
            exigirAridade("BANCO.criarTabela", args, 2, ctx);
            exigirClasse("BANCO.criarTabela", args.get(0), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            java.util.Map<String, String> colunas = new java.util.LinkedHashMap<>();
            if (args.get(1) instanceof ValorThz.Registro reg) {
                reg.campos().forEach((k, v) -> colunas.put(k, v.formatar()));
            } else if (args.get(1) instanceof ValorThz.Texto t) {
                // Formato "id:INTEGER, nome:TEXT, valor:DECIMAL"
                for (String par : t.valor().split(",")) {
                    String[] kv = par.trim().split(":");
                    if (kv.length == 2) colunas.put(kv[0].trim(), kv[1].trim());
                }
            }
            return ValorThz.LOGICO(thz.lang.db.ThzDatabaseBridge.criarTabela(tabela, colunas));
        });
        registrar(m, "BANCO.consultarVetorial", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BANCO.consultarVetorial exige tabela, colunaVetor e vetorBusca.");
            exigirClasse("BANCO.consultarVetorial", args.get(0), "TEXTO", ctx);
            exigirClasse("BANCO.consultarVetorial", args.get(1), "TEXTO", ctx);
            String tabela = ((ValorThz.Texto) args.get(0)).valor();
            String colunaVetor = ((ValorThz.Texto) args.get(1)).valor();
            float[] vetorBusca = extrairVetorArg(args.get(2), ctx);
            int limite = args.size() > 3 && args.get(3) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 5;
            var registros = thz.lang.db.ThzDatabaseBridge.consultarVetorial(tabela, colunaVetor, vetorBusca, limite);
            List<ValorThz> lista = new java.util.ArrayList<>(registros);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "BANCO.fechar", (args, ctx, interp) -> {
            if (args.isEmpty()) {
                thz.lang.db.ThzDb.fechar();
            } else {
                thz.lang.db.ThzDb.fechar(((ValorThz.Texto) args.get(0)).valor());
            }
            return ValorThz.LOGICO(true);
        });

        // ---------------- WEBVIEW ----------------
        registrar(m, "WEBVIEW.iniciar", (args, ctx, interp) -> {
            exigirAridade("WEBVIEW.iniciar", args, 1, ctx);
            exigirClasse("WEBVIEW.iniciar", args.get(0), "TEXTO", ctx);
            String html = ((ValorThz.Texto) args.get(0)).valor();
            thz.lang.webview.ThzWebviewBridge.iniciar(html);
            return ValorThz.TEXTO(thz.lang.webview.ThzWebviewBridge.getUrl());
        });
        registrar(m, "WEBVIEW.emitir", (args, ctx, interp) -> {
            exigirAridade("WEBVIEW.emitir", args, 2, ctx);
            exigirClasse("WEBVIEW.emitir", args.get(0), "TEXTO", ctx);
            exigirClasse("WEBVIEW.emitir", args.get(1), "TEXTO", ctx);
            thz.lang.webview.ThzWebviewBridge.emitirParaJs(
                    ((ValorThz.Texto) args.get(0)).valor(),
                    ((ValorThz.Texto) args.get(1)).valor()
            );
            return ValorThz.LOGICO(true);
        });
        registrar(m, "WEBVIEW.parar", (args, ctx, interp) -> {
            thz.lang.webview.ThzWebviewBridge.parar();
            return ValorThz.LOGICO(true);
        });

        // ---------------- UI (ThzUiMaker) ----------------
        registrar(m, "UI.temaPadrao", (args, ctx, interp) -> {
            return ValorThz.TEXTO("THZ Dark Glass");
        });
        registrar(m, "UI.renderizarHtml", (args, ctx, interp) -> {
            exigirAridade("UI.renderizarHtml", args, 2, ctx);
            exigirClasse("UI.renderizarHtml", args.get(0), "TEXTO", ctx);
            exigirClasse("UI.renderizarHtml", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String rotuloBotao = ((ValorThz.Texto) args.get(1)).valor();
            var tela = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                c.adicionar(thz.lang.ui.ThzUiMaker.card("card_principal", titulo, card -> {
                    card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_info", "info", "Tela construída com ThzUiMaker"));
                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_acao", rotuloBotao, "ExecutarAcao"));
                }));
            });
            return ValorThz.TEXTO(tela.renderizarHtml(titulo, thz.lang.ui.ThzUiTema.escuroGlass()));
        });
        registrar(m, "UI.gerarCodigo", (args, ctx, interp) -> {
            exigirAridade("UI.gerarCodigo", args, 1, ctx);
            exigirClasse("UI.gerarCodigo", args.get(0), "TEXTO", ctx);
            String nome = ((ValorThz.Texto) args.get(0)).valor();
            var tela = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                c.adicionar(thz.lang.ui.ThzUiMaker.card("card_app", nome, card -> {
                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_ok", "Confirmar", "ConfirmarAcao"));
                }));
            });
            return ValorThz.TEXTO(tela.gerarCodigoThz(nome));
        });

        // ---------------- ESTATISTICA ----------------
        registrar(m, "ESTATISTICA.media", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.media", args, 1, ctx);
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.media(valores));
        });
        registrar(m, "ESTATISTICA.mediana", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.mediana", args, 1, ctx);
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.mediana(valores));
        });
        registrar(m, "ESTATISTICA.moda", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.moda", args, 1, ctx);
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.moda(valores));
        });
        registrar(m, "ESTATISTICA.desvioPadrao", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] ESTATISTICA.desvioPadrao exige lista de valores.");
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            boolean amostral = args.size() <= 1 || (args.get(1) instanceof ValorThz.Logico l && l.valor());
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.desvioPadrao(valores, amostral));
        });
        registrar(m, "ESTATISTICA.variancia", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] ESTATISTICA.variancia exige lista de valores.");
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            boolean amostral = args.size() <= 1 || (args.get(1) instanceof ValorThz.Logico l && l.valor());
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.variancia(valores, amostral));
        });
        registrar(m, "ESTATISTICA.correlacao", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.correlacao", args, 2, ctx);
            List<Double> x = extrairListaDoubles(args.get(0), ctx);
            List<Double> y = extrairListaDoubles(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.correlacaoPearson(x, y));
        });
        registrar(m, "ESTATISTICA.percentil", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.percentil", args, 2, ctx);
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            double p = args.get(1) instanceof ValorThz.Decimal d ? Double.parseDouble(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(1)).valor().doubleValue();
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.percentil(valores, p));
        });
        registrar(m, "ESTATISTICA.zScore", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.zScore", args, 2, ctx);
            double val = args.get(0) instanceof ValorThz.Decimal d ? Double.parseDouble(d.valor().formatar()) : ((ValorThz.Inteiro) args.get(0)).valor().doubleValue();
            List<Double> amostra = extrairListaDoubles(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzEstatistica.zScore(val, amostra));
        });
        registrar(m, "ESTATISTICA.outliers", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.outliers", args, 1, ctx);
            List<Double> valores = extrairListaDoubles(args.get(0), ctx);
            var outliers = thz.lang.analytics.ThzEstatistica.detectarOutliers(valores);
            return new ValorThz.Fatia("DECIMAL", outliers);
        });
        registrar(m, "ESTATISTICA.regressao", (args, ctx, interp) -> {
            exigirAridade("ESTATISTICA.regressao", args, 2, ctx);
            List<Double> x = extrairListaDoubles(args.get(0), ctx);
            List<Double> y = extrairListaDoubles(args.get(1), ctx);
            var reg = thz.lang.analytics.ThzEstatistica.regressaoLinear(x, y);
            Map<String, ValorThz> campos = new LinkedHashMap<>();
            campos.put("inclinacao", ValorThz.DECIMAL(reg.inclinacao()));
            campos.put("intercepto", ValorThz.DECIMAL(reg.intercepto()));
            campos.put("rQuadrado", ValorThz.DECIMAL(reg.rQuadrado()));
            return new ValorThz.Registro("RegressaoResultado", campos);
        });

        // ---------------- DAX / BI / METRICAS ANALITICAS ----------------
        registrar(m, "DAX.acumuladoAno", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.acumuladoAno exige tabela, campoData, campoValor e anoAlvo.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campoData = ((ValorThz.Texto) args.get(1)).valor();
            String campoValor = ((ValorThz.Texto) args.get(2)).valor();
            int anoAlvo = ((ValorThz.Inteiro) args.get(3)).valor().intValue();
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDaxEngine.totalYtd(linhas, campoData, campoValor, anoAlvo));
        });
        registrar(m, "DAX.variacaoPeriodo", (args, ctx, interp) -> {
            exigirAridade("DAX.variacaoPeriodo", args, 2, ctx);
            double vAtual = extrairDoubleArg(args.get(0), ctx);
            double vAnt = extrairDoubleArg(args.get(1), ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDaxEngine.variacaoPeriodo(vAtual, vAnt));
        });
        registrar(m, "DAX.contagemDistintos", (args, ctx, interp) -> {
            exigirAridade("DAX.contagemDistintos", args, 2, ctx);
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campo = ((ValorThz.Texto) args.get(1)).valor();
            return ValorThz.INTEIRO(thz.lang.analytics.ThzDaxEngine.contagemDistintos(linhas, campo));
        });
        registrar(m, "DAX.ranking", (args, ctx, interp) -> {
            if (args.size() < 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.ranking exige tabela e campoValor.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campoValor = ((ValorThz.Texto) args.get(1)).valor();
            boolean desc = args.size() <= 2 || (args.get(2) instanceof ValorThz.Logico l && l.valor());
            var res = thz.lang.analytics.ThzDaxEngine.calcularRanking(linhas, campoValor, desc);
            List<ValorThz> lista = new ArrayList<>(res);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "DAX.percentualTotal", (args, ctx, interp) -> {
            exigirAridade("DAX.percentualTotal", args, 2, ctx);
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campoValor = ((ValorThz.Texto) args.get(1)).valor();
            var res = thz.lang.analytics.ThzDaxEngine.percentualSobreTotal(linhas, campoValor);
            List<ValorThz> lista = new ArrayList<>(res);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "DAX.kpi", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DAX.kpi exige nome, valorRealizado e valorMeta.");
            String nome = ((ValorThz.Texto) args.get(0)).valor();
            double vReal = extrairDoubleArg(args.get(1), ctx);
            double vMeta = extrairDoubleArg(args.get(2), ctx);
            double tol = args.size() > 3 ? extrairDoubleArg(args.get(3), ctx) : 5.0;
            return thz.lang.analytics.ThzDaxEngine.avaliarKpi(nome, vReal, vMeta, tol);
        });

        // ---------------- PLANILHA / CSV / TABELAS ----------------
        registrar(m, "PLANILHA.lerCsv", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.lerCsv exige caminho do arquivo.");
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            String sep = args.size() > 1 && args.get(1) instanceof ValorThz.Texto t ? t.valor() : "auto";
            try {
                var linhas = thz.lang.analytics.ThzPlanilhaCsv.lerCsv(java.nio.file.Path.of(caminho), sep);
                List<ValorThz> lista = new ArrayList<>(linhas);
                return ValorThz.FATIA(lista);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao ler CSV: " + e.getMessage());
            }
        });
        registrar(m, "PLANILHA.escreverCsv", (args, ctx, interp) -> {
            if (args.size() < 2) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.escreverCsv exige destino e tabela.");
            String caminho = ((ValorThz.Texto) args.get(0)).valor();
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(1), ctx);
            String sep = args.size() > 2 && args.get(2) instanceof ValorThz.Texto t ? t.valor() : ";";
            try {
                boolean ok = thz.lang.analytics.ThzPlanilhaCsv.escreverCsv(java.nio.file.Path.of(caminho), linhas, sep);
                return ValorThz.LOGICO(ok);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao escrever CSV: " + e.getMessage());
            }
        });
        registrar(m, "PLANILHA.procv", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.procv exige tabela, campoBusca, valorBusca e campoRetorno.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campoBusca = ((ValorThz.Texto) args.get(1)).valor();
            String valorBusca = args.get(2).formatar();
            String campoRetorno = ((ValorThz.Texto) args.get(3)).valor();
            return thz.lang.analytics.ThzPlanilhaCsv.procv(linhas, campoBusca, valorBusca, campoRetorno);
        });
        registrar(m, "PLANILHA.pivotar", (args, ctx, interp) -> {
            if (args.size() < 4) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] PLANILHA.pivotar exige tabela, campoLinha, campoColuna e campoValor.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campoLinha = ((ValorThz.Texto) args.get(1)).valor();
            String campoColuna = ((ValorThz.Texto) args.get(2)).valor();
            String campoValor = ((ValorThz.Texto) args.get(3)).valor();
            String op = args.size() > 4 && args.get(4) instanceof ValorThz.Texto t ? t.valor() : "SUM";
            var pivot = thz.lang.analytics.ThzPlanilhaCsv.pivotar(linhas, campoLinha, campoColuna, campoValor, op);
            List<ValorThz> lista = new ArrayList<>(pivot);
            return ValorThz.FATIA(lista);
        });

        // ---------------- DADOS & DATA QUALITY ----------------
        registrar(m, "DADOS.sanitizar", (args, ctx, interp) -> {
            exigirAridade("DADOS.sanitizar", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.sanitizarTexto(args.get(0).formatar()));
        });
        registrar(m, "DADOS.decimalPtBr", (args, ctx, interp) -> {
            exigirAridade("DADOS.decimalPtBr", args, 1, ctx);
            return ValorThz.DECIMAL(thz.lang.analytics.ThzDataQuality.parsearDecimalPtBr(args.get(0).formatar()));
        });
        registrar(m, "DADOS.dataPtBr", (args, ctx, interp) -> {
            exigirAridade("DADOS.dataPtBr", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.parsearDataPtBr(args.get(0).formatar()));
        });
        registrar(m, "DADOS.validarCpf", (args, ctx, interp) -> {
            exigirAridade("DADOS.validarCpf", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.analytics.ThzDataQuality.validarCpf(args.get(0).formatar()));
        });
        registrar(m, "DADOS.validarCnpj", (args, ctx, interp) -> {
            exigirAridade("DADOS.validarCnpj", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.analytics.ThzDataQuality.validarCnpj(args.get(0).formatar()));
        });
        registrar(m, "DADOS.mascarar", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.mascarar exige texto.");
            String texto = args.get(0).formatar();
            int inicio = args.size() > 1 && args.get(1) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 3;
            int fim = args.size() > 2 && args.get(2) instanceof ValorThz.Inteiro in ? in.valor().intValue() : 2;
            return ValorThz.TEXTO(thz.lang.analytics.ThzDataQuality.mascararDadoSensivel(texto, inicio, fim));
        });
        registrar(m, "DADOS.removerDuplicatas", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.removerDuplicatas exige tabela.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            List<String> chaves = List.of();
            if (args.size() > 1 && args.get(1) instanceof ValorThz.Fatia f) {
                chaves = f.elementos().stream().map(ValorThz::formatar).toList();
            }
            var dedup = thz.lang.analytics.ThzDataQuality.removerDuplicatas(linhas, chaves);
            List<ValorThz> lista = new ArrayList<>(dedup);
            return ValorThz.FATIA(lista);
        });
        registrar(m, "DADOS.imputarNulos", (args, ctx, interp) -> {
            if (args.size() < 3) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] DADOS.imputarNulos exige tabela, campo e valorPadrao.");
            List<ValorThz.Registro> linhas = extrairListaRegistros(args.get(0), ctx);
            String campo = ((ValorThz.Texto) args.get(1)).valor();
            ValorThz padrao = args.get(2);
            var imp = thz.lang.analytics.ThzDataQuality.imputarNulos(linhas, campo, padrao);
            List<ValorThz> lista = new ArrayList<>(imp);
            return ValorThz.FATIA(lista);
        });

        // ---------------- BRASIL (CEP, PIX, Boletos, Documentos, Feriados) ----------------
        registrar(m, "BRASIL.cep", (args, ctx, interp) -> {
            exigirAridade("BRASIL.cep", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCep(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.consultarCep", (args, ctx, interp) -> {
            exigirAridade("BRASIL.consultarCep", args, 1, ctx);
            return thz.lang.brasil.ThzBrasilEngine.consultarCep(args.get(0).formatar());
        });
        registrar(m, "BRASIL.cadastrarCep", (args, ctx, interp) -> {
            if (args.size() < 7) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.cadastrarCep exige 7 argumentos: cep, logradouro, bairro, cidade, uf, ibge, ddd.");
            boolean ok = thz.lang.brasil.ThzInternalDatabase.cadastrarCep(
                    args.get(0).formatar(), args.get(1).formatar(), args.get(2).formatar(),
                    args.get(3).formatar(), args.get(4).formatar(), args.get(5).formatar(), args.get(6).formatar()
            );
            return ValorThz.LOGICO(ok);
        });
        registrar(m, "BRASIL.validarUf", (args, ctx, interp) -> {
            exigirAridade("BRASIL.validarUf", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarUf(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.regiaoUf", (args, ctx, interp) -> {
            exigirAridade("BRASIL.regiaoUf", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.regiaoUf(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.formatarEndereco", (args, ctx, interp) -> {
            if (args.size() < 7) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.formatarEndereco exige 7 argumentos: logradouro, numero, complemento, bairro, cidade, uf, cep.");
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarEndereco(
                    args.get(0).formatar(), args.get(1).formatar(), args.get(2).formatar(),
                    args.get(3).formatar(), args.get(4).formatar(), args.get(5).formatar(), args.get(6).formatar()
            ));
        });
        registrar(m, "BRASIL.pixCopiaECola", (args, ctx, interp) -> {
            if (args.size() < 5) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.pixCopiaECola exige chave, nome, cidade, valor, txId.");
            String chave = args.get(0).formatar();
            String nome = args.get(1).formatar();
            String cidade = args.get(2).formatar();
            java.math.BigDecimal valor = new java.math.BigDecimal(String.valueOf(extrairDoubleArg(args.get(3), ctx)));
            String txId = args.get(4).formatar();
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.gerarPixCopiaECola(chave, nome, cidade, valor, txId));
        });
        registrar(m, "BRASIL.validarChavePix", (args, ctx, interp) -> {
            if (args.isEmpty()) throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] BRASIL.validarChavePix exige a chave.");
            String tipo = args.size() > 1 ? args.get(1).formatar() : "AUTO";
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarChavePix(args.get(0).formatar(), tipo));
        });
        registrar(m, "BRASIL.validarLinhaDigitavel", (args, ctx, interp) -> {
            exigirAridade("BRASIL.validarLinhaDigitavel", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarLinhaDigitavel(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.linhaParaCodigoBarras", (args, ctx, interp) -> {
            exigirAridade("BRASIL.linhaParaCodigoBarras", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.linhaDigitavelParaCodigoBarras(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.valorBoleto", (args, ctx, interp) -> {
            exigirAridade("BRASIL.valorBoleto", args, 1, ctx);
            return ValorThz.DECIMAL(thz.lang.brasil.ThzBrasilEngine.extrairValorBoleto(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.formatarCpf", (args, ctx, interp) -> {
            exigirAridade("BRASIL.formatarCpf", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCpf(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.formatarCnpj", (args, ctx, interp) -> {
            exigirAridade("BRASIL.formatarCnpj", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarCnpj(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.formatarTelefone", (args, ctx, interp) -> {
            exigirAridade("BRASIL.formatarTelefone", args, 1, ctx);
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.formatarTelefone(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.validarTituloEleitor", (args, ctx, interp) -> {
            exigirAridade("BRASIL.validarTituloEleitor", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarTituloEleitor(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.validarCnh", (args, ctx, interp) -> {
            exigirAridade("BRASIL.validarCnh", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarCnh(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.validarPis", (args, ctx, interp) -> {
            exigirAridade("BRASIL.validarPis", args, 1, ctx);
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.validarPis(args.get(0).formatar()));
        });
        registrar(m, "BRASIL.ehFeriadoNacional", (args, ctx, interp) -> {
            exigirAridade("BRASIL.ehFeriadoNacional", args, 1, ctx);
            java.time.LocalDate dt = extrairDataArg(args.get(0));
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.ehFeriadoNacional(dt));
        });
        registrar(m, "BRASIL.ehDiaUtil", (args, ctx, interp) -> {
            exigirAridade("BRASIL.ehDiaUtil", args, 1, ctx);
            java.time.LocalDate dt = extrairDataArg(args.get(0));
            return ValorThz.LOGICO(thz.lang.brasil.ThzBrasilEngine.ehDiaUtil(dt));
        });
        registrar(m, "BRASIL.proximoDiaUtil", (args, ctx, interp) -> {
            exigirAridade("BRASIL.proximoDiaUtil", args, 1, ctx);
            java.time.LocalDate dt = extrairDataArg(args.get(0));
            java.time.LocalDate prox = thz.lang.brasil.ThzBrasilEngine.proximoDiaUtil(dt);
            return ValorThz.TEXTO(prox.toString());
        });
        registrar(m, "BRASIL.valorPorExtenso", (args, ctx, interp) -> {
            exigirAridade("BRASIL.valorPorExtenso", args, 1, ctx);
            java.math.BigDecimal val = new java.math.BigDecimal(String.valueOf(extrairDoubleArg(args.get(0), ctx)));
            return ValorThz.TEXTO(thz.lang.brasil.ThzBrasilEngine.valorPorExtenso(val));
        });

        // ---------------- SNAPSHOT & COMPACTACAO CACHE ----------------
        registrar(m, "SNAPSHOT.criar", (args, ctx, interp) -> {
            java.nio.file.Path origem = args.isEmpty() ? java.nio.file.Path.of(".") : java.nio.file.Path.of(args.get(0).formatar());
            java.nio.file.Path destino = args.size() > 1 ? java.nio.file.Path.of(args.get(1).formatar()) : null;
            try {
                java.nio.file.Path criado = thz.lang.snapshot.ThzSnapshotEngine.criarSnapshot(origem, destino);
                return ValorThz.TEXTO(criado.toAbsolutePath().toString().replace("\\", "/"));
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] " + e.getMessage());
            }
        });
        registrar(m, "SNAPSHOT.restaurar", (args, ctx, interp) -> {
            java.nio.file.Path snap = args.isEmpty() ? null : java.nio.file.Path.of(args.get(0).formatar());
            java.nio.file.Path destino = args.size() > 1 ? java.nio.file.Path.of(args.get(1).formatar()) : java.nio.file.Path.of(".");
            try {
                boolean ok = thz.lang.snapshot.ThzSnapshotEngine.restaurarSnapshot(snap, destino);
                return ValorThz.LOGICO(ok);
            } catch (Exception e) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] Falha ao restaurar snapshot: " + e.getMessage());
            }
        });
        registrar(m, "SNAPSHOT.tamanho", (args, ctx, interp) -> {
            return ValorThz.INTEIRO(thz.lang.snapshot.ThzSnapshotEngine.obterTamanhoSnapshot());
        });
        registrar(m, "SNAPSHOT.limpar", (args, ctx, interp) -> {
            return ValorThz.LOGICO(thz.lang.snapshot.ThzSnapshotEngine.limparSnapshot());
        });
        registrar(m, "SNAPSHOT.verificar", (args, ctx, interp) -> {
            java.nio.file.Path snap = args.isEmpty() ? null : java.nio.file.Path.of(args.get(0).formatar());
            return ValorThz.LOGICO(thz.lang.snapshot.ThzSnapshotEngine.verificarIntegridade(snap));
        });

        // ---------------- NATIVO / RUST INLINE BRIDGE ----------------
        registrar(m, "NATIVO.somar_rapido", (args, ctx, interp) -> {
            exigirAridade("NATIVO.somar_rapido", args, 2, ctx);
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("somar_rapido", args);
        });
        registrar(m, "NATIVO.calcular_hash_customizado", (args, ctx, interp) -> {
            exigirAridade("NATIVO.calcular_hash_customizado", args, 1, ctx);
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("calcular_hash_customizado", args);
        });
        registrar(m, "NATIVO.versao_rust", (args, ctx, interp) -> {
            return thz.lang.rust.ThzRustRunner.invocarFuncaoNativa("versao_rust", args);
        });

        // Base imutável; extensões de módulos (registrar) são aplicadas sobre cópia concorrente
        return new java.util.concurrent.ConcurrentHashMap<>(m);
    }

    private static List<Double> extrairListaDoubles(ValorThz v, ExprAst ctx) {
        List<Double> lista = new ArrayList<>();
        if (v instanceof ValorThz.Fatia fatia) {
            for (ValorThz elem : fatia.elementos()) {
                if (elem instanceof ValorThz.Decimal d) {
                    lista.add(Double.parseDouble(d.valor().formatar()));
                } else if (elem instanceof ValorThz.Inteiro in) {
                    lista.add(in.valor().doubleValue());
                } else if (elem instanceof ValorThz.Texto t) {
                    try { lista.add(Double.parseDouble(t.valor().trim().replace(",", "."))); } catch (Exception ignored) {}
                }
            }
        }
        return lista;
    }

    private static List<ValorThz.Registro> extrairListaRegistros(ValorThz v, ExprAst ctx) {
        List<ValorThz.Registro> lista = new ArrayList<>();
        if (v instanceof ValorThz.Fatia fatia) {
            for (ValorThz elem : fatia.elementos()) {
                if (elem instanceof ValorThz.Registro r) {
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    private static double extrairDoubleArg(ValorThz v, ExprAst ctx) {
        if (v instanceof ValorThz.Decimal d) return Double.parseDouble(d.valor().formatar());
        if (v instanceof ValorThz.Inteiro in) return in.valor().doubleValue();
        if (v instanceof ValorThz.Texto t) {
            try { return Double.parseDouble(t.valor().trim().replace(",", ".")); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    private static java.time.LocalDate extrairDataArg(ValorThz v) {
        if (v instanceof ValorThz.Data d) {
            return java.time.LocalDate.of(d.valor().getAno(), d.valor().getMes(), d.valor().getDia());
        }
        if (v instanceof ValorThz.DataHora dh) {
            return java.time.LocalDate.of(dh.valor().getData().getAno(), dh.valor().getData().getMes(), dh.valor().getData().getDia());
        }
        String iso = thz.lang.analytics.ThzDataQuality.parsearDataPtBr(v.formatar());
        try {
            return java.time.LocalDate.parse(iso);
        } catch (Exception e) {
            return java.time.LocalDate.now();
        }
    }
}
