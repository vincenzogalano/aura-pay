package com.aurapay.ledger.dto.response;

import java.time.Instant;

public record MerchantBalanceResponse(
        String merchantId,
        long availableBalanceCents,
        String currency,
        boolean isTest,
        Instant asOf
) {}
