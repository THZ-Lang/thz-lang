package thz.lang.interpretador;

import thz.lang.ast.ExprAst;
import thz.lang.gui.RenderizadorFormularioSwing;
import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.ErroData;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        registrar(m, "TELA.renderizarFormulario", (args, ctx, interp) -> {
            exigirAridade("TELA.renderizarFormulario", args, 2, ctx);
            if (!(args.get(0) instanceof ValorThz.Registro reg)) {
                throw new ErroExecucao("[Erro de Execução][Linha " + ctx.linha() + ":" + ctx.coluna() + "] TELA.renderizarFormulario exige REGISTRO como 1º argumento, recebido " + args.get(0).classe());
            }
            exigirClasse("TELA.renderizarFormulario", args.get(1), "TEXTO", ctx);
            String opAlvo = ((ValorThz.Texto) args.get(1)).valor();
            String msg = RenderizadorFormularioSwing.renderizar(reg, opAlvo, interp);
            return ValorThz.TEXTO(msg);
        });

        registrar(m, "TELA.alerta", (args, ctx, interp) -> {
            exigirAridade("TELA.alerta", args, 2, ctx);
            exigirClasse("TELA.alerta", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.alerta", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                javax.swing.SwingUtilities.invokeLater(() ->
                        javax.swing.JOptionPane.showMessageDialog(null, mensagem, titulo, javax.swing.JOptionPane.INFORMATION_MESSAGE)
                );
            }
            return ValorThz.TEXTO("OK");
        });

        registrar(m, "TELA.confirmar", (args, ctx, interp) -> {
            exigirAridade("TELA.confirmar", args, 2, ctx);
            exigirClasse("TELA.confirmar", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.confirmar", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String mensagem = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                int r = javax.swing.JOptionPane.showConfirmDialog(null, mensagem, titulo, javax.swing.JOptionPane.YES_NO_OPTION);
                return ValorThz.LOGICO(r == javax.swing.JOptionPane.YES_OPTION);
            }
            return ValorThz.LOGICO(true);
        });

        registrar(m, "TELA.pedirTexto", (args, ctx, interp) -> {
            exigirAridade("TELA.pedirTexto", args, 2, ctx);
            exigirClasse("TELA.pedirTexto", args.get(0), "TEXTO", ctx);
            exigirClasse("TELA.pedirTexto", args.get(1), "TEXTO", ctx);
            String titulo = ((ValorThz.Texto) args.get(0)).valor();
            String prompt = ((ValorThz.Texto) args.get(1)).valor();
            boolean modoNaoInterativo = Boolean.getBoolean("thz.nao_interativo") || java.awt.GraphicsEnvironment.isHeadless();
            if (!modoNaoInterativo) {
                String r = javax.swing.JOptionPane.showInputDialog(null, prompt, titulo, javax.swing.JOptionPane.QUESTION_MESSAGE);
                return ValorThz.TEXTO(r != null ? r : "");
            }
            return ValorThz.TEXTO("");
        });

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

        return Collections.unmodifiableMap(m);
    }
}
