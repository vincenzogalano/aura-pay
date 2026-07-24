package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationStatusResponse {

    private UUID merchantId;
    private MerchantStatus status;
    private String details;
    private List<RawApiKeyDto> liveApiKeys; // populated only when verification transition to VERIFIED happens
}
