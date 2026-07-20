package com.aurapay.core.security;

import com.aurapay.core.exception.CryptoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AESCryptoUtilsTest {

    private final String secretKey = "01234567890123456789012345678901"; // 32 bytes
    private final String plainText = "Sensitive-PAN-4532015899001111";

    @Test
    @DisplayName("Should encrypt and decrypt plaintext accurately with AES-256 GCM")
    void shouldEncryptAndDecrypt() {
        String encrypted = AESCryptoUtils.encrypt(plainText, secretKey);

        assertThat(encrypted)
                .isNotNull()
                .isNotEqualTo(plainText);

        String decrypted = AESCryptoUtils.decrypt(encrypted, secretKey);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should generate different ciphertexts for same plaintext due to random IV")
    void shouldProduceDifferentCiphertexts() {
        String encrypted1 = AESCryptoUtils.encrypt(plainText, secretKey);
        String encrypted2 = AESCryptoUtils.encrypt(plainText, secretKey);

        assertThat(encrypted1).isNotEqualTo(encrypted2);

        assertThat(AESCryptoUtils.decrypt(encrypted1, secretKey)).isEqualTo(plainText);
        assertThat(AESCryptoUtils.decrypt(encrypted2, secretKey)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should throw CryptoException when decrypting with wrong key")
    void shouldFailWithWrongKey() {
        String encrypted = AESCryptoUtils.encrypt(plainText, secretKey);
        String wrongKey = "99999567890123456789012345678901";

        assertThatThrownBy(() -> AESCryptoUtils.decrypt(encrypted, wrongKey))
                .isInstanceOf(CryptoException.class);
    }
}
