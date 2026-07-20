package com.aurapay.core.security;

import com.aurapay.core.exception.CryptoException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for HMAC-SHA256 signature generation and constant-time verification.
 * Used for Webhook payload signatures and presigned URL authentication.
 */
public final class HmacUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private HmacUtils() {
        // Utility class
    }

    /**
     * Calculates the HMAC-SHA256 signature for a given payload string using a secret key.
     *
     * @param payload The raw string content to sign.
     * @param secret  The secret key used for signing.
     * @return Hexadecimal encoded signature string.
     */
    public static String calculateHmacSha256(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new CryptoException("Payload and secret key must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    /**
     * Verifies an expected signature against a payload and secret in constant time
     * to prevent timing attack vulnerabilities.
     *
     * @param payload           The payload string.
     * @param expectedSignature The signature to verify.
     * @param secret            The secret key used for signing.
     * @return True if signature matches validly, false otherwise.
     */
    public static boolean verifyHmacSha256(String payload, String expectedSignature, String secret) {
        if (payload == null || expectedSignature == null || secret == null) {
            return false;
        }
        try {
            String computedSignature = calculateHmacSha256(payload, secret);
            byte[] a = computedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] b = expectedSignature.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            return false;
        }
    }
}
