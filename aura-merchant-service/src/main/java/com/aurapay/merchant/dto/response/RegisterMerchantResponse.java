package com.aurapay.merchant.dto.response;

import java.util.List;

public record RegisterMerchantResponse(
        MerchantResponse merchant,
        List<RawApiKeyResponse> testApiKeys,
        String message
) {}
