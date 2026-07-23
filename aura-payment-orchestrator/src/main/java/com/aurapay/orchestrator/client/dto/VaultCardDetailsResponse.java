package com.aurapay.orchestrator.client.dto;

public record VaultCardDetailsResponse(
        String cardNumber,
        String cardholderName,
        Integer expirationMonth,
        Integer expirationYear,
        String cvv,
        String maskedPan,
        String cardBrand,
        boolean livemode
) {}
