package com.aurapay.core.security;

import com.aurapay.core.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
public final class AESCryptoUtils {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AESCryptoUtils() {
    }
    public static String encrypt(String plainText, String secretKey) {
        if (plainText == null || secretKey == null) {
            throw new CryptoException("Plaintext and secret key must not be null");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            byte[] keyBytes = parseKey(secretKey);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }
    public static String decrypt(String base64Encrypted, String secretKey) {
        if (base64Encrypted == null || secretKey == null) {
            throw new CryptoException("Encrypted string and secret key must not be null");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Encrypted);
            if (decoded.length < GCM_IV_LENGTH) {
                throw new CryptoException("Invalid encrypted payload length");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            byte[] keyBytes = parseKey(secretKey);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }
    private static byte[] parseKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 32) {
            return keyBytes;
        }
        
        if (key.length() == 64) {
            try {
                return HexFormat.of().parseHex(key);
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        throw new CryptoException("Invalid key length: key must be exactly 256 bits (32 raw bytes or 64 hex characters).");
    }
}
