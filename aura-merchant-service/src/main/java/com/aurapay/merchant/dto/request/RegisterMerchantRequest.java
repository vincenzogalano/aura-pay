package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterMerchantRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @NotBlank(message = "VAT number is required")
    @Size(min = 8, max = 20, message = "VAT number must be between 8 and 20 characters")
    private String vatNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
