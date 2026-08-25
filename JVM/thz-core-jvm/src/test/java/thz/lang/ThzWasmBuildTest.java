package thz.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.driver.ThzCompilerDriver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThzWasmBuildTest {

    @Test
    @DisplayName("Deve compilar programa para alvo WebAssembly (WASM) com sucesso")
    void testCompilacaoWasm() {
        String codigo = """
            PROGRAMA ValidadorFiscalWasm
            ESTRUTURA NotaFiscal
                numero: INTEIRO64
                valor: DECIMAL(18, 2)
            FIM_ESTRUTURA

            REGRA_NEGOCIO ValidarNota
                OPERACAO Validar(nf: NotaFiscal) : LOGICO
                INICIO
                    SE nf.valor > 0.00
                        RETORNE VERDADEIRO
                    SENAO
                        RETORNE FALSO
                    FIM_SE
                FIM
            FIM_REGRA_NEGOCIO
            FIM_PROGRAMA
            """;

        var res = ThzCompilerDriver.compilarOuExecutar(codigo, ThzCompilerDriver.Alvo.WEBASSEMBLY, false, Map.of());
        if (!res.sucesso()) {
            System.err.println("Erros semanticos: " + res.erros());
        }
        assertTrue(res.sucesso(), "Compilação para WebAssembly deve ter sucesso: " + res.erros());
        assertNotNull(res.saidaTexto());
        assertTrue(res.saidaTexto().contains("WebAssembly (WASM)"));
        assertTrue(res.saidaTexto().contains("class NotaFiscal"));
    }
}
