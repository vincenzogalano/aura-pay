package com.aurapay.merchant.dto.request;

import com.aurapay.merchant.domain.enums.ApiKeyEnvironment;
import jakarta.validation.constraints.NotNull;

public record CreateApiKeyRequest(
        @NotNull(message = "Environment (TEST or LIVE) is required")
        ApiKeyEnvironment environment
) {}
