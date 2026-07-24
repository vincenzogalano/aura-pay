package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.MerchantWebhookConfig;
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
public class WebhookConfigResponse {

    private UUID id;
    private UUID merchantId;
    private String targetUrl;
    private String secretKey;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public static WebhookConfigResponse fromEntity(MerchantWebhookConfig config) {
        if (config == null) return null;
        return WebhookConfigResponse.builder()
                .id(config.getId())
                .merchantId(config.getMerchantId())
                .targetUrl(config.getTargetUrl())
                .secretKey(config.getSecretKey())
                .enabled(config.isEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
