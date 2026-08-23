package thz.lang.cli;

import thz.lang.ast.*;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.formato.Formatador;
import thz.lang.formato.JsonEscritor;
import thz.lang.interpretador.*;
import thz.lang.runtime.ArenaMemoria;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ThzCli {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        String comando = args.length > 0 ? args[0] : "run";
        List<String> argumentos = new ArrayList<>(Arrays.asList(args));
        if (!argumentos.isEmpty()) argumentos.remove(0);
        boolean estrito = argumentos.contains("--estrito");
        if (comando.equals("repl")) {
            thz.lang.repl.Repl.executar();
            return;
        }
        if (comando.equals("gui")) {
            javax.swing.SwingUtilities.invokeLater(() -> new thz.lang.gui.ThzGui().setVisible(true));
            return;
        }
        String arquivo = resolverArquivo(argumentos);
        if (comando.equals("check") || comando.equals("ast") || comando.equals("fmt") || comando.equals("run")) {
            if (!Files.exists(Path.of(arquivo))) { System.err.println("[ERRO] Arquivo não encontrado: " + arquivo); System.exit(1); }
            String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
            try {
                List<Token> tokens = new ThzLexer(fonte).tokenize();
                ProgramaAst ast = new ThzParser(tokens).parse();
                if (comando.equals("check")) {
                    List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
                    if (!erros.isEmpty()) {
                        List<DiagnosticoEntrada> diags = erros.stream().map(e->new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
                        for (String bloco : Diagnosticos.formatarDiagnosticos(fonte, diags, "Semântico")) System.err.println(bloco + "\n");
                        System.err.println("[THZ CHECK] " + erros.size() + " erro(s) semântico(s).");
                        System.exit(1);
                    }
                    String versao = ast.versaoLinguagem()!=null ? " (ver. linguagem declarada: " + ast.versaoLinguagem() + ")" : " (sem pragma VERSAO_LINGUAGEM — assumindo versão corrente)";
                    System.out.println("[THZ CHECK] Código validado com sucesso! AST íntegra para o programa: " + ast.nome() + versao + (estrito ? " [lint estrito aprovado]" : ""));
                    return;
                }
                if (comando.equals("ast")) {
                    System.out.println(JsonEscritor.paraJson(ast));
                    return;
                }
                if (comando.equals("fmt")) {
                    boolean check = argumentos.contains("--check");
                    boolean escrever = argumentos.contains("--escrever") || argumentos.contains("-w");
                    String idxSaida = null;
                    int idx = argumentos.indexOf("--saida");
                    if (idx >= 0 && idx + 1 < argumentos.size()) idxSaida = argumentos.get(idx + 1);
                    String formatado = Formatador.formatar(ast);
                    if (check) {
                        if (!fonte.equals(formatado)) {
                            System.err.println("[THZ FMT] Arquivo não está formatado. Use `thz fmt --escrever` para corrigir.");
                            String[] a = fonte.split("\n", -1); String[] b = formatado.split("\n", -1);
                            for (int i=0;i<Math.max(a.length,b.length);i++) if (!Objects.equals(i<a.length?a[i]:null, i<b.length?b[i]:null)) {
                                System.err.println("  Linha "+(i+1)+" esperada: "+q(b.length>i?b[i]:"")); System.err.println("  Linha "+(i+1)+" obtida:   "+q(a.length>i?a[i]:"")); break;
                            }
                            System.exit(1);
                        }
                        System.out.println("[THZ FMT] OK — arquivo já está canônico.");
                        return;
                    }
                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".thz") ? Path.of(idxSaida) : Path.of(idxSaida, Path.of(arquivo).getFileName().toString());
                        Files.createDirectories(alvo.getParent()!=null?alvo.getParent():Path.of("."));
                        Files.writeString(alvo, formatado, StandardCharsets.UTF_8);
                        System.out.println("[THZ FMT] Arquivo formatado gravado em: " + alvo);
                        return;
                    }
                    if (escrever) { Files.writeString(Path.of(arquivo), formatado, StandardCharsets.UTF_8); System.out.println("[THZ FMT] " + arquivo + " formatado."); return; }
                    System.out.println(formatado);
                    return;
                }
                if (comando.equals("run")) {
                    System.out.println("================================================================================");
                    System.out.println("   EXECUTANDO MOTOR NATIVO THZ-LANG: " + ast.nome());
                    System.out.println("================================================================================\n");
                    ArenaMemoria arena = new ArenaMemoria(64); arena.alocar(2048);
                    String dom = ast.metadados()!=null?ast.metadados().dominio():"N/A";
                    String slo = ast.metadados()!=null?ast.metadados().sloLatencia():"N/A";
                    String conf = ast.metadados()!=null && ast.metadados().conformidade()!=null?String.join(", ", ast.metadados().conformidade()):"N/A";
                    System.out.println("[ARQUITETURA] Domínio: " + dom + " | SLO: " + slo);
                    System.out.println("[CONFORMIDADE] Diretrizes ativas: " + conf + "\n");
                    int ip = argumentos.indexOf("--principal");
                    final String nomePrincipal = (ip>=0 && ip+1<argumentos.size()) ? argumentos.get(ip+1) : null;
                    Map<String,String> mapaArgs = parseArgsMapa(argumentos);
                    boolean precisaEntrada = precisaEntrada(ast);
                    java.util.function.Supplier<String> entrada = precisaEntrada ? criarLeitorEntrada() : null;
                    InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);
                    if (nomePrincipal != null) {
                        ProcedimentoAst proc = ast.procedimentos()!=null?ast.procedimentos().stream().filter(p->p.nome().equals(nomePrincipal)).findFirst().orElse(null):null;
                        if (proc != null) {
                            System.out.println("[PROCEDIMENTO] " + proc.nome() + "()\n");
                            Map<String, ValorThz> a = construirArgsProc(proc, mapaArgs);
                            interp.executarProcedimento(proc.nome(), a);
                            arena.liberarTudo(); System.out.println("\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros."); return;
                        }
                        var ops = interp.listarOperacoesExecutaveis().stream().filter(o->o.operacao().nome().equals(nomePrincipal)).findFirst().orElse(null);
                        if (ops != null) {
                            System.out.println("[REGRA] " + ops.regra().nome() + (ops.regra().identificador()!=null?" ("+ops.regra().identificador()+")":"")+" :: "+ops.operacao().nome()+"()\n");
                            Map<String,ValorThz> a = new HashMap<>();
                            for (ParametroOperacaoAst p: ops.operacao().parametros()) {
                                String bruto = mapaArgs.get(p.nome());
                                if (bruto!=null) a.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), bruto));
                                else {
                                    var m = java.util.regex.Pattern.compile("^FATIA\\[(\\w+)\\]$").matcher(p.tipo());
                                    if(m.matches()){ EstruturaAst est = ast.estruturas().stream().filter(e2->e2.nome().equals(m.group(1))).findFirst().orElse(null); if(est==null) throw new RuntimeException("Estrutura '"+m.group(1)+"' não declarada."); List<ValorThz> elems = new ArrayList<>(); for(Object[] linha: LOTE) elems.add(registroDe(est, linha, v->interp.validarInvariantes(v))); a.put(p.nome(), new ValorThz.Fatia(m.group(1), elems)); } else a.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), bruto!=null?bruto:0));
                                }
                            }
                            ValorThz res = interp.executarOperacao(ops.operacao().nome(), a);
                            System.out.println("--------------------------------------------------------------");
                            if(res!=null) System.out.println("[RESULTADO] " + interp.formatar(res));
                            arena.liberarTudo(); System.out.println("\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros."); return;
                        }
                        System.err.println("[ERRO] Entrada '--principal "+nomePrincipal+"' não encontrada como PROCEDIMENTO nem OPERACAO."); System.exit(1);
                    }
                    ProcedimentoAst procPrincipal = ast.procedimentos()!=null?ast.procedimentos().stream().filter(p->p.nome().equals("Principal")).findFirst().orElse(null):null;
                    if(procPrincipal!=null){
                        System.out.println("[PROCEDIMENTO] Principal()\n");
                        Map<String,ValorThz> a = procPrincipal.parametros().isEmpty()?Map.of():construirArgsProc(procPrincipal, mapaArgs);
                        interp.executarProcedimento("Principal", a);
                        arena.liberarTudo(); System.out.println("\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros."); return;
                    }
                    var execs = interp.listarOperacoesExecutaveis();
                    if(execs.isEmpty()){ System.err.println("[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal."); System.exit(1); }
                    var prim = execs.get(0);
                    System.out.println("[REGRA] " + prim.regra().nome() + (prim.regra().identificador()!=null?" ("+prim.regra().identificador()+")":"") + " :: " + prim.operacao().nome() + "()\n");
                    Map<String,ValorThz> a = construirArgsOperacao(prim.operacao(), ast, v->interp.validarInvariantes(v));
                    ValorThz res = interp.executarOperacao(prim.operacao().nome(), a);
                    System.out.println("--------------------------------------------------------------");
                    if(res!=null) System.out.println("[RESULTADO] " + interp.formatar(res));
                    arena.liberarTudo(); System.out.println("\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.");
                    return;
                }
            } catch (Exception ex) {
                String msg = ex.getMessage()!=null?ex.getMessage():"";
                var m = java.util.regex.Pattern.compile("\\[Linha (\\d+):(\\d+)\\]").matcher(msg);
                if(m.find()){
                    int linha=Integer.parseInt(m.group(1)), col=Integer.parseInt(m.group(2));
                    System.err.println(Diagnosticos.formatarDiagnosticos(fonte, List.of(new DiagnosticoEntrada(linha,col,msg)), "").get(0));
                } else System.err.println(msg);
                System.exit(1);
            }
        } else {
            System.err.println("Comando desconhecido: "+comando+" (use check|ast|fmt|run|repl|gui)"); System.exit(1);
        }
    }

    private static final Object[][] LOTE = {
        new Object[]{"a1b2c3d4-0000-0000-0000-000000000001","PROD-SKU-901",10,"150.5000","18.00","0"},
        new Object[]{"a1b2c3d4-0000-0000-0000-000000000002","PROD-SKU-902",5,"320.0000","12.00","0"}
    };
    private static ValorThz registroDe(EstruturaAst est, Object[] vals, java.util.function.Consumer<ValorThz> validar){
        Map<String,ValorThz> campos=new HashMap<>();
        for(int i=0;i<est.campos().size();i++){
            var campo=est.campos().get(i);
            Object bruto=i<vals.length?vals[i]:null;
            if(bruto!=null) campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(), bruto));
            else if(campo.tipo().startsWith("NATURAL")||campo.tipo().startsWith("INTEIRO")) campos.put(campo.nome(), ValorThz.INTEIRO(java.math.BigInteger.ZERO));
            else if(campo.tipo().startsWith("DECIMAL")||campo.tipo().startsWith("MONETARIO")) campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(),"0"));
            else campos.put(campo.nome(), InterpretadorThz.valorThzDe(campo.tipo(),""));
        }
        ValorThz reg=new ValorThz.Registro(est.nome(), campos);
        if(validar!=null) validar.accept(reg);
        return reg;
    }
    private static Map<String,ValorThz> construirArgsOperacao(OperacaoAst op, ProgramaAst ast, java.util.function.Consumer<ValorThz> validar){
        Map<String,ValorThz> out=new HashMap<>();
        for(ParametroOperacaoAst p: op.parametros()){
            var m=java.util.regex.Pattern.compile("^FATIA\\[(\\w+)\\]$").matcher(p.tipo());
            if(m.matches()){
                EstruturaAst est=ast.estruturas().stream().filter(e->e.nome().equals(m.group(1))).findFirst().orElse(null);
                if(est==null) throw new RuntimeException("Estrutura '"+m.group(1)+"' referenciada por '"+p.tipo()+"' não declarada.");
                List<ValorThz> elems=new ArrayList<>(); for(Object[] linha: LOTE) elems.add(registroDe(est, linha, validar));
                out.put(p.nome(), new ValorThz.Fatia(m.group(1), elems));
            } else out.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), 0));
        }
        return out;
    }
    private static Map<String,ValorThz> construirArgsProc(ProcedimentoAst proc, Map<String,String> mapa){
        Map<String,ValorThz> out=new HashMap<>();
        for(ParametroOperacaoAst p: proc.parametros()){
            String bruto=mapa.get(p.nome());
            if(bruto==null) throw new RuntimeException("[Erro de Execução] Parâmetro '"+p.nome()+"' não fornecido. Use --arg "+p.nome()+"=valor");
            out.put(p.nome(), InterpretadorThz.valorThzDe(p.tipo(), bruto));
        }
        return out;
    }
    private static Map<String,String> parseArgsMapa(List<String> args){
        Map<String,String> mapa=new HashMap<>();
        for(int i=0;i<args.size();i++){
            String a=args.get(i);
            if(a.equals("--arg") && i+1<args.size()){ String par=args.get(++i); int eq=par.indexOf('='); if(eq>=0) mapa.put(par.substring(0,eq),par.substring(eq+1)); }
            else if(a.startsWith("--arg=")){ String par=a.substring(6); int eq=par.indexOf('='); if(eq>=0) mapa.put(par.substring(0,eq),par.substring(eq+1)); }
        }
        return mapa;
    }
    private static String resolverArquivo(List<String> args){
        Set<String> flags=new HashSet<>(Set.of("--saida","--principal","--arg"));
        for(int i=0;i<args.size();i++){
            String a=args.get(i);
            if(flags.contains(a)){ i++; continue; }
            if(a.startsWith("--arg=")) continue;
            if(a.startsWith("-")) continue;
            return a;
        }
        return "exemplos/faturamento.thz";
    }
    private static boolean precisaEntrada(ProgramaAst ast){
        java.util.function.Predicate<List<ComandoAst>> temLer = new java.util.function.Predicate<>(){ public boolean test(List<ComandoAst> cmds){ for(ComandoAst c: cmds){ if(c instanceof ComandoAst.Ler) return true; if(c instanceof ComandoAst.Se s) if(test(s.entao())||test(s.senao())) return true; if(c instanceof ComandoAst.Enquanto e) if(test(e.corpo())) return true; if(c instanceof ComandoAst.Para p) if(test(p.corpo())) return true; if(c instanceof ComandoAst.VetorizarPara v) if(test(v.corpo())) return true; if(c instanceof ComandoAst.BlocoMemoria b) if(test(b.corpo())) return true; } return false; } };
        List<List<ComandoAst>> todos=new ArrayList<>();
        for(RegraNegocioAst r: ast.regras()) for(OperacaoAst o: r.operacoes()) todos.add(o.corpo());
        if(ast.procedimentos()!=null) for(ProcedimentoAst p: ast.procedimentos()) todos.add(p.corpo());
        for(List<ComandoAst> c: todos) if(temLer.test(c)) return true;
        return false;
    }
    private static java.util.function.Supplier<String> criarLeitorEntrada(){
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        return ()->{ try{ String l=br.readLine(); return l; } catch(IOException e){ return null; } };
    }
    private static String q(String s){ return "\""+(s!=null?s:"")+"\""; }
}
