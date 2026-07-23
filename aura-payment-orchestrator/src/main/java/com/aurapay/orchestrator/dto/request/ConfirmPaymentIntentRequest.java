package com.aurapay.orchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentIntentRequest(
        @NotBlank(message = "Payment method token is required")
        String paymentMethodToken
) {}
