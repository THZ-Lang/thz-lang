package thz.lang.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thz.lang.api.service.ThzService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThzApiServiceTest {

    private final ThzService thzService = new ThzService();

    @Test
    @DisplayName("ThzService deve analisar programa com sucesso e extrair AST e símbolos")
    void testAnaliseService() {
        String src = """
                PROGRAMA ApiDemo
                METADADOS_ARQUITETURA
                    DOMINIO: "Servicos"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "Time API"
                    SLO_LATENCIA_MAXIMA: "20ms"
                FIM_METADADOS
                ESTRUTURA Usuario
                    id : TEXTO
                    ativo : LOGICO
                FIM_ESTRUTURA
                REGRA_NEGOCIO RegraLogin
                    OPERACAO Validar() : LOGICO
                    INICIO
                        RETORNE verdadeiro
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        var res = thzService.analisar(src, false);
        assertNotNull(res);
        assertNotNull(res.astJson());
        assertFalse(res.simbolos().isEmpty());
    }

    @Test
    @DisplayName("ThzService deve gerar auditoria, documentação, IR e validar SIMD")
    void testGeradoresService() {
        String src = """
                PROGRAMA GeradoresDemo
                METADADOS_ARQUITETURA
                    DOMINIO: "Vendas"
                    CAMADA: "Dominio"
                    VERSAO: "1.0.0"
                    AUTOR: "Arquiteto"
                    SLO_LATENCIA_MAXIMA: "20ms"
                FIM_METADADOS
                REGRA_NEGOCIO Regra1
                    IDENTIFICADOR_REGRA: "BR-001"
                    RASTREIO_REQUISITO: "REQ-001"
                    OPERACAO Calcular(a : INTEIRO32) : INTEIRO32
                    INICIO
                        RETORNE a + 42
                    FIM
                FIM_REGRA_NEGOCIO
                FIM_PROGRAMA
                """;

        // Auditoria
        var audit = thzService.auditar(src);
        assertNotNull(audit);
        assertNotNull(audit.markdown());

        // Docgen
        var doc = thzService.gerarDocumentacao(src);
        assertNotNull(doc);
        assertTrue(doc.markdown().contains("GeradoresDemo"));

        // IR
        var ir = thzService.gerarIr(src);
        assertNotNull(ir);
        assertTrue(ir.irJson().contains("versaoIr") || ir.irJson().contains("thz-ir"));

        // SIMD
        var simd = thzService.validarSimd(src);
        assertNotNull(simd);
    }
}
