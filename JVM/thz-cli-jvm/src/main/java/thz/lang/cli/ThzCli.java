package thz.lang.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import thz.lang.cli.comandos.ComandoAst;
import thz.lang.cli.comandos.ComandoAudit;
import thz.lang.cli.comandos.ComandoCheck;
import thz.lang.cli.comandos.ComandoCli;
import thz.lang.cli.comandos.ComandoCompile;
import thz.lang.cli.comandos.ComandoCompileAll;
import thz.lang.cli.comandos.ComandoDev;
import thz.lang.cli.comandos.ComandoDoc;
import thz.lang.cli.comandos.ComandoFmt;
import thz.lang.cli.comandos.ComandoInit;
import thz.lang.cli.comandos.ComandoIr;
import thz.lang.cli.comandos.ComandoLivro;
import thz.lang.cli.comandos.ComandoRun;
import thz.lang.cli.comandos.ComandoUi;

/**
 * CLI principal da THZ-LANG — despachante delgado.
 * Cada comando é delegado para sua classe especializada.
 */
public class ThzCli {

    private static final Map<String, ComandoCli> COMANDOS = new HashMap<>();

    static {
        registrar(new ComandoInit());
        registrar(new ComandoLivro());
        registrar(new ComandoCompileAll());
        registrar(new ComandoCompile());
        registrar(new ComandoDev());
        registrar(new ComandoCheck());
        registrar(new ComandoFmt());
        registrar(new ComandoAst());
        registrar(new ComandoDoc());
        registrar(new ComandoIr());
        registrar(new ComandoAudit());
        registrar(new ComandoUi());
        registrar(new ComandoRun());
    }

    private static void registrar(ComandoCli cmd) {
        for (String nome : cmd.nomes()) {
            COMANDOS.put(nome, cmd);
        }
    }

    public static void main(String[] args) throws Exception {
        CliLogger.inicializar();
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

        thz.lang.config.ThzProjectConfig.recarregar(java.nio.file.Path.of("."));

        if (args.length == 0 || args[0].equals("--ajuda") || args[0].equals("-h") || args[0].equals("ajuda")
                || args[0].equals("help")) {
            exibirAjuda();
            return;
        }

        if (args[0].equals("--versao") || args[0].equals("-v") || args[0].equals("versao")
                || args[0].equals("version")) {
            CliLogger.saida("THZ-LANG Engine v" + thz.lang.version.ThzVersion.ATUAL + " (GraalVM / Java 25)");
            return;
        }

        if (args[0].equals("repl")) {
            thz.lang.repl.Repl.executar();
            return;
        }

        if (args[0].equals("gui")) {
            lancarGuiSeDisponivel();
            return;
        }

        String comandoRaw = args[0];
        String comando = comandoRaw.startsWith("--") ? comandoRaw.substring(2)
                : comandoRaw.startsWith("-") ? comandoRaw.substring(1) : comandoRaw;
        List<String> argumentos = new ArrayList<>(Arrays.asList(args));
        argumentos.remove(0);
        boolean estrito = argumentos.contains("--estrito");

        Set<String> comandosConhecidos = Set.of(
                "init", "inicializar", "livro", "manual", "book",
                "compile-all", "compilar-tudo", "compile", "compilar", "build",
                "dev", "serve", "check", "ast", "fmt", "run", "audit", "doc", "ir", "ui"
        );

        if (!comandosConhecidos.contains(comando.toLowerCase())) {
            var achado = thz.lang.io.ThzLocalizadorRecursos.localizarArquivo(comandoRaw, java.nio.file.Path.of("."), List.of(".thz", ".thzui"));
            if (achado.isPresent()) {
                argumentos.add(0, achado.get().toString());
                comando = "run";
            }
        }

        ComandoCli cmd = COMANDOS.get(comando);
        if (cmd != null) {
            if (cmd instanceof ComandoRun runCmd) {
                runCmd.executar(argumentos, estrito, modoWeb);
            } else {
                cmd.executar(argumentos, estrito);
            }
        } else {
            ErrosCli.erroComandoDesconhecido(comando);
        }
    }

    private static void exibirAjuda() {
        CliLogger.saida("================================================================================");
        CliLogger.saida("   THZ-LANG Engine — JVM (v" + thz.lang.version.ThzVersion.ATUAL + ")");
        CliLogger.saida("   Linguagem Corporativa de Sistemas, Governança e Alta Performance");
        CliLogger.saida("================================================================================\n");
        CliLogger.saida("Uso:");
        CliLogger.saida("  thz <comando> [arquivo.thz] [opções]\n");
        CliLogger.saida("Comandos Disponíveis:");
        CliLogger.saida("  init                                         Inicializa o projeto criando o manifesto thz.config.json");
        CliLogger.saida("  compile-all [--origem <dir>] [--saida <dir>] Compila todos os exemplos em IR, LLVM, WASM, Doc e Auditoria");
        CliLogger.saida("  compile <arquivo> [--saida <dir>]            Compila um programa em THZ-IR, LLVM IR e WASM");
        CliLogger.saida("  check <arquivo> [--estrito]                  Verifica a integridade sintática e semântica");
        CliLogger.saida("  run <arquivo> [--principal <Nome>]           Executa o programa via interpretador com arena O(1)");
        CliLogger.saida("  fmt <arquivo> [--check|--escrever|--saida]   Formata o código canonicamente");
        CliLogger.saida("  ast <arquivo>                               Exibe a AST (Abstract Syntax Tree) em JSON");
        CliLogger.saida("  audit <arquivo> [--json] [--estrito]        Gera relatório de auditoria e governança (G4)");
        CliLogger.saida("  doc <arquivo> [--saida <caminho.md>]        Gera documentação técnica com diagramas Mermaid");
        CliLogger.saida("  ir <arquivo> [--llvm] [--saida <caminho>]   Gera a Representação Intermediária (THZ-IR/1)");
        CliLogger.saida("  ui <arquivo[.thzui]> [--html]               Renderiza ou exporta a interface declarativa (ThzUiMaker)");
        CliLogger.saida("  livro [--saida <caminho.pdf>]               Compila toda a documentação Markdown em Livro-Manual PDF");
        CliLogger.saida("  repl                                        Inicia o shell interativo multi-linha");
        CliLogger.saida("  gui                                         Abre a Desktop IDE oficial (Swing + FlatLaf)");
        CliLogger.saida("Exemplos:");
        CliLogger.saida("  thz check exemplos/faturamento.thz --estrito");
        CliLogger.saida("  thz run exemplos/faturamento.thz");
        CliLogger.saida("  thz audit exemplos/faturamento.thz");
        CliLogger.saida("  thz doc exemplos/faturamento.thz --saida docs/faturamento.md");
        CliLogger.saida("  thz livro --saida dist/MANUAL_THZ_LANG.pdf");
        CliLogger.saida("  thz gui");
    }

    private static void lancarGuiSeDisponivel() {
        try {
            Class<?> gui = Class.forName("thz.lang.gui.ThzGui");
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    Object janela = gui.getConstructor().newInstance();
                    gui.getMethod("setVisible", boolean.class).invoke(janela, true);
                } catch (ReflectiveOperationException e) {
                    ErrosCli.erroFalhaAoIniciarGui(e.getMessage());
                }
            });
        } catch (ClassNotFoundException e) {
            ErrosCli.erroGuiNaoEncontrada();
        }
    }
}
