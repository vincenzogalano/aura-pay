package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.MerchantWebhookConfig;

import java.time.Instant;
import java.util.UUID;

public record WebhookConfigResponse(
        UUID id,
        UUID merchantId,
        String targetUrl,
        String secretKey,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookConfigResponse fromEntity(MerchantWebhookConfig config) {
        if (config == null) return null;
        return new WebhookConfigResponse(
                config.getId(),
                config.getMerchantId(),
                config.getTargetUrl(),
                config.getSecretKey(),
                config.isEnabled(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
