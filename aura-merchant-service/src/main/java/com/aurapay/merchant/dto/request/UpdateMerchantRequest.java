package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateMerchantRequest(
        @NotBlank(message = "Business name is required")
        String businessName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {}
