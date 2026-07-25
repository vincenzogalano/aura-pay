package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;

import java.util.UUID;

public record RawApiKeyResponse(
        UUID id,
        ApiKeyEnvironment environment,
        ApiKeyType keyType,
        String keyPrefix,
        String rawKey
) {}
