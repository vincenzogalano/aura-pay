package com.aurapay.vault.client;

import com.aurapay.core.exception.CryptoException;
import com.aurapay.vault.client.dto.VaultDecryptRequest;
import com.aurapay.vault.client.dto.VaultDecryptResponse;
import com.aurapay.vault.client.dto.VaultEncryptRequest;
import com.aurapay.vault.client.dto.VaultEncryptResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component("hashiCorpVaultClient")
@Slf4j
public class HashiCorpVaultClient {

    private static final String KEY_NAME = "aura-pay-key";

    private final RestClient restClient;

    public HashiCorpVaultClient(
            @Value("${aurapay.vault.vault-url:http://localhost:8200}") String vaultUrl,
            @Value("${aurapay.vault.vault-token:root}") String vaultToken) {
        this.restClient = RestClient.builder()
                .baseUrl(vaultUrl)
                .defaultHeader("X-Vault-Token", vaultToken)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @PostConstruct
    public void initialize() {
        try {
            log.info("Checking HashiCorp Vault initialization...");

            // Step 1: Try to enable transit engine, Vault returns 400 if already enabled
            try {
                restClient.post()
                        .uri("/v1/sys/mounts/transit")
                        .body(Map.of("type", "transit"))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Transit Secrets Engine enabled in Vault");
            } catch (Exception e) {
                log.debug("Transit Secrets Engine might already be enabled: {}", e.getMessage());
            }

            // Step 2: Check if key exists, create if not
            boolean keyExists = false;
            try {
                restClient.get()
                        .uri("/v1/transit/keys/" + KEY_NAME)
                        .retrieve()
                        .toBodilessEntity();
                keyExists = true;
                log.info("Transit key '{}' already exists in Vault", KEY_NAME);
            } catch (Exception e) {
                log.debug("Key '{}' does not exist, will create it: {}", KEY_NAME, e.getMessage());
            }

            if (!keyExists) {
                restClient.post()
                        .uri("/v1/transit/keys/" + KEY_NAME)
                        .body(Map.<String, Object>of())
                        .retrieve()
                        .toBodilessEntity();
                log.info("Transit key '{}' created successfully in Vault", KEY_NAME);
            }

            log.info("HashiCorp Vault initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize HashiCorp Vault client: {}. Vault service might not be running.", e.getMessage());
        }
    }

    /**
     * Encrypts plaintext string using Vault Transit.
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            String base64Plain = Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
            VaultEncryptRequest requestBody = new VaultEncryptRequest(base64Plain);

            VaultEncryptResponse response = restClient.post()
                    .uri("/v1/transit/encrypt/" + KEY_NAME)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new CryptoException("Vault encryption failed: " + resp.getStatusCode());
                    })
                    .body(VaultEncryptResponse.class);

            if (response == null || response.data() == null) {
                throw new CryptoException("Invalid response from Vault");
            }

            return response.data().ciphertext();
        } catch (Exception e) {
            throw new CryptoException("Encryption failed via Vault", e);
        }
    }

    /**
     * Decrypts ciphertext string using Vault Transit.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            VaultDecryptRequest requestBody = new VaultDecryptRequest(cipherText);

            VaultDecryptResponse response = restClient.post()
                    .uri("/v1/transit/decrypt/" + KEY_NAME)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new CryptoException("Vault decryption failed: " + resp.getStatusCode());
                    })
                    .body(VaultDecryptResponse.class);

            if (response == null || response.data() == null) {
                throw new CryptoException("Invalid response from Vault");
            }

            String base64Plain = response.data().plaintext();
            byte[] decoded = Base64.getDecoder().decode(base64Plain);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed via Vault", e);
        }
    }
}
