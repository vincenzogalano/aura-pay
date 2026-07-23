package com.aurapay.vault.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RetrieveRequest(
        @NotBlank(message = "Token is required")
        String token
) {}
