package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.enums.MerchantStatus;

import java.util.List;
import java.util.UUID;

public record VerificationStatusResponse(
        UUID merchantId,
        MerchantStatus status,
        String details,
        List<RawApiKeyResponse> liveApiKeys
) {}
