package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.ApiKey;
import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import com.aurapay.merchant.domain.enums.ApiKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private UUID id;
    private UUID merchantId;
    private String keyPrefix;
    private ApiKeyEnvironment environment;
    private ApiKeyType keyType;
    private Instant createdAt;
    private Instant revokedAt;
    private boolean active;

    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        if (apiKey == null) return null;
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .merchantId(apiKey.getMerchantId())
                .keyPrefix(apiKey.getKeyPrefix())
                .environment(apiKey.getEnvironment())
                .keyType(apiKey.getKeyType())
                .createdAt(apiKey.getCreatedAt())
                .revokedAt(apiKey.getRevokedAt())
                .active(!apiKey.isRevoked())
                .build();
    }
}
