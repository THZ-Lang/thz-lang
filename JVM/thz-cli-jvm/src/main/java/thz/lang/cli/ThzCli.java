package thz.lang.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import thz.lang.ast.ComandoAst;
import thz.lang.ast.OperacaoAst;
import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.ast.RegraNegocioAst;
import thz.lang.diagnosticos.DiagnosticoEntrada;
import thz.lang.diagnosticos.Diagnosticos;
import thz.lang.formato.Formatador;
import thz.lang.formato.JsonEscritor;
import thz.lang.interpretador.InjetorLoteDemo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.runtime.BlocoMemoria;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.ErroSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;

public class ThzCli {
    public static void main(String[] args) throws Exception {
        boolean modoWeb = Arrays.asList(args).contains("--web") || Arrays.asList(args).contains("--webview");
        if (modoWeb) {
            BibliotecaConsole.registrar();
        } else {
            try {
                thz.lang.gui.BibliotecaTela.registrar();
            } catch (Throwable t) {
                BibliotecaConsole.registrar();
            }
        }

        // Carrega automaticamente o manifesto thz.config.json ou thz.json se presente
        thz.lang.config.ThzProjectConfig.recarregar(Path.of("."));

        if (args.length == 0 || args[0].equals("--ajuda") || args[0].equals("-h") || args[0].equals("ajuda")
                || args[0].equals("help")) {
            exibirAjuda();
            return;
        }

        if (args[0].equals("--versao") || args[0].equals("-v") || args[0].equals("versao")
                || args[0].equals("version")) {
            System.out.println("THZ-LANG Engine v" + thz.lang.version.ThzVersion.ATUAL + " (GraalVM / Java 25)");
            return;
        }

        // Normaliza --gui -> gui para shim ./thz --gui
        String comandoRaw = args[0];
        String comando = comandoRaw.startsWith("--") ? comandoRaw.substring(2)
                : comandoRaw.startsWith("-") ? comandoRaw.substring(1) : comandoRaw;
        List<String> argumentos = new ArrayList<>(Arrays.asList(args));
        argumentos.remove(0);
        boolean estrito = argumentos.contains("--estrito");

        Set<String> comandosConhecidos = Set.of(
                "init", "inicializar", "repl", "gui", "livro", "manual", "book",
                "compile-all", "compilar-tudo", "compile", "compilar", "build",
                "dev", "serve", "check", "ast", "fmt", "run", "audit", "doc", "ir", "ui"
        );

        if (!comandosConhecidos.contains(comando.toLowerCase())) {
            var achado = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(comandoRaw, Path.of("."), List.of(".thz", ".thzui"));
            if (achado.isPresent()) {
                argumentos.add(0, achado.get().toString());
                comando = "run";
            }
        }

        if (comando.equals("init") || comando.equals("inicializar")) {
            Path arquivoConfig = Path.of("thz.config.json");
            if (Files.exists(arquivoConfig)) {
                System.out.println("[THZ INIT] O arquivo de manifesto 'thz.config.json' já existe neste projeto.");
                return;
            }
            var padrao = thz.lang.config.ThzProjectConfig.criarPadrao(arquivoConfig);
            String jsonModelo = thz.lang.config.ThzProjectConfig.gerarJsonModelo(padrao);
            Files.writeString(arquivoConfig, jsonModelo, StandardCharsets.UTF_8);
            System.out.println("================================================================================");
            System.out.println("   PROJETO THZ-LANG INICIALIZADO COM SUCESSO!");
            System.out.println("   Manifesto criado: " + arquivoConfig.toAbsolutePath());
            System.out.println("================================================================================\n");
            System.out.println("Configurações padrão ativas:");
            System.out.println("  • Dialeto: pt-BR");
            System.out.println("  • Banco de Dados: Auto (SQLite / PostgreSQL / MySQL / JDBC)");
            System.out.println("  • Mensageria: Auto (RabbitMQ / Kafka / AWS SQS / Embutido)");
            System.out.println("  • IA & Embeddings: Local FNV-1a L2");
            System.out.println("\nEdite 'thz.config.json' para personalizar suas preferências de banco e mensageria.");
            return;
        }
        if (comando.equals("repl")) {
            thz.lang.repl.Repl.executar();
            return;
        }
        if (comando.equals("gui")) {
            lancarGuiSeDisponivel();
            return;
        }
        if (comando.equals("livro") || comando.equals("manual") || comando.equals("book")) {
            boolean linguaEn = argumentos.contains("--en") || argumentos.contains("--en-us") || argumentos.contains("--english");
            boolean linguaPt = argumentos.contains("--pt") || argumentos.contains("--pt-br") || argumentos.contains("--portugues");

            Path raizWorkspace = Path.of(".").toAbsolutePath().normalize();
            Path dirDist = Path.of("dist").toAbsolutePath().normalize();
            try {
                Files.createDirectories(dirDist);
                if (linguaEn && !linguaPt) {
                    Path destino = dirDist.resolve("MANUAL_THZ_LANG_EN.pdf");
                    System.out.println("Compilando Livro-Manual PDF em Inglês (EN-US)...");
                    Path gerado = thz.lang.documento.ThzLivroManualPdf.gerarLivroManual(raizWorkspace, destino, thz.lang.lexico.DialetoLinguagem.EN_US);
                    System.out.println("[SUCESSO] Livro-Manual EN-US gerado em: " + gerado);
                } else if (linguaPt && !linguaEn) {
                    Path destino = dirDist.resolve("MANUAL_THZ_LANG_PT.pdf");
                    System.out.println("Compilando Livro-Manual PDF em Português (PT-BR)...");
                    Path gerado = thz.lang.documento.ThzLivroManualPdf.gerarLivroManual(raizWorkspace, destino, thz.lang.lexico.DialetoLinguagem.PT_BR);
                    System.out.println("[SUCESSO] Livro-Manual PT-BR gerado em: " + gerado);
                } else {
                    System.out.println("Compilando todos os documentos Markdown (.md) em Livros-Manuais PDF (PT-BR & EN-US)...");
                    List<Path> gerados = thz.lang.documento.ThzLivroManualPdf.gerarTodosManuais(raizWorkspace, dirDist);
                    for (Path p : gerados) {
                        System.out.println("  • " + p.getFileName() + " -> " + p);
                    }
                    System.out.println("[SUCESSO] Manuais gerados com sucesso!");
                }
            } catch (Exception e) {
                System.err.println("[ERRO] Falha ao compilar manuais PDF: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }
        if (comando.equals("compile-all") || comando.equals("compilar-tudo")) {
            String dirOrigem = "exemplos";
            int idxOrigem = argumentos.indexOf("--origem");
            if (idxOrigem >= 0 && idxOrigem + 1 < argumentos.size()) dirOrigem = argumentos.get(idxOrigem + 1);

            String dirSaida = "dist/exemplos_compilados";
            int idxSaida = argumentos.indexOf("--saida");
            if (idxSaida >= 0 && idxSaida + 1 < argumentos.size()) dirSaida = argumentos.get(idxSaida + 1);

            Path raizOrigem = Path.of(dirOrigem);
            Path raizSaida = Path.of(dirSaida);

            if (!Files.exists(raizOrigem)) {
                System.err.println("[ERRO] Diretório de origem não encontrado: " + raizOrigem.toAbsolutePath());
                System.exit(1);
            }

            System.out.println("================================================================================");
            System.out.println("   COMPILANDO TODOS OS EXEMPLOS THZ-LANG (v" + thz.lang.version.ThzVersion.ATUAL + ")");
            System.out.println("   Origem: " + raizOrigem.toAbsolutePath());
            System.out.println("   Destino: " + raizSaida.toAbsolutePath());
            System.out.println("================================================================================\n");

            Path dirIr = raizSaida.resolve("ir");
            Path dirLlvm = raizSaida.resolve("llvm");
            Path dirWasm = raizSaida.resolve("wasm");
            Path dirAudit = raizSaida.resolve("auditoria");
            Path dirDoc = raizSaida.resolve("doc");

            Files.createDirectories(dirIr);
            Files.createDirectories(dirLlvm);
            Files.createDirectories(dirWasm);
            Files.createDirectories(dirAudit);
            Files.createDirectories(dirDoc);

            List<Path> arquivosThz = new ArrayList<>();
            try (var stream = Files.walk(raizOrigem)) {
                stream.filter(p -> p.toString().endsWith(".thz")).sorted().forEach(arquivosThz::add);
            }

            int sucesso = 0;
            int falhas = 0;

            for (Path arq : arquivosThz) {
                String nomeBase = arq.getFileName().toString().replace(".thz", "");
                try {
                    String fonte = Files.readString(arq, StandardCharsets.UTF_8);
                    List<Token> tokens = new ThzLexer(fonte).tokenize();
                    ProgramaAst ast = new ThzParser(tokens).parse();

                    // 1. THZ-IR
                    String jsonIr = thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast));
                    Files.writeString(dirIr.resolve(nomeBase + "_ir.json"), jsonIr, StandardCharsets.UTF_8);

                    // 2. LLVM IR
                    String codigoLlvm = thz.lang.ir.GeradorIr.emitirLlvm(ast);
                    Files.writeString(dirLlvm.resolve(nomeBase + ".ll"), codigoLlvm, StandardCharsets.UTF_8);

                    // 3. WebAssembly / JS Module
                    String codigoWasm = "// THZ-LANG v3.0.0 — WebAssembly Module\n" + thz.lang.js.ThzJsEmitter.emitir(ast);
                    Files.writeString(dirWasm.resolve(nomeBase + ".wasm.js"), codigoWasm, StandardCharsets.UTF_8);

                    // 4. Auditoria & Governança
                    var rel = thz.lang.governanca.AuditorGovernanca.auditar(ast);
                    String mdAudit = thz.lang.governanca.AuditorGovernanca.gerarMarkdownGovernanca(rel);
                    Files.writeString(dirAudit.resolve(nomeBase + "_auditoria.md"), mdAudit, StandardCharsets.UTF_8);

                    // 5. Documentação Técnica DocGen
                    String mdDoc = thz.lang.docgen.ThzDocGen.gerarDocumentacao(ast);
                    Files.writeString(dirDoc.resolve(nomeBase + "_doc.md"), mdDoc, StandardCharsets.UTF_8);

                    sucesso++;
                    System.out.println("  [OK] " + arq.getFileName() + " -> IR, LLVM, WASM, AUDIT, DOC");
                } catch (Exception ex) {
                    falhas++;
                    System.err.println("  [FALHA] " + arq.getFileName() + " : " + ex.getMessage());
                }
            }

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("   RESUMO DA COMPILAÇÃO:");
            System.out.println("   • Total de arquivos processados: " + arquivosThz.size());
            System.out.println("   • Compilados com sucesso: " + sucesso);
            System.out.println("   • Falhas: " + falhas);
            System.out.println("   • Diretório de saída: " + raizSaida.toAbsolutePath());
            System.out.println("--------------------------------------------------------------------------------\n");
            return;
        }

        String arquivo = resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank()) {
            System.err.println("[ERRO] Nenhum arquivo .thz ou .thzui especificado. Use: thz " + comando
                    + " <caminho.thz|caminho.thzui>");
            System.exit(1);
        }

        // Resolução inteligente e recursiva antes de qualquer falha
        var arquivoResolvidoOpt = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(arquivo, Path.of("."), List.of(".thz", ".thzui"));
        if (arquivoResolvidoOpt.isPresent()) {
            arquivo = arquivoResolvidoOpt.get().toString();
        }

        if (comando.equals("compile") || comando.equals("compilar") || comando.equals("build")) {
            if (!Files.exists(Path.of(arquivo))) {
                System.err.println("[ERRO] Arquivo não encontrado após pesquisa recursiva: " + arquivo);
                System.exit(1);
            }
            String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
            List<Token> tokens = new ThzLexer(fonte).tokenize();
            ProgramaAst ast = new ThzParser(tokens).parse();
            String nomeBase = Path.of(arquivo).getFileName().toString().replace(".thz", "");

            String dirSaida = "dist/exemplos_compilados";
            int idxSaida = argumentos.indexOf("--saida");
            if (idxSaida >= 0 && idxSaida + 1 < argumentos.size()) dirSaida = argumentos.get(idxSaida + 1);
            Path raizSaida = Path.of(dirSaida);

            Path dirIr = raizSaida.resolve("ir");
            Path dirLlvm = raizSaida.resolve("llvm");
            Path dirWasm = raizSaida.resolve("wasm");
            Files.createDirectories(dirIr);
            Files.createDirectories(dirLlvm);
            Files.createDirectories(dirWasm);

            Files.writeString(dirIr.resolve(nomeBase + "_ir.json"), thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast)), StandardCharsets.UTF_8);
            Files.writeString(dirLlvm.resolve(nomeBase + ".ll"), thz.lang.ir.GeradorIr.emitirLlvm(ast), StandardCharsets.UTF_8);
            Files.writeString(dirWasm.resolve(nomeBase + ".wasm.js"), "// THZ-LANG v3.0.0 WASM\n" + thz.lang.js.ThzJsEmitter.emitir(ast), StandardCharsets.UTF_8);

