package thz.lang.gui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.semantico.AnalisadorSemantico;
import thz.lang.semantico.OpcoesAnalise;
import thz.lang.sintatico.ThzParser;

import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FormularioGuiTest {

    @BeforeAll
    public static void setup() {
        System.setProperty("thz.nao_interativo", "true");
        BibliotecaTela.registrar();
    }

    private ProgramaAst parse(String codigo) {
        var tokens = new ThzLexer(codigo).tokenize();
        var parser = new ThzParser(tokens);
        return parser.parse();
    }

    @Test
    public void exemploCadastroClientePassaNoAnalisadorSemantico() throws Exception {
        Path caminhoExemplo = Path.of("exemplos", "cadastro_cliente_gui.thz");
        if (!Files.exists(caminhoExemplo)) {
            caminhoExemplo = Path.of("thz-lang-engine-JVM", "exemplos", "cadastro_cliente_gui.thz");
        }
        assertTrue(Files.exists(caminhoExemplo), "Arquivo de exemplo cadastro_cliente_gui.thz deve existir");

        String codigo = Files.readString(caminhoExemplo);
        ProgramaAst ast = parse(codigo);
        assertNotNull(ast);

        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        var erros = semantico.analisar(new OpcoesAnalise(true));
        assertTrue(erros.isEmpty(), "Exemplo não deve ter erros semânticos: " + erros);
    }

    @Test
    public void execucaoMontarTelaRetornaStatusValido() throws Exception {
        Path caminhoExemplo = Path.of("exemplos", "cadastro_cliente_gui.thz");
        if (!Files.exists(caminhoExemplo)) {
            caminhoExemplo = Path.of("thz-lang-engine-JVM", "exemplos", "cadastro_cliente_gui.thz");
        }
        String codigo = Files.readString(caminhoExemplo);
        ProgramaAst ast = parse(codigo);

        List<String> saida = new ArrayList<>();
        InterpretadorThz interp = new InterpretadorThz(ast, saida::add, () -> "");
        ValorThz ret = interp.executarOperacao("MontarTela");

        assertNotNull(ret);
        assertTrue(ret instanceof ValorThz.Texto);
        String texto = ((ValorThz.Texto) ret).valor();
        assertTrue(texto.contains("Cadastro de Cliente"), "Deve conter o título do formulário: " + texto);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void renderizadorFormularioSubmeteComSucessoEContrato() throws Exception {
        String codigo = """
            PROGRAMA TesteForm
            ESTRUTURA Form
                titulo: TEXTO
                nome: TEXTO
                limite: DECIMAL(12, 2)
            FIM_ESTRUTURA
            REGRA_NEGOCIO Regra
                IDENTIFICADOR_REGRA: "RN-TEST"
                CONTRATO_ENTRADA
                    EXIGE TEXTO.comprimento(nome) >= 3
                    EXIGE limite > 0.00
                FIM_CONTRATO_ENTRADA
                OPERACAO Salvar(nome: TEXTO, limite: DECIMAL(12, 2)): TEXTO
                INICIO
                    RETORNE "OK:" + nome
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        ProgramaAst ast = parse(codigo);
        InterpretadorThz interp = new InterpretadorThz(ast);

        ValorThz.Registro reg = new ValorThz.Registro("Form", new java.util.LinkedHashMap<>(Map.of(
                "titulo", ValorThz.TEXTO("Meu Form"),
                "nome", ValorThz.TEXTO(""),
                "limite", ValorThz.DECIMAL(thz.lang.runtime.DecimalFixo.deTexto("100.00", 2))
        )));

        RenderizadorFormularioSwing renderer = new RenderizadorFormularioSwing(reg, "Regra.Salvar", interp);

        // Acessa reflexivamente campos privados para simular preenchimento e clique
        Field fieldCampos = RenderizadorFormularioSwing.class.getDeclaredField("camposEntrada");
        fieldCampos.setAccessible(true);
        Map<String, JComponent> campos = (Map<String, JComponent>) fieldCampos.get(renderer);

        Field fieldStatus = RenderizadorFormularioSwing.class.getDeclaredField("lblStatus");
        fieldStatus.setAccessible(true);
        JLabel lblStatus = new JLabel();
        fieldStatus.set(renderer, lblStatus);

        Field fieldPainelStatus = RenderizadorFormularioSwing.class.getDeclaredField("painelStatus");
        fieldPainelStatus.setAccessible(true);
        fieldPainelStatus.set(renderer, new JPanel());

        JTextField tfNome = new JTextField("Lucas");
        JTextField tfLimite = new JTextField("250.00");
        campos.put("nome", tfNome);
        campos.put("limite", tfLimite);

        // 1. Submissão Válida
        renderer.executarSubmissao();
        assertTrue(lblStatus.getText().contains("OK:Lucas"), "Status deve conter retorno de sucesso: " + lblStatus.getText());

        // 2. Submissão Inválida por Violação de Contrato EXIGE (nome < 3 letras)
        tfNome.setText("Lu");
        renderer.executarSubmissao();
        assertTrue(lblStatus.getText().contains("Violação de Contrato EXIGE"), "Deve detectar violação de contrato: " + lblStatus.getText());
    }

    @Test
    public void moduloTelaAlertaEConfirmarFuncionam() {
        String codigo = """
            PROGRAMA TesteTela
            REGRA_NEGOCIO Regra
                IDENTIFICADOR_REGRA: "RN-TELA"
                OPERACAO Testar(): LOGICO
                INICIO
                    VARIAVEL a : TEXTO <- TELA.alerta("Info", "Mensagem de Teste")
                    VARIAVEL c : LOGICO <- TELA.confirmar("Pergunta", "Confirmar?")
                    VARIAVEL p : TEXTO <- TELA.pedirTexto("Entrada", "Digite:")
                    RETORNE c
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        ProgramaAst ast = parse(codigo);
        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        assertTrue(semantico.analisar().isEmpty());

        InterpretadorThz interp = new InterpretadorThz(ast);
        ValorThz ret = interp.executarOperacao("Testar");
        assertNotNull(ret);
        assertTrue(ret instanceof ValorThz.Logico);
    }

    @Test
    public void exemploShowcaseWidgetsPassaNoAnalisadorSemantico() throws Exception {
        Path caminhoExemplo = Path.of("exemplos", "showcase_widgets_gui.thz");
        if (!Files.exists(caminhoExemplo)) {
            caminhoExemplo = Path.of("thz-lang-engine-JVM", "exemplos", "showcase_widgets_gui.thz");
        }
        assertTrue(Files.exists(caminhoExemplo), "showcase_widgets_gui.thz deve existir");

        String codigo = Files.readString(caminhoExemplo);
        ProgramaAst ast = parse(codigo);
        assertNotNull(ast);

        AnalisadorSemantico semantico = new AnalisadorSemantico(ast);
        var erros = semantico.analisar(new OpcoesAnalise(true));
        assertTrue(erros.isEmpty(), "showcase_widgets_gui não deve ter erros semânticos: " + erros);

        List<String> saida = new ArrayList<>();
        InterpretadorThz interp = new InterpretadorThz(ast, saida::add, () -> "");
        ValorThz ret = interp.executarOperacao("MontarTela");
        assertNotNull(ret);
        assertTrue(ret instanceof ValorThz.Texto);
    }

    @Test
    public void formularioLongoAjustaAlturaDentroDaTelaECentraliza() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        Path caminhoExemplo = Path.of("exemplos", "showcase_widgets_gui.thz");
        if (!Files.exists(caminhoExemplo)) {
            caminhoExemplo = Path.of("thz-lang-engine-JVM", "exemplos", "showcase_widgets_gui.thz");
        }
        String codigo = Files.readString(caminhoExemplo);
        ProgramaAst ast = parse(codigo);
        InterpretadorThz interp = new InterpretadorThz(ast);

        // Cria um registro volumoso simulando o formulário completo
        Map<String, ValorThz> campos = new java.util.LinkedHashMap<>();
        campos.put("titulo", ValorThz.TEXTO("Showcase Longo"));
        for (int i = 1; i <= 20; i++) {
            campos.put("campoTextoLong" + i, ValorThz.TEXTO("Valor " + i));
        }
        ValorThz.Registro reg = new ValorThz.Registro("PainelControleMaster", campos);

        RenderizadorFormularioSwing renderer = new RenderizadorFormularioSwing(reg, "GestaoEstacao.AplicarConfiguracoes", interp);
        var methodConstruir = RenderizadorFormularioSwing.class.getDeclaredMethod("construirInterface");
        methodConstruir.setAccessible(true);
        methodConstruir.invoke(renderer);

        JFrame frame = renderer.getFrame();
        assertNotNull(frame, "JFrame deve ter sido instanciado");

        java.awt.GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc == null) {
            gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        java.awt.Rectangle telaBounds = gc.getBounds();
        java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int maxAlturaUtil = telaBounds.height - insets.top - insets.bottom;

        // Verifica que a altura da janela não ultrapassa 88% da área visível da tela
        assertTrue(frame.getHeight() <= (int) (maxAlturaUtil * 0.88) + 10,
                "A altura do formulário (" + frame.getHeight() + "px) deve caber na área visível da tela (máx útil: " + maxAlturaUtil + "px)");

        // Verifica que a largura é ampla e confortável para exibir tabelas e controles sem cortes
        assertTrue(frame.getWidth() >= 700, "Largura do formulário deve ser confortável (>= 700px): " + frame.getWidth() + "px");
        assertTrue(frame.getWidth() <= telaBounds.width, "Largura do formulário não deve exceder a tela");

        // Verifica que a posição Y não está cortando fora da tela
        assertTrue(frame.getY() >= telaBounds.y + insets.top, "Janela deve estar dentro do topo da tela");
        assertTrue(frame.getY() + frame.getHeight() <= telaBounds.y + telaBounds.height, "Janela não deve ultrapassar o rodapé da tela");

        frame.dispose();
    }
}
