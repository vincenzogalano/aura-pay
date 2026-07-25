package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.ApiKey;
import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        UUID merchantId,
        String keyPrefix,
        ApiKeyEnvironment environment,
        ApiKeyType keyType,
        Instant createdAt,
        Instant revokedAt,
        boolean active
) {
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        if (apiKey == null) return null;
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getMerchantId(),
                apiKey.getKeyPrefix(),
                apiKey.getEnvironment(),
                apiKey.getKeyType(),
                apiKey.getCreatedAt(),
                apiKey.getRevokedAt(),
                !apiKey.isRevoked()
        );
    }
}
