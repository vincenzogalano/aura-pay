package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
        @NotBlank(message = "Company registration number or tax ID is required")
        String registrationNumber,

        @NotBlank(message = "Address is required")
        String businessAddress,

        @NotBlank(message = "Legal representative name is required")
        String legalRepresentative
) {}
