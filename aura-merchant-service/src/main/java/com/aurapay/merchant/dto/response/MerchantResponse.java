package com.aurapay.merchant.dto.response;

import com.aurapay.merchant.domain.Merchant;
import com.aurapay.merchant.domain.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String businessName,
        String vatNumber,
        String email,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantResponse fromEntity(Merchant merchant) {
        if (merchant == null) return null;
        return new MerchantResponse(
                merchant.getId(),
                merchant.getBusinessName(),
                merchant.getVatNumber(),
                merchant.getEmail(),
                merchant.getStatus(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
        );
    }
}
