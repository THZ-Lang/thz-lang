package thz.lang.brasil;

import thz.lang.interpretador.ValorThz;
import thz.lang.runtime.DataThz;
import thz.lang.runtime.DecimalFixo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ThzBrasilEngine — Motor de Funcionalidades Nativas e Cotidianas do Brasil Digital.
 * Inclui:
 * 1. Endereços, CEPs e integração com o banco interno offline (.thzdbi)
 * 2. PIX: Geração de payload "PIX Copia e Cola" (EMVco / BR Code) com CRC16 e validação de chaves
 * 3. Boletos Bancários: Validação de linha digitável, conversão para código de barras e extração de vencimento/valor
 * 4. Documentos Nacionais: CPF, CNPJ, Título de Eleitor, CNH, PIS/PASEP e Telefones com DDD
 * 5. Calendário Bancário & Feriados Nacionais: Feriados fixos (Lei 14.759), feriados móveis (Páscoa/Carnaval/Corpus Christi) e dias úteis
 * 6. Valores por Extenso em moeda corrente (Real / Centavos).
 */
public final class ThzBrasilEngine {

    private static final Set<String> UFS_VALIDAS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private static final Map<String, String> REGIOES_UF = Map.ofEntries(
            Map.entry("SP", "Sudeste"), Map.entry("RJ", "Sudeste"), Map.entry("MG", "Sudeste"), Map.entry("ES", "Sudeste"),
            Map.entry("PR", "Sul"), Map.entry("SC", "Sul"), Map.entry("RS", "Sul"),
            Map.entry("DF", "Centro-Oeste"), Map.entry("GO", "Centro-Oeste"), Map.entry("MT", "Centro-Oeste"), Map.entry("MS", "Centro-Oeste"),
            Map.entry("BA", "Nordeste"), Map.entry("PE", "Nordeste"), Map.entry("CE", "Nordeste"), Map.entry("MA", "Nordeste"),
            Map.entry("PB", "Nordeste"), Map.entry("RN", "Nordeste"), Map.entry("AL", "Nordeste"), Map.entry("SE", "Nordeste"), Map.entry("PI", "Nordeste"),
            Map.entry("AM", "Norte"), Map.entry("PA", "Norte"), Map.entry("RO", "Norte"), Map.entry("TO", "Norte"),
            Map.entry("AC", "Norte"), Map.entry("AP", "Norte"), Map.entry("RR", "Norte")
    );

    private ThzBrasilEngine() {}

    // =========================================================================
    // 1. ENDEREÇOS E CEPS (.thzdbi)
    // =========================================================================

    public static String formatarCep(String cep) {
        if (cep == null) return "";
        String digitos = cep.replaceAll("\\D", "");
        if (digitos.length() != 8) return cep;
        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }

    public static boolean validarUf(String uf) {
        if (uf == null) return false;
        return UFS_VALIDAS.contains(uf.trim().toUpperCase());
    }

    public static String regiaoUf(String uf) {
        if (uf == null) return "";
        return REGIOES_UF.getOrDefault(uf.trim().toUpperCase(), "Desconhecida");
    }

    public static ValorThz.Registro consultarCep(String cep) {
        Map<String, String> dados = ThzInternalDatabase.consultarCep(cep);
        Map<String, ValorThz> campos = new LinkedHashMap<>();
        campos.put("cep", ValorThz.TEXTO(dados.getOrDefault("cep", formatarCep(cep))));
        campos.put("logradouro", ValorThz.TEXTO(dados.getOrDefault("logradouro", "")));
        campos.put("bairro", ValorThz.TEXTO(dados.getOrDefault("bairro", "")));
        campos.put("cidade", ValorThz.TEXTO(dados.getOrDefault("cidade", "")));
        campos.put("uf", ValorThz.TEXTO(dados.getOrDefault("uf", "")));
        campos.put("ibge", ValorThz.TEXTO(dados.getOrDefault("ibge", "")));
        campos.put("ddd", ValorThz.TEXTO(dados.getOrDefault("ddd", "")));
        campos.put("regiao", ValorThz.TEXTO(regiaoUf(dados.getOrDefault("uf", ""))));
        return new ValorThz.Registro("EnderecoCep", campos);
    }

