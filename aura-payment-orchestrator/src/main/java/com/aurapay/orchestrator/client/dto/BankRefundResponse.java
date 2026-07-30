package com.aurapay.orchestrator.client.dto;

import java.time.Instant;

public record BankRefundResponse(
        String refundTransactionId,
        boolean success,
        String responseCode,
        Instant timestamp
) {
    public static BankRefundResponse ok(String refundTransactionId) {
        return new BankRefundResponse(refundTransactionId, true, "100", Instant.now());
    }

    public static BankRefundResponse declined(String responseCode) {
        return new BankRefundResponse(null, false, responseCode != null ? responseCode : "51", Instant.now());
    }
}
