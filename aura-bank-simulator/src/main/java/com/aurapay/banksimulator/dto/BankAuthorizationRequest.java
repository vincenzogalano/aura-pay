package com.aurapay.banksimulator.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BankAuthorizationRequest(
        @NotNull(message = "paymentIntentId is required")
        UUID paymentIntentId,

        @NotNull(message = "merchantId is required")
        UUID merchantId,

        @NotNull(message = "amountCents is required")
        @Min(value = 1, message = "amountCents must be at least 1")
        Long amountCents,

        @NotBlank(message = "currency is required")
        String currency,

        @NotBlank(message = "cardToken is required")
        String cardToken,

        boolean isTest
) {}
