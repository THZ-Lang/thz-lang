package thz.lang.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.*;

import thz.lang.ast.ProgramaAst;
import thz.lang.formato.Formatador;
import thz.lang.governanca.AuditorGovernanca;
import thz.lang.governanca.RelatorioAuditoria;
import thz.lang.ir.GeradorIr;
import thz.lang.ir.IrPrograma;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.lexico.TokenType;
import thz.lang.sintatico.ThzParser;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ThzLanguageServerImpl implements LanguageServer, LanguageClientAware {

    private static final Pattern LINHA_COLUNA = Pattern.compile("\\[Linha (\\d+):(\\d+)\\]");

    private final ThzTextDocumentService textDocumentService = new ThzTextDocumentService(this);
    private final ThzWorkspaceService workspaceService = new ThzWorkspaceService();

    private LanguageClient client;
    private boolean lintEstrito = false;

    // Cache de documentos abertos: uri -> conteúdo
    private final ConcurrentHashMap<String, String> documentos = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        // Xtend gera setTextDocumentSync(Either<TextDocumentSyncKind, TextDocumentSyncOptions>)
        capabilities.setTextDocumentSync(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(TextDocumentSyncKind.Incremental));
        capabilities.setHoverProvider(true);

        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(".", " ", ":"));
        capabilities.setCompletionProvider(completionOptions);

        capabilities.setDocumentSymbolProvider(true);
        capabilities.setDefinitionProvider(true);
        capabilities.setDocumentFormattingProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    public LanguageClient getClient() {
        return client;
    }

    public boolean isLintEstrito() {
        return lintEstrito;
    }

    public void setLintEstrito(boolean lintEstrito) {
        this.lintEstrito = lintEstrito;
    }

    // ---- Endpoints Customizados (THZ Extensões) ----

    @JsonRequest("thz/audit")
    public CompletableFuture<Map<String, Object>> audit(Map<String, Object> params) {
        String uri = (String) params.get("uri");
        String fonte = obterDocumento(uri);
        if (fonte == null) return CompletableFuture.completedFuture(Map.of("error", "Documento não encontrado: " + uri));
        try {
            ResultadoAnalise r = analisar(fonte);
            if (r.ast() == null) return CompletableFuture.completedFuture(Map.of("error", "Não foi possível auditar programa com erros sintáticos."));
            RelatorioAuditoria relatorio = AuditorGovernanca.auditar(r.ast());
            String markdown = AuditorGovernanca.gerarMarkdownGovernanca(relatorio);
            return CompletableFuture.completedFuture(Map.of("markdown", markdown));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
        }
    }

    @JsonRequest("thz/ir")
    public CompletableFuture<Map<String, Object>> ir(Map<String, Object> params) {
        String uri = (String) params.get("uri");
        String fonte = obterDocumento(uri);
        if (fonte == null) return CompletableFuture.completedFuture(Map.of("error", "Documento não encontrado: " + uri));
        try {
            ResultadoAnalise r = analisar(fonte);
            if (r.ast() == null) return CompletableFuture.completedFuture(Map.of("error", "Não foi possível gerar IR de programa com erros sintáticos."));
            IrPrograma ir = GeradorIr.baixarParaIr(r.ast());
            String json = GeradorIr.serializarIrJson(ir);
            return CompletableFuture.completedFuture(Map.of("text", json));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
        }
    }

    @JsonRequest("thz/llvm")
    public CompletableFuture<Map<String, Object>> llvm(Map<String, Object> params) {
        String uri = (String) params.get("uri");
        String fonte = obterDocumento(uri);
        if (fonte == null) return CompletableFuture.completedFuture(Map.of("error", "Documento não encontrado: " + uri));
        try {
            ResultadoAnalise r = analisar(fonte);
            if (r.ast() == null) return CompletableFuture.completedFuture(Map.of("error", "Não foi possível gerar LLVM IR de programa com erros sintáticos."));
            String llvm = GeradorIr.emitirLlvm(r.ast());
            return CompletableFuture.completedFuture(Map.of("text", llvm));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
        }
    }

    // ---- Helpers compartilhados ----

    public void atualizarDocumento(String uri, String texto) {
        documentos.put(uri, texto);
    }

    public void removerDocumento(String uri) {
        documentos.remove(uri);
    }

    public String obterDocumento(String uri) {
        return documentos.get(uri);
    }

    /**
     * Pipeline de análise completo: léxico -> sintático -> semântico.
     */
    public ResultadoAnalise analisar(String fonte) {
        List<DiagnosticoLsp> diagnosticos = new ArrayList<>();
        ProgramaAst ast = null;

        // 1) Léxico
        List<Token> tokens;
        try {
            tokens = new ThzLexer(fonte).tokenize();
        } catch (Exception e) {
            LinhaColuna lc = extrairLinhaColuna(e.getMessage());
            diagnosticos.add(new DiagnosticoLsp(lc.linha, lc.coluna, e.getMessage(), "lexico"));
            return new ResultadoAnalise(null, diagnosticos, List.of());
        }

        // 2) Sintático
        try {
            ast = new ThzParser(tokens).parse();
        } catch (Exception e) {
            LinhaColuna lc = extrairLinhaColuna(e.getMessage());
            diagnosticos.add(new DiagnosticoLsp(lc.linha, lc.coluna, e.getMessage(), "sintatico"));
            return new ResultadoAnalise(null, diagnosticos, List.of());
        }

        // 3) Semântico
        List<ErroSemantico> errosSemanticos = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(lintEstrito));
        for (ErroSemantico e : errosSemanticos) {
            diagnosticos.add(new DiagnosticoLsp(e.linha(), e.coluna(), e.mensagem(), "semantico"));
        }

        List<SimboloLsp> simbolos = extrairSimbolos(ast, tokens);
        return new ResultadoAnalise(ast, diagnosticos, simbolos);
    }

    /**
     * Converte diagnósticos internos para o formato LSP.
     */
    public List<Diagnostic> paraLspDiagnostics(List<DiagnosticoLsp> diagnosticos, String fonte) {
        List<Diagnostic> result = new ArrayList<>();
        String[] linhas = fonte.split("\\r?\\n", -1);

        for (DiagnosticoLsp d : diagnosticos) {
            int line = Math.max(0, d.linha - 1);
            int col = Math.max(0, d.coluna - 1);
            String conteudo = (line < linhas.length) ? linhas[line] : "";
            int fim = col;
            while (fim < conteudo.length() && isIdentChar(conteudo.charAt(fim))) fim++;
            if (fim == col) fim = col + 1;

            Diagnostic diag = new Diagnostic();
            diag.setSeverity(DiagnosticSeverity.Error);
            diag.setRange(new Range(
                    new Position(line, col),
                    new Position(line, fim)
            ));
            diag.setMessage(d.mensagem);
            diag.setSource("thz-" + d.origem);
            result.add(diag);
        }
        return result;
    }

    /**
     * Hover: resolve símbolo sob cursor.
     */
    public HoverResult obterHover(String fonte, int linha, int coluna) {
        ResultadoAnalise r = analisar(fonte);
        if (r.ast == null || r.simbolos.isEmpty()) return null;

        String[] linhas = fonte.split("\\r?\\n", -1);
        if (linha - 1 >= linhas.length || linha - 1 < 0) return null;
        String conteudo = linhas[linha - 1];
        int idx = Math.max(0, Math.min(coluna - 1, conteudo.length()));
        if (idx >= conteudo.length()) return null;

        char c = conteudo.charAt(idx);
        if (!isIdentChar(c) && c != '_') return null;

        int ini = idx;
        int fim = idx;
        while (ini > 0 && isIdentChar(conteudo.charAt(ini - 1))) ini--;
        while (fim < conteudo.length() && isIdentChar(conteudo.charAt(fim))) fim++;

        String palavra = conteudo.substring(ini, fim);
        if (palavra.isBlank()) return null;

        for (SimboloLsp s : r.simbolos) {
            if (s.nome.equals(palavra)) {
                String conteudoHover = switch (s.categoria) {
                    case "estrutura" -> "**ESTRUTURA** `" + palavra + "`";
                    case "campo" -> "**campo** `" + palavra + "` : `" + (s.detalhe != null ? s.detalhe : "?") + "` — em `" + s.container + "`";
                    case "enumeracao" -> "**ENUMERACAO** `" + palavra + "`";
                    case "membro-enum" -> "**membro** `" + palavra + "` : `" + (s.container != null ? s.container : "ENUMERACAO") + "`";
                    case "regra" -> "**REGRA_NEGOCIO** `" + palavra + "`";
                    case "operacao" -> "**OPERACAO** `" + palavra + "(" + (s.detalhe != null ? s.detalhe : "") + ")` — em `" + (s.container != null ? s.container : "") + "`";
                    case "parametro" -> "**parametro** `" + palavra + "` : `" + (s.detalhe != null ? s.detalhe : "?") + "`";
                    case "variavel" -> "**variavel** `" + palavra + "` : `" + (s.detalhe != null ? s.detalhe : "?") + "`";
                    case "programa" -> "**PROGRAMA** `" + palavra + "`";
                    default -> "**" + s.categoria + "** `" + palavra + "`";
                };
                Range range = new Range(
                        new Position(linha - 1, ini),
                        new Position(linha - 1, fim)
                );
                return new HoverResult(conteudoHover, range);
            }
        }
        return null;
    }

    /**
     * Formatação canônica.
     */
    public String formatar(String fonte) {
        ResultadoAnalise r = analisar(fonte);
        if (r.ast == null) return null;
        return Formatador.formatar(r.ast);
    }

    /**
     * Extração de símbolos da AST.
     */
    private List<SimboloLsp> extrairSimbolos(ProgramaAst ast, List<Token> tokens) {
        List<SimboloLsp> simbolos = new ArrayList<>();

        Map<String, List<int[]>> posPorNome = tokens.stream()
                .filter(t -> t.type() == TokenType.IDENTIFICADOR)
                .collect(Collectors.groupingBy(
                        Token::value,
                        Collectors.mapping(t -> new int[]{t.line(), t.column()}, Collectors.toList())
                ));

        java.util.function.Function<String, int[]> primeiraPos = (String nome) -> {
            List<int[]> lista = posPorNome.get(nome);
            return (lista != null && !lista.isEmpty()) ? lista.getFirst() : new int[]{1, 1};
        };

        // Módulo / Programa
        {
            int[] p = primeiraPos.apply(ast.nome());
            String detalhe = ast.tipoModulo() != null ? ast.tipoModulo().descricao() : "PROGRAMA";
            simbolos.add(new SimboloLsp(ast.nome(), "modulo", detalhe, p[0], p[1], null));
        }

        // Estruturas
        if (ast.estruturas() != null) {
            for (var e : ast.estruturas()) {
                int[] p = primeiraPos.apply(e.nome());
                simbolos.add(new SimboloLsp(e.nome(), "estrutura", e.layoutColunar() ? "LAYOUT_COLUNAR" : null, p[0], p[1], null));
                if (e.campos() != null) {
                    for (var c : e.campos()) {
                        int[] pc = primeiraPos.apply(c.nome());
                        simbolos.add(new SimboloLsp(c.nome(), "campo", c.tipo(), pc[0], pc[1], e.nome()));
                    }
                }
            }
        }

        // Enumerações
        if (ast.enumeracoes() != null) {
            for (var en : ast.enumeracoes()) {
                int[] p = primeiraPos.apply(en.nome());
                simbolos.add(new SimboloLsp(en.nome(), "enumeracao", null, p[0], p[1], null));
                if (en.membros() != null) {
                    for (String m : en.membros()) {
                        int[] pm = primeiraPos.apply(m);
                        simbolos.add(new SimboloLsp(m, "membro-enum", null, pm[0], pm[1], en.nome()));
                    }
                }
            }
        }

        // Regras de negócio
        if (ast.regras() != null) {
            for (var regra : ast.regras()) {
                int[] pr = primeiraPos.apply(regra.nome());
                simbolos.add(new SimboloLsp(regra.nome(), "regra", regra.identificador(), pr[0], pr[1], null));
                if (regra.operacoes() != null) {
                    for (var op : regra.operacoes()) {
                        int[] po = primeiraPos.apply(op.nome());
                        String assinatura = op.parametros() != null
                                ? op.parametros().stream().map(p -> p.nome() + ": " + p.tipo()).collect(Collectors.joining(", "))
                                : "";
                        assinatura += " : " + op.tipoRetorno();
                        simbolos.add(new SimboloLsp(op.nome(), "operacao", assinatura, po[0], po[1], regra.nome()));
                        if (op.parametros() != null) {
                            for (var param : op.parametros()) {
                                int[] pp = primeiraPos.apply(param.nome());
                                simbolos.add(new SimboloLsp(param.nome(), "parametro", param.tipo(), pp[0], pp[1], op.nome()));
                            }
                        }
                    }
                }
            }
        }

        return simbolos;
    }

    // ---- Helpers ----

    private LinhaColuna extrairLinhaColuna(String mensagem) {
        if (mensagem == null) return new LinhaColuna(1, 1);
        Matcher m = LINHA_COLUNA.matcher(mensagem);
        if (m.find()) {
            return new LinhaColuna(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }
        return new LinhaColuna(1, 1);
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ---- Records internos ----

    public record DiagnosticoLsp(int linha, int coluna, String mensagem, String origem) {}
    public record SimboloLsp(String nome, String categoria, String detalhe, int linha, int coluna, String container) {}
    public record ResultadoAnalise(ProgramaAst ast, List<DiagnosticoLsp> diagnosticos, List<SimboloLsp> simbolos) {}
    public record HoverResult(String conteudo, Range range) {}
    private record LinhaColuna(int linha, int coluna) {}
}
