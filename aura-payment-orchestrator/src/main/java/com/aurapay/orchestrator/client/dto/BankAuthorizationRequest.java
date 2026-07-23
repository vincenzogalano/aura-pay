package com.aurapay.orchestrator.client.dto;

import java.util.UUID;

public record BankAuthorizationRequest(
        UUID paymentIntentId,
        UUID merchantId,
        Long amountCents,
        String currency,
        String cardToken,
        boolean isTest
) {}
