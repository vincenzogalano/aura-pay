package com.aurapay.banksimulator.dto;

import com.aurapay.core.events.BankResponseCode;

import java.time.Instant;

public record BankRefundResponse(
        String refundTransactionId,
        boolean success,
        String responseCode,
        Instant timestamp
) {
    public static BankRefundResponse ok(String refundTransactionId) {
        return new BankRefundResponse(refundTransactionId, true, BankResponseCode.APPROVED.getCode(), Instant.now());
    }

    public static BankRefundResponse failed(String responseCode) {
        return new BankRefundResponse(null, false, responseCode, Instant.now());
    }
}
