package thz.lang.cli.comandos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import thz.lang.ast.ProcedimentoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.cli.CliHelper;
import thz.lang.cli.CliLogger;
import thz.lang.cli.CliErros;
import thz.lang.cli.ThzCli;
import thz.lang.interpretador.InjetorLoteDemo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.runtime.BlocoMemoria;
import thz.lang.sintatico.ThzParser;

public class ComandoRun implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("run");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        executar(argumentos, estrito, false);
    }

    public void executar(List<String> argumentos, boolean estrito, boolean modoWeb) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank()) {
            CliErros.erroNenhumArquivoEspecificado("thz run <arquivo.thz|arquivo.thzui>");
        }

        var resolved = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(arquivo, Path.of("."), List.of(".thz", ".thzui"));
        if (resolved.isPresent()) arquivo = resolved.get().toString();

        if (!Files.exists(Path.of(arquivo))) {
            CliErros.erroArquivoNaoEncontrado(arquivo);
        }

        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

        boolean ehArquivoUi = arquivo.toLowerCase().endsWith(".thzui")
                || (ast.tipoModulo() == thz.lang.ast.TipoModulo.TELA)
                || (ast.tipoModulo() == thz.lang.ast.TipoModulo.PROGRAMA_VISUAL)
                || argumentos.contains("--gui")
                || argumentos.contains("--swing")
                || argumentos.contains("--web")
                || argumentos.contains("--webview");

        if (ehArquivoUi) {
            executarUi(ast, argumentos, modoWeb);
        } else {
            executarCli(ast, argumentos);
        }
    }

    private void executarUi(ProgramaAst ast, List<String> argumentos, boolean modoWeb) throws Exception {
        boolean executarModoWeb = modoWeb
                || argumentos.contains("--web")
                || argumentos.contains("--html")
                || argumentos.contains("--webview")
                || argumentos.contains("--vaadin")
                || java.awt.GraphicsEnvironment.isHeadless();

        boolean usarVaadin = argumentos.contains("--vaadin");

        boolean precisaEntrada = CliHelper.precisaEntrada(ast);
        java.util.function.Supplier<String> entrada = precisaEntrada ? CliHelper.criarLeitorEntrada() : null;
        InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);

        if (executarModoWeb) {
            CliLogger.info("================================================================================");
            CliLogger.info("   EXECUTANDO INTERFACE DECLARATIVA THZ-UI (MODO " + (usarVaadin ? "VAADIN FLOW" : "WEB / HTML5") + "): " + ast.nome());
            CliLogger.info("================================================================================\n");

            List<String> logsExecucao = new ArrayList<>();
            InterpretadorThz interpWeb = new InterpretadorThz(ast, logsExecucao::add, entrada);
            thz.lang.webview.ThzWebViewBridge.setFallbackRpcHandler(acao -> {
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
                maker = construirMakerVaadin(ast);
            } else {
                maker = construirMakerHtml(ast);
            }

            String html;
            if (usarVaadin) {
                html = maker.renderizarVaadin(ast.nome(), true);
            } else {
                html = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
            }

            String url = thz.lang.webview.ThzWebViewLauncher.abrirHtml("THZ-UI: " + ast.nome(), html, 1024, 768);
            CliLogger.info("[THZ-UI " + (usarVaadin ? "VAADIN" : "WEB") + "] Interface declarativa '" + ast.nome() + "' aberta em: " + url);
            return;
        }

        // Modo Desktop Swing + FlatLaf
        CliLogger.info("================================================================================");
        CliLogger.info("   EXECUTANDO INTERFACE DECLARATIVA THZ-UI (MODO SWING GUI): " + ast.nome());
        CliLogger.info("================================================================================\n");
        try {
            Object frame = thz.lang.gui.ui.ThzUiSwingRenderer.renderizarOuExibir(ast, interp);
            if (frame instanceof javax.swing.JFrame jf) {
                CliLogger.info("[THZ-UI SWING] Janela gráfica interativa '" + ast.nome() + "' exibida com sucesso.");
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
            CliErros.displaySwingIndisponivel(t.getMessage());
            var maker = thz.lang.ui.ThzUiMaker.container("raiz", c -> {});
            String html = maker.renderizarHtml(ast.nome(), thz.lang.ui.ThzUiTema.escuroGlass());
            String url = thz.lang.webview.ThzWebViewLauncher.abrirHtml("THZ-UI: " + ast.nome(), html, 1024, 768);
            CliLogger.info("[THZ-UI WEB] Interface aberta em: " + url);
            return;
        }
    }

    private void executarCli(ProgramaAst ast, List<String> argumentos) throws Exception {
        CliLogger.info(
                "================================================================================");
        CliLogger.info("   EXECUTANDO MOTOR NATIVO THZ-LANG (MODO CLI): " + ast.nome());
        CliLogger.info(
                "================================================================================\n");
        BlocoMemoria blocoMemoria = new BlocoMemoria(64);
        blocoMemoria.alocar(2048);
        String dom = ast.metadados() != null ? ast.metadados().dominio() : "N/A";
        String slo = ast.metadados() != null ? ast.metadados().sloLatencia() : "N/A";
        String conf = ast.metadados() != null && ast.metadados().conformidade() != null
                ? String.join(", ", ast.metadados().conformidade())
                : "N/A";
        CliLogger.info("[ARQUITETURA] Domínio: " + dom + " | SLO: " + slo);
        CliLogger.info("[CONFORMIDADE] Diretrizes ativas: " + conf + "\n");
        int ip = argumentos.indexOf("--principal");
        final String nomePrincipal = (ip >= 0 && ip + 1 < argumentos.size()) ? argumentos.get(ip + 1) : null;
        Map<String, String> mapaArgs = CliHelper.parseArgsMapa(argumentos);
        boolean precisaEntrada = CliHelper.precisaEntrada(ast);
        java.util.function.Supplier<String> entrada = precisaEntrada ? CliHelper.criarLeitorEntrada() : null;
        InterpretadorThz interp = new InterpretadorThz(ast, System.out::println, entrada);
        if (nomePrincipal != null) {
            ProcedimentoAst proc = ast.procedimentos() != null
                    ? ast.procedimentos().stream().filter(p -> p.nome().equals(nomePrincipal)).findFirst().orElse(null)
                    : null;
            if (proc != null) {
                CliLogger.info("[PROCEDIMENTO] " + proc.nome() + "()\n");
                Map<String, ValorThz> a = InjetorLoteDemo.construirArgsProc(proc, p -> mapaArgs.get(p.nome()));
                interp.executarProcedimento(proc.nome(), a);
                blocoMemoria.liberarTudo();
                CliLogger.info("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                return;
            }

            var ops = interp.listarOperacoesExecutaveis().stream()
                    .filter(o -> o.operacao().nome().equals(nomePrincipal)).findFirst().orElse(null);
            if (ops != null) {
                CliLogger.info("[REGRA] " + ops.regra().nome()
                        + (ops.regra().identificador() != null ? " (" + ops.regra().identificador() + ")" : "")
                        + " :: " + ops.operacao().nome() + "()\n");
                Map<String, ValorThz> a = InjetorLoteDemo.construirArgsOperacao(ops.operacao(), ast,
                        interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
                ValorThz res = interp.executarOperacao(ops.operacao().nome(), a);
                CliLogger.info("--------------------------------------------------------------");
                if (res != null)
                    CliLogger.info("[RESULTADO] " + interp.formatar(res));
                blocoMemoria.liberarTudo();
                CliLogger.info("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
                return;
            }
            CliErros.erroPrincipalNaoEncontrado(nomePrincipal);
        }
        var procs = interp.listarProcedimentos();
        if (!procs.isEmpty()) {
            var proc = procs.stream().filter(p -> p.nome().equalsIgnoreCase("Principal")).findFirst()
                    .orElse(procs.get(0));
            CliLogger.info("[PROCEDIMENTO] " + proc.nome() + "()\n");
            Map<String, ValorThz> a = proc.parametros().isEmpty() ? Map.of()
                    : InjetorLoteDemo.construirArgsProc(proc, p -> mapaArgs.get(p.nome()));
            interp.executarProcedimento(proc.nome(), a);
            blocoMemoria.liberarTudo();
            CliLogger.info("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
            return;
        }
        var execs = interp.listarOperacoesExecutaveis();
        if (execs.isEmpty()) {
            CliErros.erroNenhumaOperacaoExecutavel();
        }
        var prim = execs.get(0);
        CliLogger.info("[REGRA] " + prim.regra().nome()
                + (prim.regra().identificador() != null ? " (" + prim.regra().identificador() + ")" : "")
                + " :: " + prim.operacao().nome() + "()\n");
        Map<String, ValorThz> a = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), ast,
                interp::validarInvariantes, p -> mapaArgs.get(p.nome()));
        ValorThz res = interp.executarOperacao(prim.operacao().nome(), a);
        CliLogger.info("--------------------------------------------------------------");
        if (res != null)
            CliLogger.info("[RESULTADO] " + interp.formatar(res));
        blocoMemoria.liberarTudo();
        CliLogger.info("\n[MEMÓRIA] Bloco de memória temporária liberado com sucesso.");
    }

    private thz.lang.ui.ThzUiMaker construirMakerVaadin(ProgramaAst ast) {
        return thz.lang.ui.ThzUiMaker.container("raiz", c -> {
            c.adicionar(thz.lang.ui.ThzUiMaker.alerta("alerta_header", "info",
                    "Tela: " + ast.nome() + " [" + ast.tipoModulo() + "] · Vaadin Lumo Dark · Showcase Completo THZ-LANG v3.0.0"));
            c.adicionar(thz.lang.ui.ThzUiMaker.grade("kpi_grade", 4, g -> {
                g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_receita", "Receita Total", "R$ 2.847.500,00", "+12.5% vs mês anterior", "sucesso"));
                g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_despesa", "Despesas", "R$ 1.230.800,00", "-3.2% otimizado", "aviso"));
                g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_lucro", "Lucro Líquido", "R$ 1.616.700,00", "+56.8% EBITDA", "sucesso"));
                g.adicionar(thz.lang.ui.ThzUiMaker.metrica("kpi_clientes", "Clientes Ativos", "1.247", "+42 novos", "info"));
            }));
            c.adicionar(thz.lang.ui.ThzUiMaker.linha("linha_emblemas", row -> {
                row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_sox", "SOX-404", "sucesso"));
                row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_iso", "ISO-4217", "primario"));
                row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_lgpd", "LGPD", "aviso"));
                row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_iso10967", "ISO-10967", "primario"));
                row.adicionar(thz.lang.ui.ThzUiMaker.emblema("badge_pci", "PCI-DSS", "erro"));
            }));
            c.adicionar(thz.lang.ui.ThzUiMaker.divisor());
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
    }

    private thz.lang.ui.ThzUiMaker construirMakerHtml(ProgramaAst ast) {
        return thz.lang.ui.ThzUiMaker.container("raiz", c -> {
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
}
