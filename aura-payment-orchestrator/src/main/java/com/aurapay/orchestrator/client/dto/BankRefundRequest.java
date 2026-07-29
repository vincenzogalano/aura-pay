package com.aurapay.orchestrator.client.dto;

import java.util.UUID;

public record BankRefundRequest(
        String originalTransactionId,
        UUID merchantId,
        Long amountCents,
        String reason
) {}
