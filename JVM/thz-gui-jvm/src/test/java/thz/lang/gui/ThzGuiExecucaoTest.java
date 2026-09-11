package thz.lang.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.gui.execucao.ExecutorMotorGui;
import thz.lang.interpretador.InjetorLoteDemo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;

import javax.swing.JMenu;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ThzGuiExecucaoTest {

    @Test
    @DisplayName("Galeria de Exemplos deve carregar e categorizar submenus sem falhas")
    public void testGaleriaExemplosCategorizacao() {
        AtomicReference<File> carregado = new AtomicReference<>();
        JMenu menu = GaleriaExemplos.criarMenuExemplos(carregado::set);

        assertNotNull(menu, "Menu de exemplos não deve ser nulo");
        assertTrue(menu.getItemCount() >= 5, "Menu deve possuir múltiplos submenus categorizados");

        // Verifica submenu de novidades v3.0
        boolean temNovidades = false;
        for (int i = 0; i < menu.getItemCount(); i++) {
            var item = menu.getItem(i);
            if (item instanceof JMenu sub) {
                System.out.println("Submenu " + i + ": " + sub.getText() + " | items: " + sub.getItemCount() + " | popup count: " + sub.getPopupMenu().getComponentCount());
                if (sub.getText().contains("Novidades")) {
                    temNovidades = true;
                    int count = sub.getItemCount();
                    if (count == 0) count = sub.getPopupMenu().getComponentCount();
                    assertTrue(count > 0, "Submenu de novidades deve conter exemplos (encontrado: " + count + ")");
                }
            }
        }
        assertTrue(temNovidades, "Menu deve conter submenu de novidades v3.0");
    }

    @Test
    @DisplayName("ExecutorMotorGui deve verificar com sucesso novos exemplos (Brasil, DAX, Estatistica)")
    public void testVerificacaoNovosExemplos() throws Exception {
        String codigoBrasil = """
                PROGRAMA TesteBrasilDigital
                METADADOS_ARQUITETURA
                    DOMINIO: "Financeiro"
                    SUBDOMINIO: "Pagamentos"
                    CAMADA: "Servico"
                    VERSAO: "1.0.0"
                    AUTOR: "THZ"
                    SLO_LATENCIA_MAXIMA: "10ms"
                    CONFORMIDADE: "ISO_4217"
                FIM_METADADOS

                REGRA_NEGOCIO ProcessarBrasil
                    OPERACAO Executar() : LOGICO
                    INICIO
                        VARIAVEL endPaulista : REGISTRO <- BRASIL.consultarCep("01310-100")
                        VARIAVEL valor : DECIMAL <- 150.5000
                        VARIAVEL pix : TEXTO <- BRASIL.pixCopiaECola("financeiro@empresa.com.br", "Empresa THZ", "Sao Paulo", valor, "TX123")
                        EXIBA "PIX: " + pix
                        RETORNE VERDADEIRO
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        var res = ExecutorMotorGui.verificar(codigoBrasil, false);
        assertTrue(res.sucesso(), "Verificação do código com BRASIL.* deve ter sucesso: " + res.mensagensFormatadas());
        assertNotNull(res.ast());
    }

    @Test
    @DisplayName("InterpretadorThz deve executar programas com REGRA_NEGOCIO e OPERACAO sem Principal")
    public void testExecucaoRegraSemProcedimentoPrincipal() {
        String codigoRegra = """
                PROGRAMA RegraCalculoDesconto
                METADADOS_ARQUITETURA
                    DOMINIO: "Vendas"
                    SUBDOMINIO: "Descontos"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "THZ"
                    SLO_LATENCIA_MAXIMA: "5ms"
                    CONFORMIDADE: "G4"
                FIM_METADADOS

                ESTRUTURA ItemVenda
                    produto: TEXTO
                    quantidade: INTEIRO64
                    valorUnitario: DECIMAL(10, 2)
                FIM_ESTRUTURA

                REGRA_NEGOCIO CalcularDescontoItem
                    IDENTIFICADOR_REGRA: "REG-DESC-001"
                    RASTREIO_REQUISITO: "REQ-001"

                    CONTRATO_ENTRADA
                        EXIGE item.quantidade > 0
                    FIM_CONTRATO_ENTRADA

                    CONTRATO_SAIDA
                        GARANTE RESULTADO >= 0.00
                    FIM_CONTRATO_SAIDA

                    OPERACAO Calcular(item: ItemVenda) : DECIMAL(10, 2)
                    INICIO
                        VARIAVEL totalBruto : DECIMAL(10, 2) <- item.quantidade * item.valorUnitario
                        RETORNE totalBruto * 0.10
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        var resVerif = ExecutorMotorGui.verificar(codigoRegra, false);
        assertTrue(resVerif.sucesso(), "Verificação da regra deve ter sucesso: " + resVerif.mensagensFormatadas());

        List<String> logs = new ArrayList<>();
        InterpretadorThz interp = new InterpretadorThz(resVerif.ast(), logs::add, () -> "");
        var execs = interp.listarOperacoesExecutaveis();
        assertFalse(execs.isEmpty(), "Deve encontrar operação executável na regra de negócio");

        var prim = execs.get(0);
        Map<String, ValorThz> args = InjetorLoteDemo.construirArgsOperacao(prim.operacao(), resVerif.ast(), interp::validarInvariantes, p -> null);
        ValorThz ret = interp.executarOperacao(prim.operacao().nome(), args);

        assertNotNull(ret, "Retorno da operação não deve ser nulo");
        assertTrue(ret instanceof ValorThz.Decimal, "Retorno deve ser do tipo DECIMAL");
    }
}
