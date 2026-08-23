package thz.lang.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DecimalMonetarioTest {

    @Test
    public void somaESubtracaoDecimais() {
        var a = DecimalFixo.deTexto("10.5000", 4);
        var b = DecimalFixo.deTexto("4.2500", 4);
        assertEquals("14.7500", a.somar(b).formatar());
        assertEquals("6.2500", a.subtrair(b).formatar());
    }

    @Test
    public void multiplicacaoDecimais() {
        var a = DecimalFixo.deTexto("2.5000", 4);
        var b = DecimalFixo.deTexto("4.0000", 4);
        assertEquals("10.0000", a.multiplicar(b).formatar());
    }

    @Test
    public void arredondamentoBancarioMeioPar() {
        // 1.005 com escala 2 arredonda para o par mais próximo: 1.00
        var d1 = DecimalFixo.deTexto("1.005", 3).paraEscala(2, ModoArredondamento.BANCARIO);
        assertEquals("1.00", d1.formatar());

        // 1.015 com escala 2 arredonda para o par mais próximo: 1.02
        var d2 = DecimalFixo.deTexto("1.015", 3).paraEscala(2, ModoArredondamento.BANCARIO);
        assertEquals("1.02", d2.formatar());

        // 2.500 com escala 0 arredonda para 2
        var d3 = DecimalFixo.deTexto("2.500", 3).paraEscala(0, ModoArredondamento.BANCARIO);
        assertEquals("2", d3.formatar());

        // 3.500 com escala 0 arredonda para 4
        var d4 = DecimalFixo.deTexto("3.500", 3).paraEscala(0, ModoArredondamento.BANCARIO);
        assertEquals("4", d4.formatar());
    }

    @Test
    public void divisaoDecimaisEDivisaoPorZero() {
        var a = DecimalFixo.deTexto("10.0000", 4);
        var b = DecimalFixo.deTexto("3.0000", 4);
        assertEquals("3.3333", a.dividir(b).formatar());

        var zero = DecimalFixo.deTexto("0.0000", 4);
        assertThrows(ErroDecimal.class, () -> a.dividir(zero));
    }

    @Test
    public void comparacaoENegativos() {
        var a = DecimalFixo.deTexto("-0.5000", 4);
        var b = DecimalFixo.deTexto("0.5000", 4);
        assertTrue(a.comparar(b) < 0);
        assertTrue(b.comparar(a) > 0);
        assertEquals("-0.5000", a.formatar());
        assertEquals("0.5000", a.abs().formatar());
        assertEquals("0.5000", a.negar().formatar());
    }

    @Test
    public void monetarioMesmaMoeda() {
        var m1 = Monetario.deTexto("150.75", "BRL");
        var m2 = Monetario.deTexto("50.25", "BRL");
        assertEquals("201.00", m1.somar(m2).quantia.formatar());
        assertEquals("100.50", m1.subtrair(m2).quantia.formatar());
    }

    @Test
    public void monetarioMultiplicacaoPorEscalar() {
        var m = Monetario.deTexto("100.00", "USD");
        var fator = DecimalFixo.deTexto("1.5000", 4);
        var res = m.multiplicar(fator);
        assertEquals("150.0000", res.quantia.formatar());
        assertEquals("150.00", res.quantia.paraEscala(res.moeda.casas()).formatar());
        assertEquals("USD", res.moeda.codigo());
    }

    @Test
    public void monetarioJpySemCasasDecimais() {
        var jpy = Monetario.deTexto("5000", "JPY");
        assertEquals("5000", jpy.quantia.formatar());
        assertEquals(0, jpy.moeda.casas());
    }

    @Test
    public void monetarioMisturaDeMoedasRejeitada() {
        var brl = Monetario.deTexto("100.00", "BRL");
        var usd = Monetario.deTexto("100.00", "USD");
        assertThrows(ErroMonetario.class, () -> brl.somar(usd));
        assertThrows(ErroMonetario.class, () -> brl.subtrair(usd));
    }

    @Test
    public void monetarioMoedaInvalidaRejeitada() {
        assertThrows(ErroMonetario.class, () -> Monetario.deTexto("100.00", "XYZ_INVALID"));
    }
}
