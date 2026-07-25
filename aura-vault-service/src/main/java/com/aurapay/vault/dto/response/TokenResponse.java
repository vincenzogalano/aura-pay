package com.aurapay.vault.dto.response;

import com.aurapay.vault.domain.enums.CardBrand;

import java.time.Instant;

public record TokenResponse(
        String token,
        String maskedPan,
        CardBrand cardBrand,
        String cardholderName,
        Integer expirationMonth,
        Integer expirationYear,
        Instant createdAt,
        Instant expiresAt,
        boolean livemode
) {}
