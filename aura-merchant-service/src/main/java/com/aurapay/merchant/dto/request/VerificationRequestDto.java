package com.aurapay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequestDto {

    @NotBlank(message = "Company registration number or tax ID is required")
    private String registrationNumber;

    @NotBlank(message = "Address is required")
    private String businessAddress;

    @NotBlank(message = "Legal representative name is required")
    private String legalRepresentative;
}
