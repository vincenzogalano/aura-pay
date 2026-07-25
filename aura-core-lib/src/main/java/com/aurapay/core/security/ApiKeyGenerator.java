package com.aurapay.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
public final class ApiKeyGenerator {

    public static final String PK_TEST_PREFIX = "pk_test_";
    public static final String SK_TEST_PREFIX = "sk_test_";
    public static final String PK_LIVE_PREFIX = "pk_live_";
    public static final String SK_LIVE_PREFIX = "sk_live_";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final PasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder();

    private ApiKeyGenerator() {

    }
    public static String generateApiKey(String prefix) {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return prefix + randomPart;
    }
    public static String extractPrefix(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        if (apiKey.startsWith(PK_TEST_PREFIX)) return PK_TEST_PREFIX;
        if (apiKey.startsWith(SK_TEST_PREFIX)) return SK_TEST_PREFIX;
        if (apiKey.startsWith(PK_LIVE_PREFIX)) return PK_LIVE_PREFIX;
        if (apiKey.startsWith(SK_LIVE_PREFIX)) return SK_LIVE_PREFIX;
        return "";
    }
    public static boolean isTestKey(String apiKey) {
        String prefix = extractPrefix(apiKey);
        return PK_TEST_PREFIX.equals(prefix) || SK_TEST_PREFIX.equals(prefix);
    }
    public static String hashWithBcrypt(String rawKey) {
        return BCRYPT_ENCODER.encode(rawKey);
    }
    public static boolean verifyBcrypt(String rawKey, String bcryptHash) {
        if (rawKey == null || bcryptHash == null) {
            return false;
        }
        return BCRYPT_ENCODER.matches(rawKey, bcryptHash);
    }
    public static String hashWithSha256(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
