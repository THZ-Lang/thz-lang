package thz.lang.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import thz.lang.ast.ComandoAst;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;

/**
 * Utilitários compartilhados entre os comandos CLI.
 */
public final class CliHelper {

    private CliHelper() {}

    public static Map<String, String> parseArgsMapa(List<String> args) {
        Map<String, String> mapa = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            if (a.equals("--arg") && i + 1 < args.size()) {
                String par = args.get(++i);
                int eq = par.indexOf('=');
                if (eq >= 0)
                    mapa.put(par.substring(0, eq), par.substring(eq + 1));
            } else if (a.startsWith("--arg=")) {
                String par = a.substring(6);
                int eq = par.indexOf('=');
                if (eq >= 0)
                    mapa.put(par.substring(0, eq), par.substring(eq + 1));
            }
        }
        return mapa;
    }

    public static String resolverArquivo(List<String> args) {
        Set<String> flags = new HashSet<>(Set.of("--saida", "--principal", "--arg"));
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            if (flags.contains(a)) {
                i++;
                continue;
            }
            if (a.startsWith("--arg="))
                continue;
            if (a.startsWith("-"))
                continue;
            return a;
        }
        return "exemplos/faturamento.thz";
    }

    public static boolean precisaEntrada(ProgramaAst ast) {
        java.util.function.Predicate<List<ComandoAst>> temLer = new java.util.function.Predicate<>() {
            public boolean test(List<ComandoAst> cmds) {
                for (ComandoAst c : cmds) {
                    if (c instanceof ComandoAst.Ler)
                        return true;
                    if (c instanceof ComandoAst.Se s)
                        if (test(s.entao()) || test(s.senao()))
                            return true;
                    if (c instanceof ComandoAst.Enquanto e)
                        if (test(e.corpo()))
                            return true;
                    if (c instanceof ComandoAst.Para p)
                        if (test(p.corpo()))
                            return true;
                    if (c instanceof ComandoAst.VetorizarPara v)
                        if (test(v.corpo()))
                            return true;
                    if (c instanceof ComandoAst.BlocoMemoria b)
                        if (test(b.corpo()))
                            return true;
                }
                return false;
            }
        };
        List<List<ComandoAst>> todos = new ArrayList<>();
        for (RegraNegocioAst r : ast.regras())
            for (OperacaoAst o : r.operacoes())
                todos.add(o.corpo());
        if (ast.procedimentos() != null)
            for (ProcedimentoAst p : ast.procedimentos())
                todos.add(p.corpo());
        for (List<ComandoAst> c : todos)
            if (temLer.test(c))
                return true;
        return false;
    }

    public static Supplier<String> criarLeitorEntrada() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        return () -> {
            try {
                return br.readLine();
            } catch (IOException e) {
                return null;
            }
        };
    }

    public static String q(String s) {
        return "\"" + (s != null ? s : "") + "\"";
    }

    public static int obterIndiceArg(List<String> args, String... chaves) {
        for (String chave : chaves) {
            int idx = args.indexOf(chave);
            if (idx >= 0) return idx;
        }
        return -1;
    }

    public static String obterValorArg(List<String> args, String... chaves) {
        int idx = obterIndiceArg(args, chaves);
        if (idx >= 0 && idx + 1 < args.size()) return args.get(idx + 1);
        return null;
    }
}
