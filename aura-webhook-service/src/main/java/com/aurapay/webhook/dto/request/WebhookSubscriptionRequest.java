package com.aurapay.webhook.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record WebhookSubscriptionRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotBlank(message = "Target URL is required")
        @Pattern(regexp = "^https?://.*", message = "Target URL must start with http:// or https://")
        String targetUrl,

        String secretKey,

        Boolean enabled,

        @JsonAlias("events")
        JsonNode subscribedEvents
) {
    public String getSubscribedEventsString() {
        if (subscribedEvents == null || subscribedEvents.isNull()) {
            return "*";
        }
        if (subscribedEvents.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : subscribedEvents) {
                if (!sb.isEmpty()) sb.append(",");
                sb.append(item.asText());
            }
            return sb.toString();
        }
        return subscribedEvents.asText();
    }
}
