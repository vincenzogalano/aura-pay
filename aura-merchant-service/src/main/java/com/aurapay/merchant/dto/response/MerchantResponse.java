package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.Merchant;
import com.aurapay.merchant.domain.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantResponse {

    private UUID id;
    private String businessName;
    private String vatNumber;
    private String email;
    private MerchantStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static MerchantResponse fromEntity(Merchant merchant) {
        if (merchant == null) return null;
        return MerchantResponse.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .vatNumber(merchant.getVatNumber())
                .email(merchant.getEmail())
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }
}
