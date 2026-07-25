package com.aurapay.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyGeneratorTest {

    @Test
    @DisplayName("Should generate API key with correct prefix")
    void shouldGenerateApiKeyWithPrefix() {
        String testKey = ApiKeyGenerator.generateApiKey(ApiKeyGenerator.SK_TEST_PREFIX);
        String liveKey = ApiKeyGenerator.generateApiKey(ApiKeyGenerator.PK_LIVE_PREFIX);

        assertThat(testKey).startsWith("sk_test_");
        assertThat(liveKey).startsWith("pk_live_");

        assertThat(ApiKeyGenerator.isTestKey(testKey)).isTrue();
        assertThat(ApiKeyGenerator.isTestKey(liveKey)).isFalse();
    }

    @Test
    @DisplayName("Should hash and verify API key with BCrypt")
    void shouldHashAndVerifyBcrypt() {
        String key = ApiKeyGenerator.generateApiKey(ApiKeyGenerator.SK_TEST_PREFIX);
        String hash = ApiKeyGenerator.hashWithBcrypt(key);

        assertThat(hash)
                .isNotNull()
                .startsWith("$2a$");

        assertThat(ApiKeyGenerator.verifyBcrypt(key, hash)).isTrue();
        assertThat(ApiKeyGenerator.verifyBcrypt(key + "_wrong", hash)).isFalse();
    }

    @Test
    @DisplayName("Should hash API key with SHA-256")
    void shouldHashSha256() {
        String key = ApiKeyGenerator.generateApiKey(ApiKeyGenerator.SK_TEST_PREFIX);
        String sha256 = ApiKeyGenerator.hashWithSha256(key);

        assertThat(sha256)
                .isNotNull()
                .hasSize(64);
    }
}
