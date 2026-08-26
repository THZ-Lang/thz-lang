package thz.lang.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ComandoLivro implements ComandoCli {

    @Override
    public List<String> nomes() {
        return List.of("livro", "manual", "book");
    }

    @Override
    public void executar(List<String> argumentos, boolean estrito) throws Exception {
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
    }
}
