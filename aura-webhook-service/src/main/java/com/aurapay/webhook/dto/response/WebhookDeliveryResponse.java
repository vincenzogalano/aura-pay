package com.aurapay.webhook.dto.response;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
        UUID id,
        String eventId,
        UUID merchantId,
        String eventType,
        String targetUrl,
        String payload,
        Integer httpStatus,
        String responseBody,
        int attemptCount,
        int maxAttempts,
        DeliveryStatus status,
        Instant nextRetryAt,
        Instant deliveredAt,
        Instant createdAt,
        boolean isTest
) {
    public static WebhookDeliveryResponse fromEntity(WebhookDelivery delivery) {
        if (delivery == null) return null;
        return new WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getEventId(),
                delivery.getMerchantId(),
                delivery.getEventType(),
                delivery.getTargetUrl(),
                delivery.getPayload(),
                delivery.getHttpStatus(),
                delivery.getResponseBody(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getStatus(),
                delivery.getNextRetryAt(),
                delivery.getDeliveredAt(),
                delivery.getCreatedAt(),
                delivery.isTest()
        );
    }
}
