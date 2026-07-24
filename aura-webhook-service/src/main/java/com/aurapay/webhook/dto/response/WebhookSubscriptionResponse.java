package com.aurapay.webhook.dto.response;

import com.aurapay.webhook.domain.WebhookSubscription;
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
public class WebhookSubscriptionResponse {

    private UUID id;
    private UUID merchantId;
    private String targetUrl;
    private String secretKey;
    private boolean enabled;
    private String subscribedEvents;
    private Instant createdAt;
    private Instant updatedAt;

    public static WebhookSubscriptionResponse fromEntity(WebhookSubscription sub) {
        if (sub == null) return null;
        return WebhookSubscriptionResponse.builder()
                .id(sub.getId())
                .merchantId(sub.getMerchantId())
                .targetUrl(sub.getTargetUrl())
                .secretKey(sub.getSecretKey())
                .enabled(sub.isEnabled())
                .subscribedEvents(sub.getSubscribedEvents())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
