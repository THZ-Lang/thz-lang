package thz.lang.interpretador;

import thz.lang.runtime.DataHoraThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Universo de valores THZ — port exato de interpretador.ts ValorThz.
 * Sealed interface com records para cada classe THZ.
 */
public sealed interface ValorThz permits
        ValorThz.Inteiro,
        ValorThz.Decimal,
        ValorThz.Monetario,
        ValorThz.Texto,
        ValorThz.Logico,
        ValorThz.Nulo,
        ValorThz.Enumerado,
        ValorThz.Resultado,
        ValorThz.Registro,
        ValorThz.Fatia,
        ValorThz.Data,
        ValorThz.DataHora {

    record Inteiro(BigInteger valor) implements ValorThz {}
    record Decimal(DecimalFixo valor) implements ValorThz {}
    record Monetario(thz.lang.runtime.Monetario valor) implements ValorThz {}
    record Texto(String valor) implements ValorThz {}
    record Logico(boolean valor) implements ValorThz {}
    record Nulo() implements ValorThz {}
    record Enumerado(String nomeEnumeracao, String valor) implements ValorThz {}
    record Resultado(boolean sucesso, ValorThz valor, ValorThz erro) implements ValorThz {}
    record Registro(String nomeEstrutura, Map<String, ValorThz> campos) implements ValorThz {}
    record Fatia(String tipoInterno, List<ValorThz> elementos) implements ValorThz {}
    record Data(DataThz valor) implements ValorThz {}
    record DataHora(DataHoraThz valor) implements ValorThz {}

    // Singleton NULO
    Nulo NULO = new Nulo();

    // Helpers de fábrica — espelham TS INTEIRO/DECIMAL/etc
    static Inteiro INTEIRO(BigInteger v) { return new Inteiro(v); }
    static Inteiro INTEIRO(long v) { return new Inteiro(BigInteger.valueOf(v)); }
    static Decimal DECIMAL(DecimalFixo v) { return new Decimal(v); }
    static Monetario MONETARIO(thz.lang.runtime.Monetario v) { return new Monetario(v); }
    static Texto TEXTO(String v) { return new Texto(v); }
    static Logico LOGICO(boolean v) { return new Logico(v); }
    static Enumerado ENUMERADO(String nomeEnum, String v) { return new Enumerado(nomeEnum, v); }
    static Data DATA(DataThz v) { return new Data(v); }
    static DataHora DATA_HORA(DataHoraThz v) { return new DataHora(v); }

    /** Retorna nome da classe THZ para mensagens de erro. */
    default String classe() {
        if (this instanceof Inteiro) return "INTEIRO";
        if (this instanceof Decimal) return "DECIMAL";
        if (this instanceof Monetario) return "MONETARIO";
        if (this instanceof Texto) return "TEXTO";
        if (this instanceof Logico) return "LOGICO";
        if (this instanceof Nulo) return "NULO";
        if (this instanceof Enumerado) return "ENUMERADO";
        if (this instanceof Resultado) return "RESULTADO";
        if (this instanceof Registro) return "REGISTRO";
        if (this instanceof Fatia) return "FATIA";
        if (this instanceof Data) return "DATA";
        if (this instanceof DataHora) return "DATA_HORA";
        return "DESCONHECIDO";
    }
}
