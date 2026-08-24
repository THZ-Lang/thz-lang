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
import thz.lang.runtime.BlocoMemoria;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ThzCli {
    public static void main(String[] args) throws Exception {
        BibliotecaConsole.registrar();

        if (args.length == 0 || args[0].equals("--ajuda") || args[0].equals("-h") || args[0].equals("ajuda") || args[0].equals("help")) {
            exibirAjuda();
            return;
        }

        if (args[0].equals("--versao") || args[0].equals("-v") || args[0].equals("versao") || args[0].equals("version")) {
            System.out.println("THZ-LANG Engine v2.4.0 (GraalVM / Java 25)");
            return;
        }

        String comando = args[0];
        List<String> argumentos = new ArrayList<>(Arrays.asList(args));
        argumentos.remove(0);
        boolean estrito = argumentos.contains("--estrito");
        if (comando.equals("repl")) {
            thz.lang.repl.Repl.executar();
            return;
        }
        if (comando.equals("gui")) {
            lancarGuiSeDisponivel();
            return;
        }
        String arquivo = resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank()) {
            System.err.println("[ERRO] Nenhum arquivo .thz ou .thzui especificado. Use: thz " + comando + " <caminho.thz|caminho.thzui>");
            System.exit(1);
        }
        if (comando.equals("dev") || comando.equals("serve")) {
            String arquivoDev = resolverArquivo(argumentos);
            int porta = 8080;
            ThzDevServer.iniciar(arquivoDev, porta);
            return;
        }
        if (comando.equals("check") || comando.equals("ast") || comando.equals("fmt") || comando.equals("run") || comando.equals("audit") || comando.equals("doc") || comando.equals("ir") || comando.equals("ui")) {
            if (!Files.exists(Path.of(arquivo))) { System.err.println("[ERRO] Arquivo não encontrado: " + arquivo); System.exit(1); }
            String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);

            try {
                List<Token> tokens = new ThzLexer(fonte).tokenize();
                ProgramaAst ast = new ThzParser(tokens).parse();
                if (comando.equals("doc")) {
                    String idxSaida = null;
                    int idx = argumentos.indexOf("--saida");
                    if (idx >= 0 && idx + 1 < argumentos.size()) idxSaida = argumentos.get(idx + 1);

                    String doc = thz.lang.docgen.ThzDocGen.gerarDocumentacao(ast);
                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida) : Path.of(idxSaida, ast.nome() + "_documentacao.md");
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, doc, StandardCharsets.UTF_8);
                        System.out.println("[THZ DOC] Documentação gerada em: " + alvo);
                    } else {
                        System.out.println(doc);
                    }
                    return;
                }
                if (comando.equals("ir")) {
                    boolean llvm = argumentos.contains("--llvm");
                    String idxSaida = null;
                    int idx = argumentos.indexOf("--saida");
                    if (idx >= 0 && idx + 1 < argumentos.size()) idxSaida = argumentos.get(idx + 1);

                    String resultado = llvm
                            ? thz.lang.ir.GeradorIr.emitirLlvm(ast)
                            : thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast));

                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida) : Path.of(idxSaida, ast.nome() + (llvm ? ".ll" : "_ir.json"));
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, resultado, StandardCharsets.UTF_8);
                        System.out.println("[THZ IR] Saída (" + (llvm ? "LLVM IR" : "THZ-IR/1") + ") gravada em: " + alvo);
                    } else {
                        System.out.println(resultado);
                    }
                    return;
                }
                if (comando.equals("ui")) {
                    boolean html = argumentos.contains("--html");
                    var maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                        c.adicionar(thz.lang.ui.ThzUiMaker.card("card_" + ast.nome(), ast.nome(), card -> {
                            card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_modulo", "info", "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "]"));
                            if (ast.procedimentos() != null) {
                                for (var p : ast.procedimentos()) {
                                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_" + p.nome(), p.nome(), p.nome()));
                                }
                            }
                        }));
                    });
                    if (html) {
                        System.out.println(maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass()));
                    } else {
                        System.out.println(maker.gerarCodigoThz(ast.nome()));
                    }
                    return;
                }
                if (comando.equals("audit")) {
                    boolean emJson = argumentos.contains("--json");
                    boolean auditGit = argumentos.contains("--git");
                    String idxSaida = null;
                    int idx = argumentos.indexOf("--saida");
                    if (idx >= 0 && idx + 1 < argumentos.size()) idxSaida = argumentos.get(idx + 1);

                    thz.lang.governanca.RelatorioAuditoria rel = thz.lang.governanca.AuditorGovernanca.auditar(ast);
                    String resultado = emJson
                            ? thz.lang.governanca.AuditorGovernanca.gerarJsonGovernanca(rel)
                            : thz.lang.governanca.AuditorGovernanca.gerarMarkdownGovernanca(rel);

                    if (auditGit) {
                        var gitRel = thz.lang.governanca.ThzGitAuditEngine.auditarGit(ast, ".");
                        resultado += "\n\n### 🌿 Auditoria de Governança Git\n"
                                + "- **Branch:** " + gitRel.branchAtual() + "\n"
                                + "- **Requisitos Impactados:** " + gitRel.requisitosImpactados() + "\n"
                                + "- **Alertas:** " + gitRel.alertasGovernanca() + "\n";
                    }

                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida) : Path.of(idxSaida, ast.nome() + "_auditoria." + (emJson ? "json" : "md"));
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, resultado, StandardCharsets.UTF_8);
                        System.out.println("[THZ AUDIT] Relatório de governança gravado em: " + alvo);
                    } else {
                        System.out.println(resultado);
                    }

                    if (estrito && !rel.metricas().aprovado()) {
                        System.err.println("\n[THZ AUDIT] Falha de conformidade estrita: o programa possui pendências críticas de governança.");
                        System.exit(1);
                    }
                    return;
                }

                if (comando.equals("check")) {
                    List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
                    if (!erros.isEmpty()) {
                        List<DiagnosticoEntrada> diags = erros.stream().map(e->new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
                        for (String bloco : Diagnosticos.formatarDiagnosticos(fonte, diags, "Semântico")) System.err.println(bloco + "\n");
                        System.err.println("[THZ CHECK] " + erros.size() + " erro(s) semântico(s).");
                        System.exit(1);
                    }
                    String versao = "";
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
                    BlocoMemoria blocoMemoria = new BlocoMemoria(64); blocoMemoria.alocar(2048);
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
                            Map<String, ValorThz> a = InjetorLoteDemo.construirArgsProc(proc, p -> mapaArgs.get(p.nome()));
                            interp.executarProcedimento(proc.nome(), a);
                            blocoMemoria.liberarTudo(); System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso."); return;
                        }

                        var ops = interp.listarOperacoesExecutaveis().stream().filter(o->o.operacao().nome().equals(nomePrincipal)).findFirst().orElse(null);
                        if (ops != null) {
                            System.out.println("[REGRA] " + ops.regra().nome() + (ops.regra().identificador()!=null?" ("+ops.regra().identificador()+")":"")+" :: "+ops.operacao().nome()+"()\n");
                            Map<String, ValorThz> a = InjetorLoteDemo.construirArgsOperacao(ops.operacao(), ast, interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
                            ValorThz res = interp.executarOperacao(ops.operacao().nome(), a);
                            System.out.println("--------------------------------------------------------------");
                            if(res!=null) System.out.println("[RESULTADO] " + interp.formatar(res));
                            blocoMemoria.liberarTudo(); System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso."); return;
                        }
                        System.err.println("[ERRO] Entrada '--principal "+nomePrincipal+"' não encontrada como PROCEDIMENTO nem OPERACAO."); System.exit(1);
                    }
                    var procs = interp.listarProcedimentos();
                    if (!procs.isEmpty()) {
                        var proc = procs.stream().filter(p -> p.nome().equalsIgnoreCase("Principal")).findFirst().orElse(procs.get(0));
                        System.out.println("[PROCEDIMENTO] " + proc.nome() + "()\n");
                        Map<String, ValorThz> a = proc.parametros().isEmpty() ? Map.of() : InjetorLoteDemo.construirArgsProc(proc, p -> mapaArgs.get(p.nome()));
                        interp.executarProcedimento(proc.nome(), a);
                        blocoMemoria.liberarTudo(); System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                        return;
                    }
                    var execs = interp.listarOperacoesExecutaveis();
                    if(execs.isEmpty()){ System.err.println("[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal."); System.exit(1); }
                    var prim = execs.get(0);
                    System.out.println("[REGRA] " + prim.regra().nome() + (prim.regra().identificador()!=null?" ("+prim.regra().identificador()+")":"") + " :: " + prim.operacao().nome() + "()\n");
                    Map<String,ValorThz> a = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), ast, interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
                    ValorThz res = interp.executarOperacao(prim.operacao().nome(), a);
                    System.out.println("--------------------------------------------------------------");
                    if(res!=null) System.out.println("[RESULTADO] " + interp.formatar(res));
                    blocoMemoria.liberarTudo(); System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
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
            System.err.println("Comando desconhecido: " + comando + " (use: check | run | fmt | ast | audit | doc | ir | repl | gui | --ajuda)");
            System.exit(1);
        }
    }

    private static void exibirAjuda() {
        System.out.println("================================================================================");
        System.out.println("   THZ-LANG Engine — JVM (v2.3.0)");
        System.out.println("   Linguagem Corporativa de Sistemas, Governança e Alta Performance");
        System.out.println("================================================================================\n");
        System.out.println("Uso:");
        System.out.println("  thz <comando> [arquivo.thz] [opções]\n");
        System.out.println("Comandos Disponíveis:");
        System.out.println("  check <arquivo> [--estrito]               Verifica a integridade sintática e semântica");
        System.out.println("  run <arquivo> [--principal <Nome>]        Executa o programa via interpretador com arena O(1)");
        System.out.println("  fmt <arquivo> [--check|--escrever|--saida] Formata o código canonicamente");
        System.out.println("  ast <arquivo>                             Exibe a AST (Abstract Syntax Tree) em JSON");
        System.out.println("  audit <arquivo> [--json] [--estrito]      Gera relatório de auditoria e governança (G4)");
        System.out.println("  doc <arquivo> [--saida <caminho.md>]      Gera documentação técnica com diagramas Mermaid");
        System.out.println("  ir <arquivo> [--llvm] [--saida <caminho>] Gera a Representação Intermediária (THZ-IR/1)");
        System.out.println("  ui <arquivo[.thzui]> [--html]             Renderiza ou exporta a interface declarativa (ThzUiMaker)");
        System.out.println("  repl                                      Inicia o shell interativo multi-linha");
        System.out.println("  gui                                       Abre a IDE Desktop Swing com realce e exemplos\n");
        System.out.println("Exemplos:");
        System.out.println("  thz check exemplos/faturamento.thz --estrito");
        System.out.println("  thz run exemplos/faturamento.thz");
        System.out.println("  thz audit exemplos/faturamento.thz");
        System.out.println("  thz doc exemplos/faturamento.thz --saida docs/faturamento.md");
        System.out.println("  thz gui");
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

    /**
     * Lança a IDE Desktop (módulo thz-gui) via reflexão, se presente no classpath.
     * Mantém thz-cli autônomo: sem dependência de compilação com thz-gui.
     */
    private static void lancarGuiSeDisponivel() {
        try {
            Class<?> gui = Class.forName("thz.lang.gui.ThzGui");
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    Object janela = gui.getConstructor().newInstance();
                    gui.getMethod("setVisible", boolean.class).invoke(janela, true);
                } catch (ReflectiveOperationException e) {
                    System.err.println("[ERRO] Falha ao iniciar a IDE Desktop: " + e.getMessage());
                    System.exit(1);
                }
            });
        } catch (ClassNotFoundException e) {
            System.err.println("[ERRO] IDE Desktop não encontrada no classpath. O módulo 'thz-gui' não está incluído nesta distribuição.");
            System.err.println("       Use: .\\gradlew :thz-gui:gui   ou inclua o JAR do thz-gui.");
            System.exit(1);
        }
    }
}
