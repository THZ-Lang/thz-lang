package thz.lang.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import thz.lang.lexico.ThzLexer;
import thz.lang.lexico.Token;
import thz.lang.sintatico.ThzParser;
import thz.lang.ast.ProgramaAst;

public class ComandoAudit implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("audit");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
        String arquivo = CliHelper.resolverArquivo(argumentos);
        if (arquivo == null || arquivo.isBlank() || !Files.exists(Path.of(arquivo))) {
            System.err.println("[ERRO] Arquivo não encontrado: " + arquivo);
            System.exit(1);
        }
        String fonte = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8);
        List<Token> tokens = new ThzLexer(fonte).tokenize();
        ProgramaAst ast = new ThzParser(tokens).parse();

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
    }
}
