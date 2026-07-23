package com.aurapay.orchestrator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentIntentRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotNull(message = "Amount in cents is required")
        @Min(value = 1, message = "Amount in cents must be at least 1")
        Long amountCents,

        String currency,
        String description,
        Boolean isTest
) {}
