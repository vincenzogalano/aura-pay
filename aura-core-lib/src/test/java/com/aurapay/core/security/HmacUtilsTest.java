package com.aurapay.core.security;

import com.aurapay.core.exception.CryptoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacUtilsTest {

    private final String payload = "{\"paymentIntentId\":\"pi_123\",\"amount\":1000}";
    private final String secret = "whsec_test_secret_key_123456789";

    @Test
    @DisplayName("Should calculate consistent HMAC-SHA256 signature")
    void shouldCalculateHmacSha256() {
        String signature1 = HmacUtils.calculateHmacSha256(payload, secret);
        String signature2 = HmacUtils.calculateHmacSha256(payload, secret);

        assertThat(signature1)
                .isNotNull()
                .isNotEmpty()
                .hasSize(64)
                .isEqualTo(signature2);
    }

    @Test
    @DisplayName("Should verify valid HMAC signature in constant time")
    void shouldVerifyValidSignature() {
        String signature = HmacUtils.calculateHmacSha256(payload, secret);
        boolean isValid = HmacUtils.verifyHmacSha256(payload, signature, secret);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject tampered payload or signature")
    void shouldRejectTamperedPayload() {
        String signature = HmacUtils.calculateHmacSha256(payload, secret);
        String tamperedPayload = payload + " ";

        boolean isValidPayload = HmacUtils.verifyHmacSha256(tamperedPayload, signature, secret);
        boolean isValidSecret = HmacUtils.verifyHmacSha256(payload, signature, secret + "_invalid");

        assertThat(isValidPayload).isFalse();
        assertThat(isValidSecret).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when payload or secret is null")
    void shouldThrowOnNullInput() {
        assertThatThrownBy(() -> HmacUtils.calculateHmacSha256(null, secret))
                .isInstanceOf(CryptoException.class);

        assertThatThrownBy(() -> HmacUtils.calculateHmacSha256(payload, null))
                .isInstanceOf(CryptoException.class);
    }
}
