package thz.lang.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * ThzSecurity — Hashing criptográfico, criptografia autenticada AES-256-GCM, HMAC, PBKDF2 e tokens seguros.
 */
public final class ThzSecurity {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int PBKDF2_ITERATIONS = 65536;

    private ThzSecurity() {}

    public static String sha256(String entrada) {
        return hash("SHA-256", entrada);
    }

    public static String sha512(String entrada) {
        return hash("SHA-512", entrada);
    }

    private static String hash(String algoritmo, String entrada) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritmo);
            byte[] digest = md.digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash não disponível: " + algoritmo, e);
        }
    }

    public static String hmacSha256(String texto, String chave) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmac = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    public static String criptografarAes(String textoClaro, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha na criptografia AES-GCM: " + e.getMessage(), e);
        }
    }

    public static String descriptografarAes(String payloadBase64, String chaveHex) {
        try {
            byte[] keyBytes = normalizarChave(chaveHex);
            byte[] payload = Base64.getUrlDecoder().decode(payloadBase64);

            if (payload.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Payload cifrado inválido.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(payload, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Falha na descriptografia AES-GCM: " + e.getMessage(), e);
        }
    }

    public static String hashSenha(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    public static boolean verificarSenha(String senha, String hashArmazenado) {
        String[] partes = hashArmazenado.split(":");
        if (partes.length != 2) return false;
        byte[] salt = HexFormat.of().parseHex(partes[0]);
        byte[] hashEsperado = HexFormat.of().parseHex(partes[1]);
        byte[] hashCalculado = pbkdf2(senha.toCharArray(), salt);
        return MessageDigest.isEqual(hashEsperado, hashCalculado);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Falha no PBKDF2: " + e.getMessage(), e);
        }
    }

    public static String gerarToken(int tamanhoBytes) {
        byte[] bytes = new byte[Math.max(16, tamanhoBytes)];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String gerarUuid() {
        return UUID.randomUUID().toString();
    }

    private static byte[] normalizarChave(String chave) {
        try {
            if (chave.length() == 64 && chave.matches("^[0-9a-fA-F]+$")) {
                return HexFormat.of().parseHex(chave);
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(chave.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
