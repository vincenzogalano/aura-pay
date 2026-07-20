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

/**
 * AES-256 GCM authenticated encryption/decryption utility.
 * Prepends a randomly generated 12-byte IV to the encrypted ciphertext.
 */
public final class AESCryptoUtils {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AESCryptoUtils() {
        // Utility class
    }

    /**
     * Encrypts plaintext string using AES-256 GCM with a secret key string (Hex or Base64 or 32-byte key).
     *
     * @param plainText Plain text to encrypt.
     * @param secretKey 32-byte secret key (or Hex representation of 32 bytes).
     * @return Base64 encoded string containing [12-byte IV + Ciphertext].
     */
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

    /**
     * Decrypts Base64 payload [IV + Ciphertext] using AES-256 GCM.
     *
     * @param base64Encrypted Base64 encoded string containing [12-byte IV + Ciphertext].
     * @param secretKey       Secret key used for encryption.
     * @return Decrypted plaintext string.
     */
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
        // Try hex decoding if string length is 64 hex characters
        if (key.length() == 64) {
            try {
                return HexFormat.of().parseHex(key);
            } catch (IllegalArgumentException ignored) {
                // Fallback to padding/truncating below
            }
        }
        // Pad or truncate to 32 bytes for AES-256
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
        return padded;
    }
}
