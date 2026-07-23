package com.aurapay.orchestrator.client.dto;

import java.time.Instant;

public record BankAuthorizationResponse(
        String transactionId,
        boolean authorized,
        String responseCode,
        String authorizationCode,
        String declineReason,
        Instant timestamp
) {
    public static BankAuthorizationResponse approved(String transactionId, String authorizationCode) {
        return new BankAuthorizationResponse(transactionId, true, "00", authorizationCode, null, Instant.now());
    }

    public static BankAuthorizationResponse declined(String responseCode, String declineReason) {
        return new BankAuthorizationResponse(null, false, responseCode, null, declineReason, Instant.now());
    }
}