    public static String formatarEndereco(String logradouro, String numero, String complemento, String bairro, String cidade, String uf, String cep) {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null && !logradouro.isBlank()) sb.append(logradouro);
        if (numero != null && !numero.isBlank()) sb.append(", ").append(numero);
        if (complemento != null && !complemento.isBlank()) sb.append(" - ").append(complemento);
        if (bairro != null && !bairro.isBlank()) sb.append(", ").append(bairro);
        if (cidade != null && !cidade.isBlank()) sb.append(" - ").append(cidade);
        if (uf != null && !uf.isBlank()) sb.append("/").append(uf.toUpperCase());
        if (cep != null && !cep.isBlank()) sb.append(", CEP: ").append(formatarCep(cep));
        return sb.toString();
    }

    // =========================================================================
    // 2. PIX (EMVco / BR Code & Validação de Chaves)
    // =========================================================================

    public static String gerarPixCopiaECola(String chave, String nomeRecebedor, String cidadeRecebedor, BigDecimal valor, String txId) {
        if (chave == null || chave.isBlank()) return "";
        String nome = sanitizarTextoEmv(nomeRecebedor != null && !nomeRecebedor.isBlank() ? nomeRecebedor : "RECEBEDOR", 25);
        String cidade = sanitizarTextoEmv(cidadeRecebedor != null && !cidadeRecebedor.isBlank() ? cidadeRecebedor : "SAO PAULO", 15);
        String tid = (txId != null && !txId.isBlank()) ? txId.replaceAll("[^A-Za-z0-9]", "") : "***";
        if (tid.length() > 25) tid = tid.substring(0, 25);

        // Sub-payload da conta (GUI 26 -> 00: br.gov.bcb.pix, 01: chave)
        String gui = formatarCampoEmv("00", "br.gov.bcb.pix");
        String chaveCampo = formatarCampoEmv("01", chave.trim());
        String merchantAccountInfo = formatarCampoEmv("26", gui + chaveCampo);

        StringBuilder payload = new StringBuilder();
        payload.append(formatarCampoEmv("00", "01")); // Formato
        payload.append(formatarCampoEmv("01", "12")); // Ponto de Iniciação (12 = dinâmico / 11 = estático)
        payload.append(merchantAccountInfo);
        payload.append(formatarCampoEmv("52", "0000")); // Merchant Category Code
        payload.append(formatarCampoEmv("53", "986"));  // Moeda BRL
        if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
            payload.append(formatarCampoEmv("54", valor.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString()));
        }
        payload.append(formatarCampoEmv("58", "BR")); // País
        payload.append(formatarCampoEmv("59", nome)); // Nome
        payload.append(formatarCampoEmv("60", cidade)); // Cidade

        // Campo 62: Additional Data Field (TxId)
        String campoTxId = formatarCampoEmv("05", tid);
        payload.append(formatarCampoEmv("62", campoTxId));

        // Inicia Campo 63 com CRC
        payload.append("6304");

        // Calcula CRC16-CCITT
        String crc = calcularCrc16Ccitt(payload.toString());
        return payload.toString() + crc;
    }

    public static boolean validarChavePix(String chave, String tipo) {
        if (chave == null || chave.isBlank()) return false;
        String t = tipo != null ? tipo.trim().toUpperCase() : "AUTO";
        String limpo = chave.trim();

        return switch (t) {
            case "CPF" -> thz.lang.analytics.ThzDataQuality.validarCpf(limpo);
            case "CNPJ" -> thz.lang.analytics.ThzDataQuality.validarCnpj(limpo);
            case "EMAIL" -> Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$").matcher(limpo).matches();
            case "TELEFONE" -> limpo.matches("^\\+55\\d{10,11}$") || limpo.replaceAll("\\D", "").matches("^55\\d{10,11}$");
            case "ALEATORIA", "EVP" -> Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matcher(limpo).matches();
            default -> {
                // AUTO: testa todas
                yield thz.lang.analytics.ThzDataQuality.validarCpf(limpo)
                        || thz.lang.analytics.ThzDataQuality.validarCnpj(limpo)
                        || Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$").matcher(limpo).matches()
                        || limpo.replaceAll("\\D", "").length() >= 10
                        || Pattern.compile("^[0-9a-fA-F-]{36}$").matcher(limpo).matches();
            }
        };
    }

    private static String formatarCampoEmv(String id, String valor) {
        int len = valor.getBytes(StandardCharsets.UTF_8).length;
        return id + String.format(Locale.US, "%02d", len) + valor;
    }

    private static String sanitizarTextoEmv(String texto, int maxLen) {
        String limpo = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", "")
                .trim();
        if (limpo.length() > maxLen) limpo = limpo.substring(0, maxLen);
        return limpo;
    }

    private static String calcularCrc16Ccitt(String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        int crc = 0xFFFF;
        int polinomio = 0x1021;
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i)) & 1) == 1;
                boolean c15 = ((crc >> 15) & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) crc ^= polinomio;
            }
        }
        crc &= 0xFFFF;
        return String.format(Locale.US, "%04X", crc);
    }

    // =========================================================================
    // 3. BOLETO BANCÁRIO (Linha Digitável, Código de Barras & Vencimento)
    // =========================================================================

    public static boolean validarLinhaDigitavel(String linha) {
        if (linha == null) return false;
        String digitos = linha.replaceAll("\\D", "");
        if (digitos.length() != 47) return false;

        // Campo 1: posições 0..9 (DV é o 10º caractere)
        if (!validarModulo10(digitos.substring(0, 9), digitos.charAt(9) - '0')) return false;
        // Campo 2: posições 10..20 (DV é o 21º caractere)
        if (!validarModulo10(digitos.substring(10, 20), digitos.charAt(20) - '0')) return false;
        // Campo 3: posições 21..31 (DV é o 32º caractere)
        if (!validarModulo10(digitos.substring(21, 31), digitos.charAt(31) - '0')) return false;

        return true;
    }

    public static String linhaDigitavelParaCodigoBarras(String linha) {
        if (linha == null) return "";
        String d = linha.replaceAll("\\D", "");
        if (d.length() != 47) return "";

        // Posições no padrão Febraban:
        // Banco (3) + Moeda (1) + DV Geral (1) + Fator/Valor (14) + Campo Livre (25)
        String bancoEMoeda = d.substring(0, 4);
        String dvGeral = d.substring(32, 33);
        String fatorEValor = d.substring(33, 47);
        String campoLivre1 = d.substring(4, 9);
        String campoLivre2 = d.substring(10, 20);
        String campoLivre3 = d.substring(21, 31);

        return bancoEMoeda + dvGeral + fatorEValor + campoLivre1 + campoLivre2 + campoLivre3;
    }

    public static DecimalFixo extrairValorBoleto(String linha) {
        if (linha == null) return DecimalFixo.ZERO;
        String d = linha.replaceAll("\\D", "");
        if (d.length() < 47) return DecimalFixo.ZERO;
        String valorCentavosStr = d.substring(37, 47);
        try {
            long centavos = Long.parseLong(valorCentavosStr);
            BigDecimal valor = BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_EVEN);
            return DecimalFixo.deTexto(valor.toPlainString(), 4);
        } catch (Exception e) {
            return DecimalFixo.ZERO;
        }
    }

    private static boolean validarModulo10(String bloco, int dvEsperado) {
        int soma = 0;
        int peso = 2;
        for (int i = bloco.length() - 1; i >= 0; i--) {
            int num = (bloco.charAt(i) - '0') * peso;
            if (num > 9) num = (num / 10) + (num % 10);
            soma += num;
            peso = (peso == 2) ? 1 : 2;
        }
        int dezenaSuperior = ((soma + 9) / 10) * 10;
        int dvCalculado = dezenaSuperior - soma;
        if (dvCalculado == 10) dvCalculado = 0;
        return dvCalculado == dvEsperado;
    }

    // =========================================================================
    // 4. DOCUMENTOS NACIONAIS & IDENTIDADES
    // =========================================================================

    public static String formatarCpf(String cpf) {
        if (cpf == null) return "";
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) return cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static String formatarCnpj(String cnpj) {
        if (cnpj == null) return "";
        String d = cnpj.replaceAll("\\D", "");
        if (d.length() != 14) return cnpj;
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    public static String formatarTelefone(String tel) {
        if (tel == null) return "";
        String d = tel.replaceAll("\\D", "");
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        } else if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return tel;
    }

    public static boolean validarTituloEleitor(String titulo) {
        if (titulo == null) return false;
        String d = titulo.replaceAll("\\D", "");
        if (d.length() != 12) return false;
        if (d.matches("(\\d)\\1{11}")) return false;

        // Estado emissor (dígitos 9 e 10) deve estar entre 01 e 28
        int estado = Integer.parseInt(d.substring(8, 10));
        if (estado < 1 || estado > 28) return false;

        // DV1
        int soma1 = 0;
        for (int i = 0; i < 8; i++) {
            soma1 += (d.charAt(i) - '0') * (i + 2);
        }
        int resto1 = soma1 % 11;
        int dv1 = (resto1 == 10 || resto1 == 0) ? 0 : resto1;
        if (dv1 != (d.charAt(10) - '0')) return false;

        // DV2
        int soma2 = (d.charAt(8) - '0') * 7 + (d.charAt(9) - '0') * 8 + dv1 * 9;
        int resto2 = soma2 % 11;
        int dv2 = (resto2 == 10 || resto2 == 0) ? 0 : resto2;
        return dv2 == (d.charAt(11) - '0');
    }

    public static boolean validarCnh(String cnh) {
        if (cnh == null) return false;
        String d = cnh.replaceAll("\\D", "");
        if (d.length() != 11) return false;
        if (d.matches("(\\d)\\1{10}")) return false;

        int soma1 = 0;
        int peso1 = 9;
        for (int i = 0; i < 9; i++) {
            soma1 += (d.charAt(i) - '0') * peso1--;
        }
        int dv1 = soma1 % 11;
        if (dv1 >= 10) dv1 = 0;
        if (dv1 != (d.charAt(9) - '0')) return false;

        int soma2 = 0;
        int peso2 = 1;
        for (int i = 0; i < 9; i++) {
            soma2 += (d.charAt(i) - '0') * peso2++;
        }
        int dv2 = soma2 % 11;
        if (dv2 >= 10) dv2 = 0;
        return dv2 == (d.charAt(10) - '0');
    }

    public static boolean validarPis(String pis) {
        if (pis == null) return false;
        String d = pis.replaceAll("\\D", "");
        if (d.length() != 11) return false;
        if (d.matches("(\\d)\\1{10}")) return false;

        int[] pesos = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (d.charAt(i) - '0') * pesos[i];
        }
        int resto = 11 - (soma % 11);
        int dv = (resto == 10 || resto == 11) ? 0 : resto;
        return dv == (d.charAt(10) - '0');
    }

    // =========================================================================
    // 5. CALENDÁRIO BANCÁRIO & FERIADOS NACIONAIS DO BRASIL
    // =========================================================================

    public static boolean ehFeriadoNacional(LocalDate data) {
        if (data == null) return false;
        int ano = data.getYear();
        int mes = data.getMonthValue();
        int dia = data.getDayOfMonth();

        // 1. Feriados Nacionais Fixos (Leis 662/49, 10.607/02 e 14.759/23)
        if (mes == 1 && dia == 1) return true;   // Confraternização Universal
        if (mes == 4 && dia == 21) return true;  // Tiradentes
        if (mes == 5 && dia == 1) return true;   // Dia do Trabalho
        if (mes == 9 && dia == 7) return true;   // Independência do Brasil
        if (mes == 10 && dia == 12) return true; // Nossa Senhora Aparecida
        if (mes == 11 && dia == 2) return true;  // Finados
        if (mes == 11 && dia == 15) return true; // Proclamação da República
        if (mes == 11 && dia == 20) return true; // Consciência Negra (Lei 14.759/23)
        if (mes == 12 && dia == 25) return true; // Natal

        // 2. Feriados Móveis (baseados no Domingo de Páscoa)
        LocalDate pascoa = calcularDomingoPascoa(ano);
        LocalDate carnavalSegunda = pascoa.minusDays(48);
        LocalDate carnavalTerca = pascoa.minusDays(47);
        LocalDate sextaFeiraSanta = pascoa.minusDays(2);
        LocalDate corpusChristi = pascoa.plusDays(60);

        return data.equals(carnavalSegunda) || data.equals(carnavalTerca) || data.equals(sextaFeiraSanta) || data.equals(corpusChristi);
    }

    public static boolean ehDiaUtil(LocalDate data) {
        if (data == null) return false;
        DayOfWeek dow = data.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return !ehFeriadoNacional(data);
    }

    public static LocalDate proximoDiaUtil(LocalDate data) {
        if (data == null) return LocalDate.now();
        LocalDate cur = data;
        while (!ehDiaUtil(cur)) {
            cur = cur.plusDays(1);
        }
        return cur;
    }

    private static LocalDate calcularDomingoPascoa(int ano) {
        // Algoritmo de Butcher / Meeus para cálculo eclesiástico da Páscoa
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    // =========================================================================
    // 6. VALOR POR EXTENSO EM REAIS
    // =========================================================================

    private static final String[] UNIDADES = {
            "", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez",
            "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove"
    };
    private static final String[] DEZENAS = {
            "", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa"
    };
    private static final String[] CENTENAS = {
            "", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos"
    };

    public static String valorPorExtenso(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            return "zero reais";
        }

        boolean negativo = valor.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal positivo = valor.abs().setScale(2, java.math.RoundingMode.HALF_EVEN);
        long inteiros = positivo.longValue();
        int centavos = positivo.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        StringBuilder sb = new StringBuilder();
        if (negativo) sb.append("menos ");

        if (inteiros > 0) {
            sb.append(escreverNumeroExtenso(inteiros));
            sb.append(inteiros == 1 ? " real" : " reais");
        }

        if (centavos > 0) {
            if (inteiros > 0) sb.append(" e ");
            sb.append(escreverNumeroExtenso(centavos));
            sb.append(centavos == 1 ? " centavo" : " centavos");
        }

        return sb.toString();
    }

    private static String escreverNumeroExtenso(long n) {
        if (n == 0) return "zero";
        if (n == 100) return "cem";

        if (n >= 1_000_000_000) {
            long bi = n / 1_000_000_000;
            long resto = n % 1_000_000_000;
            String termo = bi == 1 ? "um bilhão" : escreverNumeroExtenso(bi) + " bilhões";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 1_000_000) {
            long mi = n / 1_000_000;
            long resto = n % 1_000_000;
            String termo = mi == 1 ? "um milhão" : escreverNumeroExtenso(mi) + " milhões";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 1000) {
            long mil = n / 1000;
            long resto = n % 1000;
            String termo = mil == 1 ? "mil" : escreverNumeroExtenso(mil) + " mil";
            return resto > 0 ? termo + (resto <= 100 || resto % 100 == 0 ? " e " : " ") + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 100) {
            int c = (int) (n / 100);
            long resto = n % 100;
            String termo = CENTENAS[c];
            return resto > 0 ? termo + " e " + escreverNumeroExtenso(resto) : termo;
        }

        if (n >= 20) {
            int d = (int) (n / 10);
            long resto = n % 10;
            String termo = DEZENAS[d];
            return resto > 0 ? termo + " e " + escreverNumeroExtenso(resto) : termo;
        }

        return UNIDADES[(int) n];
    }
}
