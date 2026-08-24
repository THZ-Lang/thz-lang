package thz.lang.api.service;

import org.springframework.stereotype.Service;
import thz.lang.ast.ProgramaAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.api.dto.*;
import thz.lang.formato.Formatador;
import thz.lang.formato.JsonEscritor;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.docgen.ThzDocGen;
import thz.lang.interpretador.InterpretadorThz;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ThzService {

    private static final Pattern LINHA_COLUNA = Pattern.compile("\\[Linha (\\d+):(\\d+)\\]");

    /**
     * Pipeline completo: léxico -> sintático -> semântico.
     * Retorna a lista unificada de diagnósticos + AST + símbolos.
     */
    public AnaliseResult analisar(String fonte, boolean estrito) {
        List<DiagnosticoApi> diagnosticos = new ArrayList<>();
        ProgramaAst ast = null;

        // 1) Léxico
        List<Token> tokens;
        try {
            tokens = new ThzLexer(fonte).tokenize();
        } catch (Exception e) {
            LinhaColuna lc = extrairLinhaColuna(e.getMessage());
            diagnosticos.add(new DiagnosticoApi(lc.linha, lc.coluna, e.getMessage(), "lexico", "erro"));
            return new AnaliseResult(null, diagnosticos, List.of(), true, List.of(), null);
        }

        // 2) Sintático
        try {
            ast = new ThzParser(tokens).parse();
        } catch (Exception e) {
            LinhaColuna lc = extrairLinhaColuna(e.getMessage());
            diagnosticos.add(new DiagnosticoApi(lc.linha, lc.coluna, e.getMessage(), "sintatico", "erro"));
            List<String> textoDiags = Diagnosticos.formatarDiagnosticos(
                    fonte,
                    diagnosticos.stream().map(d -> new DiagnosticoEntrada(d.linha(), d.coluna(), d.mensagem())).toList(),
                    ""
            );
            return new AnaliseResult(null, diagnosticos, textoDiags, true, List.of(), null);
        }

        // 3) Semântico
        List<ErroSemantico> errosSemanticos = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
        for (ErroSemantico e : errosSemanticos) {
            diagnosticos.add(new DiagnosticoApi(e.linha(), e.coluna(), e.mensagem(), "semantico", "erro"));
        }

        boolean temErros = !diagnosticos.isEmpty();
        List<String> textoDiags = temErros
                ? Diagnosticos.formatarDiagnosticos(
                        fonte,
                        diagnosticos.stream().map(d -> new DiagnosticoEntrada(d.linha(), d.coluna(), d.mensagem())).toList(),
                        ""
                )
                : List.of();

        List<SimboloApi> simbolos = extrairSimbolos(ast, tokens);
        String astJson = JsonEscritor.paraJson(ast);

        return new AnaliseResult(astJson, diagnosticos, textoDiags, temErros, simbolos, astJson);
    }

    /**
     * Formata o código fonte THZ de forma canônica e idempotente.
     */
    public FormatacaoResult formatar(String fonte) {
        AnaliseResult r = analisar(fonte, false);
        if (r.astJson() == null) {
            return new FormatacaoResult(fonte, false);
        }
        ProgramaAst ast = parseAstDaJson(r.astJson(), fonte);
        if (ast == null) return new FormatacaoResult(fonte, false);
        String fmt = Formatador.formatar(ast);
        return new FormatacaoResult(fmt, !fmt.equals(fonte));
    }

    /**
     * Gera documentação Markdown + Mermaid a partir do código fonte.
     */
    public DocumentacaoResult gerarDocumentacao(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new DocumentacaoResult(null);
        String markdown = ThzDocGen.gerarDocumentacao(ast);
        return new DocumentacaoResult(markdown);
    }

    /**
     * Realiza auditoria de governança (G4).
     */
    public AuditoriaResult auditar(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new AuditoriaResult(null, null);
        RelatorioAuditoria relatorio = AuditorGovernanca.auditar(ast);
        String json = serializarRelatorio(relatorio);
        String markdown = thz.lang.governanca.AuditorGovernanca.gerarMarkdownGovernanca(relatorio);
        return new AuditoriaResult(json, markdown);
    }

    /**
     * Gera a representação intermediária THZ-IR/1.
     */
    public IrResult gerarIr(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new IrResult(null, null);
        IrPrograma ir = GeradorIr.baixarParaIr(ast);
        String json = GeradorIr.serializarIr(ir);
        return new IrResult(json, null);
    }

    /**
     * Valida regras de vetorização SIMD (R1-R5).
     */
    public SimdResult validarSimd(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new SimdResult(List.of());
        List<ResultadoValidacaoSimd> resultados = ValidadorSimd.analisarTudo(ast);
        List<SimdResultadoApi> api = resultados.stream().map(r ->
                new SimdResultadoApi(
                        r.loopIdentificador(),
                        r.variavel(),
                        r.passoSimd(),
                        r.vetorizavel(),
                        r.regrasAtendidas(),
                        r.violacoes(),
                        r.avisos()
                )
        ).toList();
        return new SimdResult(api);
    }

    /**
     * Executa o código THZ e retorna a saída.
     */
    public ExecucaoResult executar(String fonte, String operacao) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new ExecucaoResult(List.of(), List.of("Falha ao parsear o código"), null);

        List<String> saida = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            InterpretadorThz interp = new InterpretadorThz(ast, saida::add, null);
            if (operacao != null && !operacao.isBlank()) {
                interp.executarOperacao(operacao);
            } else {
                // Lista operações disponíveis
                var ops = interp.listarOperacoesExecutaveis();
                if (!ops.isEmpty()) {
                    interp.executarOperacao(ops.getFirst().operacao().nome());
                } else {
                    var procs = interp.listarProcedimentos();
                    if (!procs.isEmpty()) {
                        interp.executarProcedimento(procs.getFirst().nome());
                    }
                }
            }
        } catch (Exception e) {
            erros.add(e.getMessage());
        }

        return new ExecucaoResult(saida, erros, null);
    }

    /**
     * Retorna o AST como JSON.
     */
    public AstResult obterAst(String fonte) {
        ProgramaAst ast = parseAst(fonte);
        if (ast == null) return new AstResult(null, null);
        return new AstResult(JsonEscritor.paraJson(ast), ast.nome());
    }

    /**
     * Resolve hover (tipo/assinatura) para a posição informada.
     */
    public HoverResult obterHover(String fonte, int linha, int coluna) {
        AnaliseResult r = analisar(fonte, false);
        if (r.astJson() == null || r.simbolos() == null) return null;

        // Encontra a palavra na posição
        String[] linhas = fonte.split("\\r?\\n", -1);
        if (linha - 1 >= linhas.length || linha - 1 < 0) return null;
        String conteudo = linhas[linha - 1];
        int idx = Math.max(0, Math.min(coluna - 1, conteudo.length()));

        if (idx >= conteudo.length()) return null;
        char c = conteudo.charAt(idx);
        if (!Character.isLetterOrDigit(c) && c != '_') return null;

        int ini = idx;
        int fim = idx;
        while (ini > 0 && Character.isLetterOrDigit(conteudo.charAt(ini - 1)) || ini > 0 && conteudo.charAt(ini - 1) == '_') ini--;
        while (fim < conteudo.length() && Character.isLetterOrDigit(conteudo.charAt(fim)) || fim < conteudo.length() && conteudo.charAt(fim) == '_') fim++;

        String palavra = conteudo.substring(ini, fim);
        if (palavra.isBlank()) return null;

        // Busca o símbolo
        for (SimboloApi s : r.simbolos()) {
            if (s.nome().equals(palavra)) {
                String conteudoHover = switch (s.categoria()) {
                    case "estrutura" -> "**ESTRUTURA** `" + palavra + "`";
                    case "campo" -> "**campo** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "` — em `" + s.container() + "`";
                    case "enumeracao" -> "**ENUMERACAO** `" + palavra + "`";
                    case "membro-enum" -> "**membro** `" + palavra + "` : `" + (s.container() != null ? s.container() : "ENUMERACAO") + "`";
                    case "regra" -> "**REGRA_NEGOCIO** `" + palavra + "`";
                    case "operacao" -> "**OPERACAO** `" + palavra + "(" + (s.detalhe() != null ? s.detalhe() : "") + ")` — em `" + (s.container() != null ? s.container() : "") + "`";
                    case "parametro" -> "**parametro** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "`";
                    case "variavel" -> "**variavel** `" + palavra + "` : `" + (s.detalhe() != null ? s.detalhe() : "?") + "`";
                    case "programa" -> "**PROGRAMA** `" + palavra + "`";
                    default -> "**" + s.categoria() + "** `" + palavra + "`";
                };
                return new HoverResult(conteudoHover, new HoverResponse.HoverRange(linha, ini + 1, palavra.length()));
            }
        }

        return null;
    }

    // ---- Helpers internos ----

    private ProgramaAst parseAst(String fonte) {
        try {
            List<Token> tokens = new ThzLexer(fonte).tokenize();
            return new ThzParser(tokens).parse();
        } catch (Exception e) {
            return null;
        }
    }

    private ProgramaAst parseAstDaJson(String json, String fonte) {
        return parseAst(fonte);
    }

    private LinhaColuna extrairLinhaColuna(String mensagem) {
        if (mensagem == null) return new LinhaColuna(1, 1);
        Matcher m = LINHA_COLUNA.matcher(mensagem);
        if (m.find()) {
            return new LinhaColuna(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }
        return new LinhaColuna(1, 1);
    }

    private List<SimboloApi> extrairSimbolos(ProgramaAst ast, List<Token> tokens) {
        List<SimboloApi> simbolos = new ArrayList<>();

        // Índice token → lookup rápido
        var posPorNome = tokens.stream()
                .filter(t -> t != null && t.type() == TokenType.IDENTIFICADOR)
                .collect(Collectors.groupingBy(
                        t -> t.value(),
                        Collectors.mapping(t -> new int[]{t.line(), t.column()}, Collectors.toList())
                ));

        java.util.function.Function<String, int[]> primeiraPos = (String nome) -> {
            var lista = posPorNome.get(nome);
            return (lista != null && !lista.isEmpty()) ? lista.getFirst() : new int[]{1, 1};
        };

        // Programa
        {
            int[] p = primeiraPos.apply(ast.nome());
            simbolos.add(new SimboloApi(ast.nome(), "programa", null, p[0], p[1], null));
        }

        // Estruturas
        if (ast.estruturas() != null) {
            for (var e : ast.estruturas()) {
                int[] p = primeiraPos.apply(e.nome());
                simbolos.add(new SimboloApi(e.nome(), "estrutura", e.layoutColunar() ? "LAYOUT_COLUNAR" : null, p[0], p[1], null));
                if (e.campos() != null) {
                    for (var c : e.campos()) {
                        int[] pc = primeiraPos.apply(c.nome());
                        simbolos.add(new SimboloApi(c.nome(), "campo", c.tipo(), pc[0], pc[1], e.nome()));
                    }
                }
            }
        }

        // Enumerações
        if (ast.enumeracoes() != null) {
            for (var en : ast.enumeracoes()) {
                int[] p = primeiraPos.apply(en.nome());
                simbolos.add(new SimboloApi(en.nome(), "enumeracao", null, p[0], p[1], null));
                if (en.membros() != null) {
                    for (String m : en.membros()) {
                        int[] pm = primeiraPos.apply(m);
                        simbolos.add(new SimboloApi(m, "membro-enum", null, pm[0], pm[1], en.nome()));
                    }
                }
            }
        }

        // Regras de negócio
        if (ast.regras() != null) {
            for (var regra : ast.regras()) {
                int[] pr = primeiraPos.apply(regra.nome());
                simbolos.add(new SimboloApi(regra.nome(), "regra", regra.identificador(), pr[0], pr[1], null));
                if (regra.operacoes() != null) {
                    for (var op : regra.operacoes()) {
                        int[] po = primeiraPos.apply(op.nome());
                        String assinatura = op.parametros() != null
                                ? op.parametros().stream().map(p -> p.nome() + ": " + p.tipo()).collect(Collectors.joining(", "))
                                : "";
                        assinatura += " : " + op.tipoRetorno();
                        simbolos.add(new SimboloApi(op.nome(), "operacao", assinatura, po[0], po[1], regra.nome()));
                        if (op.parametros() != null) {
                            for (var param : op.parametros()) {
                                int[] pp = primeiraPos.apply(param.nome());
                                simbolos.add(new SimboloApi(param.nome(), "parametro", param.tipo(), pp[0], pp[1], op.nome()));
                            }
                        }
                    }
                }
            }
        }

        return simbolos;
    }

    private String serializarRelatorio(RelatorioAuditoria r) {
        if (r == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"nomePrograma\": \"").append(escape(r.nomePrograma())).append("\",\n");
        sb.append("  \"metricas\": {\n");
        sb.append("    \"totalRegras\": ").append(r.metricas().totalRegras()).append(",\n");
        sb.append("    \"regrasComRastreio\": ").append(r.metricas().regrasComRastreio()).append(",\n");
        sb.append("    \"totalContratosExige\": ").append(r.metricas().totalContratosExige()).append(",\n");
        sb.append("    \"totalContratosGarante\": ").append(r.metricas().totalContratosGarante()).append(",\n");
        sb.append("    \"totalInvariantes\": ").append(r.metricas().totalInvariantes()).append(",\n");
        sb.append("    \"totalOperacoesIdempotentes\": ").append(r.metricas().totalOperacoesIdempotentes()).append(",\n");
        sb.append("    \"percentualConformidade\": ").append(r.metricas().percentualConformidade()).append(",\n");
        sb.append("    \"aprovado\": ").append(r.metricas().aprovado()).append("\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record LinhaColuna(int linha, int coluna) {}

    // ---- Records internos para resultados ----
    public record AnaliseResult(String astJson, List<DiagnosticoApi> diagnosticos, List<String> textoDiagnosticos,
                                boolean temErros, List<SimboloApi> simbolos, String ast) {}
    public record FormatacaoResult(String resultado, boolean alterou) {}
    public record DocumentacaoResult(String markdown) {}
    public record AuditoriaResult(String relatorioJson, String markdown) {}
    public record IrResult(String irJson, String llvm) {}
    public record SimdResult(List<SimdResultadoApi> resultados) {}
    public record ExecucaoResult(List<String> saida, List<String> erros, String resultado) {}
    public record AstResult(String astJson, String nomePrograma) {}
    public record HoverResult(String conteudo, HoverResponse.HoverRange range) {}
}
