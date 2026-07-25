package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WebhookConfigRequest(
        @NotBlank(message = "Target URL is required")
        @Pattern(regexp = "^https?://.*", message = "Target URL must start with http:// or https://")
        String targetUrl,

        Boolean enabled
) {}
