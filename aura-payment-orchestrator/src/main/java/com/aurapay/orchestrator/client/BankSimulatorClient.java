package com.aurapay.orchestrator.client;

import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import com.aurapay.orchestrator.client.dto.BankAuthorizationRequest;
import com.aurapay.orchestrator.client.dto.BankAuthorizationResponse;
import com.aurapay.orchestrator.client.dto.BankRefundRequest;
import com.aurapay.orchestrator.client.dto.BankRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BankSimulatorClient {

    private final RestClient restClient;

    public BankSimulatorClient(@Value("${aurapay.services.bank-simulator-url:http://localhost:8086}") String bankBaseUrl,
                               RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(bankBaseUrl)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();
    }

    public BankAuthorizationResponse authorizePayment(BankAuthorizationRequest request) {
        log.info("Calling Bank Simulator for paymentIntentId: {}", request.paymentIntentId());
        try {
            return restClient.post()
                    .uri("/v1/bank/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BankAuthorizationResponse.class);
        } catch (Exception e) {
            log.error("Failed to authorize payment via Bank Simulator: {}", e.getMessage(), e);
            throw new BusinessException(AuraErrorCode.BANK_UNAVAILABLE, "Bank Simulator request failed: " + e.getMessage());
        }
    }

    public BankRefundResponse refundPayment(BankRefundRequest request) {
        log.info("Calling Bank Simulator for refund: transactionId={}", request.originalTransactionId());
        try {
            return restClient.post()
                    .uri("/v1/bank/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BankRefundResponse.class);
        } catch (Exception e) {
            log.error("Failed to process refund via Bank Simulator: {}", e.getMessage(), e);
            throw new BusinessException(AuraErrorCode.BANK_UNAVAILABLE, "Bank Simulator refund request failed: " + e.getMessage());
        }
    }
}
