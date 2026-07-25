package com.aurapay.webhook.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ReplayRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        Instant endTime
) {}
