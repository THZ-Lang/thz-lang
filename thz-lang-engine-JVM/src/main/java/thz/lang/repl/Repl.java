package thz.lang.repl;

import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.sintatico.ThzParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Repl {
    private Repl(){}
    public static void executar() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        List<String> declaracoesTopo = new ArrayList<>();
        List<String> corpo = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        System.out.println("THZ-LANG REPL v2.3 — digite \".ajuda\" para os comandos, \".sair\" para encerrar.");
        System.out.print("thz> ");
        System.out.flush();
        String linha;
        while ((linha = br.readLine()) != null) {
            String trim = linha.trim();
            if (buffer.isEmpty() && trim.startsWith(".")) {
                switch (trim) {
                    case ".sair", ".quit" -> { System.out.println("\n[SESSAO] Encerrada. Arena liberada."); return; }
                    case ".limpar" -> { declaracoesTopo.clear(); corpo.clear(); System.out.println("[SESSAO] Sessão reiniciada."); }
                    case ".codigo" -> System.out.println(montarPrograma(declaracoesTopo, corpo));
                    case ".ajuda" -> System.out.println("Bloco de comandos + <enter> em linha vazia avalia o bloco.\n.ajuda  esta ajuda\n.limpar reinicia a sessão\n.codigo exibe o programa acumulado\n.sair   encerra o REPL");
                    default -> System.out.println("[REPL] Comando desconhecido: '" + trim + "'. Use .ajuda.");
                }
                System.out.print("thz> "); System.out.flush(); continue;
            }
            if (trim.isEmpty()) {
                if (!buffer.isEmpty()) {
                    String chunk = String.join("\n", buffer);
                    buffer.clear();
                    avaliarChunk(chunk, declaracoesTopo, corpo);
                }
                System.out.print("thz> "); System.out.flush(); continue;
            }
            buffer.add(linha);
        }
        System.out.println("\n[SESSAO] Encerrada. Arena liberada.");
    }

    private static String montarPrograma(List<String> topo, List<String> corpo) {
        List<String> partes = new ArrayList<>();
        partes.add("VERSAO_LINGUAGEM \"2.3\"");
        partes.add("PROGRAMA SESSAO");
        partes.addAll(topo);
        partes.add("REGRA_NEGOCIO Sessao");
        partes.add("OPERACAO Principal() : DECIMAL(38, 10)");
        partes.add("INICIO");
        partes.addAll(corpo);
        partes.add("FIM");
        partes.add("FIM_REGRA_NEGOCIO");
        partes.add("FIM_PROGRAMA");
        return String.join("\n", partes);
    }

    private static void avaliarChunk(String chunk, List<String> topo, List<String> corpo) {
        String t = chunk.trim();
        if (t.startsWith("ESTRUTURA") || t.startsWith("ENUMERACAO")) { topo.add(chunk); System.out.println("[SESSAO] Declaração registrada."); return; }
        corpo.add(chunk);
        String fonte = montarPrograma(topo, corpo);
        int linhaInicioCorpo = 3 + topo.size() + 3 + 1;
        try {
            var tokens = new ThzLexer(fonte).tokenize();
            var ast = new ThzParser(tokens).parse();
            var erros = new AnalisadorSemantico(ast).analisar();
            if (!erros.isEmpty()) {
                for (ErroSemantico e : erros) System.err.println(Diagnosticos.formatarErroComCaret(chunk, new DiagnosticoEntrada(e.linha() - linhaInicioCorpo + 1, e.coluna(), e.mensagem())));
                corpo.remove(corpo.size()-1);
                return;
            }
            List<String> saidas = new ArrayList<>();
            var interp = new InterpretadorThz(ast, saidas::add, null);
            interp.executarOperacao("Principal", java.util.Map.of());
            for (String s : saidas) System.out.println(s);
        } catch (Exception ex) {
            String msg = ex.getMessage()!=null?ex.getMessage():"";
            var m = java.util.regex.Pattern.compile("\\[Linha (\\d+):(\\d+)\\]").matcher(msg);
            if (m.find()) {
                int gl = Integer.parseInt(m.group(1)), gc=Integer.parseInt(m.group(2));
                int local = gl - linhaInicioCorpo + 1;
                if (local>=1 && local<=chunk.split("\n",-1).length) System.err.println(Diagnosticos.formatarErroComCaret(chunk, new DiagnosticoEntrada(local, gc, msg)));
                else System.err.println(msg);
            } else System.err.println(msg);
            corpo.remove(corpo.size()-1);
        }
    }
}
