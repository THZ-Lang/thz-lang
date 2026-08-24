package thz.lang.semantico;

import thz.lang.ast.ImportacaoAst;
import thz.lang.ast.ProgramaAst;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolvedor de módulos e importações do THZ-LANG.
 *
 * <p>Esta classe orquestra o carregamento, parsing e cacheamento de módulos
 * externos importados através da cláusula {@code IMPORTAR ... DE ...}. Suporta tanto
 * arquivos no sistema de arquivos quanto módulos virtuais registrados em memória.</p>
 *
 * @author THZ-LANG Core Team
 * @version 2.4.0
 */
public class ResolvedorModulos {

    private final Map<String, ProgramaAst> cache = new HashMap<>();
    private final Map<String, String> modulosVirtuais = new HashMap<>();

    /**
     * Registra um módulo em memória para resolução sem necessidade de I/O em disco.
     *
     * @param nomeOuCaminho Nome do módulo ou caminho relativo
     * @param codigoFonte Código-fonte do módulo THZ
     */
    public void registrarModuloVirtual(String nomeOuCaminho, String codigoFonte) {
        Objects.requireNonNull(nomeOuCaminho, "O nome do módulo não pode ser nulo");
        Objects.requireNonNull(codigoFonte, "O código-fonte do módulo não pode ser nulo");
        modulosVirtuais.put(nomeOuCaminho, codigoFonte);
    }

    /**
     * Resolve uma cláusula de importação, retornando a {@link ProgramaAst} correspondente.
     *
     * @param importacao Nó de importação na AST
     * @param diretorioBase Diretório base para resolução de caminhos relativos
     * @return A AST do módulo importado ou {@code null} se não for encontrado
     */
    public ProgramaAst resolver(ImportacaoAst importacao, Path diretorioBase) {
        if (importacao == null) return null;
        String chave = importacao.caminho() != null ? importacao.caminho() : importacao.modulo();
        if (cache.containsKey(chave)) {
            return cache.get(chave);
        }

        String fonte = null;
        if (modulosVirtuais.containsKey(chave)) {
            fonte = modulosVirtuais.get(chave);
        } else if (modulosVirtuais.containsKey(importacao.modulo())) {
            fonte = modulosVirtuais.get(importacao.modulo());
        } else if (importacao.caminho() != null) {
            Path p = diretorioBase != null ? diretorioBase.resolve(importacao.caminho()) : Path.of(importacao.caminho());
            if (Files.exists(p)) {
                try {
                    fonte = Files.readString(p);
                } catch (IOException e) {
                    throw new IllegalStateException("Erro ao ler arquivo de importação: " + importacao.caminho(), e);
                }
            }
        }

        if (fonte == null) {
            return null;
        }

        ProgramaAst ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
        cache.put(chave, ast);
        return ast;
    }
}
