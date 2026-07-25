package com.aurapay.orchestrator.client;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.orchestrator.client.dto.VaultCardDetailsResponse;
import com.aurapay.orchestrator.client.dto.VaultRetrieveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("orchestratorVaultServiceClient")
public class VaultServiceClient {

    private final RestClient restClient;

    public VaultServiceClient(@Value("${aurapay.services.vault-url:http://localhost:8084}") String vaultBaseUrl,
                               RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(vaultBaseUrl)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();
    }

    public VaultCardDetailsResponse retrieveCardDetails(String cardToken) {
        log.info("Calling Vault Service to retrieve card details for token: {}", cardToken);
        try {
            return restClient.post()
                    .uri("/v1/tokens/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VaultRetrieveRequest(cardToken))
                    .retrieve()
                    .body(VaultCardDetailsResponse.class);
        } catch (Exception e) {
            log.error("Failed to retrieve card details from Vault Service: {}", e.getMessage(), e);
            throw new BusinessException(AuraErrorCode.VAULT_SERVICE_UNAVAILABLE, "Vault Service request failed: " + e.getMessage());
        }
    }
}
