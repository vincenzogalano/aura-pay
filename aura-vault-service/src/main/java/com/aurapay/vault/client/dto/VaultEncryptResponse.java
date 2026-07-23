package com.aurapay.vault.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultEncryptResponse(
        VaultEncryptData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VaultEncryptData(
            String ciphertext
    ) {}
}