            System.out.println("[THZ COMPILE] " + arquivo + " compilado com sucesso para IR, LLVM e WASM em: " + raizSaida.toAbsolutePath());
            return;
        }
        if (comando.equals("dev") || comando.equals("serve") || comando.equals("web") || comando.equals("vaadin")) {
            int porta = 8080;
            int idxPorta = argumentos.indexOf("--porta");
            if (idxPorta < 0) idxPorta = argumentos.indexOf("-p");
            if (idxPorta < 0) idxPorta = argumentos.indexOf("--port");
            if (idxPorta >= 0 && idxPorta + 1 < argumentos.size()) {
                try {
                    porta = Integer.parseInt(argumentos.get(idxPorta + 1));
                } catch (NumberFormatException ignored) {}
            }
            boolean abrir = argumentos.contains("--abrir") || argumentos.contains("--open");
            boolean vaadin = argumentos.contains("--vaadin") || comando.equals("vaadin");
            ThzDevServer.iniciar(arquivo, porta, abrir, vaadin);
            if (!Boolean.getBoolean("thz.test.mode")) {
                synchronized (ThzCli.class) {
                    try {
                        ThzCli.class.wait();
                    } catch (InterruptedException ignore) {}
                }
            }
            return;
        }
        if (comando.equals("check") || comando.equals("ast") || comando.equals("fmt") || comando.equals("run")
                || comando.equals("audit") || comando.equals("doc") || comando.equals("ir") || comando.equals("ui")) {
            if (!Files.exists(Path.of(arquivo))) {
                System.err.println("[ERRO] Arquivo não encontrado: " + arquivo);
                System.exit(1);
            }
            String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);

            try {
                List<Token> tokens = new ThzLexer(fonte).tokenize();
                ProgramaAst ast = new ThzParser(tokens).parse();
                if (comando.equals("doc")) {
                    String idxSaida = null;
                    int idx = argumentos.indexOf("--saida");
                    if (idx >= 0 && idx + 1 < argumentos.size())
                        idxSaida = argumentos.get(idx + 1);

                    String doc = thz.lang.docgen.ThzDocGen.gerarDocumentacao(ast);
                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida)
                                : Path.of(idxSaida, ast.nome() + "_documentacao.md");
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
                    if (idx >= 0 && idx + 1 < argumentos.size())
                        idxSaida = argumentos.get(idx + 1);

                    String resultado = llvm
                            ? thz.lang.ir.GeradorIr.emitirLlvm(ast)
                            : thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast));

                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida)
                                : Path.of(idxSaida, ast.nome() + (llvm ? ".ll" : "_ir.json"));
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, resultado, StandardCharsets.UTF_8);
                        System.out.println(
                                "[THZ IR] Saída (" + (llvm ? "LLVM IR" : "THZ-IR/1") + ") gravada em: " + alvo);
                    } else {
                        System.out.println(resultado);
                    }
                    return;
                }
                if (comando.equals("ui")) {
                    boolean html = argumentos.contains("--html") || argumentos.contains("--web") || argumentos.contains("--webview");
                    boolean swing = argumentos.contains("--swing") || argumentos.contains("--gui");

                    if (swing || (!html && !java.awt.GraphicsEnvironment.isHeadless())) {
                        try {
                            boolean precisaEntrada = precisaEntrada(ast);
                            java.util.function.Supplier<String> entrada = precisaEntrada ? criarLeitorEntrada() : null;
                            InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);
                            Object frame = thz.lang.gui.ui.ThzUiSwingRenderer.renderizarOuExibir(ast, interp);
                            if (frame instanceof javax.swing.JFrame jf) {
                                System.out.println("[THZ-UI SWING] Interface gráfica declarativa '" + ast.nome() + "' exibida com sucesso.");
                                if (!Boolean.getBoolean("thz.test.mode")) {
                                    synchronized (ThzCli.class) {
                                        while (jf.isDisplayable()) {
                                            try {
                                                ThzCli.class.wait(1000);
                                            } catch (InterruptedException ignore) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                return;
                            }
                        } catch (Throwable t) {
                            System.err.println("[THZ-UI] Display gráfico indisponível (" + t.getMessage() + "). Alternando para modo HTML/Web...");
                        }
                    }

                    var maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                        c.adicionar(thz.lang.ui.ThzUiMaker.card("card_" + ast.nome(), ast.nome(), card -> {
                            card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_modulo", "info",
                                    "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "]"));
                            if (ast.procedimentos() != null) {
                                for (var p : ast.procedimentos()) {
                                    card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_" + p.nome(), p.nome(), p.nome()));
                                }
                            }
                        }));
                    });
                    if (html) {
                        String codigoHtml = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
                        String url = thz.lang.webview.LancadorWebviewNativo.abrirHtml("THZ-UI: " + ast.nome(), codigoHtml, 1024, 768);
                        System.out.println("[THZ-UI WEB] Interface '" + ast.nome() + "' aberta via Web / WebView em: " + url);
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
                    if (idx >= 0 && idx + 1 < argumentos.size())
                        idxSaida = argumentos.get(idx + 1);

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
                        Path alvo = idxSaida.contains(".") ? Path.of(idxSaida)
                                : Path.of(idxSaida, ast.nome() + "_auditoria." + (emJson ? "json" : "md"));
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, resultado, StandardCharsets.UTF_8);
                        System.out.println("[THZ AUDIT] Relatório de governança gravado em: " + alvo);
                    } else {
                        System.out.println(resultado);
                    }

                    if (estrito && !rel.metricas().aprovado()) {
                        System.err.println(
                                "\n[THZ AUDIT] Falha de conformidade estrita: o programa possui pendências críticas de governança.");
                        System.exit(1);
                    }
                    return;
                }

                if (comando.equals("check")) {
                    List<ErroSemantico> erros = new AnalisadorSemantico(ast).analisar(new OpcoesAnalise(estrito));
                    if (!erros.isEmpty()) {
                        List<DiagnosticoEntrada> diags = erros.stream()
                                .map(e -> new DiagnosticoEntrada(e.linha(), e.coluna(), e.mensagem())).toList();
                        for (String bloco : Diagnosticos.formatarDiagnosticos(fonte, diags, "Semântico"))
                            System.err.println(bloco + "\n");
                        System.err.println("[THZ CHECK] " + erros.size() + " erro(s) semântico(s).");
                        System.exit(1);
                    }
                    String versao = "";
                    System.out.println("[THZ CHECK] Código validado com sucesso! AST íntegra para o programa: "
                            + ast.nome() + versao + (estrito ? " [lint estrito aprovado]" : ""));
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
                    if (idx >= 0 && idx + 1 < argumentos.size())
                        idxSaida = argumentos.get(idx + 1);
                    String formatado = Formatador.formatar(ast);
                    if (check) {
                        if (!fonte.equals(formatado)) {
                            System.err.println(
                                     "[THZ FMT] Arquivo não está formatado. Use `thz fmt --escrever` para corrigir.");
                            String[] a = fonte.split("\n", -1);
                            String[] b = formatado.split("\n", -1);
                            for (int i = 0; i < Math.max(a.length, b.length); i++)
                                if (!Objects.equals(i < a.length ? a[i] : null, i < b.length ? b[i] : null)) {
                                    System.err.println(
                                            "  Linha " + (i + 1) + " esperada: " + q(b.length > i ? b[i] : ""));
                                    System.err.println(
                                            "  Linha " + (i + 1) + " obtida:   " + q(a.length > i ? a[i] : ""));
                                    break;
                                }
                            System.exit(1);
                        }
                        System.out.println("[THZ FMT] OK — arquivo já está canônico.");
                        return;
                    }
                    if (idxSaida != null) {
                        Path alvo = idxSaida.contains(".thz") ? Path.of(idxSaida)
                                : Path.of(idxSaida, Path.of(arquivo).getFileName().toString());
                        Files.createDirectories(alvo.getParent() != null ? alvo.getParent() : Path.of("."));
                        Files.writeString(alvo, formatado, StandardCharsets.UTF_8);
                        System.out.println("[THZ FMT] Arquivo formatado gravado em: " + alvo);
                        return;
                    }
                    if (escrever) {
                        Files.writeString(Path.of(arquivo), formatado, StandardCharsets.UTF_8);
                        System.out.println("[THZ FMT] " + arquivo + " formatado.");
                        return;
                    }
                    System.out.println(formatado);
                    return;
                }
                if (comando.equals("run")) {
                    boolean ehArquivoUi = arquivo.toLowerCase().endsWith(".thzui")
                            || (ast.tipoModulo() == thz.lang.ast.TipoModulo.TELA)
                            || (ast.tipoModulo() == thz.lang.ast.TipoModulo.PROGRAMA_VISUAL)
                            || argumentos.contains("--gui")
                            || argumentos.contains("--swing")
                            || argumentos.contains("--web")
                            || argumentos.contains("--webview");

                    // =========================================================
                    // 1. Arquivos .thzui ou interfaces declarativas -> GUI / Web
                    // =========================================================
                    if (ehArquivoUi) {
                        boolean executarModoWeb = modoWeb
                                || argumentos.contains("--web")
                                || argumentos.contains("--html")
                                || argumentos.contains("--webview")
                                || argumentos.contains("--vaadin")
                                || java.awt.GraphicsEnvironment.isHeadless();

                        boolean usarVaadin = argumentos.contains("--vaadin");

                        boolean precisaEntrada = precisaEntrada(ast);
                        java.util.function.Supplier<String> entrada = precisaEntrada ? criarLeitorEntrada() : null;
                        InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);

                        if (executarModoWeb) {
                            System.out.println("================================================================================");
                            System.out.println("   EXECUTANDO INTERFACE DECLARATIVA THZ-UI (MODO " + (usarVaadin ? "VAADIN FLOW" : "WEB / HTML5") + "): " + ast.nome());
                            System.out.println("================================================================================\n");

                            List<String> logsExecucao = new ArrayList<>();
                            InterpretadorThz interpWeb = new InterpretadorThz(ast, logsExecucao::add, entrada);
                            thz.lang.webview.ThzWebviewBridge.setFallbackRpcHandler(acao -> {
                                logsExecucao.clear();
                                ProcedimentoAst proc = ast.procedimentos() != null ? ast.procedimentos().stream()
                                        .filter(p -> p.nome().equalsIgnoreCase(acao)).findFirst().orElse(null) : null;
                                if (proc != null) {
                                    try {
                                        interpWeb.executarProcedimento(proc.nome(), Map.of());
                                        String log = logsExecucao.isEmpty()
                                                ? "Procedimento '" + proc.nome() + "' executado com sucesso."
                                                : String.join("\n", logsExecucao);
                                        return thz.lang.webview.ThzJson.okMensagem(log);
                                    } catch (Exception ex) {
                                        return thz.lang.webview.ThzJson.erro(ex.getMessage());
                                    }
                                }
                                var op = interpWeb.listarOperacoesExecutaveis().stream()
                                        .filter(o -> o.operacao().nome().equalsIgnoreCase(acao)).findFirst().orElse(null);
                                if (op != null) {
                                    try {
                                        ValorThz res = interpWeb.executarOperacao(op.operacao().nome(), Map.of());
                                        String log = !logsExecucao.isEmpty()
                                                ? String.join("\n", logsExecucao) + "\nResultado: " + interpWeb.formatar(res)
                                                : "Resultado: " + (res != null ? interpWeb.formatar(res) : "OK");
                                        return thz.lang.webview.ThzJson.okMensagem(log);
                                    } catch (Exception ex) {
                                        return thz.lang.webview.ThzJson.erro(ex.getMessage());
                                    }
                                }
                                return thz.lang.webview.ThzJson.okMensagem("Ação '" + acao + "' executada.");
                            });

                            thz.lang.ui.ThzUiMaker maker;
                            if (usarVaadin) {
                                maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                                    // Header corporativo
                                    c.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_header", "info",
                                            "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "] · Vaadin Lumo Dark · Showcase Completo THZ-LANG v3.0.0"));

                                    // KPIs — GRADE 4 colunas
                                    c.adicionar(thz.lang.ui.ThzUiMaker.grade("kpi_grade", 4, g -> {
                                        g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_receita", "Receita Total", "R$ 2.847.500,00", "+12.5% vs mês anterior", "sucesso"));
                                        g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_despesa", "Despesas", "R$ 1.230.800,00", "-3.2% otimizado", "aviso"));
                                        g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_lucro", "Lucro Líquido", "R$ 1.616.700,00", "+56.8% EBITDA", "sucesso"));
                                        g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_clientes", "Clientes Ativos", "1.247", "+42 novos", "info"));
                                    }));

                                    // Emblemas + divisor
                                    c.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_emblemas", row -> {
                                        row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_sox", "SOX-404", "sucesso"));
                                        row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_iso", "ISO-4217", "primario"));
                                        row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_lgpd", "LGPD", "aviso"));
                                        row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_iso10967", "ISO-10967", "primario"));
                                        row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_pci", "PCI-DSS", "erro"));
                                    }));
                                    c.adicionar(thz.lang.ui.ThzUiMaker.divisor());

                                    // Card Formulário Completo — demonstra CAMPO_TEXTO, CAMPO_MOEDA, CAMPO_NUMERO, CAMPO_DATA, SELECAO, INTERRUPTOR
                                    c.adicionar(thz.lang.ui.ThzUiMaker.card("card_form", "Cadastro Corporativo — Formulário Completo", card -> {
                                        card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_form", "warning",
                                                "Todos os campos com * são obrigatórios. Validação corporativa ativa (REQ-UI-001)."));
                                        card.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_form1", l1 -> {
                                            l1.adicionar(thz.lang.ui.ThzUiMaker.campoTexto("campo_nome", "Nome Completo *", "Ex: Maria Silva", "nome_cliente"));
                                            l1.adicionar(thz.lang.ui.ThzUiMaker.campoTexto("campo_email", "E-mail Corporativo *", "nome@empresa.com.br", "email"));
                                        }));
                                        card.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_form2", l2 -> {
                                            l2.adicionar(thz.lang.ui.ThzUiMaker.campoMoeda("campo_saldo", "Saldo / Valor", "BRL", "saldo"));
                                            l2.adicionar(thz.lang.ui.ThzUiMaker.campoNumero("campo_estoque", "Quantidade", "estoque"));
                                            l2.adicionar(thz.lang.ui.ThzUiMaker.campoData("campo_data", "Data de Emissão", "data_emissao"));
                                        }));
                                        card.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_form3", l3 -> {
                                            l3.adicionar(thz.lang.ui.ThzUiMaker.selecao("campo_cidade", "Cidade",
                                                    java.util.List.of("São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba", "Porto Alegre"), "cidade"));
                                            l3.adicionar(thz.lang.ui.ThzUiMaker.selecao("campo_categoria", "Categoria",
                                                    java.util.List.of("Receita", "Despesa", "Investimento", "Transferência"), "categoria"));
                                            l3.adicionar(thz.lang.ui.ThzUiMaker.interruptor("switch_ativo", "Cliente Ativo", "ativo"));
                                        }));
                                        card.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_acoes_form", la -> {
                                            la.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_validar", "Validar Cadastro", "ValidarCadastro").comPropriedade("variante", "primario"));
                                            la.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_limpar", "Limpar", "__thz_restaurar__").comPropriedade("variante", "contorno"));
                                        }));
                                    }));

                                    // Grade central: TABELA_DADOS + Central de Ações
                                    c.adicionar(thz.lang.ui.ThzUiMaker.grade("grade_central", 2, g2 -> {
                                        g2.adicionar(thz.lang.ui.ThzUiMaker.card("card_tabela", "Transações Recentes", tbl -> {
                                            var tabela = thz.lang.ui.ThzUiMaker.novo("tabela_transacoes", thz.lang.ui.ThzUiComponente.TipoUi.TABELA_DADOS)
                                                    .comPropriedade("colunas", java.util.List.of("Data", "Descrição", "Categoria", "Valor", "Status"))
                                                    .comPropriedade("rotulo", "Transações");
                                            tbl.adicionar(tabela);
                                            tbl.adicionar(thz.lang.ui.ThzUiMaker.divisor());
                                            tbl.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_tabela_acoes", l -> {
                                                l.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_exportar", "Exportar Relatório", "ExportarRelatorio").comPropriedade("variante", "sucesso"));
                                                l.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_nota", "Gerar NF-e", "GerarNota").comPropriedade("variante", "primario"));
                                            }));
                                        }));
                                        g2.adicionar(thz.lang.ui.ThzUiMaker.card("card_acoes", "Central de Ações — Virtual Threads", ac -> {
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_vt", "success",
                                                    "Motor THZ-LANG com Virtual Threads (Java 25) e Arena O(1) — Performance garantida."));
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_dashboard", "Atualizar Dashboard", "AtualizarDashboard").comPropriedade("variante", "primario"));
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_lote", "Processar Lote (1.2k)", "ProcessarLote").comPropriedade("variante", "aviso"));
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_email", "Enviar por E-mail", "EnviarEmail").comPropriedade("variante", "secundario"));
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.divisor());
                                            ac.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_status", ls -> {
                                                ls.adicionar(thz.lang.ui.ThzUiMaker.emblema("emb_online", "ONLINE", "sucesso"));
                                                ls.adicionar(thz.lang.ui.ThzUiMaker.emblema("emb_lat", "15ms SLO", "primario"));
                                                ls.adicionar(thz.lang.ui.ThzUiMaker.emblema("emb_mem", "Arena 2.1MB", "aviso"));
                                            }));
                                        }));
                                    }));

                                    // Procedimentos dinâmicos do AST como botões extras
                                    if (ast.procedimentos() != null && !ast.procedimentos().isEmpty()) {
                                        c.adicionar(thz.lang.ui.ThzUiMaker.card("card_procedimentos", "Procedimentos do Módulo: " + ast.nome(), pc -> {
                                            pc.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_procs", lp -> {
                                                for (var p : ast.procedimentos()) {
                                                    lp.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_proc_" + p.nome(), p.nome(), p.nome()).comPropriedade("variante", "primario"));
                                                }
                                            }));
                                        }));
                                    }

                                    c.adicionar(thz.lang.ui.ThzUiMaker.espaco());
                                    c.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_footer", "info",
                                            "THZ-LANG Engine v3.0.0 · Vaadin Lumo · Glassmorphism · ISO-10967 · Renderização server-driven · Todos os componentes demonstrados"));
                                });
                            } else {
                                maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {
                                    c.adicionar(thz.lang.ui.ThzUiMaker.card("card_" + ast.nome(), ast.nome(), card -> {
                                        card.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_modulo", "info",
                                                "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "]"));
                                        if (ast.procedimentos() != null) {
                                            for (var p : ast.procedimentos()) {
                                                card.adicionar(thz.lang.ui.ThzUiMaker.botao("btn_" + p.nome(), p.nome(), p.nome()));
                                            }
                                        }
                                    }));
                                });
                            }

                            String html;
                            if (usarVaadin) {
                                html = maker.renderizarVaadin(ast.nome(), true);
                            } else {
                                html = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
                            }

                            String url = thz.lang.webview.LancadorWebviewNativo.abrirHtml("THZ-UI: " + ast.nome(), html, 1024, 768);
                            System.out.println("[THZ-UI " + (usarVaadin ? "VAADIN" : "WEB") + "] Interface declarativa '" + ast.nome() + "' aberta em: " + url);
                            return;
                        }

                        // Modo Desktop Swing + FlatLaf
                        System.out.println("================================================================================");
                        System.out.println("   EXECUTANDO INTERFACE DECLARATIVA THZ-UI (MODO SWING GUI): " + ast.nome());
                        System.out.println("================================================================================\n");
                        try {
                            Object frame = thz.lang.gui.ui.ThzUiSwingRenderer.renderizarOuExibir(ast, interp);
                            if (frame instanceof javax.swing.JFrame jf) {
                                System.out.println("[THZ-UI SWING] Janela gráfica interativa '" + ast.nome() + "' exibida com sucesso.");
                                if (!Boolean.getBoolean("thz.test.mode")) {
                                    synchronized (ThzCli.class) {
                                        while (jf.isDisplayable()) {
                                            try {
                                                ThzCli.class.wait(1000);
                                            } catch (InterruptedException ignore) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                return;
                            }
                        } catch (Throwable t) {
                            System.err.println("[THZ-UI SWING] Display gráfico indisponível (" + t.getMessage() + "). Alternando para modo Web...");
                            var maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {});
                            String html = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
                            String url = thz.lang.webview.LancadorWebviewNativo.abrirHtml("THZ-UI: " + ast.nome(), html, 1024, 768);
                            System.out.println("[THZ-UI WEB] Interface aberta em: " + url);
                            return;
                        }
                    }

                    // =========================================================
                    // 2. Arquivos .thz -> Execução CLI (Console stdout & arena)
                    // =========================================================
                    System.out.println(
                            "================================================================================");
                    System.out.println("   EXECUTANDO MOTOR NATIVO THZ-LANG (MODO CLI): " + ast.nome());
                    System.out.println(
                            "================================================================================\n");
                    BlocoMemoria blocoMemoria = new BlocoMemoria(64);
                    blocoMemoria.alocar(2048);
                    String dom = ast.metadados() != null ? ast.metadados().dominio() : "N/A";
                    String slo = ast.metadados() != null ? ast.metadados().sloLatencia() : "N/A";
                    String conf = ast.metadados() != null && ast.metadados().conformidade() != null
                            ? String.join(", ", ast.metadados().conformidade())
                            : "N/A";
                    System.out.println("[ARQUITETURA] Domínio: " + dom + " | SLO: " + slo);
                    System.out.println("[CONFORMIDADE] Diretrizes ativas: " + conf + "\n");
                    int ip = argumentos.indexOf("--principal");
                    final String nomePrincipal = (ip >= 0 && ip + 1 < argumentos.size()) ? argumentos.get(ip + 1)
                            : null;
                    Map<String, String> mapaArgs = parseArgsMapa(argumentos);
                    boolean precisaEntrada = precisaEntrada(ast);
                    java.util.function.Supplier<String> entrada = precisaEntrada ? criarLeitorEntrada() : null;
                    InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);
                    if (nomePrincipal != null) {
                        ProcedimentoAst proc = ast.procedimentos() != null
                                ? ast.procedimentos().stream().filter(p -> p.nome().equals(nomePrincipal)).findFirst()
                                        .orElse(null)
                                : null;
                        if (proc != null) {
                            System.out.println("[PROCEDIMENTO] " + proc.nome() + "()\n");
                            Map<String, ValorThz> a = InjetorLoteDemo.construirArgsProc(proc,
                                    p -> mapaArgs.get(p.nome()));
                            interp.executarProcedimento(proc.nome(), a);
                            blocoMemoria.liberarTudo();
                            System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                            return;
                        }

                        var ops = interp.listarOperacoesExecutaveis().stream()
                                .filter(o -> o.operacao().nome().equals(nomePrincipal)).findFirst().orElse(null);
                        if (ops != null) {
                            System.out.println("[REGRA] " + ops.regra().nome()
                                    + (ops.regra().identificador() != null ? " (" + ops.regra().identificador() + ")"
                                            : "")
                                    + " :: " + ops.operacao().nome() + "()\n");
                            Map<String, ValorThz> a = InjetorLoteDemo.construirArgsOperacao(ops.operacao(), ast,
                                    interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
                            ValorThz res = interp.executarOperacao(ops.operacao().nome(), a);
                            System.out.println("--------------------------------------------------------------");
                            if (res != null)
                                System.out.println("[RESULTADO] " + interp.formatar(res));
                            blocoMemoria.liberarTudo();
                            System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                            return;
                        }
                        System.err.println("[ERRO] Entrada '--principal " + nomePrincipal
                                + "' não encontrada como PROCEDIMENTO nem OPERACAO.");
                        System.exit(1);
                    }
                    var procs = interp.listarProcedimentos();
                    if (!procs.isEmpty()) {
                        var proc = procs.stream().filter(p -> p.nome().equalsIgnoreCase("Principal")).findFirst()
                                .orElse(procs.get(0));
                        System.out.println("[PROCEDIMENTO] " + proc.nome() + "()\n");
                        Map<String, ValorThz> a = proc.parametros().isEmpty() ? Map.of()
                                : InjetorLoteDemo.construirArgsProc(proc, p -> mapaArgs.get(p.nome()));
                        interp.executarProcedimento(proc.nome(), a);
                        blocoMemoria.liberarTudo();
                        System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                        return;
                    }
                    var execs = interp.listarOperacoesExecutaveis();
                    if (execs.isEmpty()) {
                        System.err.println(
                                "[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal.");
                        System.exit(1);
                    }
                    var prim = execs.get(0);
                    System.out.println("[REGRA] " + prim.regra().nome()
                            + (prim.regra().identificador() != null ? " (" + prim.regra().identificador() + ")" : "")
                            + " :: " + prim.operacao().nome() + "()\n");
                    Map<String, ValorThz> a = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), ast,
                            interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
                    ValorThz res = interp.executarOperacao(prim.operacao().nome(), a);
                    System.out.println("--------------------------------------------------------------");
                    if (res != null)
                        System.out.println("[RESULTADO] " + interp.formatar(res));
                    blocoMemoria.liberarTudo();
                    System.out.println("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                    return;
                }
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "";
                var m = java.util.regex.Pattern.compile("\\[Linha (\\d+):(\\d+)\\]").matcher(msg);
                if (m.find()) {
                    int linha = Integer.parseInt(m.group(1)), col = Integer.parseInt(m.group(2));
                    System.err.println(Diagnosticos
                            .formatarDiagnosticos(fonte, List.of(new DiagnosticoEntrada(linha, col, msg)), "").get(0));
                } else
                    System.err.println(msg);
                System.exit(1);
            }
        } else {
            System.err.println("Comando desconhecido: " + comando
                    + " (use: check | run | fmt | ast | audit | doc | ir | repl | gui | --ajuda)");
            System.exit(1);
        }
    }

    private static void exibirAjuda() {
        System.out.println("================================================================================");
        System.out.println("   THZ-LANG Engine — JVM (v" + thz.lang.version.ThzVersion.ATUAL + ")");
        System.out.println("   Linguagem Corporativa de Sistemas, Governança e Alta Performance");
        System.out.println("================================================================================\n");
        System.out.println("Uso:");
        System.out.println("  thz <comando> [arquivo.thz] [opções]\n");
        System.out.println("Comandos Disponíveis:");
        System.out.println("  init                                         Inicializa o projeto criando o manifesto thz.config.json");
        System.out.println("  compile-all [--origem <dir>] [--saida <dir>] Compila todos os exemplos em IR, LLVM, WASM, Doc e Auditoria");
        System.out.println("  compile <arquivo> [--saida <dir>]            Compila um programa em THZ-IR, LLVM IR e WASM");
        System.out.println("  check <arquivo> [--estrito]                  Verifica a integridade sintática e semântica");
        System.out.println(
                "  run <arquivo> [--principal <Nome>]        Executa o programa via interpretador com arena O(1)");
        System.out.println("  fmt <arquivo> [--check|--escrever|--saida] Formata o código canonicamente");
        System.out.println("  ast <arquivo>                             Exibe a AST (Abstract Syntax Tree) em JSON");
        System.out.println("  audit <arquivo> [--json] [--estrito]      Gera relatório de auditoria e governança (G4)");
        System.out
                .println("  doc <arquivo> [--saida <caminho.md>]      Gera documentação técnica com diagramas Mermaid");
        System.out.println("  ir <arquivo> [--llvm] [--saida <caminho>] Gera a Representação Intermediária (THZ-IR/1)");
        System.out.println("  ui <arquivo[.thzui]> [--html]             Renderiza ou exporta a interface declarativa (ThzUiMaker)");
        System.out.println("  livro [--saida <caminho.pdf>]             Compila toda a documentação Markdown em Livro-Manual PDF");
        System.out.println("  repl                                      Inicia o shell interativo multi-linha");
        System.out.println("  gui                                       Abre a Desktop IDE oficial (Swing + FlatLaf)");
        System.out.println("Exemplos:");
        System.out.println("  thz check exemplos/faturamento.thz --estrito");
        System.out.println("  thz run exemplos/faturamento.thz");
        System.out.println("  thz audit exemplos/faturamento.thz");
        System.out.println("  thz doc exemplos/faturamento.thz --saida docs/faturamento.md");
        System.out.println("  thz livro --saida dist/MANUAL_THZ_LANG.pdf");
        System.out.println("  thz gui");
    }

    private static Map<String, String> parseArgsMapa(List<String> args) {
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

    private static String resolverArquivo(List<String> args) {
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

    private static boolean precisaEntrada(ProgramaAst ast) {
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

    private static java.util.function.Supplier<String> criarLeitorEntrada() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        return () -> {
            try {
                String l = br.readLine();
                return l;
            } catch (IOException e) {
                return null;
            }
        };
    }

    private static String q(String s) {
        return "\"" + (s != null ? s : "") + "\"";
    }

    /**
     * Lança a Desktop IDE oficial Swing + FlatLaf (módulo thz-gui-jvm) via
     * reflexão.
     */
    private static void lancarGuiSeDisponivel() {
        try {
            Class<?> gui = Class.forName("thz.lang.gui.ThzGui");
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    Object janela = gui.getConstructor().newInstance();
                    gui.getMethod("setVisible", boolean.class).invoke(janela, true);
                } catch (ReflectiveOperationException e) {
                    System.err.println("[ERRO] Falha ao iniciar a Desktop IDE Swing: " + e.getMessage());
                }
            });
        } catch (ClassNotFoundException e) {
            System.err.println("[ERRO] Desktop IDE Swing não encontrada (módulo thz-gui-jvm ausente no classpath).");
            System.err.println(
                    "       Inicie via: ./gradlew :thz-gui-jvm:gui ou utilize o script scripts/gui.ps1 / scripts/gui.sh");
        }
    }
}
