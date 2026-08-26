package thz.lang.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;

public class ComandoCompileAll implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("compile-all", "compilar-tudo");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
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

                String jsonIr = thz.lang.ir.GeradorIr.serializarIrJson(thz.lang.ir.GeradorIr.baixarParaIr(ast));
                Files.writeString(dirIr.resolve(nomeBase + "_ir.json"), jsonIr, StandardCharsets.UTF_8);

                String codigoLlvm = thz.lang.ir.GeradorIr.emitirLlvm(ast);
                Files.writeString(dirLlvm.resolve(nomeBase + ".ll"), codigoLlvm, StandardCharsets.UTF_8);

                String codigoWasm = "// THZ-LANG v3.0.0 — WebAssembly Module\n" + thz.lang.js.ThzJsEmitter.emitir(ast);
                Files.writeString(dirWasm.resolve(nomeBase + ".wasm.js"), codigoWasm, StandardCharsets.UTF_8);

                var rel = thz.lang.governanca.AuditorGovernanca.auditar(ast);
                String mdAudit = thz.lang.governanca.AuditorGovernanca.gerarMarkdownGovernanca(rel);
                Files.writeString(dirAudit.resolve(nomeBase + "_auditoria.md"), mdAudit, StandardCharsets.UTF_8);

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
    }
}
