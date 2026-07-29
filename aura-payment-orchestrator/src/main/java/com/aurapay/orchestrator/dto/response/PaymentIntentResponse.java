package com.aurapay.orchestrator.dto.response;

import com.aurapay.orchestrator.domain.PaymentIntent;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID id,
        UUID merchantId,
        Long amountCents,
        String currency,
        PaymentStatus status,
        String clientSecret,
        String description,
        String customerEmail,
        Long refundedAmountCents,
        String paymentMethodToken,
        String authorizationCode,
        String transactionId,
        String failureReason,
        boolean isTest,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentIntentResponse fromEntity(PaymentIntent intent) {
        return new PaymentIntentResponse(
                intent.getId(),
                intent.getMerchantId(),
                intent.getAmountCents(),
                intent.getCurrency(),
                intent.getStatus(),
                intent.getClientSecret(),
                intent.getDescription(),
                intent.getCustomerEmail(),
                intent.getRefundedAmountCents() != null ? intent.getRefundedAmountCents() : 0L,
                intent.getPaymentMethodToken(),
                intent.getAuthorizationCode(),
                intent.getTransactionId(),
                intent.getFailureReason(),
                intent.isTest(),
                intent.getCreatedAt(),
                intent.getUpdatedAt()
        );
    }
}
