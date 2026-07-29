package com.aurapay.orchestrator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundPaymentRequest(
        @NotNull(message = "Amount in cents is required")
        @Min(value = 1, message = "Amount in cents must be at least 1")
        Long amountCents,

        String reason
) {}
