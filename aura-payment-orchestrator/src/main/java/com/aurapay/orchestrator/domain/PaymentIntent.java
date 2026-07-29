package com.aurapay.orchestrator.domain;

import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "client_secret", nullable = false, unique = true)
    private String clientSecret;

    @Column(name = "description")
    private String description;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "refunded_amount_cents", nullable = false)
    @Builder.Default
    private Long refundedAmountCents = 0L;

    @Column(name = "payment_method_token")
    private String paymentMethodToken;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "is_test", nullable = false)
    private boolean isTest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (clientSecret == null) {
            clientSecret = "pi_" + id.toString().replace("-", "") + "_secret_" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (currency == null) {
            currency = "EUR";
        }
        if (status == null) {
            status = PaymentStatus.CREATED;
        }
        if (refundedAmountCents == null) {
            refundedAmountCents = 0L;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
