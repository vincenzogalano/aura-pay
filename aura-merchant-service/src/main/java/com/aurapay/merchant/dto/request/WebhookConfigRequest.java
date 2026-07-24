package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfigRequest {

    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*", message = "Target URL must start with http:// or https://")
    private String targetUrl;

    private Boolean enabled;
}
