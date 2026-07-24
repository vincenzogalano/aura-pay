package com.aurapay.webhook.dto.response;

import com.aurapay.webhook.domain.WebhookDelivery;
import com.aurapay.webhook.domain.enums.DeliveryStatus;
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
public class WebhookDeliveryResponse {

    private UUID id;
    private String eventId;
    private UUID merchantId;
    private String eventType;
    private String targetUrl;
    private String payload;
    private Integer httpStatus;
    private String responseBody;
    private int attemptCount;
    private int maxAttempts;
    private DeliveryStatus status;
    private Instant nextRetryAt;
    private Instant deliveredAt;
    private Instant createdAt;
    private boolean isTest;

    public static WebhookDeliveryResponse fromEntity(WebhookDelivery delivery) {
        if (delivery == null) return null;
        return WebhookDeliveryResponse.builder()
                .id(delivery.getId())
                .eventId(delivery.getEventId())
                .merchantId(delivery.getMerchantId())
                .eventType(delivery.getEventType())
                .targetUrl(delivery.getTargetUrl())
                .payload(delivery.getPayload())
                .httpStatus(delivery.getHttpStatus())
                .responseBody(delivery.getResponseBody())
                .attemptCount(delivery.getAttemptCount())
                .maxAttempts(delivery.getMaxAttempts())
                .status(delivery.getStatus())
                .nextRetryAt(delivery.getNextRetryAt())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .isTest(delivery.isTest())
                .build();
    }
}
