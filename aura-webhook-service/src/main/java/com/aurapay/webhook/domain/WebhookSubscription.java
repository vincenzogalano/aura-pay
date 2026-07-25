package com.aurapay.webhook.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID merchantId;

    @Column(name = "target_url", nullable = false, length = 512)
    private String targetUrl;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "subscribed_events", length = 1024)
    private String subscribedEvents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
