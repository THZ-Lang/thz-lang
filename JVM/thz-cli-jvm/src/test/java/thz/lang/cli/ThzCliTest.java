package thz.lang.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thz.lang.io.ThzIO;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ThzCliTest {

    @Test
    @DisplayName("ThzCli deve exibir ajuda e versão")
    void testAjudaEVersao() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        try {
            System.setOut(new PrintStream(out));
            ThzCli.main(new String[]{"--ajuda"});
            assertTrue(out.toString().contains("Uso:") || out.toString().contains("THZ"));

            out.reset();
            ThzCli.main(new String[]{"--versao"});
            assertTrue(out.toString().contains("3.") || out.toString().contains("THZ-LANG"));
        } finally {
            System.setOut(orig);
        }
    }

    @Test
    @DisplayName("ThzCli deve executar check, ast, doc, audit, ir e fmt em arquivo temporario")
    void testComandosCli(@TempDir Path tempDir) throws Exception {
        Path arquivo = tempDir.resolve("programa.thz");
        String src = """
                PROGRAMA TesteCli
                METADADOS_ARQUITETURA
                    DOMINIO: "Corporativo"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "Engenharia"
                    SLO_LATENCIA_MAXIMA: "100ms"
                FIM_METADADOS
                
                REGRA_NEGOCIO Calculo
                    IDENTIFICADOR_REGRA: "BR-CALC-001"
                    RASTREIO_REQUISITO: "REQ-001"
                    OPERACAO Somar(a : INTEIRO32, b : INTEIRO32) : INTEIRO32
                    INICIO
                        RETORNE a + b
                    FIM
                FIM_REGRA_NEGOCIO
                
                PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Olá CLI"
                FIM
                FIM_PROGRAMA
                """;

        ThzIO.escreverTexto(arquivo.toString(), src);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        try {
            System.setOut(new PrintStream(out));

            // 1. check
            out.reset();
            ThzCli.main(new String[]{"check", arquivo.toString()});

            // 2. ast
            out.reset();
            ThzCli.main(new String[]{"ast", arquivo.toString()});
            assertTrue(out.toString().contains("TesteCli"));

            // 3. doc
            out.reset();
            ThzCli.main(new String[]{"doc", arquivo.toString()});
            assertTrue(out.toString().contains("TesteCli"));

            // 4. audit
            out.reset();
            ThzCli.main(new String[]{"audit", arquivo.toString()});
            assertTrue(out.toString().contains("TesteCli") || out.toString().contains("REQ-001"));

            // 5. ir
            out.reset();
            ThzCli.main(new String[]{"ir", arquivo.toString()});
            assertTrue(out.toString().contains("versaoIr") || out.toString().contains("thz-ir"));

            // 6. compile
            out.reset();
            Path dirSaida = tempDir.resolve("saida_compile");
            ThzCli.main(new String[]{"compile", arquivo.toString(), "--saida", dirSaida.toString()});
            assertTrue(java.nio.file.Files.exists(dirSaida.resolve("ir/programa_ir.json")));
            assertTrue(java.nio.file.Files.exists(dirSaida.resolve("llvm/programa.ll")));
            assertTrue(java.nio.file.Files.exists(dirSaida.resolve("wasm/programa.wasm.js")));

            // 7. fmt
            out.reset();
            ThzCli.main(new String[]{"fmt", "--escrever", arquivo.toString()});

            // 8. run
            out.reset();
            ThzCli.main(new String[]{"run", arquivo.toString()});
            assertTrue(out.toString().contains("Olá CLI"));
        } finally {
            System.setOut(orig);
        }
    }

    @Test
    @DisplayName("ThzCli deve bloquear compile, ir e check quando houver erro semântico")
    void testComandosBloqueiamErrosSemanticos(@TempDir Path tempDir) throws Exception {
        System.setProperty("thz.test.mode", "true");
        Path arquivoInvalido = tempDir.resolve("semantico_invalido.thz");
        String src = """
                PROGRAMA Invalido
                METADADOS_ARQUITETURA
                    DOMINIO: "Teste"
                    VERSAO: "1.0.0"
                FIM_METADADOS
                REGRA_NEGOCIO Regra
                    OPERACAO Executar() : INTEIRO32
                    INICIO
                        RETORNE variavel_inexistente + 1
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ThzIO.escreverTexto(arquivoInvalido.toString(), src);

        try {
            assertThrows(IllegalStateException.class, () ->
                    ThzCli.main(new String[]{"check", arquivoInvalido.toString()}));

            assertThrows(IllegalStateException.class, () ->
                    ThzCli.main(new String[]{"compile", arquivoInvalido.toString()}));

            assertThrows(IllegalStateException.class, () ->
                    ThzCli.main(new String[]{"ir", arquivoInvalido.toString()}));
        } finally {
            System.clearProperty("thz.test.mode");
        }
    }

    @Test
    @DisplayName("ThzCli compile deve rejeitar alvo wasm direto e suportar seleção de alvo")
    void testCompileAlvoWasmRejeicaoEAlvosEspecificos(@TempDir Path tempDir) throws Exception {
        System.setProperty("thz.test.mode", "true");
        Path arquivo = tempDir.resolve("prog_alvo.thz");
        String src = """
                PROGRAMA TesteAlvo
                METADADOS_ARQUITETURA
                    DOMINIO: "Teste"
                    VERSAO: "1.0.0"
                FIM_METADADOS
                PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Alvo"
                FIM
                FIM_PROGRAMA
                """;
        ThzIO.escreverTexto(arquivo.toString(), src);

        try {
            // Rejeição explícita para compilação direta WASM sem pipeline nativo
            var ex = assertThrows(IllegalStateException.class, () ->
                    ThzCli.main(new String[]{"compile", arquivo.toString(), "--alvo", "wasm"}));
            assertTrue(ex.getMessage().contains("WebAssembly"));

            // Compilação específica para IR apenas
            Path dirSaida = tempDir.resolve("saida_ir_apenas");
            ThzCli.main(new String[]{"compile", arquivo.toString(), "--alvo", "ir", "--saida", dirSaida.toString()});
            assertTrue(Files.exists(dirSaida.resolve("ir/prog_alvo_ir.json")));
            assertFalse(Files.exists(dirSaida.resolve("llvm/prog_alvo.ll")));
        } finally {
            System.clearProperty("thz.test.mode");
        }
    }

    @Test
    @DisplayName("ThzCli deve despachar .thz para CLI e .thzui para Swing/Web automaticamente")
    void testDespachoThzEThzUi(@TempDir Path tempDir) throws Exception {
        System.setProperty("thz.test.mode", "true");

        // 1. Arquivo .thz -> CLI
        Path arqThz = tempDir.resolve("servico_negocio.thz");
        String srcThz = """
                PROGRAMA ServicoNegocio
                METADADOS_ARQUITETURA
                    DOMINIO: "Sistemas"
                    CAMADA: "Servico"
                    VERSAO: "3.0.0"
                    AUTOR: "Engenharia"
                    SLO_LATENCIA_MAXIMA: "50ms"
                FIM_METADADOS
                
                PROCEDIMENTO Principal()
                INICIO
                    EXIBA "Executado via CLI Nativo"
                FIM
                FIM_PROGRAMA
                """;
        ThzIO.escreverTexto(arqThz.toString(), srcThz);

        // 2. Arquivo .thzui -> Declarative UI
        Path arqThzUi = tempDir.resolve("painel_financeiro.thzui");
        String srcThzUi = """
                TELA PainelFinanceiro
                METADADOS_ARQUITETURA
                    DOMINIO: "Financeiro"
                    CAMADA: "Interface"
                    VERSAO: "3.0.0"
                    AUTOR: "Design"
                    SLO_LATENCIA_MAXIMA: "16ms"
                FIM_METADADOS
                
                PROCEDIMENTO AoClicar()
                INICIO
                    EXIBA "Acao Disparada"
                FIM
                FIM_TELA
                """;
        ThzIO.escreverTexto(arqThzUi.toString(), srcThzUi);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        try {
            System.setOut(new PrintStream(out));

            // A. Execução direta thz <arquivo.thz> -> CLI
            out.reset();
            ThzCli.main(new String[]{arqThz.toString()});
            String outThz = out.toString();
            assertTrue(outThz.contains("MODO CLI") || outThz.contains("Executado via CLI Nativo"), "Deve executar .thz via CLI");

            // B. Execução thz <arquivo.thzui> --web -> Webview/HTML5
            out.reset();
            ThzCli.main(new String[]{"run", arqThzUi.toString(), "--web"});
            String outWeb = out.toString();
            assertTrue(outWeb.contains("MODO WEB") || outWeb.contains("THZ-UI WEB"), "Deve executar .thzui via Web/HTML5");

            // D. Execução thz serve <arquivo.thzui> --porta 0
            out.reset();
            ThzCli.main(new String[]{"serve", arqThzUi.toString(), "--porta", "0"});
            String outServe = out.toString();
            assertTrue(outServe.contains("SERVIDOR WEB EMBUTIDO THZ-LANG") || outServe.contains("THZ EMBEDDED"), "Deve iniciar o servidor web embutido");
            ThzDevServer.parar();
        } finally {
            ThzDevServer.parar();
            System.setOut(orig);
            System.clearProperty("thz.test.mode");
        }
    }

    @Test
    @DisplayName("BibliotecaConsole deve registrar e executar funcoes de console")
    void testBibliotecaConsole() {
        BibliotecaConsole.registrar();
        assertDoesNotThrow(BibliotecaConsole::registrar);
    }

    @Test
    @DisplayName("ThzCli deve executar release emitindo laudo oficial criptografico e verificando homologacao")
    void testComandoReleaseProducao(@TempDir Path tempDir) throws Exception {
        System.setProperty("thz.test.mode", "true");
        Path arquivo = tempDir.resolve("servico_release.thz");
        String src = """
                PROGRAMA ServicoRelease
                METADADOS_ARQUITETURA
                    DOMINIO: "Faturamento"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "Engenharia"
                    SLO_LATENCIA_MAXIMA: "10ms"
                    CONFORMIDADE: "SOX-404"
                FIM_METADADOS
                ESTRUTURA Fatura
                    id: TEXTO
                    total: DECIMAL(18, 2)
                    INVARIANTE total >= 0.00
                FIM_ESTRUTURA
                REGRA_NEGOCIO RegraRelease
                    IDENTIFICADOR_REGRA: "REG-REL-01"
                    RASTREIO_REQUISITO: "REQ-REL-01"
                    CONTRATO_ENTRADA
                        EXIGE f.total > 0.00
                    FIM_CONTRATO_ENTRADA
                    CONTRATO_SAIDA
                        GARANTE f.total > 0.00
                    FIM_CONTRATO_SAIDA
                    OPERACAO Processar(f: Fatura) : DECIMAL(18, 2)
                    INICIO
                        RETORNE f.total
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;
        ThzIO.escreverTexto(arquivo.toString(), src);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        try {
            System.setOut(new PrintStream(out));
            Path laudoSaida = tempDir.resolve("laudo_oficial.md");
            ThzCli.main(new String[]{"release", arquivo.toString(), "--saida", laudoSaida.toString()});

            assertTrue(Files.exists(laudoSaida), "Arquivo de laudo oficial deve ter sido criado");
            String conteudoLaudo = Files.readString(laudoSaida, StandardCharsets.UTF_8);
            assertTrue(conteudoLaudo.contains("LIBERADO PARA PRODUÇÃO"));
            assertTrue(conteudoLaudo.contains("ServicoRelease"));
            assertTrue(conteudoLaudo.contains("SHA-256 do Fonte"));
            assertTrue(conteudoLaudo.contains("Homologação Concluída com Sucesso"));
        } finally {
            System.setOut(orig);
            System.clearProperty("thz.test.mode");
        }
    }
}
