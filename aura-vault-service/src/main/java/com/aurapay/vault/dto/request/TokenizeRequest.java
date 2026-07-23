package com.aurapay.vault.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TokenizeRequest(
        @NotBlank(message = "Card number must not be blank")
        @Pattern(regexp = "[\\d\\s-]{13,24}", message = "Card number format is invalid")
        String cardNumber,

        @NotBlank(message = "Cardholder name must not be blank")
        String cardholderName,

        @NotNull(message = "Expiration month is required")
        @Min(value = 1, message = "Expiration month must be between 1 and 12")
        @Max(value = 12, message = "Expiration month must be between 1 and 12")
        Integer expirationMonth,

        @NotNull(message = "Expiration year is required")
        @Min(value = 2024, message = "Expiration year must be current or in the future")
        Integer expirationYear,

        @NotBlank(message = "CVV must not be blank")
        @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3 or 4 digits")
        String cvv
) {}
