package com.aurapay.vault.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultDecryptResponse(
        VaultDecryptData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VaultDecryptData(
            String plaintext
    ) {}
}
