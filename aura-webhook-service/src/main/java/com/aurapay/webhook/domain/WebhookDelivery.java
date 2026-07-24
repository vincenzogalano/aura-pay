package com.aurapay.webhook.domain;

import com.aurapay.webhook.domain.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "target_url", nullable = false, length = 512)
    private String targetUrl;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_test", nullable = false)
    private boolean isTest;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
    }
}
