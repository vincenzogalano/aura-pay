package com.aurapay.webhook.dto.response;

import com.aurapay.webhook.domain.WebhookSubscription;

import java.time.Instant;
import java.util.UUID;

public record WebhookSubscriptionResponse(
        UUID id,
        UUID merchantId,
        String targetUrl,
        String secretKey,
        boolean enabled,
        String subscribedEvents,
        Instant createdAt,
        Instant updatedAt
) {
    public static WebhookSubscriptionResponse fromEntity(WebhookSubscription sub) {
        if (sub == null) return null;
        return new WebhookSubscriptionResponse(
                sub.getId(),
                sub.getMerchantId(),
                sub.getTargetUrl(),
                sub.getSecretKey(),
                sub.isEnabled(),
                sub.getSubscribedEvents(),
                sub.getCreatedAt(),
                sub.getUpdatedAt()
        );
    }
}
