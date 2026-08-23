package thz.lang.formato;

import thz.lang.ast.*;
import thz.lang.sintatico.ThzParser;
import java.util.List;

public final class Formatador {
    private static final String IND = "    ";
    private Formatador() {}

    private static String tipoCanonico(String tipo) {
        return tipo.replaceAll("\\s+", "").replace(",", ", ");
    }
    private static String linha(String v, int nivel) { return IND.repeat(nivel) + v; }
    private static String formatarExpr(ExprAst e) { return ThzParser.textoCanonicoDe(e); }

    private static List<String> formatarComandos(List<ComandoAst> comandos, int nivel) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.DeclVariavel d ->
                    out.add(linha("VARIAVEL " + d.nome() + " : " + tipoCanonico(d.tipoDado()) + " <- " + formatarExpr(d.inicializacao()), nivel));
                case ComandoAst.Atribuicao a ->
                    out.add(linha(String.join(".", a.alvo()) + " <- " + formatarExpr(a.expressao()), nivel));
                case ComandoAst.Se s -> {
                    out.add(linha("SE " + formatarExpr(s.condicao()), nivel));
                    out.addAll(formatarComandos(s.entao(), nivel + 1));
                    if (!s.senao().isEmpty()) {
                        out.add(linha("SENAO", nivel));
                        out.addAll(formatarComandos(s.senao(), nivel + 1));
                    }
                    out.add(linha("FIM_SE", nivel));
                }
                case ComandoAst.Enquanto e -> {
                    out.add(linha("ENQUANTO " + formatarExpr(e.condicao()), nivel));
                    out.addAll(formatarComandos(e.corpo(), nivel + 1));
                    out.add(linha("FIM_ENQUANTO", nivel));
                }
                case ComandoAst.VetorizarPara v -> {
                    String passo = v.passoSimd() != null ? " PASSO_SIMD " + v.passoSimd() : "";
                    out.add(linha("VETORIZAR_PARA " + v.variavel() + " EM " + String.join(".", v.fonte()) + passo, nivel));
                    out.addAll(formatarComandos(v.corpo(), nivel + 1));
                    out.add(linha("FIM_PARA", nivel));
                }
                case ComandoAst.Para p -> {
                    String passo = p.passo() != null ? " PASSO " + formatarExpr(p.passo()) : "";
                    out.add(linha("PARA " + p.variavel() + " DE " + formatarExpr(p.inicio()) + " ATE " + formatarExpr(p.fim()) + passo, nivel));
                    out.addAll(formatarComandos(p.corpo(), nivel + 1));
                    out.add(linha("FIM_PARA", nivel));
                }
                case ComandoAst.BlocoMemoria b -> {
                    out.add(linha("USAR_BLOCO_MEMORIA " + b.nome(), nivel));
                    out.addAll(formatarComandos(b.corpo(), nivel + 1));
                    out.add(linha("FIM_BLOCO_MEMORIA", nivel));
                }
                case ComandoAst.Exiba e ->
                    out.add(linha("EXIBA " + formatarExpr(e.expressao()), nivel));
                case ComandoAst.Ler l ->
                    out.add(linha("LER " + String.join(".", l.alvo()), nivel));
                case ComandoAst.Chamada ch ->
                    out.add(linha(formatarExpr(ch.expressao()), nivel));
                case ComandoAst.Retorne r ->
                    out.add(linha(r.expressao() != null ? "RETORNE " + formatarExpr(r.expressao()) : "RETORNE", nivel));
                case ComandoAst.FalharCom f ->
                    out.add(linha("FALHAR_COM " + formatarExpr(f.expressao()), nivel));
            }
        }
        return out;
    }


    public static String formatar(ProgramaAst ast) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (ast.versaoLinguagem() != null) { out.add("VERSAO_LINGUAGEM \"" + ast.versaoLinguagem() + "\""); out.add(""); }
        out.add("PROGRAMA " + ast.nome()); out.add("");
        if (ast.metadados() != null) {
            out.add("METADADOS_ARQUITETURA");
            var m = ast.metadados();
            if (m.dominio() != null) out.add(linha("DOMINIO: \"" + m.dominio() + "\"", 1));
            if (m.subdominio() != null) out.add(linha("SUBDOMINIO: \"" + m.subdominio() + "\"", 1));
            if (m.camada() != null) out.add(linha("CAMADA: \"" + m.camada() + "\"", 1));
            if (m.versao() != null) out.add(linha("VERSAO: \"" + m.versao() + "\"", 1));
            if (m.autor() != null) out.add(linha("AUTOR: \"" + m.autor() + "\"", 1));
            if (m.sloLatencia() != null) out.add(linha("SLO_LATENCIA_MAXIMA: \"" + m.sloLatencia() + "\"", 1));
            if (m.conformidade() != null && !m.conformidade().isEmpty()) {
                String lista = m.conformidade().stream().map(c -> "\"" + c + "\"").reduce((a,b)->a+", "+b).orElse("");
                out.add(linha("CONFORMIDADE: " + lista, 1));
            }
            out.add("FIM_METADADOS"); out.add("");
        }
        for (EstruturaAst est : ast.estruturas()) {
            String layout = est.layoutColunar() ? " LAYOUT_COLUNAR" : "";
            out.add("ESTRUTURA " + est.nome() + layout);
            for (CampoEstruturaAst campo : est.campos()) out.add(linha(campo.nome() + " : " + tipoCanonico(campo.tipo()), 1));
            for (InvarianteAst inv : est.invariantes()) out.add(linha("INVARIANTE " + inv.textoCanonico(), 1));
            out.add("FIM_ESTRUTURA"); out.add("");
        }
        for (EnumeracaoAst en : ast.enumeracoes()) {
            out.add("ENUMERACAO " + en.nome());
            for (String mem : en.membros()) out.add(linha(mem, 1));
            out.add("FIM_ENUMERACAO"); out.add("");
        }
        for (RegraNegocioAst regra : ast.regras()) {
            out.add("REGRA_NEGOCIO " + regra.nome());
            if (regra.identificador() != null) out.add(linha("IDENTIFICADOR_REGRA: \"" + regra.identificador() + "\"", 1));
            if (regra.rastreioRequisito() != null) out.add(linha("RASTREIO_REQUISITO: \"" + regra.rastreioRequisito() + "\"", 1));
            if (regra.descricao() != null) out.add(linha("DESCRICAO: \"" + regra.descricao() + "\"", 1));
            if (!regra.clausulasEntrada().isEmpty()) {
                out.add(linha("CONTRATO_ENTRADA", 1));
                for (ClausulaContratoAst c : regra.clausulasEntrada()) out.add(linha("EXIGE " + c.textoCanonico(), 2));
                out.add(linha("FIM_CONTRATO_ENTRADA", 1));
            }
            if (!regra.clausulasSaida().isEmpty()) {
                out.add(linha("CONTRATO_SAIDA", 1));
                for (ClausulaContratoAst c : regra.clausulasSaida()) out.add(linha("GARANTE " + c.textoCanonico(), 2));
                out.add(linha("FIM_CONTRATO_SAIDA", 1));
            }
            for (OperacaoAst op : regra.operacoes()) {
                String params = op.parametros().stream().map(p->p.nome()+": "+tipoCanonico(p.tipo())).reduce((a,b)->a+", "+b).orElse("");
                out.add(linha("OPERACAO " + op.nome() + "(" + params + ") : " + tipoCanonico(op.tipoRetorno()), 1));
                if (!op.corpo().isEmpty()) {
                    out.add(linha("INICIO", 1));
                    out.addAll(formatarComandos(op.corpo(), 2));
                    out.add(linha("FIM", 1));
                }
            }
            out.add("FIM_REGRA_NEGOCIO"); out.add("");
        }
        for (ProcedimentoAst proc : ast.procedimentos() != null ? ast.procedimentos() : List.<ProcedimentoAst>of()) {
            String params = proc.parametros().stream().map(p->p.nome()+": "+tipoCanonico(p.tipo())).reduce((a,b)->a+", "+b).orElse("");
            out.add("PROCEDIMENTO " + proc.nome() + "(" + params + ")");
            if (!proc.corpo().isEmpty()) {
                out.add(linha("INICIO", 1));
                out.addAll(formatarComandos(proc.corpo(), 2));
                out.add(linha("FIM", 1));
            }
            out.add("");
        }
        out.add("FIM_PROGRAMA"); out.add("");
        return String.join("\n", out);
    }
}
