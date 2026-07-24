package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMerchantRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
