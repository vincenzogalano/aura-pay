package com.aurapay.vault.dto.response;

import com.aurapay.vault.domain.enums.CardBrand;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CardDetailsResponse(
        String cardNumber,
        String cardholderName,
        Integer expirationMonth,
        Integer expirationYear,
        String cvv,
        String maskedPan,
        CardBrand cardBrand,
        boolean livemode
) {}
